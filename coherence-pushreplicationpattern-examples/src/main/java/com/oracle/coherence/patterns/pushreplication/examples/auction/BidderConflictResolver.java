/*
 * File: BidderConflictResolver.java
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

package com.oracle.coherence.patterns.pushreplication.examples.auction;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.EntryUpdatedEvent;

import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolution;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;

import com.tangosol.util.BinaryEntry;

/**
 * A {@link BidderConflictResolver} provides conflict resolution for the auction example, that of which
 * uses multi-way replication between multiple clusters.
 * <p>
 * Conflicts are resolved by always choosing the highest price of a bid. If a bid arrives from another cluster and it
 * is higher than the local bid, the arriving bid becomes the new highest price. If arriving bid is lower than the local
 * bid, the arriving bid is ignored dropped (and the local entry is not updated as it's the highest).
 * <p>
 * When the arriving and local bids are the same, the customerIds are used to resolve the conflict
 * (the highest customerId wins).  Not really fair we know, but it's just an example.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Robert Hanckel
 * @author Brian Oliver
 */
public class BidderConflictResolver implements ConflictResolver
{
    /**
     * {@inheritDoc}
     */
    @Override
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
                BiddingItem localBiddingItem    = (BiddingItem) localEntry.getValue();
                BiddingItem incomingBiddingItem = (BiddingItem) entryEvent.getEntry().getValue();

                if (incomingBiddingItem.hasCustomerReference())
                {
                    if (localBiddingItem.merge(incomingBiddingItem))
                    {
                        resolution.useMergedValue(localBiddingItem);
                    }
                    else
                    {
                        resolution.useLocalValue();
                    }
                }
                else
                {
                    resolution.useInComingValue();
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
