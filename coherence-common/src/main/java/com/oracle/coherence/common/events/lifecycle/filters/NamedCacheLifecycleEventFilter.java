/*
 * File: NamedCacheLifecycleEventFilter.java
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

package com.oracle.coherence.common.events.lifecycle.filters;

import com.oracle.coherence.common.events.lifecycle.NamedCacheLifecycleEvent;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;

/**
 * A {@link NamedCacheLifecycleEventFilter} may be used to filter {@link NamedCacheLifecycleEvent}s
 * for a specifically named {@link NamedCache}s.
 * <p>
 * NOTE 1: {@link NamedCacheLifecycleEventFilter}s are not serializable as they are only ever used locally.
 * <p>
 * NOTE 2: You may use Coherence cache name mapping wildcards for the specified named cache.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class NamedCacheLifecycleEventFilter implements Filter
{
    /**
     * The name of the {@link NamedCache} we're filtering.  May be a Coherence Cache Name wildcard.
     */
    private String cacheName;


    /**
     * Standard Constructor.
     *
     * @param cacheName The name of the cache we're looking for (may contain wildcards)
     */
    public NamedCacheLifecycleEventFilter(String cacheName)
    {
        this.cacheName = cacheName.trim();
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate(Object object)
    {
        if (object instanceof NamedCacheLifecycleEvent)
        {
            NamedCacheLifecycleEvent event = ((NamedCacheLifecycleEvent) object);

            if (cacheName.equals("*"))
            {
                // a * wildcard matches any cache name
                return true;
            }
            else if (cacheName.equals(event.getCacheName()))
            {
                // there's an exact match
                return true;
            }
            else
            {
                int pos = cacheName.indexOf("*");

                if (pos >= 0)
                {
                    // does the prefix (up to the wildcard) match exactly with the start of the cache name
                    String prefix = cacheName.substring(0, pos);

                    return event.getCacheName().startsWith(prefix);
                }
                else
                {
                    // no wildcard and thus no match
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
    }
}
