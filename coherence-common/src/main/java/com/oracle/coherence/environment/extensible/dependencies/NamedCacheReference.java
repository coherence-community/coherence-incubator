/*
 * File: NamedCacheReference.java
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

package com.oracle.coherence.environment.extensible.dependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

/**
 * An implementation of a {@link DependencyReference} for a
 * Coherence{@link NamedCache}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class NamedCacheReference extends AbstractNamedDependencyReference
{
    /**
     * Standard Constructor (for use with {@link NamedCache}s).
     *
     * @param namedCache The {@link Service} for which to create a reference.
     */
    public NamedCacheReference(NamedCache namedCache)
    {
        this(namedCache.getCacheName());
    }


    /**
     * Standard Constructor.
     *
     * @param cacheName The name of the {@link NamedCache} to reference.
     */
    public NamedCacheReference(String cacheName)
    {
        super(cacheName);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isReferencing(Object object)
    {
        // if the object is a service that the cache uses, we're good!
        return object != null && object instanceof Service
               && CacheFactory.getCache(getName()).getCacheService().getInfo().getServiceName()
                   .equals(((Service) object).getInfo().getServiceName());
    }
}
