/*
 * File: NamedCacheLifecycleEvent.java
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

package com.oracle.coherence.common.events.lifecycle;

/**
 * <p>The {@link NamedCacheLifecycleEvent} represents {@link LifecycleEvent}s,
 * specifically for named caches.</p>
 *
 * @author Brian Oliver
 */
public abstract class NamedCacheLifecycleEvent extends AbstractLifecycleEvent<String>
{
    /**
     * <p>Standard Constructor.</p>
     *
     * @param cacheName The name of the cache for the {@link NamedCacheLifecycleEvent}.
     */
    public NamedCacheLifecycleEvent(String cacheName)
    {
        super(cacheName);
    }


    /**
     * <p>Returns the name of the cache about which this Event concerned.</p>
     *
     * @return The name of the cache
     */
    public String getCacheName()
    {
        return getSource();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("%s{cacheName=%s}", this.getClass().getName(), getCacheName());
    }
}
