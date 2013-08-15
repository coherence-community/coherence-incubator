/*
 * File: AbstractMessagingTest.java
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

import com.oracle.coherence.patterns.messaging.DefaultMessagingSession;
import com.oracle.coherence.patterns.messaging.MessagingSession;

import com.oracle.tools.junit.AbstractCoherenceTest;

import com.tangosol.net.CacheFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

/**
 * The base class for all Messaging Pattern Functional Tests.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractMessagingTest extends AbstractCoherenceTest
{
    /**
     * The MessagingSession to be used by all tests.
     */
    private MessagingSession m_messagingSession;


    /**
     * Acquire the MessagingSession to use for the tests.
     *
     * @return the MessagingSession
     */
    public MessagingSession getMessagingSession()
    {
        return m_messagingSession;
    }


    /**
     * {@inheritDoc}
     */
    @Before
    @Override
    public void onBeforeEachTest()
    {
        super.onBeforeEachTest();

        System.setProperty("tangosol.coherence.cacheconfig", "coherence-messagingpattern-test-cache-config.xml");
        System.setProperty("tangosol.pof.config", "coherence-messagingpattern-test-pof-config.xml");

        // create a shared MessagingSession for the tests
        m_messagingSession = DefaultMessagingSession.getInstance();
    }


    /**
     * {@inheritDoc}
     */
    @After
    @Override
    public void onAfterEachTest()
    {
        // NOTE: We deliberately avoid cleaning up as these test depend on
        // Coherence to stay running!
    }


    /**
     * Method description
     */
    @AfterClass
    public static void onAfterTestsInClass()
    {
        // shutdown the cluster after each class
        CacheFactory.shutdown();
    }
}
