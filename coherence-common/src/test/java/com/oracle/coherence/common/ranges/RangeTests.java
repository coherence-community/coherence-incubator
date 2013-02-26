/*
 * File: RangeTests.java
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

package com.oracle.coherence.common.ranges;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link Range} implementations.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver, Christer Fahlgren
 */
public class RangeTests
{
    /**
     * Tests union of ranges.
     */
    @Test
    public void testUnion()
    {
        Range range = new SparseRange(new ContiguousRange(4, 4), new ContiguousRange(2, 2));

        assertTrue(range.size() == 2);
        assertTrue(range.getFrom() == 2);
        assertTrue(range.getTo() == 4);

        Iterator<Long> iterator = range.iterator();

        assertTrue(iterator.hasNext() && iterator.next() == 2);
        assertTrue(iterator.hasNext() && iterator.next() == 4);
        assertTrue(!iterator.hasNext());

        range = range.union(range.union(new ContiguousRange(1, 1)));

        assertTrue(range.size() == 3);
        assertTrue(range.getFrom() == 1);
        assertTrue(range.getTo() == 4);

        iterator = range.iterator();
        assertTrue(iterator.hasNext() && iterator.next() == 1);
        assertTrue(iterator.hasNext() && iterator.next() == 2);
        assertTrue(iterator.hasNext() && iterator.next() == 4);
        assertTrue(!iterator.hasNext());
    }


    /**
     * Tests remove.
     */
    @Test
    public void testRemove()
    {
        Range range = new SparseRange(new ContiguousRange(4, 4), new ContiguousRange(2, 2));

        range = range.remove(2);

        assertTrue(range.size() == 1);
        assertTrue(range.getFrom() == 4);
        assertTrue(range.getTo() == 4);

        Iterator<Long> iterator = range.iterator();

        assertTrue(iterator.hasNext() && iterator.next() == 4);
        assertTrue(!iterator.hasNext());

        range = range.remove(4);
        assertTrue(range.size() == 0);
        assertTrue(range.isEmpty());
    }
}
