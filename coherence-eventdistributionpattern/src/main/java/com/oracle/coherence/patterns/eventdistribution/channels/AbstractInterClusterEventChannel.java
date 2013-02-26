/*
 * File: AbstractInterClusterEventChannel.java
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

import com.oracle.coherence.common.cluster.ClusterMetaInfo;
import com.oracle.coherence.common.cluster.LocalClusterMetaInfo;
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AbstractInterClusterEventChannel} is a base implementation of the {@link InterClusterEventChannel}
 * that can be used to provide contextual information regarding the source and target of an {@link Event}.  This
 * information can then be used by the {@link InterClusterEventChannel} to determine if an {@link Event}
 * should be distributed, especially to prevent infinite-cluster feedback, aka: ping-pong between clusters.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractInterClusterEventChannel implements InterClusterEventChannel
{
    /**
     * The {@link Logger} to use for this class.
     */
    private static Logger logger = Logger.getLogger(AbstractInterClusterEventChannel.class.getName());

    /**
     * The {@link DistributionRole} of the {@link InterClusterEventChannel}.
     */
    private DistributionRole distributionRole;

    /**
     * The {@link ClusterMetaInfo} of the {@link Cluster} in which the {@link InterClusterEventChannel} is operating.
     */
    protected ClusterMetaInfo localClusterMetaInfo;

    /**
     * The {@link ClusterMetaInfo} of the target Coherence cluster to which the {@link InterClusterEventChannel} will
     * distribute {@link Event}s.
     */
    private ClusterMetaInfo targetClusterMetaInfo;


    /**
     * Standard Constructor.
     *
     * @param distributionRole The {@link com.oracle.coherence.patterns.eventdistribution.channels.InterClusterEventChannel.DistributionRole}
     * of the {@link EventChannel}.
     */
    public AbstractInterClusterEventChannel(DistributionRole distributionRole)
    {
        super();

        this.distributionRole = distributionRole;

        // determine the ClusterMetaInfo for the Cluster in which we are operating
        this.localClusterMetaInfo = LocalClusterMetaInfo.getInstance();

        // note: this will be initialized during startup of the implementing EventChannel.
        this.targetClusterMetaInfo = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DistributionRole getDistributionRole()
    {
        return distributionRole;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterMetaInfo getTargetClusterMetaInfo()
    {
        return targetClusterMetaInfo;
    }


    /**
     * Sets the {@link ClusterMetaInfo} for the determined target cluster.
     *
     * @param targetClusterMetaInfo The {@link ClusterMetaInfo} of the target cluster.
     */
    protected void setTargetClusterMetaInfo(ClusterMetaInfo targetClusterMetaInfo)
    {
        this.targetClusterMetaInfo = targetClusterMetaInfo;
    }


    /**
     * Helper method to get the BackingMapManagerContext.
     *
     * @param cacheName  The cache name associated with the BackingMapManagerContext
     *
     * @return The requested BackingMapManagerContext
     */
    private BackingMapManagerContext getBackingMapManagerCtx(String cacheName)
    {
        NamedCache               cache   = CacheFactory.getCache(cacheName);
        CacheService             service = cache.getCacheService();
        BackingMapManager        manager = service.getBackingMapManager();
        BackingMapManagerContext ctx     = manager.getContext();

        return ctx;
    }


    /**
     * Extracts the {@link ClusterMetaInfo} of the {@link Cluster} from where the {@link EntryEvent} originated.
     *
     * @param entryEvent The {@link DistributableEntryEvent} from which to determine the {@link ClusterMetaInfo}.
     *
     * @return {@link ClusterMetaInfo}
     */
    @SuppressWarnings({"rawtypes"})
    private ClusterMetaInfo getSourceClusterMetaInfo(DistributableEntryEvent entryEvent)
    {
        Map decorations =
            (Map) entryEvent.getEntry().getContext().getInternalValueDecoration(entryEvent.getEntry().getBinaryValue(),
                                                                                BackingMapManagerContext.DECO_CUSTOM);

        if (decorations == null)
        {
            throw new IllegalStateException("Can't extract the CLUSTER_INFO_DECORATION_KEY from the EntryEvent. The BinaryValue is not decorated");
        }

        ClusterMetaInfo sourceClusterMetaInfo =
            (ClusterMetaInfo) decorations.get(DistributableEntry.CLUSTER_META_INFO_DECORATION_KEY);

        if (sourceClusterMetaInfo == null)
        {
            throw new IllegalStateException("Expecting a non-null value for CLUSTER_INFO_DECORATION_KEY");
        }

        return sourceClusterMetaInfo;
    }


    /**
     * Helper method to determine whether an {@link EntryEvent} should be distributed.  If the entry is an artifact of
     * some update done by an application local to the cluster, it is deemed local and always replicated.  If the entry
     * is the artifact of distribution from some other cluster, we check to see whether this cluster has been configured
     * as a hub or a leaf.  It is a leaf then replication is not done.  If it is a hub the update will get propagated
     * to other clusters that are not the source cluster.
     */
    private boolean isDistributable(DistributableEntryEvent entryEvent)
    {
        // determine the source ClusterMetaInfo
        ClusterMetaInfo sourceClusterMetaInfo = getSourceClusterMetaInfo(entryEvent);

        // determine if we should distribute
        boolean isLocal = localClusterMetaInfo.equals(sourceClusterMetaInfo);

        if (isLocal)
        {
            // The entry is local, meaning it is the result of some update from an application running on this cluster.
            // (hence it is always distributed)
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST,
                           String.format("Distributing a local event from %s", sourceClusterMetaInfo.getUniqueName()));
            }

            return true;
        }
        else
        {
            // The entry is a result of some other cluster distributing to this cluster. If this a leaf cluster we don't
            // further distributed the entry. If it's not, we distribute to other EventChannels that are not the source event
            if (getDistributionRole() == InterClusterEventChannel.DistributionRole.LEAF)
            {
                if (logger.isLoggable(Level.FINEST))
                {
                    logger.log(Level.FINEST,
                               String.format("Won't distribute the non-local event %s from %s", entryEvent,
                                             sourceClusterMetaInfo.getUniqueName()));
                }

                return false;
            }
            else
            {
                return (!sourceClusterMetaInfo.equals(getTargetClusterMetaInfo()));
            }
        }
    }


    /**
     * Determines the {@link Event}s that can be distributed to the target cluster.
     * <p>
     * Note: This method performs the necessary checks to filter out {@link Event}s that would cause infinite
     * {@link Event} distribution to occur between two clusters.
     *
     * @param events An {@link Iterator} over the {@link Event}s to consider for distribution.
     *
     * @return A {@link List} of {@link Event}s to actually distribute.
     */
    protected List<Event> getEventsToDistribute(Iterator<Event> events)
    {
        ArrayList<Event> eventsToDistribute = new ArrayList<Event>();

        for (; events.hasNext(); )
        {
            Event event = events.next();

            if (event instanceof DistributableEntryEvent)
            {
                DistributableEntryEvent entryEvent = (DistributableEntryEvent) event;

                // set the backing map manager context for the DistributableEntry
                BackingMapManagerContext context = getBackingMapManagerCtx(entryEvent.getCacheName());

                entryEvent.getEntry().setContext(context);

                if (isDistributable(entryEvent))
                {
                    eventsToDistribute.add(entryEvent);
                }
            }
            else
            {
                eventsToDistribute.add(event);
            }
        }

        return eventsToDistribute;
    }
}
