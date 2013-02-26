/*
 * File: EntryOptimizingEventTransformerTest.java
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

import com.oracle.coherence.common.util.SimpleBinaryEntry;
import com.oracle.coherence.patterns.eventdistribution.EventTransformer;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryUpdatedEvent;
import com.tangosol.util.BinaryEntry;
import junit.framework.Assert;
import org.junit.Test;

/**
 * The {@link EntryOptimizingEventTransformerTest} is designed to exercise {@link EntryOptimizingEventTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EntryOptimizingEventTransformerTest
{
    /**
     * Ensure that we can optimize insert events
     */
    @Test
    public void testOptimizingEntryInsertedEvent()
    {
        EventTransformer        transformer = new EntryOptimizingEventTransformer();

        BinaryEntry             entry       = new SimpleBinaryEntry("greeting", "hello world", "gudday world");

        DistributableEntryEvent event = new DistributableEntryInsertedEvent("my-cache", new DistributableEntry(entry));

        transformer.transform(event);

        Assert.assertEquals(event.getEntry().getKey(), "greeting");
        Assert.assertEquals(event.getEntry().getValue(), "hello world");
        Assert.assertEquals(event.getEntry().getOriginalBinaryValue(), null);
    }


    /**
     * Ensure that we can optimize update events
     */
    @Test
    public void testOptimizingEntryUpdatedEvent()
    {
        EventTransformer        transformer = new EntryOptimizingEventTransformer();

        BinaryEntry             entry       = new SimpleBinaryEntry("greeting", "hello world", "gudday world");

        DistributableEntryEvent event = new DistributableEntryUpdatedEvent("my-cache", new DistributableEntry(entry));

        transformer.transform(event);

        Assert.assertEquals(event.getEntry().getKey(), "greeting");
        Assert.assertEquals(event.getEntry().getValue(), "hello world");
        Assert.assertEquals(event.getEntry().getOriginalBinaryValue(), null);
    }


    /**
     * Ensure that we can optimize remove events
     */
    @Test
    public void testOptimizingEntryRemovedEvent()
    {
        EventTransformer        transformer = new EntryOptimizingEventTransformer();

        BinaryEntry             entry       = new SimpleBinaryEntry("greeting", null, "gudday world");

        DistributableEntryEvent event = new DistributableEntryRemovedEvent("my-cache", new DistributableEntry(entry));

        transformer.transform(event);

        Assert.assertEquals(event.getEntry().getKey(), "greeting");
        Assert.assertEquals(event.getEntry().getValue(), null);
        Assert.assertEquals(event.getEntry().getOriginalBinaryValue(), null);
    }


    /**
     * Ensure that we can optimize no events
     */
    @Test
    public void testOptimizingNullEvent()
    {
        EventTransformer transformer = new EntryOptimizingEventTransformer();

        transformer.transform(null);
    }
}
