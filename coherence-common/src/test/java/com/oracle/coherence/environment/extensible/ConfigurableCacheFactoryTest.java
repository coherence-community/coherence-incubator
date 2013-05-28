/*
 * File: ConfigurableCacheFactoryTest.java
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

import com.oracle.tools.junit.AbstractCoherenceTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheFactoryBuilder;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

/**
 * Functional Tests for Coherence Configurable Cache Factories.
 * <p>
 * Copyright (c) 2013. Jonathan Knight.
 *
 * @author Jonathan Knight
 */
public class ConfigurableCacheFactoryTest extends AbstractCoherenceTest
{
    /**
     * The ConfigurableCacheFactory for each test.
     */
    private ConfigurableCacheFactory m_configurableCacheFactory;


    /**
     * {@inheritDoc}
     */
    @Before
    @Override
    public void onBeforeEachTest()
    {
        super.onBeforeEachTest();
    }


    /**
     * {@inheritDoc}
     */
    @After
    @Override
    public void onAfterEachTest()
    {
        CacheFactory.getCacheFactoryBuilder().release(m_configurableCacheFactory);

        super.onAfterEachTest();
    }


    /**
     * Ensure we can create a custom scheme using a no-args constructor.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateClassSchemeWithNoParameters() throws Exception
    {
        ClassLoader loader = Base.getContextClassLoader();

        System.setProperty("scheme.name", "class-scheme-no-params");

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        NamedCache cache = m_configurableCacheFactory.ensureCache("test-cache", loader);

        assertThat(cache, is(instanceOf(MyCache.class)));
    }


    /**
     * Ensure we can create a custom scheme using a {cache-name} parameter
     * in the constructor.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateClassSchemeWithCacheNameMacroParameter() throws Exception
    {
        ClassLoader loader = Base.getContextClassLoader();

        System.setProperty("scheme.name", "class-scheme-cache-name-macro-param");

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        NamedCache cache = m_configurableCacheFactory.ensureCache("test-cache", loader);

        assertThat(cache, is(instanceOf(MyCache.class)));
        assertThat(cache.getCacheName(), is("test-cache"));
    }


    /**
     * Ensure we can create a custom scheme using a {class-loader} parameter
     * in the constructor.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateClassSchemeWithClassLoaderMacroParameter() throws Exception
    {
        ClassLoader loader     = Base.getContextClassLoader();
        Properties  properties = new Properties();

        System.setProperty("scheme.name", "class-scheme-with-class-loader-params");

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        MyCache cache = (MyCache) m_configurableCacheFactory.ensureCache("test-cache", loader);

        assertThat(cache, is(instanceOf(MyCache.class)));
        assertThat(cache.getClassLoader(), is(sameInstance(loader)));
    }


    /**
     * Ensure we can create a custom scheme using a {cache-ref} parameter
     * in the constructor.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateClassSchemeWithCacheRefMacroParameter() throws Exception
    {
        ClassLoader loader     = Base.getContextClassLoader();
        Properties  properties = new Properties();

        System.setProperty("scheme.name", "class-scheme-cache-ref-macro-param");

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        WrapperNamedCache cache = (WrapperNamedCache) m_configurableCacheFactory.ensureCache("test-cache", loader);

        assertThat(cache, is(instanceOf(MyCache.class)));
        assertThat(cache.getCacheName(), is("local-test-cache"));

        NamedCache underlying = (NamedCache) cache.getMap();

        assertThat(underlying.getCacheName(), is("local-test-cache"));
    }


    /**
     * Ensure we can create a custom scheme using a {cache-name} and {cache-ref}
     * parameter in the constructor.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateClassSchemeWithCacheNameAndCacheRefMacroParameter() throws Exception
    {
        ClassLoader loader     = Base.getContextClassLoader();
        Properties  properties = new Properties();

        System.setProperty("scheme.name", "class-scheme-cache-name-and-cache-ref-macro-param");

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        WrapperNamedCache cache = (WrapperNamedCache) m_configurableCacheFactory.ensureCache("test-cache", loader);

        assertThat(cache, is(instanceOf(MyCache.class)));
        assertThat(cache.getCacheName(), is("test-cache"));

        NamedCache underlying = (NamedCache) cache.getMap();

        assertThat(underlying.getCacheName(), is("local-test-cache"));
    }


    /**
     * Ensure we can create a custom scheme using a {cache-ref} that contains
     * a {cache-name} parameter in the constructor.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateClassSchemeWithParameterFromMapping() throws Exception
    {
        ClassLoader loader = Base.getContextClassLoader();

        System.setProperty("scheme.name", "class-scheme-cache-name-and-cache-ref-macro-param");

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        WrapperNamedCache cache = (WrapperNamedCache) m_configurableCacheFactory.ensureCache("test-two-cache", loader);

        assertThat(cache, is(instanceOf(MyCache.class)));
        assertThat(cache.getCacheName(), is("test-two-cache"));

        NamedCache underlying = (NamedCache) cache.getMap();

        assertThat(underlying.getCacheName(), is("local-underlying-cache"));
    }


    /**
     * Ensure that the correct {@link com.tangosol.net.NamedCache} is resolved by the {@link com.tangosol.net.CacheFactory}.
     *
     * @throws Exception
     */
    @Test
    public void testWrapperNamedCacheResolution() throws Exception
    {
        ClassLoader loader = Base.getContextClassLoader();

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        NamedCache cache = CacheFactory.getCache("dist-test");

        cache.put("Key-1", "Value-1");

        NamedCache cqc = CacheFactory.getCache("cqc-test");

        assertEquals(cqc.getCacheName(), "cqc-test");
    }


    /**
     * Ensure that the correct {@link com.tangosol.net.NamedCache} implementation is resolved by the {@link com.tangosol.net.CacheFactory}
     *
     * @throws Exception
     */
    @Test
    public void testNamedCacheImplementationResolution() throws Exception
    {
        ClassLoader loader = Base.getContextClassLoader();

        m_configurableCacheFactory = createConfigurableCacheFactory("test-cache-ref-cache-config.xml", loader);

        NamedCache                  cache   = CacheFactory.getCache("dist-test");
        WrapperContinuousQueryCache wrapper = (WrapperContinuousQueryCache) CacheFactory.getCache("cqc-test");
        ContinuousQueryCache        cqc     = (ContinuousQueryCache) wrapper.getMap();

        assertTrue(cqc.getCache() == cache);
    }


    /**
     * Create a ConfigurableCacheFactory to use in tests.
     *
     * @param configName - the name of the cache configuration to load
     * @param loader     - the CacheLoader to use to load the configuration
     * @return a ConfigurableCacheFactory to use for tests.
     */
    private ConfigurableCacheFactory createConfigurableCacheFactory(String      configName,
                                                                    ClassLoader loader)
    {
        XmlDocument xmlConfig = XmlHelper.loadFileOrResource(configName, "config", loader);

        XmlHelper.replaceSystemProperties(xmlConfig, "system-property");

        DefaultConfigurableCacheFactory ccf = new DefaultConfigurableCacheFactory(xmlConfig);

        CacheFactory.setConfigurableCacheFactory(ccf);

        return ccf;
    }
}
