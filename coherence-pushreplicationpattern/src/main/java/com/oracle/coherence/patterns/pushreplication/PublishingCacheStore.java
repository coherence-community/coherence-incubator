/*
 * File: PublishingCacheStore.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.patterns.pushreplication;

import com.oracle.coherence.common.builders.SerializableNamedCacheSerializerBuilder;

import com.oracle.coherence.common.cluster.ClusterMetaInfo;
import com.oracle.coherence.common.cluster.LocalClusterMetaInfo;

import com.oracle.coherence.common.events.CacheEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.Event;

import com.oracle.coherence.common.expression.SerializableParameter;
import com.oracle.coherence.common.expression.SerializableResolvableParameterList;
import com.oracle.coherence.common.expression.SerializableScopedParameterResolver;

import com.oracle.coherence.common.resourcing.AbstractDeferredSingletonResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceProvider;

import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.configuration.EventDistributorTemplate;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryUpdatedEvent;

import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.GuardSupport;

import com.tangosol.net.cache.BackingMapBinaryEntry;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheStore;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ResourceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PublishingCacheStore} will distribute all {@link CacheEvent}s occurring on a
 * {@link BinaryEntryStore} to an {@link EventDistributor}
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see EventDistributor
 * @see com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder
 *
 * @author Brian Oliver
 */
