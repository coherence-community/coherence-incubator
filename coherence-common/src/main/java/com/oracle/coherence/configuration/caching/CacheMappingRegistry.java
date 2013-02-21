/*
 * File: CacheMappingRegistry.java
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

package com.oracle.coherence.configuration.caching;

import com.oracle.coherence.environment.Environment;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link CacheMappingRegistry} is a registry of known {@link CacheMapping}s.
 * <p>
 * To request the {@link CacheMappingRegistry} instance for an {@link Environment}, use the following;
 * <code>environment.getResource({@link CacheMappingRegistry}.class)</code>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CacheMappingRegistry
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(CacheMappingRegistry.class.getName());

    /**
     * The set of {@link CacheMapping}s.
     */
    private LinkedHashMap<String, CacheMapping> cacheMappings;


    /**
     * Standard Constructor.
     */
    public CacheMappingRegistry()
    {
        this.cacheMappings = new LinkedHashMap<String, CacheMapping>();
    }


    /**
     * Adds and/or Overrides the {@link CacheMapping} for a specific named cache in the {@link CacheMappingRegistry}.
     *
     * @param cacheMapping The {@link CacheMapping} to add/override
     *
     * @return <code>true</code> When the {@link CacheMapping} was added successfully, <code>false</code> if it was not added.
     */
    public boolean addCacheMapping(CacheMapping cacheMapping)
    {
        // ensure that an existing mapping with exactly the same name has not already been defined.
        CacheMapping existingCacheMapping = cacheMappings.get(cacheMapping.getCacheName());

        if (existingCacheMapping != null)
        {
            logger.log(Level.CONFIG,
                       "WARNING: The cache mapping for the <cache-name>{0}</cache-name> has been duplicated/overridden."
                       + "The new cache mapping declaration for <cache-name>{0}</cache-name> will be ignored, but decorations will be retained and merged.",
                       cacheMapping.getCacheName());

            existingCacheMapping.addEnrichmentsFrom(cacheMapping);

            return false;
        }
        else
        {
            // warn if the new cache mapping matches an existing definition
            existingCacheMapping = findCacheMapping(cacheMapping.getCacheName());

            if (existingCacheMapping != null)
            {
                logger.log(Level.CONFIG,
                           "WARNING: The cache mapping for the <cache-name>{0}</cache-name> matches an existing declaration called <cache-name>{1}</cache-name>."
                           + "While the cache mapping for <cache-name>{0}</cache-name> will be accepted, it may never be used due to the existing overriding declaration of <cache-name>{1}</cache-name>.",
                           new Object[] {cacheMapping.getCacheName(), existingCacheMapping.getCacheName()});
            }

            // add the cache mapping
            cacheMappings.put(cacheMapping.getCacheName(), cacheMapping);

            return true;
        }
    }


    /**
     * Attempts to find the {@link CacheMapping} that matches the specified cache name.  The matching algorithm
     * first attempts to find an exact match of a {@link CacheMapping} with the provided cache name.  Should that fail,
     * all of the currently registered wild-carded {@link CacheMapping}s are searched to find a match (in the order
     * in which they were registered).
     *
     * @param cacheName The cache name
     *
     * @return <code>null</code> if a {@link CacheMapping} could not be located for the specified cache name.
     */
    public CacheMapping findCacheMapping(String cacheName)
    {
        CacheMapping cacheMapping;

        // is there an exact match for the provided cacheName?
        // ie: is the cacheName explicitly defined as a CacheMapping without wildcards?
        if (cacheMappings.containsKey(cacheName))
        {
            cacheMapping = cacheMappings.get(cacheName);
        }
        else
        {
            // attempt to find the most specific (ie: longest) wildcard defined CacheMapping that matches the cacheName
            cacheMapping = null;

            for (Iterator<CacheMapping> iterator = cacheMappings.values().iterator(); iterator.hasNext(); )
            {
                CacheMapping mapping = iterator.next();

                if (mapping.isForCacheName(cacheName))
                {
                    if (cacheMapping == null)
                    {
                        cacheMapping = mapping;
                    }
                    else if (mapping.getCacheName().length() > cacheMapping.getCacheName().length())
                    {
                        cacheMapping = mapping;
                    }
                }
            }
        }

        return cacheMapping;
    }
}
