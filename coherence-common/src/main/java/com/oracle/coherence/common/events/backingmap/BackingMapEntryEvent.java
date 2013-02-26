/*
 * File: BackingMapEntryEvent.java
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

import com.oracle.coherence.common.events.EntryEvent;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.BinaryEntry;

/**
 * An {@link BackingMapEntryEvent} is a specialized {@link EntryEvent} representing an Event that has occurred on
 * a Coherence Backing Map {@link BinaryEntry}.  Such events always occur in a context
 * (as a {@link BackingMapManagerContext}).
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface BackingMapEntryEvent extends EntryEvent<BinaryEntry>
{
    /**
     * Returns the {@link BackingMapManagerContext} in which the {@link BackingMapEntryEvent} occurred.
     *
     * @return {@link BackingMapManagerContext}
     */
    public BackingMapManagerContext getContext();
}
