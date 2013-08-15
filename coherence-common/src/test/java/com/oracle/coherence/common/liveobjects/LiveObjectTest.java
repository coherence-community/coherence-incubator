/*
 * File: LiveObjectTest.java
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

package com.oracle.coherence.common.liveobjects;

import com.oracle.tools.junit.AbstractTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;

import com.tangosol.net.NamedCache;

import com.tangosol.net.events.Event;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ResourceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.tools.deferred.DeferredAssert.assertThat;

import static com.oracle.tools.deferred.DeferredHelper.deferred;
import static com.oracle.tools.deferred.DeferredHelper.eventually;

import static org.hamcrest.core.Is.is;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Functional Test for {@link LiveObject}s.
 * <p>
 * Copyright (c) 2012. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class LiveObjectTest extends AbstractTest
{
    private static final String INSERTS = "INSERTS";
    private static final String UPDATES = "UPDATES";
    private static final String REMOVES = "REMOVES";

    /**
     * The {@link ConfigurableCacheFactory} for the test.
     */
    private ExtensibleConfigurableCacheFactory m_ccf;
    private NamedCache                         m_cacheLiveObjects;
    private ResourceRegistry                   m_resourceRegistry;


    /**
     * Setup for each test.
     */
    @Before
    public void setup()
    {
        // we only want to run locally
        System.setProperty("tangosol.coherence.localhost", "127.0.0.1");
        System.setProperty("tangosol.coherence.ttl", "0");

        // establish the CCF for the test
        m_ccf =
            new ExtensibleConfigurableCacheFactory(DependenciesHelper.newInstance("coherence-common-cache-config.xml"));
        CacheFactory.setCacheFactoryBuilder(null);
        CacheFactory.setConfigurableCacheFactory(m_ccf);

        m_resourceRegistry = m_ccf.getResourceRegistry();

        m_cacheLiveObjects = CacheFactory.getCache("coherence.live.objects.distributed");

        m_resourceRegistry.registerResource(AtomicLong.class, INSERTS, new AtomicLong());
        m_resourceRegistry.registerResource(AtomicLong.class, REMOVES, new AtomicLong());
        m_resourceRegistry.registerResource(AtomicLong.class, UPDATES, new AtomicLong());
    }


    /**
     * Cleanup after each test.
     */
    @After
    public void cleanup()
    {
        CacheFactory.shutdown();
    }


    /**
     * Ensure LiveObjects are inserted, updated and removed as expected.
     */
    @Test
    public void testLiveObjectInsertUpdateRemove()
    {
        assertEquals(0, m_cacheLiveObjects.size());
        assertEquals(0, m_resourceRegistry.getResource(AtomicLong.class, INSERTS).get());
        assertEquals(0, m_resourceRegistry.getResource(AtomicLong.class, UPDATES).get());
        assertEquals(0, m_resourceRegistry.getResource(AtomicLong.class, REMOVES).get());

        m_cacheLiveObjects.put("alo", new AnnotatedLiveObject());

        assertEquals(1, m_cacheLiveObjects.size());
        assertThat(eventually(deferred(m_resourceRegistry.getResource(AtomicLong.class, INSERTS))), is(2L));
        assertEquals(0, m_resourceRegistry.getResource(AtomicLong.class, UPDATES).get());
        assertEquals(0, m_resourceRegistry.getResource(AtomicLong.class, REMOVES).get());

        m_cacheLiveObjects.put("alo", new AnnotatedLiveObject());

        assertEquals(1, m_cacheLiveObjects.size());
        assertThat(eventually(deferred(m_resourceRegistry.getResource(AtomicLong.class, INSERTS))), is(2L));
        assertThat(eventually(deferred(m_resourceRegistry.getResource(AtomicLong.class, UPDATES))), is(2L));
        assertEquals(0, m_resourceRegistry.getResource(AtomicLong.class, REMOVES).get());

        m_cacheLiveObjects.remove("alo");

        assertEquals(0, m_cacheLiveObjects.size());
        assertThat(eventually(deferred(m_resourceRegistry.getResource(AtomicLong.class, INSERTS))), is(2L));
        assertThat(eventually(deferred(m_resourceRegistry.getResource(AtomicLong.class, UPDATES))), is(2L));
        assertThat(eventually(deferred(m_resourceRegistry.getResource(AtomicLong.class, REMOVES))), is(2L));
    }


    /**
     * An {@link AnnotatedLiveObject}.
     */
    @LiveObject
    @SuppressWarnings("serial")
    public static class AnnotatedLiveObject implements Serializable
    {
        /**
         * Constructs an {@link AnnotatedLiveObject}.
         */
        public AnnotatedLiveObject()
        {
            // required for serialization
        }


        /**
         * Handle when the {@link AnnotatedLiveObject} is inserted.
         *
         * @param binaryEntry  the {@link BinaryEntry} on which the event occurred
         */
        @OnInserted
        @OnInserting
        public void onInsert(BinaryEntry binaryEntry)
        {
            ResourceRegistry registry =
                ((ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory()).getResourceRegistry();

            registry.getResource(AtomicLong.class, LiveObjectTest.INSERTS).incrementAndGet();
        }


        /**
         * Handle when the {@link AnnotatedLiveObject} is updated.
         *
         * @param binaryEntry  the {@link BinaryEntry} on which the event occurred
         */
        @OnUpdated
        @OnUpdating
        public void onUpdate(BinaryEntry binaryEntry)
        {
            ResourceRegistry registry =
                ((ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory()).getResourceRegistry();

            registry.getResource(AtomicLong.class, LiveObjectTest.UPDATES).incrementAndGet();
        }


        /**
         * Handle when the {@link AnnotatedLiveObject} is removed.
         *
         * @param binaryEntry  the {@link BinaryEntry} on which the event occurred
         */
        @OnRemoved
        @OnRemoving
        public void onRemove(BinaryEntry binaryEntry)
        {
            ResourceRegistry registry =
                ((ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory()).getResourceRegistry();

            registry.getResource(AtomicLong.class, LiveObjectTest.REMOVES).incrementAndGet();
        }
    }
}
