/*
 * File: CoalescingEventIteratorTransformer.java
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

package com.oracle.coherence.patterns.eventdistribution.transformers;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.util.ChainedIterator;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A {@link CoalescingEventIteratorTransformer} attempts to coalesce multiple {@link EntryEvent}s into a
 * reduced set of {@link EntryEvent}s in order to reduce the size of batches to distribute.
 * <p>
 * For example; the sequence of updates for a cache entry such as [(store,a,1), (store,a,2), (store,a,3)]
 * will be coalesced into the single operation sequence [(store,a,3)].
 * <p>
 * Note 1: It only makes sense to use {@link CoalescingEventIteratorTransformer} when the order of {@link EntryEvent}s distribution
 * is irrelevant.
 * <p>
 * Note 2: {@link EntryEvent}s such as deletes/removes are also coalesced.
 * <p>
 * For example; the sequence [(store,a,1), (remove,a), (store,a,2)] will be coalesced into the single operation
 * sequence [(store,a,2)].
 * <p>
 * Note 3: Non-{@link EntryEvent}s are preserved in the result
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoalescingEventIteratorTransformer implements EventIteratorTransformer
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public Iterator<Event> transform(Iterator<Event> events)
    {
        // an ordered hash map in which to coalesce EntryEvents
        LinkedHashMap<Object, Event> coalescedEntryEvents = new LinkedHashMap<Object, Event>();

        // an ordered list of non-EntryEvents
        ArrayList<Event> nonEntryEvents = new ArrayList<Event>();

        // coalesce the entry operations
        for (; events.hasNext(); )
        {
            Event event = events.next();

            if (event instanceof EntryEvent)
            {
                EntryEvent entryEvent = (EntryEvent<?>) event;

                coalescedEntryEvents.put(entryEvent.getEntry().getKey(), entryEvent);
            }
            else
            {
                nonEntryEvents.add(event);
            }
        }

        // return the iteration over the coalesced events
        return new ChainedIterator<Event>().addIterator(nonEntryEvents.iterator())
            .addIterator(coalescedEntryEvents.values().iterator());
    }
}
