/*
 * File: LifecycleAwareCacheEntry.java
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

package com.oracle.coherence.common.backingmaplisteners;

import com.tangosol.util.MapEvent;

/**
 * A {@link LifecycleAwareCacheEntry} is a cache entry that is capable of handling
 * backing map events that occur on itself.
 * <p>
 * In order for a {@link LifecycleAwareCacheEntry} to receive events, the underlying
 * cache storing the said {@link LifecycleAwareCacheEntry}s must be configured to use
 * a {@link DelegatingBackingMapListener}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @deprecated Use {@link com.oracle.coherence.common.events.processing.LifecycleAwareEntry} instead.
 *
 * @author Brian Oliver
 */
@Deprecated
public interface LifecycleAwareCacheEntry
{
    /**
     * Implement this method to handle life-cycle events on a cache entry
     * (issued by a {@link DelegatingBackingMapListener}).
     *
     * @param mapEvent A standard mapEvent
     * @param cause The underlying cause of the event
     */
    public void onCacheEntryLifecycleEvent(MapEvent mapEvent,
                                           Cause    cause);
}
