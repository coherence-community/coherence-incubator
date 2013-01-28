/*
 * File: ActiveActiveSumConflictResolver.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.pushreplication.examples.activeactive;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.EntryUpdatedEvent;

import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolution;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;

import com.tangosol.util.BinaryEntry;

/**
 * The {@link ActiveActiveSumConflictResolver} supports conflict resolution between two
 * clusters which are actively publishing to each other during a distributed
 * active-active replication.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Robert Hanckel
 */
public class ActiveActiveSumConflictResolver implements ConflictResolver
{
    /**
     * Standard Constructor.
     */
    public ActiveActiveSumConflictResolver()
    {
    }


    /**
     *  {@inheritDoc}
     */
    public ConflictResolution resolve(EntryEvent<DistributableEntry> entryEvent,
                                      BinaryEntry                    localEntry)
    {
        ConflictResolution resolution = new ConflictResolution();

        if (!(localEntry.isPresent()))
        {
            if (entryEvent instanceof EntryRemovedEvent)
            {
                // The source is trying to delete an entry that does not exist.
                // This is a noop, and we set conflict resolution to retain
                // the target entry.  (i.e. the entry will still not exist).

                resolution.useLocalValue();
            }
            else if (entryEvent instanceof EntryInsertedEvent)
            {
                // The source is inserting an entry that does not exist.
                // We set conflict resolution to store the in comming entry as is.

                resolution.useInComingValue();
            }
            else if (entryEvent instanceof EntryUpdatedEvent)
            {
                // The source is updating an entry that no longer exists at a target.
                // The only way this can happen is if an update and a delete
                // are crossing paths at the same time between source and target.
                // We let the delete take precedence, so we set conflict resolution
                // to retain the target entry, which is a noop.  (i.e. the entry will still not
                // exist.

                resolution.useLocalValue();
            }
        }
        else
        {
            if (entryEvent instanceof EntryRemovedEvent)
            {
                // A delete of an existing entry causes the conflict resolution to schedule a remove.
                resolution.remove();
            }
            else if (entryEvent instanceof EntryInsertedEvent || entryEvent instanceof EntryUpdatedEvent)
            {
                // An insert or update coming from a source site resolves the same way when
                // there is an existing value at the target.  (Typically an insert will find
                // an empty value at the target, but there is the potential for two inserts to happen
                // at the same time and cross paths, this case is treated like a normal update/update
                // conflict.
                ActiveActiveRunningSum tgtSum = (ActiveActiveRunningSum) localEntry.getValue();
                ActiveActiveRunningSum srcSum = (ActiveActiveRunningSum) entryEvent.getEntry().getValue();

                // Apply latest update value from source site to running sum on the target entry
                tgtSum.computeNewSum(srcSum.getLatestUpdateValue());

                resolution.useMergedValue(tgtSum);
            }
        }

        return resolution;
    }
}
