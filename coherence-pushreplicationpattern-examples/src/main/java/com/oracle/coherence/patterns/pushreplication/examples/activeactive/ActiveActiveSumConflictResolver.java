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
 * A ConflictResolver that will resolve and merge {@link ActiveActiveSum} values
 * together from different sites.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
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

        if (localEntry.isPresent())
        {
            if (entryEvent instanceof EntryRemovedEvent)
            {
                resolution.remove();
            }
            else if (entryEvent instanceof EntryInsertedEvent || entryEvent instanceof EntryUpdatedEvent)
            {
                ActiveActiveSum tgtSum = (ActiveActiveSum) localEntry.getValue();
                ActiveActiveSum srcSum = (ActiveActiveSum) entryEvent.getEntry().getValue();
                String site = entryEvent.getEntry().getClusterMetaInfo().getUniqueName();

                //contribute the srcSum site value to the tgtSum
                boolean changed = tgtSum.setValue(site, srcSum.getValue(site));

                if (changed) {
                    resolution.useMergedValue(tgtSum);
                } else {
                    resolution.useLocalValue();
                }
            }
        }
        else
        {
            if (entryEvent instanceof EntryRemovedEvent)
            {
                resolution.useLocalValue();
            }
            else if (entryEvent instanceof EntryInsertedEvent || entryEvent instanceof EntryUpdatedEvent)
            {
                resolution.useInComingValue();
            }
        }

        return resolution;
    }
}
