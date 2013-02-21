/*
 * File: MultiplexingBackingMapListener.java
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
 * A {@link MultiplexingBackingMapListener} defines a simplified {@link com.tangosol.util.MapListener} that
 * is designed to be used to receive events from backing maps (ie: the underlying mechanism
 * often used by Coherence to store objects).
 * <p>
 * Backing {@link com.tangosol.util.MapListener}s are embeddable {@link com.tangosol.util.MapListener}s that are
 * injected into Coherence Cache members (storage-enabled) for the purpose of
 * handling events directly in-process of the primary partitions (of distributed schemes).
 * <p>
 * They are extremely useful for performing in-process processing of events within Coherence itself.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface MultiplexingBackingMapListener
{
    /**
     * Implement this method to handle call-backs when {@link MapEvent}s
     * occur on a Backing Map.
     * <p>
     * NOTE: The implementing typically has to be registered or configured
     * some where to recieve events.
     *
     * @param mapEvent A standard mapEvent
     * @param cause The underlying cause of the event
     */
    public void onBackingMapEvent(MapEvent mapEvent,
                                  Cause    cause);
}
