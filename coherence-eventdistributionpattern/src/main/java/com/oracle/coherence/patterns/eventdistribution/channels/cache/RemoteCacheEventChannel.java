/*
 * File: RemoteCacheEventChannel.java
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
import com.oracle.coherence.common.cluster.ClusterMetaInfo;
import com.oracle.coherence.common.cluster.LocalClusterMetaInfo;
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.channels.AbstractInterClusterEventChannel;
import com.oracle.coherence.patterns.eventdistribution.channels.InterClusterEventChannel;
import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link RemoteCacheEventChannel} is an {@link InterClusterEventChannel} and {@link CacheEventChannel} that uses
 * a <strong>remote</strong> {@link NamedCache} in which to distribute and apply {@link EntryEvent}s.
 * <p>
 * NOTE: Although this implementation provides a mechanism to distribute {@link EntryEvent}s to remote caches,
 * it does not support "batching".  Consequently this implementation <strong>will not be very effective<strong> when
 * attempting to distribute large volumes of {@link Event}s across high-latency networks (ie: a WANs).
 * <p>
 * Instead of using the {@link RemoteCacheEventChannel}, we <strong>strongly recommend</strong> that you use a
 * {@link RemoteClusterEventChannel}, that of which will distribute {@link Event}s in batches, with only a single
 * network round-trip per batch.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class RemoteCacheEventChannel extends AbstractInterClusterEventChannel implements CacheEventChannel
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(RemoteCacheEventChannel.class.getName());

    /**
     * A simple constant to represent a remote {@link NamedCache} that is offline.
     */
    protected final static transient NamedCache OFFLINE_CACHE = new WrapperNamedCache(NullImplementation.getMap(), null)
    {
        public boolean isActive()
        {
            return false;
        }
    };

    /**
     * The name of the remote {@link CacheService} that we'll use connect to the remote cache.
     */
    private String remoteCacheServiceName;

    /**
     * The name of the cache in the remote cluster to which we'll be distributing and applying {@link EntryEvent}s.
     */
    private String targetCacheName;

    /**
     * The {@link ParameterizedBuilder} that can be used to realize a {@link ConflictResolver}.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder;

    /**
     * The remote {@link NamedCache} that we're using to apply {@link EntryEvent}s.
     */
    private NamedCache remoteCache;


    /**
     * Standard Constructor.
     */
    public RemoteCacheEventChannel(DistributionRole                       distributionRole,
                                   String                                 targetCacheName,
                                   ParameterizedBuilder<ConflictResolver> conflictResolverBuilder,
                                   String                                 remoteCacheServiceName)
    {
        super(distributionRole);

        this.targetCacheName         = targetCacheName;
        this.conflictResolverBuilder = conflictResolverBuilder;

        this.remoteCacheServiceName  = remoteCacheServiceName;
        this.remoteCache             = OFFLINE_CACHE;
    }


    /**
     * Returns the name of the remote {@link CacheService} that we'll use to apply {@link Event}s.
     */
    public String getRemoteCacheServiceName()
    {
        return remoteCacheServiceName;
    }


    /**
     * {@inheritDoc}
     */
    public void connect(EventDistributor.Identifier       distributorIdentifier,
                        EventChannelController.Identifier controllerIdentifier) throws EventChannelNotReadyException
    {
        // auto-detect the remote cluster global name
        NamedCache targetCache = getTargetNamedCache();

        setTargetClusterMetaInfo((ClusterMetaInfo) targetCache.invoke("$dumby$", new GetClusterMetaInfoProcessor()));

        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Connected to Remote Cluster {0}", getTargetClusterMetaInfo().getUniqueName());
        }
    }


    /**
     * {@inheritDoc}
     */
    public void disconnect()
    {
        if (remoteCache != OFFLINE_CACHE)
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE,
                           "Disconnected from RemoteCache {0} using the RemoteCacheService {1}",
                           new Object[] {getTargetCacheName(), remoteCacheServiceName});
            }

            remoteCache.destroy();
        }
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
    public NamedCache getTargetNamedCache()
    {
        if (remoteCache == null)
        {
            String targetCacheName = getTargetCacheName();

            try
            {
                Service service = CacheFactory.getConfigurableCacheFactory().ensureService(remoteCacheServiceName);

                if (CacheService.TYPE_REMOTE.equals(service.getInfo().getServiceType()))
                {
                    remoteCache = ((CacheService) service).ensureCache(targetCacheName,
                                                                       Thread.currentThread().getContextClassLoader());

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE,
                                   "Connected to RemoteCache {0} using the RemoteCacheService {1}",
                                   new Object[] {targetCacheName, remoteCacheServiceName});
                    }
                }
                else
                {
                    throw new IllegalArgumentException("The Service " + remoteCacheServiceName
                                                       + " is not a remote cache service");
                }
            }
            catch (RuntimeException runtimeException)
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "Failed to connect to remote cache {0} using the remote cache service {1}",
                               new Object[] {targetCacheName, remoteCacheServiceName});
                }

                remoteCache = OFFLINE_CACHE;

                throw runtimeException;
            }
        }

        if (remoteCache == OFFLINE_CACHE)
        {
            throw new IllegalStateException(String.format("The remote cache %s is currently offline",
                                                          getTargetCacheName()));
        }
        else
        {
            return remoteCache;
        }
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
    @Override
    public int send(Iterator<Event> events)
    {
        return CacheEventChannelHelper.apply(getTargetNamedCache(),
                                             getEventsToDistribute(events).iterator(),
                                             getConflictResolverBuilder());

    }


    /**
     * A {@link GetClusterMetaInfoProcessor} is an {@link EntryProcessor} that determines and returns the {@link ClusterMetaInfo}
     * of a {@link Cluster}.
     *
     * @see InterClusterEventChannel
     */
    @SuppressWarnings("serial")
    public static class GetClusterMetaInfoProcessor extends AbstractProcessor implements PortableObject
    {
        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public GetClusterMetaInfoProcessor()
        {
            // deliberately empty
        }


        /**
         * {@inheritDoc}
         */
        public Object process(Entry entry)
        {
            return LocalClusterMetaInfo.getInstance();
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            // deliberately empty - nothing to read
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            // deliberately empty - nothing to write
        }
    }
}
