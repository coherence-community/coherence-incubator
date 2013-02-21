/*
 * File: EventTypeFilterTest.java
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

package com.oracle.coherence.patterns.eventdistribution.filters;

import com.oracle.coherence.common.util.SimpleBinaryEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryUpdatedEvent;
import com.oracle.coherence.patterns.eventdistribution.filters.EventTypeFilter.EventType;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.util.Filter;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link EventTypeFilter} class.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventTypeFilterTest extends AbstractTest
{
    /**
     * Ensure that an empty {@link EventTypeFilter} evaluates to <code>false</code>.
     */
    @Test
    public void testEmptyFilter()
    {
        Filter filter = new EventTypeFilter();

        Assert.assertFalse(filter.evaluate(null));
    }


    /**
     * Ensure that an {@link EventTypeFilter} evaluates correctly with a single {@link EventType}.
     */
    @Test
    public void testSingleEventTypeFilter()
    {
        Filter filter = new EventTypeFilter(EventType.INSERTED);

        DistributableEntryEvent eventInserted = new DistributableEntryInsertedEvent("simple",
                                                                                    new DistributableEntry(new SimpleBinaryEntry("message",
            "gudday")));

        Assert.assertTrue(filter.evaluate(eventInserted));

        DistributableEntryEvent eventUpdated = new DistributableEntryUpdatedEvent("simple",
                                                                                  new DistributableEntry(new SimpleBinaryEntry("message",
            "gudday")));

        Assert.assertFalse(filter.evaluate(eventUpdated));

        DistributableEntryEvent eventRemoved = new DistributableEntryRemovedEvent("simple",
                                                                                  new DistributableEntry(new SimpleBinaryEntry("message",
            "gudday")));

        Assert.assertFalse(filter.evaluate(eventRemoved));
    }


    /**
     * Ensure that an {@link EventTypeFilter} evaluates correctly with a single {@link EventType}.
     */
    @Test
    public void testMultipleEventTypeFilter()
    {
        Filter filter = new EventTypeFilter(EventType.INSERTED, EventType.REMOVED);

        DistributableEntryEvent eventInserted = new DistributableEntryInsertedEvent("simple",
                                                                                    new DistributableEntry(new SimpleBinaryEntry("message",
            "gudday")));

        Assert.assertTrue(filter.evaluate(eventInserted));

        DistributableEntryEvent eventUpdated = new DistributableEntryUpdatedEvent("simple",
                                                                                  new DistributableEntry(new SimpleBinaryEntry("message",
            "gudday")));

        Assert.assertFalse(filter.evaluate(eventUpdated));

        DistributableEntryEvent eventRemoved = new DistributableEntryRemovedEvent("simple",
                                                                                  new DistributableEntry(new SimpleBinaryEntry("message",
            "gudday")));

        Assert.assertTrue(filter.evaluate(eventRemoved));
    }
}
