/*
 * File: CacheRefMacroTest.java
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

package com.oracle.coherence.environment.extensible;

import com.oracle.tools.junit.AbstractTest;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link CacheRefMacroTest} tests the resolution of {cache-ref}s when using {@link ExtensibleEnvironment}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 */
public class CacheRefMacroTest extends AbstractTest
{
    /**
     * The {@link ConfigurableCacheFactory} for the test.
     */
    private ConfigurableCacheFactory ccf;
    private NamedCache               cache;


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
        ccf = (ConfigurableCacheFactory) new ExtensibleEnvironment("test-cache-ref-config.xml");
        CacheFactory.setCacheFactoryBuilder(null);
        CacheFactory.setConfigurableCacheFactory(ccf);

        cache = CacheFactory.getCache("dist-test");
        cache.put("Key-1", "Value-1");
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
     * Ensure that the correct {@link NamedCache} is resolved by the {@link CacheFactory}.
     *
     * @throws Exception
     */
    @Test
    public void testWrapperNamedCacheResolution() throws Exception
    {
        NamedCache cqc = CacheFactory.getCache("cqc-test");

        assertEquals(cqc.getCacheName(), "cqc-test");
    }


    /**
     * Ensure that the correct {@link NamedCache} implementation is resolved by the {@link CacheFactory}
     *
     * @throws Exception
     */
    @Test
    public void testNamedCacheImplementationResolution() throws Exception
    {
        WrapperContinuousQueryCache wrapper = (WrapperContinuousQueryCache) CacheFactory.getCache("cqc-test");
        ContinuousQueryCache        cqc     = (ContinuousQueryCache) wrapper.getMap();

        assertTrue(cqc.getCache() == cache);
    }
}
