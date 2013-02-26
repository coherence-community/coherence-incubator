/*
 * File: CoalecsingEventIteratorTransformerTest.java
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

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.util.SimpleBinaryEntry;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryUpdatedEvent;
import com.tangosol.util.BinaryEntry;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 * The {@link CoalecsingEventIteratorTransformerTest} is designed to exercise {@link CoalescingEventIteratorTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoalecsingEventIteratorTransformerTest
{
    /**
     * An {@link ArrayList} of {@link Event}s to transform for the test.
     */
    private ArrayList<Event> events;


    /**
     * Method description
     */
    @Before
    public void setup()
    {
        // create some random DistributableEntryEvents
        events = new ArrayList<Event>();

        Random random = new Random();

        for (int i = 0; i < 100; i++)
        {
            // generate a random key that we will coalesce
            int                     key   = random.nextInt(5);

            BinaryEntry             entry = new SimpleBinaryEntry(key, "Value " + i, "Original Value " + i);

            DistributableEntryEvent event = null;

            switch (random.nextInt(3))
            {
            case 0 :
                event = new DistributableEntryInsertedEvent("my-cache", new DistributableEntry(entry));
                break;

            case 1 :
                event = new DistributableEntryUpdatedEvent("my-cache", new DistributableEntry(entry));
                break;

            case 2 :
                event = new DistributableEntryRemovedEvent("my-cache", new DistributableEntry(entry));
                break;
            }

            if (event != null)
            {
                events.add(event);
            }
        }
    }


    /**
     * Ensure that we can coalesce events in a chain
     */
    @Test
    public void testCoalesceEvents()
    {
        EventIteratorTransformer transformer = new CoalescingEventIteratorTransformer();

        Iterator<Event>          iterator    = transformer.transform(events.iterator());

        Assert.assertTrue(iterator.hasNext());

        HashSet<Integer> keysSeen = new HashSet<Integer>();

        while (iterator.hasNext())
        {
            DistributableEntryEvent event = (DistributableEntryEvent) iterator.next();

            // we should not have seen this key before
            Assert.assertFalse(keysSeen.contains(event.getEntry().getKey()));

            // now add it
            keysSeen.add((Integer) event.getEntry().getKey());
        }
    }
}
