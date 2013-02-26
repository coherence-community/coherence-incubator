/*
 * File: CacheMappingTest.java
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for a {@link CacheMapping}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CacheMappingTest
{
    /**
     * Ensure that a new CacheMapping is built with the correct state.
     */
    @Test
    public void testCacheMappingContruction()
    {
        CacheMapping cacheMapping = new CacheMapping("dist-*", "DistributedScheme", null);

        assertTrue(cacheMapping.getCacheName().equals("dist-*"));
        assertTrue(cacheMapping.isForCacheName("dist-me"));
        assertTrue(cacheMapping.isForCacheName("dist-"));
        assertFalse(cacheMapping.isForCacheName("dist"));
        assertTrue(cacheMapping.getSchemeName().equals("DistributedScheme"));
        assertFalse(cacheMapping.hasEnrichment(String.class, "message"));
    }


    /**
     * Ensure that a CacheMapping decoration addition occurs as expected.
     */
    @Test
    public void testCacheMappingDecoration()
    {
        CacheMapping cacheMapping = new CacheMapping("dist-*", "DistributedScheme", null);

        assertFalse(cacheMapping.hasEnrichment(String.class, "welcome"));

        cacheMapping.addEnrichment(String.class, "welcome", "Hello World");

        assertTrue(cacheMapping.hasEnrichment(String.class, "welcome"));
        assertTrue(cacheMapping.getEnrichment(String.class, "welcome").equals("Hello World"));

        cacheMapping.addEnrichment(String.class, "welcome", "Gudday");
        cacheMapping.addEnrichment(String.class, "goodbye", "Cheers");

        assertTrue(cacheMapping.hasEnrichment(String.class, "welcome"));
        assertTrue(cacheMapping.getEnrichment(String.class, "welcome").equals("Gudday"));

        assertTrue(cacheMapping.hasEnrichment(String.class, "welcome"));
        assertTrue(cacheMapping.getEnrichment(String.class, "goodbye").equals("Cheers"));

        CacheMapping otherCacheMapping = new CacheMapping("repl-*", "ReplicatedScheme", null);

        otherCacheMapping.addEnrichment(String.class, "welcome", "Bonjour");

        assertTrue(otherCacheMapping.hasEnrichment(String.class, "welcome"));
        assertTrue(otherCacheMapping.getEnrichment(String.class, "welcome").equals("Bonjour"));

        cacheMapping.addEnrichmentsFrom(otherCacheMapping);

        assertTrue(cacheMapping.hasEnrichment(String.class, "welcome"));
        assertTrue(cacheMapping.getEnrichment(String.class, "welcome").equals("Bonjour"));

        assertTrue(cacheMapping.hasEnrichment(String.class, "goodbye"));
        assertTrue(cacheMapping.getEnrichment(String.class, "goodbye").equals("Cheers"));

    }
}
