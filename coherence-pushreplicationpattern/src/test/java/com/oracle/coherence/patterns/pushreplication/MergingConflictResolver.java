/*
 * File: MergingConflictResolver.java
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

package com.oracle.coherence.patterns.pushreplication;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolution;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.BinaryEntry;

/**
 * A {@link ConflictResolver} that merges {@link PartitionSet}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class MergingConflictResolver implements ConflictResolver
{
    /**
     * {@inheritDocs}
     */
    @Override
    public ConflictResolution resolve(EntryEvent<DistributableEntry> entryEvent,
                                      BinaryEntry                    binaryEntry)
    {
        ConflictResolution resolution = new ConflictResolution();

        if (binaryEntry.isPresent())
        {
            if (entryEvent instanceof EntryRemovedEvent)
            {
                resolution.remove();
            }
            else
            {
                PartitionSet setIncoming = (PartitionSet) entryEvent.getEntry().getValue();
                PartitionSet setLocal    = (PartitionSet) binaryEntry.getValue();
                PartitionSet setResult = new PartitionSet(Math.max(setIncoming.getPartitionCount(),
                                                                   setLocal.getPartitionCount()));

                setResult.add(setLocal);
                setResult.add(setIncoming);
                resolution.useMergedValue(setResult);
            }
        }
        else
        {
            resolution.useInComingValue();
        }

        return resolution;
    }
}