public class PublishingCacheStore implements BinaryEntryStore
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(PublishingCacheStore.class.getName());

    /**
     * The {@link ClusterMetaInfo} of the {@link Cluster} in which the {@link PublishingCacheStore} is operating.
     */
    private final ClusterMetaInfo localClusterMetaInfo;

    /**
     * The name of the cache for which we'll be distributing cache {@link CacheEvent}s.
     */
    private final String cacheName;

    /**
     * The {@link EventDistributor}s to which this {@link CacheStore} will send {@link Event}s.
     */
    private ResourceProvider<ArrayList<EventDistributor>> eventDistributors;


    /**
     * Standard Constructor
     *
     * @param cacheName The name of the cache from which {@link CacheEvent}s will be distributed.
     */
    public PublishingCacheStore(final String cacheName)
    {
        this.cacheName = cacheName;

        // determine the ClusterMetaInfo in which we are operating
        this.localClusterMetaInfo = LocalClusterMetaInfo.getInstance();

        // all of the event distributors should be initialized only when needed (lazily)
        this.eventDistributors =
            new AbstractDeferredSingletonResourceProvider<ArrayList<EventDistributor>>("Event Distributors")
        {
            protected ArrayList<EventDistributor> ensureResource()
            {
                if (logger.isLoggable(Level.INFO))
                {
                    logger.info(String.format("Establising Event Distributors for the Cache [%s].", cacheName));
                }

                if (!(CacheFactory.getConfigurableCacheFactory() instanceof ExtensibleConfigurableCacheFactory))
                {
                    throw new IllegalArgumentException("The ConfigurableCacheFactory must be an isntance of ExtensibleConfigurableCacheFactory");
                }

                ExtensibleConfigurableCacheFactory eccf =
                    (ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory();

                // grab the CacheMapping for this cache - it has enrichments for the dependency templates
                CacheMapping cacheMapping = eccf.getCacheConfig().getCacheMappingRegistry().findCacheMapping(cacheName);

                // NOTE: This temporary code until the core Coherence resolver/builder classes are serializable
                ResolvableParameterList resolvableParams =
                    (ResolvableParameterList) cacheMapping.getParameterResolver();
                SerializableResolvableParameterList serializableParams =
                    new SerializableResolvableParameterList(resolvableParams);

                // construct the new parameter provider for the cache
                final SerializableScopedParameterResolver resolver =
                    new SerializableScopedParameterResolver(serializableParams);

                // add the standard coherence parameters to the parameter provider
                resolver.add(new SerializableParameter("cache-name", cacheName));
                resolver.add(new SerializableParameter("site-name", localClusterMetaInfo.getSiteName()));
                resolver.add(new SerializableParameter("cluster-name", localClusterMetaInfo.getClusterName()));

                // create a serializer builder for the cache that we can use when distributing events
                final ParameterizedBuilder<Serializer> serializerBuilder =
                    new SerializableNamedCacheSerializerBuilder(cacheName);

                // create the EventDistributors for the Cache
                ArrayList<EventDistributor> distributors = new ArrayList<EventDistributor>();

                EventDistributorTemplate templateEvent =
                    cacheMapping.getResourceRegistry().getResource(EventDistributorTemplate.class);

                if (templateEvent == null)
                {
                    templateEvent = eccf.getResourceRegistry().getResource(EventDistributorTemplate.class);
                }

                if (templateEvent == null)
                {
                    throw new IllegalArgumentException("EventDistribution template cannot be found in the ResourceRegistry");
                }

                // set the serializer if one has not be specified
                if (templateEvent.getSerializerBuilder() == null)
                {
                    templateEvent.setSerializerBuilder(serializerBuilder);
                }

                distributors.add(templateEvent.realize(resolver, null, new SerializableResolvableParameterList()));

                return distributors;
            }
        };

    }


    /**
     * Requests an {@link Event} to be distributed via the established {@link EventDistributor}s.
     *
     * @param event The {@link Event} to distribute.
     */
    protected void distribute(Event event)
    {
        for (EventDistributor distributor : eventDistributors.getResource())
        {
            GuardSupport.heartbeat();
            distributor.distribute(event);
        }

        GuardSupport.heartbeat();
    }


    /**
     * Requests a collection of {@link Event}s to be distributed via the established {@link EventDistributor}s.
     *
     * @param events The {@link List} of {@link Event}s to distribute.
     */
    protected void distribute(List<Event> events)
    {
        for (EventDistributor distributor : eventDistributors.getResource())
        {
            GuardSupport.heartbeat();
            distributor.distribute(events);
        }

        GuardSupport.heartbeat();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes"})
    public void loadAll(Set setBinEntries)
    {
        // NOTE: override this method it you'd like to support "pull-replication"
        // or cache loading.
    }


    /**
     * Helper method to check to see if this binary value was marked for for erase
     *
     * @param context     Backing map manager context
     * @param binaryValue The binaryValue to check
     *
     * @return true if the entry was marked for erase, else false
     */
    @SuppressWarnings({"rawtypes"})
    private boolean isMarkedForErase(BackingMapManagerContext context,
                                     Binary                   binaryValue)
    {
        Map oldDecorations = (Map) context.getInternalValueDecoration(binaryValue,
                                                                      BackingMapManagerContext.DECO_CUSTOM);

        if (oldDecorations == null)
        {
            return false;
        }
        else
        {
            Boolean isMarked = (Boolean) oldDecorations.get(DistributableEntry.MARKED_FOR_ERASE_DECORATION_KEY);

            return isMarked == null ? false : isMarked;
        }
    }


    /**
     * A helper method to clear decorations from a Binary.
     *
     * NOTE: A distributed cache may decorate keys with partition and other information. This needs to be removed
     * before distribution.
     *
     * @param binary The {@link Binary} to undecorate.
     *
     * @return A decorated Binary value
     */
    private Binary undecorateBinary(Binary binary)
    {
        if (ExternalizableHelper.isIntDecorated(binary))
        {
            Binary cleanBinary = ExternalizableHelper.removeIntDecoration(binary);

            assert(!ExternalizableHelper.isIntDecorated(cleanBinary));

            return cleanBinary;
        }
        else
        {
            return binary;
        }
    }


    /**
     * A helper method to decorate a binary value with the local {@link ClusterMetaInfo}.
     *
     * @param context      The backing map manager context.
     * @param binaryValue  The binary value to decorate.
     * @param force        Forces {@link ClusterMetaInfo} decoration to be made independent of what is in the value.
     *
     * @return A decorated Binary value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Binary decorateBinary(BackingMapManagerContext context,
                                  Binary                   binaryValue,
                                  boolean                  force)
    {
        // get the current decorations
        Map currentDecorations = (Map) context.getInternalValueDecoration(binaryValue,
                                                                          BackingMapManagerContext.DECO_CUSTOM);

        // the new decorations are based on the current ones
        Map decorations = new HashMap();

        if (currentDecorations != null)
        {
            decorations.putAll(currentDecorations);
        }

        // determine the current source of the entry
        ClusterMetaInfo sourceClusterMetaInfo =
            (ClusterMetaInfo) decorations.get(DistributableEntry.CLUSTER_META_INFO_DECORATION_KEY);

        if (sourceClusterMetaInfo == null)
        {
            // when there is no source cluster, set this cluster as the source
            decorations.put(DistributableEntry.CLUSTER_META_INFO_DECORATION_KEY, localClusterMetaInfo);
        }
        else if (force)
        {
            // when we have to force, we set this cluster as the source
            decorations.put(DistributableEntry.CLUSTER_META_INFO_DECORATION_KEY, localClusterMetaInfo);
        }

        // add the new decorations back to the binary
        Binary decoratedValue = (Binary) context.addInternalValueDecoration(binaryValue,
                                                                            BackingMapManagerContext.DECO_CUSTOM,
                                                                            decorations);

        return decoratedValue;
    }


    /**
     * The store method requests than an entry be distributed using the configured
     * {@link com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder}. The method checks to see that the entry wasn't
     * marked for removal (removes are not distributed here, they're distributed on erase). It will then
     * decorates the BinaryEntry with the affilitate associated and requests that
     * the {@link com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder} distribute the event.
     *
     * @param entry The BinaryEntry that was stored
     */
    public synchronized void store(BinaryEntry entry)
    {
        ResourceRegistry registry = entry.getContext().getManager().getCacheFactory().getResourceRegistry();

        if (isMarkedForErase(entry.getContext(), entry.getBinaryValue()))
        {
            // SKIP
        }
        else
        {
            Binary decoratedBinaryValue = decorateBinary(entry.getContext(), entry.getBinaryValue(), false);
            Binary undecoratedBinaryKey = undecorateBinary(entry.getBinaryKey());
            Binary originalBinaryValue  = entry.getOriginalBinaryValue();

            if (originalBinaryValue == null)
            {
                distribute(new DistributableEntryInsertedEvent(cacheName,
                                                               new DistributableEntry(undecoratedBinaryKey,
                                                                                      decoratedBinaryValue,
                                                                                      null,
                                                                                      entry.getContext())));
            }
            else
            {
                distribute(new DistributableEntryUpdatedEvent(cacheName,
                                                              new DistributableEntry(undecoratedBinaryKey,
                                                                                     decoratedBinaryValue,
                                                                                     originalBinaryValue,
                                                                                     entry.getContext())));
            }
        }
    }


    /**
     * The erase method distributes an {@link EntryRemovedEvent} with the {@link EventDistributor}.
     *
     * @param entry The entry that was erased
     */
    public synchronized void erase(BinaryEntry entry)
    {
        Binary decoratedBinaryValue;

        // If the entry was marked for erase we are erasing the original value.
        if (entry.getOriginalBinaryValue() == null
            || isMarkedForErase(entry.getContext(), entry.getOriginalBinaryValue()))
        {
            decoratedBinaryValue = entry.getOriginalBinaryValue();
        }
        else
        {
            // If an entry is not marked for erase it is originating locally.
            // We decorate the entry with the local cluster global name regardless
            // of whether the entry was decorated earlier.  This allows
            // this cluster to take ownership of the entry and permits it to
            // propagate correctly.

            // Without forcing this decoration an erase operation will retain
            // an old cluster name and not get propagated to that cluster.
            decoratedBinaryValue = decorateBinary(entry.getContext(), entry.getOriginalBinaryValue(), true);
        }

        Binary undecoratedBinaryKey = undecorateBinary(entry.getBinaryKey());
        Binary originalBinaryValue  = entry.getOriginalBinaryValue();

        distribute(new DistributableEntryRemovedEvent(cacheName,
                                                      new DistributableEntry(undecoratedBinaryKey,
                                                                             decoratedBinaryValue,
                                                                             originalBinaryValue,
                                                                             entry.getContext())));
    }


    /**
     * The storeAll method distributes a batch of events with the {@link EventDistributor}.
     * The method ensures that each entry wasn't marked for delete (delete events are not distributed here,
     * they're distributed on erase). It will then decorate the BinaryEntry with the {@link ClusterMetaInfo} and
     * request that the {@link EventDistributor} distributed the operation.
     *
     * @param colEntries the collection of Entries that were stored
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void storeAll(Set colEntries)
    {
        ArrayList<Event> events = new ArrayList<Event>(colEntries.size());

        for (Iterator<BinaryEntry> iter = colEntries.iterator(); iter.hasNext(); )
        {
            BinaryEntry entry = (BinaryEntry) iter.next();

            Base.azzert((entry instanceof BackingMapBinaryEntry), "Expecting instanceof BackingMapBinaryEntry");

            if (!isMarkedForErase(entry.getContext(), entry.getBinaryValue()))
            {
                Binary decoratedBinaryValue = decorateBinary(entry.getContext(), entry.getBinaryValue(), false);
                Binary                undecoratedBinaryKey = undecorateBinary(entry.getBinaryKey());

                BackingMapBinaryEntry bmbe                 = (BackingMapBinaryEntry) entry;
                Binary                originalBinaryValue  = bmbe.getOriginalBinaryValue();

                if (originalBinaryValue == null)
                {
                    events.add(new DistributableEntryInsertedEvent(cacheName,
                                                                   new DistributableEntry(undecoratedBinaryKey,
                                                                                          decoratedBinaryValue,
                                                                                          new Binary(),
                                                                                          entry.getContext())));
                }
                else
                {
                    events.add(new DistributableEntryUpdatedEvent(cacheName,
                                                                  new DistributableEntry(undecoratedBinaryKey,
                                                                                         decoratedBinaryValue,
                                                                                         originalBinaryValue,
                                                                                         entry.getContext())));
                }
            }
        }

        distribute(events);
    }


    /**
     * The eraseAll method will distribute a collection of erased entries using the {@link EventDistributor}.
     *
     * @param setEntries The entries that were erased
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void eraseAll(Set setEntries)
    {
        ArrayList<Event> events = new ArrayList<Event>(setEntries.size());

        for (Iterator<BinaryEntry> iter = setEntries.iterator(); iter.hasNext(); )
        {
            BinaryEntry entry = (BinaryEntry) iter.next();
            Binary      decoratedBinaryValue;

            if (entry.getOriginalBinaryValue() == null
                || isMarkedForErase(entry.getContext(), entry.getOriginalBinaryValue()))
            {
                decoratedBinaryValue = entry.getOriginalBinaryValue();
            }
            else
            {
                decoratedBinaryValue = decorateBinary(entry.getContext(), entry.getOriginalBinaryValue(), true);
            }

            Binary                undecoratedBinaryKey = undecorateBinary(entry.getBinaryKey());
            BackingMapBinaryEntry bmbe                 = (BackingMapBinaryEntry) entry;
            Binary                originalBinaryValue  = bmbe.getOriginalBinaryValue();

            events.add(new DistributableEntryRemovedEvent(cacheName,
                                                          new DistributableEntry(undecoratedBinaryKey,
                                                                                 decoratedBinaryValue,
                                                                                 originalBinaryValue,
                                                                                 entry.getContext())));
        }

        distribute(events);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void load(BinaryEntry binaryEntry)
    {
        // NOTE: override this method it you'd like to support "pull-replication"
        // or cache loading.
    }
}
