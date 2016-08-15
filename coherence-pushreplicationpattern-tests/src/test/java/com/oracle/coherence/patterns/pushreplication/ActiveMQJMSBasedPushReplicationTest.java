/*
 * File: ActiveMQJMSBasedPushReplicationTest.java
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
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import org.apache.activemq.broker.BrokerService;
import org.junit.After;
import org.junit.Before;

import java.io.File;

/**
 * The {@link ActiveMQJMSBasedPushReplicationTest} is an
 * {@link AbstractPushReplicationTest} designed
 * to test an ActiveMQ JMS-based {@link EventDistributor}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ActiveMQJMSBasedPushReplicationTest extends AbstractPushReplicationTest
{
    /**
     * The JMS {@link BrokerService} for ActiveMQ.
     */
    private BrokerService brokerService;

    /**
     * The JNDI Provider URL for ActiveMQ.
     */
    private String jndiProviderURL;


    @Before
    public void onBeforeEachTest()
    {
        // determine a port on which to run the Broker
        int port = LocalPlatform.get().getAvailablePorts().next();

        // setup the ActiveMQ Broker
        jndiProviderURL = String.format("tcp://%s:%d", LocalPlatform.get().getLoopbackAddress().getHostAddress(), port);

        // determine the file-system location for ActiveMQ files
        // (even turning off persistence will create some files unfortunately!)
        File userDir = new File(System.getProperty("user.dir"));

        File activeMQDir = new File(new File(new File(new File(userDir.getParentFile(),
                                                               "coherence-pushreplicationpattern"),
                                                      "target"),
                                             "tmp"),
                                    "port-" + port);

        activeMQDir.mkdirs();

        brokerService = new BrokerService();

        try
        {
            brokerService.setDataDirectoryFile(activeMQDir);
            brokerService.setPersistent(false);
            brokerService.addConnector(jndiProviderURL);
            brokerService.setDeleteAllMessagesOnStartup(true);
            brokerService.setUseJmx(false);
            brokerService.setUseShutdownHook(true);
            brokerService.start(true);
            brokerService.waitUntilStarted();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to start JMS", e);
        }
    }


    @After
    public void onAfterEachTest()
    {
        // tear down the ActiveMQ broker
        try
        {
            brokerService.stop();
            brokerService.waitUntilStopped();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to stop JMS", e);
        }
    }


    @Override
    protected OptionsByType newBaseCacheServerOptions(Capture<Integer> clusterPort)
    {
        OptionsByType optionsByType = super.newBaseCacheServerOptions(clusterPort);

        optionsByType.add(SystemProperty.of("event.distributor.config", "test-jms-based-distributor-config.xml"));
        optionsByType.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        optionsByType.add(SystemProperty.of("java.naming.provider.url", jndiProviderURL));

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
