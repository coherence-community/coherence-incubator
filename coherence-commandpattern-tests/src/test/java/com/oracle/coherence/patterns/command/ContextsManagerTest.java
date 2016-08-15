/*
 * File: ContextsManagerTest.java
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

package com.oracle.coherence.patterns.command;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.deferred.Deferred;
import com.oracle.bedrock.deferred.Eventually;
import com.oracle.bedrock.deferred.PermanentlyUnavailableException;
import com.oracle.bedrock.deferred.TemporarilyUnavailableException;
import com.oracle.bedrock.junit.CoherenceClusterOrchestration;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationListener;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.net.ConfigurableCacheFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

/**
 * Functional tests for the {@link ContextsManager}.
 * <p>
 * Copyright (c) 2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ContextsManagerTest
{
    /**
     * Field description
     */
    @Rule
    public CoherenceClusterOrchestration orchestration =
        new CoherenceClusterOrchestration().setStorageMemberCount(3)
        .withOptions(CacheConfig.of("coherence-commandpattern-test-cache-config.xml"),
                     Pof.enabled(),
                     Pof.config("coherence-commandpattern-test-pof-config.xml"),
                     Logging.at(9));


    @Test
    public void shouldOperateDuringRollingRestart()
    {
        ConfigurableCacheFactory factory    = orchestration.getSessionFor(SessionBuilders.storageDisabledMember());

        ContextsManager          manager    = DefaultContextsManager.getInstance();

        Identifier               identifier = manager.registerContext(new GenericContext<Integer>(0));

        assertThat(identifier, is(notNullValue()));

        CommandSubmitter submitter = DefaultCommandSubmitter.getInstance();

        final int        LAST      = 1000;

        for (int i = 1; i <= LAST; i++)
        {
            submitter.submitCommand(identifier, new SetValueCommand<Integer>(i));
        }

        Deferred<Integer> deferred = new Deferred<Integer>()
        {
            @Override
            public Integer get() throws TemporarilyUnavailableException, PermanentlyUnavailableException
            {
                return ((GenericContext<Integer>) manager.getContext(identifier)).getValue();
            }

            @Override
            public Class<Integer> getDeferredClass()
            {
                return Integer.class;
            }
        };

        Eventually.assertThat("Content Value is greater than 0", deferred, greaterThan(0));

        StabilityPredicate<CoherenceCluster> predicate =
            StabilityPredicate.of((cluster -> cluster.findAny().get()
            .getServiceStatus("DistributedCacheForCommandPattern") == ServiceStatus.NODE_SAFE));

        CoherenceCluster cluster = orchestration.getCluster();

        // restart a random storage enabled cluster member
        cluster.filter((member) -> member.getRoleName().contains("storage")).limit(1).relaunch(predicate);

        // submit some more commands
        for (int i = LAST; i <= LAST * 2; i++)
        {
            submitter.submitCommand(identifier, new SetValueCommand<Integer>(i));
        }

        // restart a random storage enabled cluster member
        cluster.filter((member) -> member.getRoleName().contains("storage")).limit(1).relaunch(predicate);

        // submit some more commands
        for (int i = LAST * 2; i <= LAST * 3; i++)
        {
            submitter.submitCommand(identifier, new SetValueCommand<Integer>(i));
        }

        // restart a random storage enabled cluster member
        cluster.filter((member) -> member.getRoleName().contains("storage")).limit(1).relaunch(predicate);

        Eventually.assertThat("Content Value is greater than 0", deferred, is(LAST * 3));
    }


    /**
     * A custom {@link ApplicationListener} that is an {@link Option}.
     * @param <A>
     */
    public static abstract class CustomApplicationListener<A extends Application> implements ApplicationListener<A>,
                                                                                             Option
    {
    }
}
