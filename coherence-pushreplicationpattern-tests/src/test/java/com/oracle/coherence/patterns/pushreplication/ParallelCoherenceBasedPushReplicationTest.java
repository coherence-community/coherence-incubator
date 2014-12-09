/*
 * File: ParallelCoherenceBasedPushReplicationTest.java
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

import com.oracle.tools.runtime.coherence.CoherenceCacheServerSchema;

import com.oracle.tools.util.Capture;

/**
 * The {@link com.oracle.coherence.patterns.pushreplication.ParallelCoherenceBasedPushReplicationTest}
 * is an {@link com.oracle.coherence.patterns.pushreplication.AbstractPushReplicationTest}
 * designed to test the Coherence-based {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor}
 * using a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannel}.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class ParallelCoherenceBasedPushReplicationTest extends AbstractPushReplicationTest
{
    protected CoherenceCacheServerSchema newBaseCacheServerSchema(Capture<Integer> clusterPort)
    {
        return super.newBaseCacheServerSchema(clusterPort).setSystemProperty("event.distributor.config",
                                                                             "test-coherence-based-distributor-config.xml")
                                                                                 .setSystemProperty("proxy.port",
                                                                                                    getAvailablePortIterator());
    }


    @Override
    protected CoherenceCacheServerSchema newPassiveCacheServerSchema(Capture<Integer> clusterPort)
    {
        return newBaseCacheServerSchema(clusterPort).setCacheConfigURI("test-passive-cluster-cache-config.xml")
            .setClusterName("passive");
    }


    @Override
    protected CoherenceCacheServerSchema newActiveCacheServerSchema(Capture<Integer> clusterPort)
    {
        return newBaseCacheServerSchema(clusterPort)
            .setCacheConfigURI("test-remotecluster-paralleleventchannel-cache-config.xml").setClusterName("active");
    }
}
