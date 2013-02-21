/*
 * File: InterClusterEventChannel.java
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
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver;
import com.tangosol.net.Cluster;

/**
 * An {@link InterClusterEventChannel} is a specialized {@link EventChannel} that distributes {@link Event}s to another
 * Coherence cluster.
 * <p>
 * {@link InterClusterEventChannel}s differ from regular {@link EventChannel}s in that they must provide
 * {@link ClusterMetaInfo} of the "target" {@link Cluster} so that the underlying {@link EventChannelController}s can prevent
 * infinite distribution loops (commonly known as ping-pong) between {@link Cluster}s.
 * <p>
 * Infinite distribution loops occurs when an update in one cluster (let's call it Cluster A) causes an event
 * to be distributed to another cluster (let's call it Cluster B), at which the event causes an update, which is
 * then distributed back to Cluster A, causing a yet another update event, which is then distributed back to Cluster B,
 * ad infinitum.
 * <p>
 * Without both {@link ConflictResolver}s and the ability to determine the "source" and "target"
 * of {@link EventChannel}s, infinite distribution loops would readily occur and consequently consume all available
 * band-width between sites.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface InterClusterEventChannel extends EventChannel
{
    /**
     * Defines the behavior of an {@link InterClusterEventChannel} when distributing an {@link Event} that has
     * arrived from another cluster.
     */
    public enum DistributionRole
    {
        /**
         * {@link #LEAF} indicates that the target cluster acts as an end-point/leaf for incoming {@link Event}
         * distribution.
         * <p>
         * Specifically, {@link Event}s arriving from other clusters are processed locally and will not be further
         * distributed to other clusters.
         */
        LEAF,

        /**
         * {@link #HUB} indicates that the target cluster acts as a hub for incoming distribution.
         * <p>
         * Specifically, {@link Event}s arriving from other clusters are distributed locally
         * <strong>and also</strong> distributed to other clusters (using {@link EventChannel}s, except to the cluster
         * from which they originated.
         */
        HUB
    }


    /**
     * Returns the {@link ClusterMetaInfo} of the "target" {@link Cluster} to which the {@link EventChannel} is
     * distributing {@link Event}s.
     *
     * @return A {@link ClusterMetaInfo}
     */
    public ClusterMetaInfo getTargetClusterMetaInfo();


    /**
     * Returns {@link Cluster} {@link DistributionRole} for the {@link EventChannel}s constructed with this
     * {@link EventChannelBuilder}.
     */
    public DistributionRole getDistributionRole();
}
