/*
 * File: CachingCacheStore.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.CacheStore;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link CachingCacheStore} is a {@link CacheStore} implementation that
 * writes entries to a {@link NamedCache}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 */
public class CachingCacheStore implements CacheStore
{
    /**
     * The name of the cache to which to write entries.
     */
    private String cacheName;

    /**
     * The {@link NamedCache} to which to write entries.
     */
    private NamedCache namedCache;


    /**
     * Standard Constructor
     */
    public CachingCacheStore(String cacheName)
    {
        this.cacheName = cacheName;
    }


    /**
     * Ensure we can access the {@link NamedCache}.
     *
     * @return A {@link NamedCache}
     */
    public NamedCache ensureCache()
    {
        if (namedCache == null)
        {
            namedCache = CacheFactory.getCache(cacheName);
        }

        return namedCache;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object load(Object key)
    {
        return ensureCache().get(key);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Map loadAll(Collection keys)
    {
        return ensureCache().getAll(keys);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void erase(Object key)
    {
        ensureCache().remove(key);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void eraseAll(Collection keys)
    {
        Iterator   iter  = keys.iterator();
        NamedCache cache = ensureCache();

        while (iter.hasNext())
        {
            cache.remove(iter.next());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void store(Object key,
                      Object value)
    {
        ensureCache().put(key, value);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void storeAll(Map map)
    {
        ensureCache().putAll(map);
    }
}
