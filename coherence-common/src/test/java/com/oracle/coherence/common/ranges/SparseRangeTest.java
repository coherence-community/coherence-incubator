/*
 * File: SparseRangeTest.java
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

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link SparseRange} implementation.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
public class SparseRangeTest
{
    /**
     * Tests empty {@link SparseRange}.
     */
    @Test
    public void testSparseRange()
    {
        // Test default constructor
        Range range = new SparseRange();

        assertTrue(range != null);
        assertTrue(range.isEmpty());
        assertTrue(!range.isSingleton());
        assertTrue(range != null);
    }


    /**
     * Test sparse range.
     */
    @Test
    public void testSparseRangeLong()
    {
        // Test constructor which takes a value.
        Range range = new SparseRange(5);

        assertTrue(range != null);
        assertTrue(range.isEmpty());
        assertTrue(!range.isSingleton());
        assertTrue(range.getFrom() == 5);
        assertTrue(range.getTo() == 4);
        assertTrue(range.size() == 0);
    }


    /**
     * Test merging of ranges.
     */
    @Test
    public void testSparseRangeContiguousRangeContiguousRange()
    {
        // Test of constructor which takes two ContiguousRanges
        Range range = new SparseRange(new ContiguousRange(-1, -1), new ContiguousRange(2, 2));

        assertTrue(range.size() == 2);
        assertTrue(range.getFrom() == -1);
        assertTrue(range.getTo() == 2);
        assertTrue(!range.isEmpty());
        assertTrue(!range.isSingleton());

        // Test the case when the first range is empty
        range = new SparseRange(new ContiguousRange(-1, -2), new ContiguousRange(2, 2));

        assertTrue(range.size() == 1);
        assertTrue(range.getFrom() == 2);
        assertTrue(range.getTo() == 2);
        assertTrue(!range.isEmpty());
        assertTrue(range.isSingleton());

        // Test the case when the second range is empty

        range = new SparseRange(new ContiguousRange(-1, 1), new ContiguousRange(2, 1));

        assertTrue(range.size() == 3);
        assertTrue(range.getFrom() == -1);
        assertTrue(range.getTo() == 1);
        assertTrue(!range.isEmpty());
        assertTrue(!range.isSingleton());

        // Test the case when the both ranges are empty

        range = new SparseRange(new ContiguousRange(-1, -2), new ContiguousRange(2, 1));

        assertTrue(range.size() == 0);
        assertTrue(range.getFrom() == -1);
        assertTrue(range.isEmpty());
        assertTrue(!range.isSingleton());
    }


    /**
     * Tests contains.
     */
    @Test
    public void testContains()
    {
        // Test of contains method
        Range range = new SparseRange(new ContiguousRange(-1, 1), new ContiguousRange(3, 4));

        assertTrue(range.contains(-1));
        assertTrue(range.contains(0));
        assertTrue(range.contains(1));
        assertTrue(!range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(4));
        assertTrue(!range.contains(5));
    }


    /**
     * Test adjacency and intersection.
     */
    @Test
    public void testIsAdjacentAndIntersects()
    {
        // Testing a sparse range with a single digit gap
        Range range       = new SparseRange(new ContiguousRange(-1, 1), new ContiguousRange(3, 4));
        Range inthemiddle = new ContiguousRange(2, 2);

        assertTrue(range.isAdjacent(inthemiddle));    // Verify with Brian
        assertTrue(!range.intersects(inthemiddle));

        // Testing the case where a range is overlapping a contiguous range in
        // the middle
        Range overlapmiddle = new ContiguousRange(2, 3);

        assertTrue(range.contains(3));
        assertTrue(!range.contains(2));
        assertTrue(range.isAdjacent(overlapmiddle));
        assertTrue(range.intersects(overlapmiddle));

        // Testing the case where a range is overlapping the first contiguous
        // range partially
        Range firstoverlap = new ContiguousRange(-2, 0);

        assertTrue(!range.isAdjacent(firstoverlap));
        assertTrue(range.intersects(firstoverlap));

        // Testing the case where a range is fully overlapping the sparserange
        Range fulloverlap = new ContiguousRange(-5, 15);

        assertTrue(!range.isAdjacent(fulloverlap));
        assertTrue(range.intersects(fulloverlap));

        // Testing the case where a before range is not overlapping and is not
        // adjacent
        Range waybefore = new ContiguousRange(-5, -3);

        assertTrue(!range.isAdjacent(waybefore));
        assertTrue(!range.intersects(waybefore));

        // Testing the case where a before range is not overlapping and is
        // adjacent
        Range before = new ContiguousRange(-5, -2);

        assertTrue(range.isAdjacent(before));
        assertTrue(!range.intersects(before));

        // Testing the case where an after range is not overlapping and is
        // adjacent
        Range after = new ContiguousRange(5, 6);

        assertTrue(range.isAdjacent(after));
        assertTrue(!range.intersects(after));

        // Testing the case where an after range is not overlapping and is not
        // adjacent
        Range wayafter = new ContiguousRange(6, 7);

        assertTrue(!range.isAdjacent(wayafter));
        assertTrue(!range.intersects(wayafter));

        Range sparseintersecting = new SparseRange(new ContiguousRange(0, 2), new ContiguousRange(5, 6));

        assertTrue(range.isAdjacent(sparseintersecting));
        assertTrue(range.intersects(sparseintersecting));
    }


    /**
     * Test additions.
     */
    @Test
    public void testAdd()
    {
        // Testing the addition of a value to the range
        Range range = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));

        assertTrue(range.size() == 4);

        Range newrange = range.add(3);

        assertTrue(newrange.size() == 5);
    }


    /**
     * Test removals.
     */
    @Test
    public void testRemove()
    {
        Range range = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));

        assertTrue(range.size() == 4);
        range = range.remove(2);
        assertTrue(range.size() == 3);

        // testing whether removing a non-existing element changes the size of
        // the range, which it shouldn't
        range = range.remove(0);
        assertTrue(range.size() == 3);
    }


    /**
     * Test unions.
     */
    @Test
    public void testUnion()
    {
        Range range = new SparseRange(new ContiguousRange(4, 4), new ContiguousRange(2, 2));

        assertTrue(range.size() == 2);
        assertTrue(range.getFrom() == 2);
        assertTrue(range.getTo() == 4);

        range = range.union(range.union(new ContiguousRange(1, 1)));

        assertTrue(range.size() == 3);
        assertTrue(range.getFrom() == 1);
        assertTrue(range.getTo() == 4);

        // Test making a union of two sparse ranges
        Range sparserange = new SparseRange(new ContiguousRange(1, 1), new ContiguousRange(3, 3));
        Range othersparse = new SparseRange(new ContiguousRange(2, 2), new ContiguousRange(4, 4));
        Range unionrange  = sparserange.union(othersparse);

        assertTrue(unionrange.size() == 4);
        assertTrue(unionrange.getFrom() == 1);
        assertTrue(unionrange.getTo() == 4);
    }


    /**
     * Test iterator.
     */
    @Test
    public void testIterator()
    {
        Range range = new SparseRange(new ContiguousRange(4, 4), new ContiguousRange(2, 2));

        assertTrue(range.size() == 2);
        assertTrue(range.getFrom() == 2);
        assertTrue(range.getTo() == 4);

        Iterator<Long> iterator = range.iterator();

        assertTrue(iterator.hasNext() && iterator.next() == 2);
        assertTrue(iterator.hasNext() && iterator.next() == 4);
        assertTrue(!iterator.hasNext());

        range = range.union(new ContiguousRange(3, 3));

        assertTrue(range.size() == 3);
        assertTrue(range.getFrom() == 2);
        assertTrue(range.getTo() == 4);

        iterator = range.iterator();
        assertTrue(iterator.hasNext() && iterator.next() == 2);
        assertTrue(iterator.hasNext() && iterator.next() == 3);
        assertTrue(iterator.hasNext() && iterator.next() == 4);
        assertTrue(!iterator.hasNext());

        // test what happens if we make a union with ourselves
        range.union(range);

        assertTrue(range.size() == 3);
        assertTrue(range.getFrom() == 2);
        assertTrue(range.getTo() == 4);

        iterator = range.iterator();
        assertTrue(iterator.hasNext() && iterator.next() == 2);
        assertTrue(iterator.hasNext() && iterator.next() == 3);
        assertTrue(iterator.hasNext() && iterator.next() == 4);
        assertTrue(!iterator.hasNext());
    }


    /**
     * Test equals.
     */
    @Test
    public void testEqualsObject()
    {
        // Two sparse ranges with same contiguous ranges are equal
        Range range      = new SparseRange(new ContiguousRange(4, 4), new ContiguousRange(2, 2));
        Range otherrange = new SparseRange(new ContiguousRange(2, 2), new ContiguousRange(4, 4));

        assertTrue(range.equals(otherrange));

        // Now test two sparse ranges but with different gaps
        // They should not compare equal
        range = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));    // no

        // 3
        otherrange = new SparseRange(new ContiguousRange(1, 3), new ContiguousRange(5, 5));    // no

        // 4
        assertTrue(!range.equals(otherrange));

        // Test whether a Sparserange and a contiguous range with the same range
        // are equal
        range = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(3, 4));

        // These should merge
        // to become one ContiguousRange of (1,4)
        otherrange = new ContiguousRange(1, 4);
        assertTrue(range.equals(otherrange));

        // Test whether a Sparserange and a contiguous range wiht the same range
        // are equal
        range      = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));
        otherrange = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(3, 4));
        otherrange = otherrange.add(5);
        otherrange = otherrange.remove(3);

        // Now these sparse ranges should consist of equal contiguous ranges
        assertTrue(range.equals(otherrange));

        // Now we test what happens if we are equal with a contiguous range and
        // then add a non-adjacent value
        range      = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(3, 5));
        otherrange = new ContiguousRange(1, 5);
        assertTrue(range.equals(otherrange));
        range = range.add(7);    // Now we really have a sparse range
        assertTrue(!range.equals(otherrange));

        // Now lets test a sparse range that we unionize with a partially
        // overlapping adjacent contiguous range
        range = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));
        range = range.union(new ContiguousRange(2, 4));    // this range is both

        // intersecting the
        // second and adjacent
        // to the first
        otherrange = new ContiguousRange(1, 5);
        assertTrue(range.equals(otherrange));

        // Now lets test a sparse range that we unionize with a partially
        // overlapping adjacent sparse range
        range      = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));
        range      = range.union(new ContiguousRange(7, 9));

        otherrange = new SparseRange(new ContiguousRange(0, 1), new ContiguousRange(6, 8));
        otherrange = otherrange.union(new ContiguousRange(3, 3));
        otherrange = otherrange.union(range);    // this range is both intersecting

        // the second and adjacent to the
        // first
        range = new ContiguousRange(0, 9);
        assertTrue(range.equals(otherrange));

        // Now lets test a sparse range that we unionize with an overlapping
        // adjacent sparse range with different
        // number of contiguous ranges
        range      = new SparseRange(new ContiguousRange(1, 2), new ContiguousRange(4, 5));
        range      = range.union(new ContiguousRange(7, 9));

        otherrange = new SparseRange(new ContiguousRange(0, 3), new ContiguousRange(6, 6));
        otherrange = otherrange.union(range);    // this range is both intersecting

        // the second and adjacent to the
        // first
        range = new ContiguousRange(0, 9);
        assertTrue(range.equals(otherrange));
    }


    /**
     * Test toString.
     */
    @Test
    public void testToString()
    {
        // Test that the toString output format stays consistent
        Range  range = new SparseRange(new ContiguousRange(1, 1), new ContiguousRange(4, 4));
        String str   = range.toString();

        assertTrue(str.equals("SparseRange[[ContiguousRange[1], ContiguousRange[4]]]"));

        // Test the empty range
        range = new SparseRange();
        str   = range.toString();
        assertTrue(str.equals("SparseRange[]"));
    }


    /**
     * Tests Pof serialization.
     *
     * @throws IOException if there is an IO problem.
     */
    @Test
    public void testReadExternalPofReader() throws IOException
    {
        PofReader reader = mock(PofReader.class);

        when(reader.readLong(0)).thenReturn(1L);
        when(reader.readCollection(anyInt(), anyCollection())).thenReturn(null);

        SparseRange range = new SparseRange();

        range.readExternal(reader);

        assertTrue(range.getFrom() == 1);
        assertTrue(range.isEmpty());
    }


    /**
     * Tests Pof serialization.
     *
     * @throws IOException if there is an IO problem.
     */
    @Test
    public void testWriteExternalPofWriter() throws IOException
    {
        PofWriter       writer      = mock(PofWriter.class);
        ContiguousRange firstrange  = mock(ContiguousRange.class);
        ContiguousRange secondrange = mock(ContiguousRange.class);

        when(firstrange.isEmpty()).thenReturn(false);
        when(secondrange.isEmpty()).thenReturn(false);
        when(firstrange.isAdjacent(secondrange)).thenReturn(false);
        when(firstrange.intersects(secondrange)).thenReturn(false);
        when(secondrange.compareTo(firstrange)).thenReturn(1);
        when(firstrange.getFrom()).thenReturn(1L);

        SparseRange range = new SparseRange(firstrange, secondrange);

        range.writeExternal(writer);

        verify(writer).writeLong(0, 1L);    // Now write firstrange.getFrom()
        verify(writer).writeCollection(anyInt(), anyCollection());
    }
}
