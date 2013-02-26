/*
 * File: ParallelLocalCacheEventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.resourcing.InvocationServiceSupervisedResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceProviderManager;
import com.oracle.coherence.common.resourcing.SupervisedResourceProvider;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.NullImplementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannel} is
 * a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.CacheEventChannel} implementation that will
 * distribute and apply {@link com.oracle.coherence.common.events.EntryEvent}s to a <strong>local</strong>
 * {@link com.tangosol.net.NamedCache} (ie: contained in the Coherence Cluster in which the
 * {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.CacheEventChannel} is executing).
 * <p>
 * ie: This {@link com.oracle.coherence.patterns.eventdistribution.EventChannel} is very useful for distributing
 * {@link com.oracle.coherence.common.events.Event}s to a cache in the local cluster.
 * <p>
 * To distribute {@link com.oracle.coherence.common.events.EntryEvent}s from to another cluster, you should
 * use a {@link com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel} with
 * a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannel} or
 * potentially a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.RemoteCacheEventChannel}
 * (though these have significantly lower performance and throughput than of
 * {@link com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel}s)
 * <p>
 * Note that this channel requires the use of an additional {@link com.tangosol.net.InvocationService} to publish
 * name PublishingInvocationService. See the cache configuration file provided by the Push Replication pattern.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see com.oracle.coherence.patterns.eventdistribution.channels.cache.CacheEventChannel
 * @see com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel
 * @see com.oracle.coherence.patterns.eventdistribution.channels.cache.RemoteCacheEventChannel
 *
 * @author Noah Arliss, Patrick Peralta
 */
public class ParallelLocalCacheEventChannel implements CacheEventChannel
{
    /**
     * The {@link java.util.logging.Logger} to use.
     */
    private static Logger logger = Logger.getLogger(ParallelLocalCacheEventChannel.class.getName());

