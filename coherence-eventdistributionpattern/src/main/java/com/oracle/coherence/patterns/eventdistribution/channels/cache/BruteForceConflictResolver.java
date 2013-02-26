/*
 * File: BruteForceConflictResolver.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.EntryUpdatedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.BinaryEntry;

/**
 * The {@link BruteForceConflictResolver} is an implementation of
 * a {@link ConflictResolver} that simply replays the specified {@link EntryEvent} on the {@link BinaryEntry}.
 * <p>
 * It's called "brute force" as it completely ignores the current state the existing {@link BinaryEntry}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class BruteForceConflictResolver implements ConflictResolver
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public BruteForceConflictResolver()
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    public ConflictResolution resolve(EntryEvent<DistributableEntry> entryEvent,
                                      BinaryEntry                    binaryEntry)
    {
        ConflictResolution resolution = new ConflictResolution();

        if (entryEvent instanceof EntryRemovedEvent)
        {
            if (binaryEntry.isPresent())
            {
                resolution.remove();
            }
            else
            {
                resolution.useLocalValue();
            }
        }
        else if (entryEvent instanceof EntryInsertedEvent || entryEvent instanceof EntryUpdatedEvent)
        {
            resolution.useInComingValue();
        }
        else
        {
            // TODO: unknown type of EntryEvent (log it and don't change anything)
            resolution.useLocalValue();
        }

        return resolution;
    }
}
