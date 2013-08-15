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

import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscriber;

import com.oracle.tools.junit.AbstractCoherenceTest;

import com.oracle.tools.runtime.Application;
import com.oracle.tools.runtime.LifecycleEvent;
import com.oracle.tools.runtime.LifecycleEventInterceptor;
import com.oracle.tools.runtime.PropertiesBuilder;

import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema.JMXManagementMode;

import com.oracle.tools.runtime.network.AvailablePortIterator;

import org.hamcrest.Matchers;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

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
        availablePortIterator = new AvailablePortIterator(30000);
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
     * Constructs a new {@link ClusterMemberSchema} on which sub-classes of this class should
     * configure new {@link ClusterMemberSchema}s.  ie: This method returns a pre-configured
     * {@link ClusterMemberSchema} that the tests in this class expect to use.
     *
     * @return {@link ClusterMemberSchema}
     */
    protected ClusterMemberSchema newBaseClusterMemberSchema(int iClusterPort)
    {
        ClusterMemberSchema schema =
            new ClusterMemberSchema().setEnvironmentVariables(PropertiesBuilder.fromCurrentEnvironmentVariables())
                .setSingleServerMode()
                .setClusterPort(iClusterPort == 0 ? getAvailablePortIterator().next() : iClusterPort)
                .setPofConfigURI("coherence-messagingpattern-test-pof-config.xml")
                .setJMXPort(getAvailablePortIterator()).setJMXManagementMode(JMXManagementMode.ALL);

        schema.addLifecycleInterceptor(new LifecycleEventInterceptor<ClusterMember>()
        {
            @Override
            public void onEvent(LifecycleEvent<ClusterMember> event)
            {
                if (event.getType() == Application.EventKind.REALIZED)
                {
                    ClusterMember member = event.getObject();

                    // wait for the coherence application cluster MBean to become available
                    member.getClusterMBeanInfo();

                    // wait for the defined extend tcp proxy service to become available
                    // ToDo note there must be a safer way to check for the extend service - this expects to always
                    // be able to see the proxy service on member 1
                    member.getServiceMBeanInfo("ExtendTcpProxyService", 1);
                }
            }
        });

        return schema;
    }


    protected abstract ClusterMemberSchema newClusterMemberSchema(int iPort);


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
