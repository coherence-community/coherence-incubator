/*
 * File: ChainedEventIteratorTransformerTest.java
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
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryUpdatedEvent;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * The {@link ChainedEventIteratorTransformerTest} is designed to exercise {@link ChainedEventIteratorTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ChainedEventIteratorTransformerTest
{
    /**
     * An {@link ArrayList} of {@link Event}s to transform for the test.
     */
    private ArrayList<Event> events;


    /**
     * Setup the test
     */
    @Before
    public void setup()
    {
        // create some random DistributableEntryEvents
        events = new ArrayList<Event>();

        Random random = new Random();

        for (int i = 0; i < 100; i++)
        {
            BinaryEntry             entry = new SimpleBinaryEntry(i, "Value " + i, "Original Value " + i);

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
     * Ensure that an empty chain does not transform events
     */
    @Test
    public void testEmptyChain()
    {
        ChainedEventIteratorTransformer transformer = new ChainedEventIteratorTransformer();

        Iterator<Event>                 iterator    = transformer.transform(events.iterator());

        int                             i           = 0;

        while (iterator.hasNext())
        {
            Assert.assertTrue(((DistributableEntryEvent) iterator.next()) == events.get(i++));
        }
    }


    /**
     * Ensure that we can filter events in a chain
     */
    @Test
    public void testFilterChain()
    {
        ChainedEventIteratorTransformer transformer =
            new ChainedEventIteratorTransformer(new FilteringEventIteratorTransformer(new LessFilter("getEntry.getKey",
                                                                                                     new Integer(10))), new FilteringEventIteratorTransformer(new GreaterFilter("getEntry.getKey",
            new Integer(5))));

        Iterator<Event> iterator = transformer.transform(events.iterator());

        int             i        = 6;

        while (iterator.hasNext())
        {
            Assert.assertTrue(((DistributableEntryEvent) iterator.next()) == events.get(i++));
        }

        Assert.assertEquals(10, i);
    }
}
