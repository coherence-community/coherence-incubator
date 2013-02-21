/*
 * File: CacheMappingRegistryTest.java
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

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for a {@link CacheMappingRegistry}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CacheMappingRegistryTest
{
    /**
     * Ensure that a new {@link CacheMappingRegistry} is built with the correct state
     */
    @Test
    public void testCacheMappingRegistryConstruction()
    {
        CacheMappingRegistry registry = new CacheMappingRegistry();

        assertTrue(registry.findCacheMapping("unknown") == null);
    }


    /**
     * Ensure {@link CacheMappingRegistry} find mapping returns the correct mappings
     */
    @Test
    public void testCacheMappingRegistryFind()
    {
        CacheMappingRegistry registry = new CacheMappingRegistry();

        assertTrue(registry.findCacheMapping("example") == null);

        CacheMapping distCacheMapping = new CacheMapping("dist-*", "DistributedScheme", null);

        registry.addCacheMapping(distCacheMapping);

        assertTrue(registry.findCacheMapping("dist-*") == distCacheMapping);
        assertTrue(registry.findCacheMapping("dist-me") == distCacheMapping);
        assertTrue(registry.findCacheMapping("dist-") == distCacheMapping);
        assertTrue(registry.findCacheMapping("unknown") == null);

        CacheMapping dCacheMapping = new CacheMapping("d*", "DistributedScheme", null);

        registry.addCacheMapping(dCacheMapping);

        assertTrue(registry.findCacheMapping("dist-*") == distCacheMapping);
        assertTrue(registry.findCacheMapping("dist-me") == distCacheMapping);
        assertTrue(registry.findCacheMapping("dist-") == distCacheMapping);
        assertTrue(registry.findCacheMapping("d*") == dCacheMapping);
        assertTrue(registry.findCacheMapping("d") == dCacheMapping);
        assertTrue(registry.findCacheMapping("dist") == dCacheMapping);
        assertTrue(registry.findCacheMapping("unknown") == null);

        CacheMapping genericCacheMapping = new CacheMapping("*", "DistributedScheme", null);

        registry.addCacheMapping(genericCacheMapping);

        assertTrue(registry.findCacheMapping("dist-*") == distCacheMapping);
        assertTrue(registry.findCacheMapping("dist-me") == distCacheMapping);
        assertTrue(registry.findCacheMapping("dist-") == distCacheMapping);
        assertTrue(registry.findCacheMapping("d*") == dCacheMapping);
        assertTrue(registry.findCacheMapping("d") == dCacheMapping);
        assertTrue(registry.findCacheMapping("dist") == dCacheMapping);
        assertTrue(registry.findCacheMapping("unknown") == genericCacheMapping);
    }
}
