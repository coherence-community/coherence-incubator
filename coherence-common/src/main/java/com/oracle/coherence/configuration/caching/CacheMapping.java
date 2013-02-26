/*
 * File: CacheMapping.java
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

import com.oracle.coherence.common.util.AbstractEnrichmentSupport;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;

/**
 * A {@link CacheMapping} represents the configuration information for an individual mapping from a named cache
 * to a cache scheme, including any specific {@link com.oracle.coherence.configuration.parameters.Parameter}s (represented as a {@link ParameterProvider})
 * for the said mapping.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CacheMapping extends AbstractEnrichmentSupport
{
    /**
     * The name of the cache.
     */
    private String cacheName;

    /**
     * The name of the  scheme to which the cache is mapped.
     */
    private String schemeName;

    /**
     * The {@link ParameterProvider} containing the {@link com.oracle.coherence.configuration.parameters.Parameter}s defined by the {@link CacheMapping}.
     */
    private ParameterProvider parameterProvider;


    /**
     * Standard Constructor.
     *
     * @param cacheName The name of the cache
     * @param schemeName The scheme for the cache
     * @param parameterProvider The {@link ParameterProvider} containing the {@link com.oracle.coherence.configuration.parameters.Parameter}s defined for the mapping.
     */
    public CacheMapping(String            cacheName,
                        String            schemeName,
                        ParameterProvider parameterProvider)
    {
        super();

        this.cacheName  = cacheName.trim();
        this.schemeName = schemeName.trim();
        this.parameterProvider = parameterProvider == null
                                 ? SystemPropertyParameterProvider.INSTANCE : parameterProvider;
    }


    /**
     * Returns the name of the cache for the {@link CacheMapping}.
     *
     * @return A String representing the cache name for the {@link CacheMapping}
     */
    public String getCacheName()
    {
        return cacheName;
    }


    /**
     * Determines if the {@link CacheMapping} is for (matches) the specified cache name.
     *
     * @param cacheName The cacheName to check for a match
     * @return <code>true</code> if the {@link CacheMapping} is for the specified cache name,
     *         <code>false</code> otherwise.
     */
    public boolean isForCacheName(String cacheName)
    {
        if (getCacheName().equals("*"))
        {
            return true;
        }
        else if (getCacheName().contains("*"))
        {
            String pattern = getCacheName().substring(0, getCacheName().indexOf("*"));

            return cacheName.startsWith(pattern);
        }
        else
        {
            return false;
        }
    }


    /**
     * Returns the scheme name for the {@link CacheMapping}.
     *
     * @return A String representing the scheme name for the {@link CacheMapping}
     */
    public String getSchemeName()
    {
        return schemeName;
    }


    /**
     * Returns the {@link ParameterProvider} that contains the {@link com.oracle.coherence.configuration.parameters.Parameter}s
     * defined for the {@link CacheMapping}.
     *
     * @return A {@link ParameterProvider}
     */
    public ParameterProvider getParameterProvider()
    {
        return parameterProvider;
    }
}
