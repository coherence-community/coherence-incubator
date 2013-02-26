/*
 * File: NullBackingMapListener.java
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
import com.tangosol.util.MapListener;

/**
 * An implementation of a backing {@link MapListener} that does
 * nothing, including *not* deserializing the {@link MapEvent}s.
 * <p>
 * Instances of this class are often used in cache configurations
 * that require a Backing {@link MapListener}, but don't require
 * any actions to be performed.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class NullBackingMapListener implements MapListener
{
    /**
     * Standard Constructor.
     *
     * @param backingMapManagerContext The BackingMapManagerContext associated with this listener
     */
    public NullBackingMapListener(BackingMapManagerContext backingMapManagerContext)
    {
        // SKIP: as a null implementation we don't do anything here ;)
    }


    /**
     * {@inheritDoc}
     */
    public void entryDeleted(MapEvent mapEvent)
    {
        // SKIP: as a null implementation we don't do anything here ;)
    }


    /**
     * {@inheritDoc}
     */
    public void entryInserted(MapEvent mapEvent)
    {
        // SKIP: as a null implementation we don't do anything here ;)
    }


    /**
     * {@inheritDoc}
     */
    public void entryUpdated(MapEvent mapEvent)
    {
        // SKIP: as a null implementation we don't do anything here ;)
    }
}
