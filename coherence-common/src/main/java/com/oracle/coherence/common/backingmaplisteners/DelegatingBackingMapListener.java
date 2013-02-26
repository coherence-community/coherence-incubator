/*
 * File: DelegatingBackingMapListener.java
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

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.MapEvent;

/**
 * A {@link DelegatingBackingMapListener} will delegate backing map events to
 * the object on which the event occurred (assuming the said object
 * implements {@link LifecycleAwareCacheEntry}).
 * <p>
 * For {@link MapEvent#ENTRY_INSERTED} events, the event will be delegated to the
 * {@link MapEvent#getNewValue()}.
 * <p>
 * For {@link MapEvent#ENTRY_UPDATED} events, the event will be delegated to the
 * {@link MapEvent#getNewValue()}.
 * <p>
 * For {@link MapEvent#ENTRY_DELETED} events, the event will be delegated to the
 * {@link MapEvent#getOldValue()}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @deprecated Use {@link com.oracle.coherence.common.events.dispatching.listeners.DelegatingBackingMapListener} instead.
 *
 * @author Brian Oliver
 */
@Deprecated
public class DelegatingBackingMapListener extends AbstractMultiplexingBackingMapListener
{
    /**
     * Standard Constructor.
     *
     * @param backingMapManagerContext The BackingMapManagerContext associated with this listener
     */
    public DelegatingBackingMapListener(BackingMapManagerContext backingMapManagerContext)
    {
        super(backingMapManagerContext);
    }


    /**
     * {@inheritDoc}
     */
    public void onBackingMapEvent(MapEvent mapEvent,
                                  Cause    cause)
    {
        if ((mapEvent.getId() == MapEvent.ENTRY_INSERTED || mapEvent.getId() == MapEvent.ENTRY_UPDATED)
            && mapEvent.getNewValue() instanceof LifecycleAwareCacheEntry)
        {
            ((LifecycleAwareCacheEntry) mapEvent.getNewValue()).onCacheEntryLifecycleEvent(mapEvent, cause);

        }
        else if (mapEvent.getId() == MapEvent.ENTRY_DELETED
                 && mapEvent.getOldValue() instanceof LifecycleAwareCacheEntry)
        {
            ((LifecycleAwareCacheEntry) mapEvent.getOldValue()).onCacheEntryLifecycleEvent(mapEvent, cause);
        }
    }
}
