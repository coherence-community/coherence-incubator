/*
 * File: WrapperContinuousQueryCache.java
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

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.filter.AlwaysFilter;

/**
 * A {@link WrapperContinuousQueryCache} is an implementation of a {@link WrapperNamedCache}
 * that internally uses a {@link ContinuousQueryCache} for locally maintaining a cache of another cache.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 */
public class WrapperContinuousQueryCache extends WrapperNamedCache
{
    /**
     * Constructs a {@link WrapperContinuousQueryCache}
     *
     * @param wrappedCache  the {@link NamedCache} to wrap
     * @param cacheName     the name of the wrapped cache
     */
    public WrapperContinuousQueryCache(NamedCache wrappedCache,
                                       String     cacheName)
    {
        super(new ContinuousQueryCache(wrappedCache, AlwaysFilter.INSTANCE), cacheName);
    }
}
