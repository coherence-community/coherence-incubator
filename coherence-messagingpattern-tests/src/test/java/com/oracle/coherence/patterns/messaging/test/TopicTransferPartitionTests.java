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

package com.oracle.coherence.patterns.messaging.test;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.patterns.messaging.DefaultMessagingSession;
import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscriber;

import com.oracle.tools.runtime.coherence.CoherenceCacheServer;
import com.oracle.tools.runtime.coherence.CoherenceCacheServerSchema;

import com.oracle.tools.runtime.concurrent.runnable.RuntimeExit;
import com.oracle.tools.runtime.concurrent.runnable.RuntimeHalt;
import com.oracle.tools.runtime.concurrent.runnable.SystemExit;

import com.oracle.tools.runtime.console.SystemApplicationConsole;

import com.oracle.tools.runtime.java.JavaApplicationBuilder;
import com.oracle.tools.runtime.java.LocalJavaApplicationBuilder;

import com.oracle.tools.util.Capture;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

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
    @Override
    protected CoherenceCacheServerSchema newCacheServerSchema(Capture<Integer> clusterPort)
    {
        return newBaseCacheServerSchema(clusterPort)
            .setCacheConfigURI("coherence-messagingpattern-test-cache-config.xml").setClusterName("TopicMessagePattern")
            .setSystemProperty("proxy.port",
                               getAvailablePortIterator());
    }


    /**
     * Test partition transfer logic with the addition of a node to the cluster
     */
    //TODO: refactor these tests to use Oracle Tools (so that the tests doesn't connect / join the cluster)
    //@Test
    public void partitionAddServerTest() throws Exception
    {
        CacheFactory.shutdown();

        int                                          nrServers = 1;

        ArrayList<CoherenceCacheServer>              servers   = new ArrayList<CoherenceCacheServer>(nrServers);

        JavaApplicationBuilder<CoherenceCacheServer> builder   =
            new LocalJavaApplicationBuilder<CoherenceCacheServer>();

        try
        {
            // establish the cluster
            Capture<Integer>         clusterPort = new Capture<>(getAvailablePortIterator());
            SystemApplicationConsole console     = new SystemApplicationConsole();

            // Start one of the nodes of the cluster
            for (int i = 0; i < nrServers; i++)
            {
                CoherenceCacheServerSchema serverSchema = newCacheServerSchema(clusterPort);

                servers.add(i, builder.realize(serverSchema, String.format("SERVER %s", i), console));
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            String remPort = servers.get(0).getSystemProperty("proxy.port");

            System.setProperty("remote.port", remPort);
            System.setProperty("tangosol.pof.config", "coherence-messagingpattern-test-pof-config.xml");
            System.setProperty("tangosol.pof.enabled", "true");
            System.setProperty("tangosol.coherence.cacheconfig",
                               "coherence-messagingpattern-test-client-cache-config.xml");

            String           topicName        = "topic-partitionAddServerTest";
            int              publisherCount   = 100;
            MessagingSession messagingSession = DefaultMessagingSession.getInstance();
            Identifier       topicIdentifier  = messagingSession.createTopic(topicName);
            Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

            super.concurrentPublisher(messagingSession, topicName, publisherCount);

            // Start new member
            CoherenceCacheServerSchema serverSchema = newCacheServerSchema(clusterPort);
            CoherenceCacheServer member = builder.realize(serverSchema, String.format("SERVER %s", nrServers), console);

            servers.add(member);

            super.subscriber(subscriber, publisherCount);

            subscriber.unsubscribe();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.getConfigurableCacheFactory().dispose();
            CacheFactory.shutdown();

            for (CoherenceCacheServer server : servers)
            {
                server.close(new RuntimeHalt());
            }
        }
    }


    /**
     * Test partition transfer logic with the addition of a node to the cluster
     */
    //TODO: refactor these tests to use Oracle Tools (so that the tests doesn't connect / join the cluster)
    //@Test
    public void partitionRemoveServerTest() throws Exception
    {
        CacheFactory.shutdown();

        int                                          nrServers = 2;

        ArrayList<CoherenceCacheServer>              servers   = new ArrayList<CoherenceCacheServer>(nrServers);

        JavaApplicationBuilder<CoherenceCacheServer> builder   =
            new LocalJavaApplicationBuilder<CoherenceCacheServer>();

        try
        {
            // establish the cluster
            Capture<Integer>         clusterPort = new Capture<>(getAvailablePortIterator());
            SystemApplicationConsole console     = new SystemApplicationConsole();

            // Start one of the nodes of the cluster
            for (int i = 0; i < nrServers; i++)
            {
                CoherenceCacheServerSchema serverSchema = newCacheServerSchema(clusterPort);

                servers.add(i, builder.realize(serverSchema, String.format("SERVER %s", i), console));
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            String remPort = servers.get(0).getSystemProperty("proxy.port");

            System.setProperty("remote.port", remPort);
            System.setProperty("tangosol.pof.config", "coherence-messagingpattern-test-pof-config.xml");
            System.setProperty("tangosol.pof.enabled", "true");
            System.setProperty("tangosol.coherence.cacheconfig",
                               "coherence-messagingpattern-test-client-cache-config.xml");

            String           topicName        = "topic-partitionRemoveServerTest";
            int              publisherCount   = 100;
            MessagingSession messagingSession = DefaultMessagingSession.getInstance();
            Identifier       topicIdentifier  = messagingSession.createTopic(topicName);
            Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

            super.concurrentPublisher(messagingSession, topicName, publisherCount);

            // stop member
            CoherenceCacheServer member = servers.remove(nrServers - 1);

            member.close(new RuntimeHalt());

            super.subscriber(subscriber, publisherCount);

            subscriber.unsubscribe();

        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.getConfigurableCacheFactory().dispose();
            CacheFactory.shutdown();

            for (CoherenceCacheServer server : servers)
            {
                server.close(new RuntimeHalt());
            }
        }
    }
}
