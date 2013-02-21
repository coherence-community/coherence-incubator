/*
 * File: ContiguousRangeTest.java
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ContiguousRange} implementation.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
public class ContiguousRangeTest
{
    /**
     * Tests an empty range.
     */
    @Test
    public void testContiguousRangeLongLong()
    {
        Range range = new ContiguousRange(5);

        assertTrue(range.getFrom() == 5);
        assertTrue(range.isEmpty());
        assertFalse(range.isSingleton());
    }


    /**
     * Tests an empty range.
     */
    @Test
    public void testContiguousRange()
    {
        Range range = new ContiguousRange();

        assertTrue(range.getFrom() == 0);
        assertTrue(range.isEmpty());
        assertFalse(range.isSingleton());
    }


    /**
     * Tests additions.
     */
    @Test
    public void testAdd()
    {
        Range range = new ContiguousRange(1, 5);

        // Just add one number that is adjacent
        range = range.add(6);
        assertTrue(range.size() == 6);

        // Just add one number that is adjacent
        range = range.add(0);
        assertTrue(range.size() == 7);

        // Just add one number that already exists
        range = range.add(0);
        assertTrue(range.size() == 7);

        // Add another one that is not adjacent
        range = range.add(8);
        assertTrue(range.size() == 8);
        assertTrue(range instanceof SparseRange);

    }


    /**
     * Tests unions.
     */
    @Test
    public void testUnion()
    {
        Range range = new ContiguousRange(1, 5);

        assertTrue(range.size() == 5);

        // make union with empty range
        range = range.union(new ContiguousRange());
        assertTrue(range.size() == 5);

        // make union with non adjacent range
        Range other = new ContiguousRange(7, 8);

        range = range.union(other);
        assertTrue(range.size() == 7);
        assertTrue(range instanceof SparseRange);
        assertTrue(!range.contains(6));

        // Make union with other sparserange
        range = new ContiguousRange(1, 5);
        other = new SparseRange(new ContiguousRange(7, 8), new ContiguousRange(9, 10));
        range = range.union(other);
        assertTrue(range.size() == 9);
        assertTrue(range instanceof SparseRange);

    }


    /**
     * Test compareTo.
     */
    @Test
    public void testCompareTo()
    {
        ContiguousRange range = new ContiguousRange(1, 5);
        ContiguousRange other = new ContiguousRange(6, 10);

        assertTrue(range.compareTo(other) == -1);

        other = new ContiguousRange(-5, 0);
        assertTrue(range.compareTo(other) == 1);

        other = new ContiguousRange(1, 5);
        assertTrue(range.compareTo(other) == 0);

        other = new ContiguousRange(1, 3);

        try
        {
            range.compareTo(other);
            assertTrue(false);
        }
        catch (NotComparableRuntimeException e)
        {
            assertTrue(true);
        }

    }


    /**
     * Test PofWriter.
     *
     * @throws IOException if an IO error occurs.
     */
    @Test
    public void testWriteExternalPofWriter() throws IOException
    {
        PofWriter       writer = mock(PofWriter.class);

        ContiguousRange range  = new ContiguousRange(0, 1);

        range.writeExternal(writer);

        verify(writer).writeLong(0, 0L);
        verify(writer).writeLong(1, 1L);
    }


    /**
     * Test PofReader.
     *
     * @throws IOException if there is an IO error.
     */
    @Test
    public void testReadExternalPofReader() throws IOException
    {
        PofReader reader = mock(PofReader.class);

        when(reader.readLong(0)).thenReturn(0L);
        when(reader.readLong(1)).thenReturn(5L);

        ContiguousRange range = new ContiguousRange();

        range.readExternal(reader);

        assertTrue(range.getFrom() == 0);
        assertTrue(range.getTo() == 5);
    }
}
