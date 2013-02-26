/*
 * File: BackingMapEntryUpdatedEvent.java
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

package com.oracle.coherence.common.events.backingmap;

import com.oracle.coherence.common.events.EntryUpdatedEvent;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.BinaryEntry;

import java.util.Map.Entry;

/**
 * An implementation of {@link EntryUpdatedEvent} with {@link BackingMapEntryEvent} support.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class BackingMapEntryUpdatedEvent extends AbstractBackingMapEntryEvent implements EntryUpdatedEvent<BinaryEntry>
{
    /**
     * The original {@link Entry} before it was updated.
     */
    private EventEntry originalEntry;


    /**
     * Standard Constructor.
     *
     * @param backingMapManagerContext The BackingMapManagerContext associated with this event
     * @param cacheName                The name of the cache where this event was triggered
     * @param key                      The key associated with this event
     * @param originalValue            The original value for this entry
     * @param newValue                 The new value for this entry
     */
    public BackingMapEntryUpdatedEvent(BackingMapManagerContext backingMapManagerContext,
                                       String                   cacheName,
                                       Object                   key,
                                       Object                   originalValue,
                                       Object                   newValue)
    {
        super(backingMapManagerContext, cacheName, key, newValue);
        this.originalEntry = new EventEntry(key, originalValue);
    }


    /**
     * {@inheritDoc}
     */
    public BinaryEntry getOriginalEntry()
    {
        return originalEntry;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s{cacheName=%s, originalEntry=%s, newEntry=%s}",
                             getClass().getName(),
                             getCacheName(),
                             getOriginalEntry(),
                             getEntry());
    }
}
