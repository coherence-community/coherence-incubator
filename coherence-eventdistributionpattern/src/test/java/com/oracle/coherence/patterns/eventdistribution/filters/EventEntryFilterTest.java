/*
 * File: EventEntryFilterTest.java
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

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.util.SimpleBinaryEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.util.filter.EqualsFilter;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Map.Entry;

/**
 * Unit tests for the {@link EventEntryFilter} class.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventEntryFilterTest extends AbstractTest
{
    /**
     * Ensure that we can extract an {@link Entry} from an {@link EntryEvent}.
     */
    @Test
    public void testEventEntryFilter()
    {
        SimpleBinaryEntry entry = new SimpleBinaryEntry("message", "gudday");

        DistributableEntryInsertedEvent event = new DistributableEntryInsertedEvent("my-cache",
                                                                                    new DistributableEntry(entry));

        EventEntryFilter filter = new EventEntryFilter(new EqualsFilter("toString", "gudday"));

        Assert.assertTrue(filter.evaluate(event));
    }
}
