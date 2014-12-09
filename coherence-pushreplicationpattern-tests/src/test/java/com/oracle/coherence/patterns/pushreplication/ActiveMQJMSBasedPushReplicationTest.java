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

import com.oracle.coherence.patterns.eventdistribution.EventDistributor;

import com.oracle.tools.runtime.coherence.CoherenceCacheServerSchema;

import com.oracle.tools.runtime.network.Constants;

import com.oracle.tools.util.Capture;

import org.apache.activemq.broker.BrokerService;

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


    @Override
    public void onBeforeEachTest()
    {
        super.onBeforeEachTest();

        // setup the ActiveMQ Broker

        // determine the port for connections
        int port = getAvailablePortIterator().next();

        jndiProviderURL = String.format("tcp://%s:%d", Constants.getLocalHost(), port);

        // determine the file-system location for ActiveMQ files
        // (even turning off persistence will create some files unfortunately!)
        File userDir = new File(System.getProperty("user.dir"));

        File activeMQDir = new File(new File(new File(new File(userDir.getParentFile(),
                                                               "coherence-pushreplicationpattern"),
                                                      "target"), "tmp"), "port-" + port);

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


    @Override
    public void onAfterEachTest()
    {
        super.onAfterEachTest();

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
    protected CoherenceCacheServerSchema newBaseCacheServerSchema(Capture<Integer> clusterPort)
    {
        return super.newBaseCacheServerSchema(clusterPort).setSystemProperty("event.distributor.config",
                                                                             "test-jms-based-distributor-config.xml")
                                                                                 .setSystemProperty("proxy.port",
                                                                                                    getAvailablePortIterator())
                                                                                                    .setSystemProperty("java.naming.provider.url", jndiProviderURL);
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
            .setCacheConfigURI("test-remotecluster-eventchannel-cache-config.xml").setClusterName("active");
    }
}
