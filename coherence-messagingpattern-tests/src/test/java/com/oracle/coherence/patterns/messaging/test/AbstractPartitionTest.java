/*
 * File: AbstractPartitionTest.java
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

import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscriber;

import com.oracle.tools.deferred.Eventually;

import com.oracle.tools.junit.AbstractCoherenceTest;

import com.oracle.tools.runtime.ApplicationListener;

import com.oracle.tools.runtime.coherence.CoherenceCacheServer;
import com.oracle.tools.runtime.coherence.CoherenceCacheServerSchema;
import com.oracle.tools.runtime.coherence.JMXManagementMode;

import com.oracle.tools.runtime.java.container.Container;

import com.oracle.tools.runtime.network.AvailablePortIterator;

import com.oracle.tools.runtime.options.EnvironmentVariables;

import com.oracle.tools.util.Capture;
import org.hamcrest.Matchers;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.oracle.tools.deferred.DeferredHelper.invoking;

import static org.hamcrest.core.Is.is;

import java.net.UnknownHostException;

import java.util.Date;
import java.util.Properties;

/**
 * The base class for all Messaging Pattern Functional Tests.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author David Rowlands
 */
public abstract class AbstractPartitionTest extends AbstractCoherenceTest
{
    /**
     * The {@link AvailablePortIterator} is use to find available ports.
     */
    private static AvailablePortIterator availablePortIterator;

    /**
     * The System {@link Properties} used to start the test.
     */
    private Properties systemProperties;


    /**
     * {@inheritDoc}
     */
    @Before
    @Override
    public void onBeforeEachTest()
    {
        // take a copy of the system properties (as we're going to change them)
        systemProperties = System.getProperties();
    }


    /**
     * {@inheritDoc}
     */
    @After
    @Override
    public void onAfterEachTest()
    {
        // reset the system properties
        System.setProperties(systemProperties);

    }


    /**
     * Setup the test infrastructure.
     *
     * @throws UnknownHostException When we fail to locate the localhost on which the tests will be running.
     */
    @BeforeClass
    public static void setupClass() throws UnknownHostException
    {
        availablePortIterator = Container.getAvailablePorts();
    }


    /**
     * A simple method to wait a specified amount of time, with a message to stdout.
     *
     * @param time The time to wait in ms.
     * @param rationale The rationale (message) for waiting.
     * @throws InterruptedException When interrupted while waiting.
     */
    public void wait(long   time,
                     String rationale) throws InterruptedException
    {
        System.out.printf("%s: Waiting %dms %s\n", new Date(), time, rationale);
        Thread.sleep(time);
    }


    /**
     * Determines the {@link AvailablePortIterator} to use for locating free ports on the host.
     *
     * @return {@link AvailablePortIterator}
     */
    protected static AvailablePortIterator getAvailablePortIterator()
    {
        return availablePortIterator;
    }


    /**
     * Constructs a new {@link CoherenceCacheServerSchema} on which sub-classes of this class should
     * configure new {@link CoherenceCacheServerSchema}s.  ie: This method returns a pre-configured
     * {@link CoherenceCacheServerSchema} that the tests in this class expect to use.
     *
     * @return {@link CoherenceCacheServerSchema}
     */
    protected CoherenceCacheServerSchema newBaseCacheServerSchema(Capture<Integer> clusterPort)
    {
        CoherenceCacheServerSchema schema =
            new CoherenceCacheServerSchema().addOption(EnvironmentVariables.inherited()).useLocalHostMode()
                .setClusterPort(clusterPort)
                .setPofConfigURI("coherence-messagingpattern-test-pof-config.xml")
                .setPofEnabled(true)
                .setPreferIPv4(true)
                .setJMXManagementMode(JMXManagementMode.ALL).setJMXPort(getAvailablePortIterator());

        schema.addApplicationListener(new ApplicationListener<CoherenceCacheServer>()
        {
            @Override
            public void onClosing(CoherenceCacheServer server)
            {
            }

            @Override
            public void onClosed(CoherenceCacheServer server)
            {
            }

            @Override
            public void onRealized(CoherenceCacheServer server)
            {
                Eventually.assertThat(invoking(server).isServiceRunning("ExtendTcpProxyService"), is(true));
            }
        });

        return schema;
    }


    protected abstract CoherenceCacheServerSchema newCacheServerSchema(Capture<Integer> clusterPort);


    /**
     * Ensure that we can publish concurrently to a topic.
     */
    public void concurrentPublisher(MessagingSession messagingSession,
                                    String           topicName,
                                    int              numPublisher)
    {
        final int              nPub      = numPublisher;
        final MessagingSession ms        = messagingSession;
        final String           tn        = topicName;

        Runnable               publisher = new Runnable()
        {
            public void run()
            {
                for (int i = 0; i < nPub; i++)
                {
                    ms.publishMessage(tn, Integer.valueOf(i));
                }
            }
        };

        Thread publisherThread = new Thread(publisher);

        publisherThread.setDaemon(true);
        publisherThread.start();

    }


    /**
     * Ensure that we can publish concurrently to a topic.
     */
    public void subscriber(Subscriber subscriber,
                           int        numPublisher)
    {
        int count = 0;

        while (subscriber != null && count < numPublisher)
        {
            Integer i = (Integer) subscriber.getMessage();

            Assert.assertThat(Integer.valueOf(count), Matchers.is(i));
            count++;
        }
    }
}
