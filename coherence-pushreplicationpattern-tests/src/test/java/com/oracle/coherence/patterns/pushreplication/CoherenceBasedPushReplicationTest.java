/*
 * File: CoherenceBasedPushReplicationTest.java
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

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;

/**
 * The {@link CoherenceBasedPushReplicationTest} is an {@link AbstractPushReplicationTest} designed
 * to test the Coherence-based {@link EventDistributor}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceBasedPushReplicationTest extends AbstractPushReplicationTest
{
    @Override
    protected OptionsByType newBaseCacheServerOptions(Capture<Integer> clusterPort)
    {
        OptionsByType optionsByType = super.newBaseCacheServerOptions(clusterPort);

        optionsByType.add(SystemProperty.of("event.distributor.config", "test-coherence-based-distributor-config.xml"));
        optionsByType.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));

        return optionsByType;
    }


    @Override
    protected OptionsByType newPassiveCacheServerOptions(Capture<Integer> clusterPort)
    {
        OptionsByType optionsByType = newBaseCacheServerOptions(clusterPort);

        optionsByType.add(ClusterName.of("passive"));
        optionsByType.add(CacheConfig.of("test-passive-cluster-cache-config.xml"));

        return optionsByType;
    }


    @Override
    protected OptionsByType newActiveCacheServerOptions(Capture<Integer> clusterPort)
    {
        OptionsByType optionsByType = newBaseCacheServerOptions(clusterPort);

        optionsByType.add(ClusterName.of("active"));
        optionsByType.add(CacheConfig.of("test-remotecluster-eventchannel-cache-config.xml"));

        return optionsByType;
    }
}
