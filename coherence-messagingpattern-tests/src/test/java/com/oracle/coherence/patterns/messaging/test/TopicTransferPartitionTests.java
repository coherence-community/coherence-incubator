/*
 * File: TopicTransferPartitionTests.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.messaging.test;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.patterns.messaging.DefaultMessagingSession;
import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscriber;

import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;

import com.oracle.tools.runtime.console.SystemApplicationConsole;

import com.oracle.tools.runtime.java.ExternalJavaApplicationBuilder;
import com.oracle.tools.runtime.java.JavaApplicationBuilder;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;

import org.junit.Test;

import java.util.ArrayList;

/**
 * Functional Tests for Topics using the Messaging Pattern.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *  @author David Rowlands
 */
public class TopicTransferPartitionTests extends AbstractPartitionTest
{
    /**
     * {@inheritDoc}
     */
    protected ClusterMemberSchema newClusterMemberSchema(int iPort)
    {
        return newBaseClusterMemberSchema(iPort).setCacheConfigURI("coherence-messagingpattern-test-cache-config.xml")
            .setClusterName("TopicMessagePattern").setSystemProperty("proxy.port",
                                                                     getAvailablePortIterator());
    }


    /**
     * Test partition transfer logic with the addition of a node to the cluster
     */
    @Test
    public void partitionAddServerTest() throws Exception
    {
        int                      nrServers = 1;

        ArrayList<ClusterMember> servers   = new ArrayList<ClusterMember>(nrServers);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder =
            new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

        try
        {
            // establish the cluster
            int                      iPort   = getAvailablePortIterator().next();
            SystemApplicationConsole console = new SystemApplicationConsole();

            // Start one of the nodes of the cluster
            for (int i = 0; i < nrServers; i++)
            {
                ClusterMemberSchema serverSchema = newClusterMemberSchema(iPort);

                servers.add(i, builder.realize(serverSchema, String.format("SERVER %s", i), console));
            }

            // wait for cluster to start
            for (ClusterMember server : servers)
            {
                server.getClusterMBeanInfo();
                server.getServiceMBeanInfo("ExtendTcpProxyService", 1);
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            String remPort = servers.get(0).getSystemProperty("proxy.port");

            System.setProperty("remote.port", remPort);
            System.setProperty("tangosol.pof.config", "coherence-messagingpattern-test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("coherence-messagingpattern-test-client-cache-config.xml"));

            String           topicName        = "topic-partitionAddServerTest";
            int              publisherCount   = 100;
            MessagingSession messagingSession = DefaultMessagingSession.getInstance();
            Identifier       topicIdentifier  = messagingSession.createTopic(topicName);
            Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

            super.concurrentPublisher(messagingSession, topicName, publisherCount);

            // Start new member
            ClusterMemberSchema serverSchema = newClusterMemberSchema(iPort);
            ClusterMember member = builder.realize(serverSchema, String.format("SERVER %s", nrServers + 1), console);
            servers.add(member);
            
            member.getClusterMBeanInfo();
            member.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            super.subscriber(subscriber, publisherCount);

        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();
            
            for (ClusterMember server : servers)
            {
                server.destroy();
            }

        }
    }
    
    /**
     * Test partition transfer logic with the addition of a node to the cluster
     */
    @Test
    public void partitionRemoveServerTest() throws Exception
    {
        int                      nrServers = 2;

        ArrayList<ClusterMember> servers   = new ArrayList<ClusterMember>(nrServers);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder =
            new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

        try
        {
            // establish the cluster
            int                      iPort   = getAvailablePortIterator().next();
            SystemApplicationConsole console = new SystemApplicationConsole();

            // Start one of the nodes of the cluster
            for (int i = 0; i < nrServers; i++)
            {
                ClusterMemberSchema serverSchema = newClusterMemberSchema(iPort);

                servers.add(i, builder.realize(serverSchema, String.format("SERVER %s", i), console));
            }

            // wait for cluster to start
            for (ClusterMember server : servers)
            {
                server.getClusterMBeanInfo();
                server.getServiceMBeanInfo("ExtendTcpProxyService", 1);
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            String remPort = servers.get(0).getSystemProperty("proxy.port");

            System.setProperty("remote.port", remPort);
            System.setProperty("tangosol.pof.config", "coherence-messagingpattern-test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("coherence-messagingpattern-test-client-cache-config.xml"));

            String           topicName        = "topic-partitionAddServerTest";
            int              publisherCount   = 100;
            MessagingSession messagingSession = DefaultMessagingSession.getInstance();
            Identifier       topicIdentifier  = messagingSession.createTopic(topicName);
            Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

            super.concurrentPublisher(messagingSession, topicName, publisherCount);
    
            // stop member
            ClusterMember member = servers.remove(nrServers - 1);
            member.destroy();
            
            super.subscriber(subscriber, publisherCount);

        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            for (ClusterMember server : servers)
            {
                server.destroy();
            }

        }
    }
    
}
