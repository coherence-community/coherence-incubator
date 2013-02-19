/*
 * File: LocalCacheEventChannel.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannel} is a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.CacheEventChannel} implementation that will
 * distribute and apply {@link com.oracle.coherence.common.events.EntryEvent}s to a <strong>local</strong> {@link com.tangosol.net.NamedCache} (ie: contained
 * in the Coherence Cluster in which the {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.CacheEventChannel} is executing).
 * <p>
 * ie: This {@link com.oracle.coherence.patterns.eventdistribution.EventChannel} is very useful for distributing {@link com.oracle.coherence.common.events.Event}s to a cache in the local cluster.
 * <p>
 * To distribute {@link com.oracle.coherence.common.events.EntryEvent}s from to another cluster, you should use a {@link com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel} with
 * a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannel} or potentially a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.RemoteCacheEventChannel} (though these have significantly
 * lower performance and throughput than of {@link com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel}s)
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see com.oracle.coherence.patterns.eventdistribution.channels.cache.CacheEventChannel
 * @see com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel
 * @see com.oracle.coherence.patterns.eventdistribution.channels.cache.RemoteCacheEventChannel
 *
 * @author Noah Arliss
 */
public class ParallelLocalCacheEventChannel implements CacheEventChannel
{
    /**
     * The {@link java.util.logging.Logger} to use.
     */
    private static Logger logger = Logger.getLogger(ParallelLocalCacheEventChannel.class.getName());

    /**
     * The name of the invocation service to use
     */
    public static final String INVOCATION_SVC = "PublishingInvocationService";

    /**
     * The total amount of time to wait in millis before throwing an exception
     */
    public static final long   TOTAL_WAIT_MILLIS = 60000;

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
     * The {@link ConflictResolver} that will be used to resolve any conflicts the application of {@link com.oracle.coherence.common.events.EntryEvent}s
     * may cause.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder;

    /**
     * The {@link EventChannelBuilder} that will be used to realize the remote channel at the destination member
     */
    private EventChannelBuilder remoteChannelBuilder;

    /**
     * The {@link ParameterProvider} that must be used to construct remote {@link com.oracle.coherence.patterns.eventdistribution.EventChannel}s.
     */
    private ParameterProvider parameterProvider;

    /**
     * The total number of entries that have been inserted for this batch
     */
    private int completedCount = 0;

    /**
     *
     */
    private volatile AtomicInteger cCompleted;


