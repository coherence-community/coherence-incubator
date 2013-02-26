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
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.NullImplementation;

import java.util.Iterator;

/**
 * A {@link LocalCacheEventChannel} is a {@link CacheEventChannel} implementation that will
 * distribute and apply {@link EntryEvent}s to a <strong>local</strong> {@link NamedCache} (ie: contained
 * in the Coherence Cluster in which the {@link CacheEventChannel} is executing).
 * <p>
 * ie: This {@link EventChannel} is very useful for distributing {@link Event}s to a cache in the local cluster.
 * <p>
 * To distribute {@link EntryEvent}s from to another cluster, you should use a {@link RemoteClusterEventChannel} with
 * a {@link LocalCacheEventChannel} or potentially a {@link RemoteCacheEventChannel} (though these have significantly
 * lower performance and throughput than of {@link RemoteClusterEventChannel}s)
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see CacheEventChannel
 * @see RemoteClusterEventChannel
 * @see RemoteCacheEventChannel
 *
 * @author Brian Oliver
 */
public class LocalCacheEventChannel implements CacheEventChannel
{
    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} in which the
     * {@link EventChannel} operates.
     */
    private EventDistributor.Identifier distributorIdentifier;

    /**
     * The name of the local cache on which we'll apply the {@link EntryEvent}s.
     */
    private String targetCacheName;

    /**
     * The {@link ConflictResolver} that will be used to resolve any conflicts the application of {@link EntryEvent}s
     * may cause.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder;


    /**
     * Standard Constructor.
     */
    public LocalCacheEventChannel(String                                 targetCacheName,
                                  ParameterizedBuilder<ConflictResolver> conflictResolverBuilder)
    {
        super();

        this.targetCacheName         = targetCacheName;
        this.conflictResolverBuilder = conflictResolverBuilder;
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
        return CacheEventChannelHelper.apply(getTargetNamedCache(), events, getConflictResolverBuilder());
    }
}
