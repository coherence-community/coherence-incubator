/*
 * File: MockitoNamedCacheFactory.java
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

package com.oracle.coherence.common.util;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlElement;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * This is a utility class derived from an Easymock variant of this class in main Coherence.
 * <p>
 * This utility class to ease the creation of mock {@link NamedCache} instances
 * using Mockito. This utility can be used like this:
 * <p>
 * For general use with {@link CacheFactory}:
 * <pre>
 *   MockitoNamedCacheFactory.activateMockitoCacheFactoryBuilder();
 *   NamedCache mockCache = CacheFactory.getCache("mock");
 *   // mockCache is backed by Mockito
 *   ...
 * </pre>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Patrick Peralta
 * @author Christer Fahlgren
 */
public class MockitoNamedCacheFactory implements ConfigurableCacheFactory
{
    /**
     * The previously created {@link CacheFactoryBuilder}.
     */
    private static CacheFactoryBuilder m_previousCacheFactoryBuilder;

    // ----- member variables -----------------------------------------------

    /**
     * Map of mock NamedCache instances keyed by cache name.
     */
    private Map<String, NamedCache> m_mockNamedCacheMap = new HashMap<String, NamedCache>();


    /**
     * {@inheritDoc}
     */
    public XmlElement getConfig()
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void setConfig(XmlElement xmlConfig)
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public synchronized NamedCache ensureCache(String      sCacheName,
                                               ClassLoader loader)
    {
        NamedCache mockNamedCache = m_mockNamedCacheMap.get(sCacheName);

        if (mockNamedCache == null)
        {
            mockNamedCache = mock(NamedCache.class);
            m_mockNamedCacheMap.put(sCacheName, mockNamedCache);
        }

        return mockNamedCache;
    }


    /**
     * {@inheritDoc}
     */
    public void releaseCache(NamedCache cache)
    {
    }


    /**
     * {@inheritDoc}
     */
    public void destroyCache(NamedCache cache)
    {
    }


    /**
     * {@inheritDoc}
     */
    public Service ensureService(String sServiceName)
    {
        throw new UnsupportedOperationException();
    }


    // ----- helper methods -------------------------------------------------

    /**
     * Helper method to configure {@link CacheFactory} to return a
     * MockNamedCache.
     */
    public static void activateMockitoCacheFactoryBuilder()
    {
        CacheFactory.setConfigurableCacheFactory(null);

        m_previousCacheFactoryBuilder = CacheFactory.getCacheFactoryBuilder();
        CacheFactory.setCacheFactoryBuilder(new MockitoNamedCacheFactoryBuilder());
    }


    /**
     * Method description
     */
    public static void deactivateMockitoCacheFactoryBuilder()
    {
        CacheFactory.setConfigurableCacheFactory(null);

        CacheFactory.setCacheFactoryBuilder(m_previousCacheFactoryBuilder);
    }


    // ----- inner classes --------------------------------------------------

    /**
     * Stub implementation of CacheFactoryBuilder that returns an instance
     * of StubCacheFactory.
     */
    public static class MockitoNamedCacheFactoryBuilder implements CacheFactoryBuilder
    {
        /**
         * The {@link ConfigurableCacheFactory} to use.
         */
        private final ConfigurableCacheFactory m_cacheFactory = new MockitoNamedCacheFactory();


        /**
         * {@inheritDoc}
         */
        public ConfigurableCacheFactory getConfigurableCacheFactory(ClassLoader loader)
        {
            return m_cacheFactory;
        }


        /**
         * {@inheritDoc}
         */
        public ConfigurableCacheFactory getConfigurableCacheFactory(String      sConfigURI,
                                                                    ClassLoader loader)
        {
            return m_cacheFactory;
        }


        /**
         * {@inheritDoc}
         */
        public void setCacheConfiguration(ClassLoader loader,
                                          XmlElement  xmlConfig)
        {
            throw new UnsupportedOperationException();
        }


        /**
         * {@inheritDoc}
         */
        public void setCacheConfiguration(String      sConfigURI,
                                          ClassLoader loader,
                                          XmlElement  xmlConfig)
        {
            throw new UnsupportedOperationException();
        }


        /**
         * {@inheritDoc}
         */
        public void releaseAll(ClassLoader loader)
        {
            throw new UnsupportedOperationException();
        }


        /**
         * {@inheritDoc}
         */
        public void release(ConfigurableCacheFactory factory)
        {
            throw new UnsupportedOperationException();
        }
    }
}
