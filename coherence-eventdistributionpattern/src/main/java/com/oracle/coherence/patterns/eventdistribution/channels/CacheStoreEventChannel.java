/*
 * File: CacheStoreEventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.EntryUpdatedEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.CacheStore;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * A {@link CacheStoreEventChannel} is an {@link EventChannel} implementation that allows
 * regular Coherence {@link CacheStore}s to be used as {@link EventChannel}s.
 * <p>
 * This allows pre-existing {@link CacheStore}s to be configured and used with the
 * Event Distribution Pattern.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 */
public class CacheStoreEventChannel implements EventChannel
{
    /**
     * The name of the {@link NamedCache} that can be used to provide a {@link BackingMapManagerContext}
     * for deserializing {@link EntryEvent}s.
     */
    private String contextCacheName;

    /**
     * The {@link CacheStore} to adapt.
     */
    private CacheStore cacheStore;


    /**
     * Standard Constructor.
     *
     * @param contextCacheName The name of the {@link NamedCache} that can be used to provide
     *                         a {@link BackingMapManagerContext}.
     * @param cacheStore       The {@link CacheStore} to adapt.
     */
    public CacheStoreEventChannel(String     contextCacheName,
                                  CacheStore cacheStore)
    {
        this.contextCacheName = contextCacheName;
        this.cacheStore       = cacheStore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(EventDistributor.Identifier       eventDistributorIdentifer,
                        EventChannelController.Identifier eventChannelControllerIdentifier)
                            throws EventChannelNotReadyException
    {
        // deliberately empty - no connection setup is required.
        // we assume the cache store will do this.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect()
    {
        // deliberately empty - no  disconnection is required.
        // we assume the cache store will do this.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int send(Iterator<Event> events)
    {
        int                           distributionCount = 0;

        LinkedHashMap<Object, Object> modifiedEntries   = new LinkedHashMap<Object, Object>();
        LinkedHashSet<Object>         removedKeys       = new LinkedHashSet<Object>();

        // determine a BackingMapManagerContext so we can deserialize EventEntries if required
        NamedCache               contextNamedCache   = CacheFactory.getCache(contextCacheName);
        CacheService             contextCacheService = (CacheService) contextNamedCache.getCacheService();
        BackingMapManagerContext context             = contextCacheService.getBackingMapManager().getContext();

        while (events.hasNext())
        {
            Event event = events.next();

            if (event instanceof EntryEvent)
            {
                EntryEvent<?> entryEvent = (EntryEvent<?>) event;

                // set the context to permit (de)serialization when required for distributable events
                if (entryEvent instanceof DistributableEntryEvent)
                {
                    ((DistributableEntryEvent) entryEvent).getEntry().setContext(context);
                }

                Object oKey = entryEvent.getEntry().getKey();

                if (entryEvent instanceof EntryRemovedEvent)
                {
                    removedKeys.add(oKey);

                    // as we've removed the entry, we must ensure that the entry is not updated/inserted as well!
                    modifiedEntries.remove(oKey);
                }
                else if (entryEvent instanceof EntryInsertedEvent || entryEvent instanceof EntryUpdatedEvent)
                {
                    Object oValue = entryEvent.getEntry().getValue();

                    modifiedEntries.put(oKey, oValue);

                    // as we've modified the entry, we must ensure that the entry is not be removed as well!
                    removedKeys.remove(oKey);
                }

                distributionCount++;
            }
            else
            {
                // SKIP: the event was not for an entry, so we ignore it.
            }
        }

        // we perform the removes first as this matches the semantics of Coherence
        if (removedKeys.size() > 0)
        {
            cacheStore.eraseAll(removedKeys);
        }

        // we then perform the updates
        if (modifiedEntries.size() > 0)
        {
            cacheStore.storeAll(modifiedEntries);
        }

        return distributionCount;
    }
}