    /**
     * Standard Constructor.
     */
    public ParallelLocalCacheEventChannel(String targetCacheName,
                                          EventChannelBuilder remoteChannelBuilder,
                                          ParameterProvider parameterProvider)
    {
        super();

        this.targetCacheName         = targetCacheName;
        this.remoteChannelBuilder    = remoteChannelBuilder;
        this.parameterProvider       = parameterProvider;
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
     * {@inheritDoc}
     */
    @Override
    public int send(Iterator<Event> events)
    {
        PartitionedService partSvc = (PartitionedService) getTargetNamedCache().getCacheService();

        // First we need to split the events according to the member who should process them
        HashMap<Member, List<Event>> memberEventMap = new HashMap<Member, List<Event>>();

        // Save off the non distributable events and just run them here...
        List<Event> nonCacheEvents = new ArrayList<Event>();

        int iCount = 0;
        while (events.hasNext())
            {
            Event e = events.next();

            if (e instanceof DistributableEntryEvent)
                {
                DistributableEntryEvent distributableEntryEvent = (DistributableEntryEvent) e;
                DistributableEntry entry = distributableEntryEvent.getEntry();
                Binary binKey = entry.getBinaryKey();

                int iPartition = binKey.calculateNaturalPartition(partSvc.getPartitionCount());

                Member member = partSvc.getPartitionOwner(iPartition);

                List<Event> eventList = memberEventMap.get(member);

                if (eventList == null)
                    {
                    eventList = new ArrayList<Event>();
                    }

                eventList.add(e);
                memberEventMap.put(member, eventList);
                }
            else
                {
                nonCacheEvents.add(e);
                }

            iCount++;
            }


        // Now that we've split up the batch, lets actually set ourselves up to send it
        for (Map.Entry<Member, List<Event>> e : memberEventMap.entrySet())
            {
            Member member = e.getKey();
            List<Event> eventList = e.getValue();

            publishList(member, eventList, partSvc);
            }

        // Now lets just process the remaining ones locally
        if (!nonCacheEvents.isEmpty())
            {
            CacheEventChannelHelper.apply(getTargetNamedCache(), nonCacheEvents.iterator(), getConflictResolverBuilder());
            incrementCompleteCount(nonCacheEvents.size());
            }

       x
        return getCompletedCount();
    }

    public void publishList(Member member, List<Event> eventList, PartitionedService partSvc)
        {
        InvocationService invocationService = (InvocationService) CacheFactory.getService(INVOCATION_SVC);
        HashSet<Member> set = new HashSet<Member>();
        set.add(member);

        invocationService.execute(new RemoteClusterEventChannel.RemoteDistributionAgent(
                distributorIdentifier, controllerIdentifier, eventList, remoteChannelBuilder, parameterProvider),
                set, new RemoteChannelAgentObserver(eventList, this, partSvc));
        }

    /**
     * Increment the completed count by the amount specified
     *
     * @param count - The amount to increment the completed count by
     */
    public synchronized void incrementCompleteCount(int count)
        {
        completedCount += count;
        notify();
        }


    /**
     *  Get the completed count
     *
     * @return the completed count
     */
    public synchronized int getCompletedCount()
        {
        return completedCount;
        }

    /**
     * This Observer will keep track or the requests we sent
     */
    public static class RemoteChannelAgentObserver implements InvocationObserver
        {

        /**
         * A reference back to the event channel that instantiated us
         */
        private ParallelLocalCacheEventChannel eventChannel;

        /**
         * The list of events that we tried to publish
         */
        private List<Event> eventList;

        /**
         * The PartitionedService used to send these events
         */
        private PartitionedService partSvc;


        /**
         * Constructor
         *
         * @param events   The list of events that we want to insert
         * @param channel  The channel that will be used to insert the events
         * @param service  The PartitionedService used to locally perform the insert
         */
        public RemoteChannelAgentObserver(List<Event> events, ParallelLocalCacheEventChannel channel,
                                          PartitionedService service)
            {
            eventChannel      = channel;
            eventList         = events;
            partSvc           = service;
            }


        @Override
        public void memberCompleted(Member member, Object o)
            {
                eventChannel.incrementCompleteCount((Integer) o);
            }

        @Override
        public void memberFailed(Member member, Throwable throwable)
            {
            // ToDo need to throw something here
            }

        @Override
        public void memberLeft(Member member)
            {
            // The member left we need to retry
            Iterator<Event> events = eventList.iterator();
            HashMap<Member, List<Event>> memberEventMap = new HashMap<Member, List<Event>>();

            while (events.hasNext())
                {
                Event e = events.next();

                DistributableEntryEvent distributableEntryEvent = (DistributableEntryEvent) e;
                DistributableEntry entry = distributableEntryEvent.getEntry();
                Binary binKey = entry.getBinaryKey();

                int iPartition = binKey.calculateNaturalPartition(partSvc.getPartitionCount());


                Member mbr = partSvc.getPartitionOwner(iPartition);

                List<Event> eventList = memberEventMap.get(member);

                if (eventList == null)
                    {
                    eventList = new ArrayList<Event>();
                    }

                eventList.add(e);
                memberEventMap.put(mbr, eventList);
                }

            for (Map.Entry<Member, List<Event>> e : memberEventMap.entrySet())
                {
                Member mbr = e.getKey();
                List<Event> eventList = e.getValue();

                eventChannel.publishList(mbr, eventList, partSvc);
                }
            }

        @Override
        public void invocationCompleted()
            {
            // This is always an invocation against a single member and we handle completion in the MemberComplete event
            }
        }


}
