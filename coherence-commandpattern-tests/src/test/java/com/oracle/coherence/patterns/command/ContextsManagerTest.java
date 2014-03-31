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

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.tools.deferred.Deferred;
import com.oracle.tools.deferred.DeferredHelper;
import com.oracle.tools.deferred.InstanceUnavailableException;
import com.oracle.tools.deferred.UnresolvableInstanceException;

import com.oracle.tools.junit.AbstractCoherenceTest;

import com.oracle.tools.runtime.ApplicationConsole;

import com.oracle.tools.runtime.actions.InteractiveActionExecutor;
import com.oracle.tools.runtime.actions.PerpetualAction;

import com.oracle.tools.runtime.coherence.Cluster;
import com.oracle.tools.runtime.coherence.ClusterBuilder;
import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;
import com.oracle.tools.runtime.coherence.actions.RestartClusterMemberAction;

import com.oracle.tools.runtime.console.SystemApplicationConsole;

import com.oracle.tools.runtime.java.ContainerBasedJavaApplicationBuilder;
import com.oracle.tools.runtime.java.JavaApplicationBuilder;
import com.oracle.tools.runtime.java.NativeJavaApplicationBuilder;
import com.oracle.tools.runtime.java.container.Container;

import com.oracle.tools.runtime.network.Constants;

import com.oracle.tools.util.Capture;
import com.oracle.tools.util.Predicate;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import org.junit.Test;

import sun.net.www.content.text.Generic;

import static com.oracle.tools.deferred.DeferredHelper.invoking;

import static com.oracle.tools.deferred.Eventually.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.Matchers.greaterThan;

import java.io.IOException;

/**
 * Functional tests for the {@link ContextsManager}.
 * <p>
 * Copyright (c) 2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ContextsManagerTest extends AbstractCoherenceTest
{
    @Test
    public void shouldOperateDuringRollingRestart() throws IOException
    {
        Capture<Integer> clusterPort = new Capture<Integer>(Container.getAvailablePorts());

        ClusterMemberSchema memberSchema =
            new ClusterMemberSchema().useLocalHostMode().setClusterPort(clusterPort).setPofEnabled(true)
                .setPofConfigURI("coherence-commandpattern-test-pof-config.xml")
                .setCacheConfigURI("coherence-commandpattern-test-cache-config.xml").setJMXSupport(true)
                .setJMXPort(Container.getAvailablePorts())
                .setJMXManagementMode(ClusterMemberSchema.JMXManagementMode.ALL).setLogLevel(9);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> memberBuilder =
            new NativeJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

        ApplicationConsole console        = new SystemApplicationConsole();

        ClusterBuilder     clusterBuilder = new ClusterBuilder();

        clusterBuilder.addBuilder(memberBuilder, memberSchema, "Server", 2);

        Cluster cluster = clusterBuilder.realize(console);

        assertThat(invoking(cluster).getClusterSize(), is(2));

        System.setProperty(ClusterMemberSchema.PROPERTY_POF_ENABLED, "true");
        System.setProperty(ClusterMemberSchema.PROPERTY_POF_CONFIG, "coherence-commandpattern-test-pof-config.xml");
        System.setProperty(ClusterMemberSchema.PROPERTY_CACHECONFIG, "coherence-commandpattern-test-cache-config.xml");
        System.setProperty(ClusterMemberSchema.PROPERTY_CLUSTER_PORT, Integer.toString(clusterPort.get()));
        System.setProperty(ClusterMemberSchema.PROPERTY_MULTICAST_TTL, "0");
        System.setProperty(ClusterMemberSchema.PROPERTY_LOCALHOST_ADDRESS, Constants.getLocalHost());
        System.setProperty(ClusterMemberSchema.PROPERTY_DISTRIBUTED_LOCALSTORAGE, "false");

        ConfigurableCacheFactory configurableCacheFactory =
            new ExtensibleConfigurableCacheFactory(ExtensibleConfigurableCacheFactory.DependenciesHelper
                .newInstance("coherence-commandpattern-test-cache-config.xml"));

        CacheFactory.setConfigurableCacheFactory(configurableCacheFactory);

        final ContextsManager manager    = DefaultContextsManager.getInstance();

        final Identifier      identifier = manager.registerContext(new GenericContext<Integer>(0));

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
            public Integer get() throws UnresolvableInstanceException, InstanceUnavailableException
            {
                return ((GenericContext<Integer>) manager.getContext(identifier)).getValue();
            }

            @Override
            public Class<Integer> getDeferredClass()
            {
                return Integer.class;
            }
        };

        assertThat(deferred, is(greaterThan(0)));

        // construct the action to restart a cluster member
        RestartClusterMemberAction restartAction = new RestartClusterMemberAction("Server",
                                                                                  memberBuilder,
                                                                                  memberSchema,
                                                                                  console,
                                                                                  new Predicate<ClusterMember>()
        {
            @Override
            public boolean evaluate(ClusterMember member)
            {
                ClusterMember.ServiceStatus status = member.getServiceStatus("DistributedCacheForCommandPattern");

                return status == ClusterMember.ServiceStatus.NODE_SAFE;
            }
        });

        // let's perpetually restart a cluster member
        PerpetualAction<ClusterMember, Cluster> perpetualAction = new PerpetualAction<ClusterMember,
                                                                                      Cluster>(restartAction);

        InteractiveActionExecutor<ClusterMember, Cluster> executor = new InteractiveActionExecutor<ClusterMember,
                                                                                                   Cluster>(cluster,
                                                                                                            perpetualAction);

        executor.executeNext();

        for (int i = LAST; i <= LAST * 2; i++)
        {
            submitter.submitCommand(identifier, new SetValueCommand<Integer>(i));
        }

        executor.executeNext();

        for (int i = LAST * 2; i <= LAST * 3; i++)
        {
            submitter.submitCommand(identifier, new SetValueCommand<Integer>(i));
        }

        executor.executeNext();

        assertThat(deferred, is(LAST * 3));

        cluster.close();
    }
}