    /**
     * The total amount of time to wait in millis for replication to complete before throwing an exception.
     */
    public static final long TOTAL_WAIT_MILLIS = 60000;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} in which the
     * {@link com.oracle.coherence.patterns.eventdistribution.EventChannel} operates.
     */
    private EventDistributor.Identifier distributorIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} of the
     * {@link EventChannelController} that owns the {@link com.oracle.coherence.patterns.eventdistribution.EventChannel}.
     */
    private EventChannelController.Identifier controllerIdentifier;

    /**
     * The name of the local cache on which we'll apply the {@link com.oracle.coherence.common.events.EntryEvent}s.
     */
    private String targetCacheName;

    /**
     * The {@link ConflictResolver} that will be used to resolve any conflicts the application of
     * {@link com.oracle.coherence.common.events.EntryEvent}s may cause.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder;

    /**
     * The {@link EventChannelBuilder} that will be used to realize the remote channel at the destination member
     */
    private EventChannelBuilder remoteChannelBuilder;

    /**
     * The {@link ParameterProvider} that must be used to construct remote
     * {@link com.oracle.coherence.patterns.eventdistribution.EventChannel}s.
     */
    private ParameterProvider parameterProvider;

    /**
     * The total number of entries that have been inserted for this batch
     */
    private final AtomicInteger completedCount = new AtomicInteger();

    /**
     * The cache service used by the entries being replicated by this channel.
     */
    private DistributedCacheService service;

    /**
     * Object used to raise {@link Object#notifyAll()} upon completion of
     * a replication invocation request.
     */
    private final Object NOTIFIER = new Object();

    /**
     * The name of the {@link com.tangosol.net.InvocationService} to use.
     */
    private String invocationServiceName;


    /**
     * Standard Constructor.
     */
    public ParallelLocalCacheEventChannel(String              targetCacheName,
                                          String              invocationServiceName,
                                          EventChannelBuilder remoteChannelBuilder,
                                          ParameterProvider   parameterProvider)
    {
        super();

        this.targetCacheName       = targetCacheName;
        this.remoteChannelBuilder  = remoteChannelBuilder;
        this.parameterProvider     = parameterProvider;
        this.invocationServiceName = invocationServiceName;
    }


    /**
     * Returns the {@link SupervisedResourceProvider} for the remote {@link InvocationService}.
     *
     * @return The {@link SupervisedResourceProvider} for the remote {@link InvocationService}
     */
    private SupervisedResourceProvider<InvocationService> getSupervisedResourceProvider()
    {
        return (SupervisedResourceProvider<InvocationService>) ((Environment) CacheFactory
            .getConfigurableCacheFactory()).getResource(ResourceProviderManager.class)
                .getResourceProvider(InvocationService.class,
                                     invocationServiceName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getTargetCacheName()
    {
        return targetCacheName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<ConflictResolver> getConflictResolverBuilder()
    {
        return conflictResolverBuilder;
    }


    /**
     * {@inheritDoc}
     */
    public void connect(EventDistributor.Identifier       distributorIdentifier,
                        EventChannelController.Identifier controllerIdentifier) throws EventChannelNotReadyException
    {
        this.distributorIdentifier = distributorIdentifier;
        this.controllerIdentifier  = controllerIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public void disconnect()
    {
        // we don't need to do anything when stopping
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NamedCache getTargetNamedCache()
    {
        if (getTargetCacheName() == null || getTargetCacheName().isEmpty())
        {
            return CacheFactory.getCache(distributorIdentifier.getSymbolicName(), NullImplementation.getClassLoader());
        }
        else
        {
            return CacheFactory.getCache(getTargetCacheName(), NullImplementation.getClassLoader());
        }
    }


    /**
     * Return the cache service used by the cache entries being replicated.
     *
     * @return cache service
     */
    protected DistributedCacheService getCacheService()
    {
        // not bothering with a DCL here because we are not initializing
        // the service; we are simply creating a reference to it; thus
        // there is no harm if we do this more than once
        if (service == null)
        {
            service = (DistributedCacheService) getTargetNamedCache().getCacheService();
        }

        return service;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int send(Iterator<Event> events)
    {
        // First we need to split the events according to the member who should process them
        Map<Member, List<Event>>        memberEventMap = mapEventsToMember(events);

        Set<RemoteChannelAgentObserver> observerSet    = new HashSet<RemoteChannelAgentObserver>();

        // Now that we've split up the batch, lets actually set ourselves up to send it
        for (Map.Entry<Member, List<Event>> event : memberEventMap.entrySet())
        {
            observerSet.add(publishList(event.getKey(), event.getValue()));
        }

        long ldtStart = System.currentTimeMillis();

        while (!observerSet.isEmpty() && (System.currentTimeMillis() - ldtStart < TOTAL_WAIT_MILLIS))
        {
            synchronized (NOTIFIER)
            {
                try
                {
                    NOTIFIER.wait(10);
                }
                catch (InterruptedException e)
                {
                    Thread.interrupted();

                    throw Base.ensureRuntimeException(e);
                }
            }

            // iterate over a copy of the observer set because we may add more entries
            // to the set if a member failed; this will protect us from a CME since
            // some iterator implementations can't tolerate modification of the
            // backing collection
            for (RemoteChannelAgentObserver observer : new HashSet<RemoteChannelAgentObserver>(observerSet))
            {
                if (observer.isCompleted())
                {
                    observerSet.remove(observer);
                }
                else if (observer.isMemberLeft())
                {
                    // the targeted member left the cluster; determine which members need to be
                    // targeted for the entries the departed member previously owned
                    for (Map.Entry<Member, List<Event>> event : mapEventsToMember(observer.getEvents()).entrySet())
                    {
                        observerSet.add(publishList(event.getKey(), event.getValue()));
                    }

                    // this observer is for a member that has left the cluster, remove
                    // it from the set of observers we are expecting results for
                    observerSet.remove(observer);
                }
                else if (observer.getException() != null)
                {
                    throw Base.ensureRuntimeException(observer.getException());
                }
            }
        }

        if (!observerSet.isEmpty())
        {
            throw new RequestTimeoutException(String
                .format("InvocationService did not receive a response from members %s after %s ms", observerSet,
                        System.currentTimeMillis() - ldtStart));
        }

        return getCompletedCount();
    }


    /**
     * Split the events provided by the iterator by member.
     *
     * @param events list of events
     *
     * @return map of events, keyed by the member that owns the partition
     *         for its list of events
     */
    protected Map<Member, List<Event>> mapEventsToMember(Iterator<Event> events)
    {
        Map<Member, List<Event>> map = new HashMap<Member, List<Event>>();

        while (events.hasNext())
        {
            Event  e = events.next();
            Member member;

            if (e instanceof DistributableEntryEvent)
            {
                DistributableEntry entry  = ((DistributableEntryEvent) e).getEntry();
                Binary             binKey = entry.getBinaryKey();
                int iPartition = getCacheService().getBackingMapManager().getContext().getKeyPartition(binKey);

                member = getCacheService().getPartitionOwner(iPartition);
            }
            else
            {
                // target local events to "this" member
                member = getCacheService().getCluster().getLocalMember();
            }

            List<Event> eventList = map.get(member);

            if (eventList == null)
            {
                eventList = new ArrayList<Event>();
            }

            eventList.add(e);
            map.put(member, eventList);
        }

        return map;
    }


    /**
     * Invoke an asynchronous request to a member to replicate a list of events.
     *
     * @param member     the target member for the request
     * @param eventList  the list of events to replicate
     *
     * @return an observer that will contain the results of the replication request
     */
    protected RemoteChannelAgentObserver publishList(Member      member,
                                                     List<Event> eventList)
    {
        // get the SupervisedResourceProvider that controls access to the underlying remote Invocation Service
        SupervisedResourceProvider<InvocationService> resourceProvider = getSupervisedResourceProvider();

        if (resourceProvider == null)
        {
            // register a SupervisedResourceProvider for the remote invocation scheme
            ((Environment) CacheFactory.getConfigurableCacheFactory()).getResource(ResourceProviderManager.class)
                .registerResourceProvider(InvocationService.class,
                                          invocationServiceName,
                                          new InvocationServiceSupervisedResourceProvider(invocationServiceName));

            // now ask for it again (just in case it was replaced by the supervisor)
            resourceProvider = getSupervisedResourceProvider();
        }

        if (resourceProvider.isResourceAccessible())
        {
            InvocationService          invocationService = resourceProvider.getResource();
            RemoteChannelAgentObserver observer          = new RemoteChannelAgentObserver(eventList);

            invocationService.execute(new RemoteClusterEventChannel.RemoteDistributionAgent(distributorIdentifier,
                                                                                            controllerIdentifier,
                                                                                            eventList,
                                                                                            remoteChannelBuilder,
                                                                                            parameterProvider),
                                      Collections.singleton(member),
                                      observer);

            return observer;
        }
        else
        {
            throw new MissingResourceException("Unable to resolve the following invocation service",
                                               InvocationService.class.toString(),
                                               invocationServiceName);
        }
    }


    /**
     * Increment the completed count by the amount specified.
     *
     * @param count - The amount to increment the completed count by
     */
    public void incrementCompleteCount(int count)
    {
        completedCount.addAndGet(count);
    }


    /**
     *  Get the completed count
     *
     * @return the completed count
     */
    public int getCompletedCount()
    {
        return completedCount.get();
    }


    /**
     * This Observer will keep track or the requests we sent
     */
    public class RemoteChannelAgentObserver implements InvocationObserver
    {
        /**
         * The list of events this invocation is attempting to replicate.
         */
        private final List<Event> eventList;

        /**
         * Flag that indicates whether the invocation successfully completed.
         */
        private volatile boolean completed;

        /**
         * Flag that indicates whether the target member for this invocation
         * left the cluster before the invocation was completed.
         */
        private volatile boolean memberLeft;

        /**
         * Exception thrown by the target member when processing the
         * invocation request.
         */
        private volatile Throwable exception;

        /**
         * The start of the operation
         */
        private volatile long ldtStart;


        /**
         * Constructor
         *
         * @param events  the list of events that we want to insert
         */
        public RemoteChannelAgentObserver(List<Event> events)
        {
            ldtStart  = Base.getSafeTimeMillis();
            eventList = events;
        }


        /**
         * Return an iterator over the events this request will attempt to replicate.
         *
         * @return iterator for events
         */
        public Iterator<Event> getEvents()
        {
            return eventList.iterator();
        }


        /**
         * Return true if this invocation successfully completed.
         *
         * @return true if this invocation successfully completed
         */
        public boolean isCompleted()
        {
            return completed;
        }


        /**
         * Return true if the target member for this invocation
         * left the cluster before the invocation was completed.
         *
         * @return true if the target member left the cluster
         */
        public boolean isMemberLeft()
        {
            return memberLeft;
        }


        /**
         * Exception thrown by the target member when processing the
         * invocation request.
         *
         * @return the exception thrown by the target member; null
         *         if no exception was thrown
         */
        public Throwable getException()
        {
            return exception;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void memberCompleted(Member member,
                                    Object o)
        {
            ParallelLocalCacheEventChannel.this.incrementCompleteCount((Integer) o);
            completed = true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void memberFailed(Member    member,
                                 Throwable throwable)
        {
            exception = throwable;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void memberLeft(Member member)
        {
            memberLeft = true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void invocationCompleted()
        {
            // This is always an invocation against a single member and we handle completion in the MemberComplete event
            synchronized (NOTIFIER)
            {
                NOTIFIER.notifyAll();
            }
        }
    }
}
