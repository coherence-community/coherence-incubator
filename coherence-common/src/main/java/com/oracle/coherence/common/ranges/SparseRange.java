/*
 * File: SparseRange.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A {@link SparseRange} is a useful data structure for representing
 * an immutable range of increasing long integer values.
 * <p>
 * Internally a {@link SparseRange} is an ordered non-intersecting set
 * of sub-{@link ContiguousRange}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SparseRange implements Range, ExternalizableLite, PortableObject, Iterable<Long>

{
    /**
     * The starting point for the {@link Range}.
     */
    private long m_lFrom;

    /**
     * The collection of values in the {@link Range} (in order)
     *
     * This may be <code>null</code> if the {@link Range} is empty.
     */
    private TreeSet<ContiguousRange> m_oSubRanges;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public SparseRange()
    {
        m_oSubRanges = new TreeSet<ContiguousRange>();
    }


    /**
     * Package Level Constructor for an empty {@link SparseRange}
     * commencing at a specified value.
     *
     * @param from the start of the range
     */
    SparseRange(long from)
    {
        m_lFrom      = from;
        m_oSubRanges = new TreeSet<ContiguousRange>();
    }


    /**
     * Private Constructor to create a {@link SparseRange} based on
     * a on-empty {@link TreeSet} of {@link ContiguousRange}s.
     *
     * @param contiguousRanges the {@link TreeSet} to base this {@link SparseRange} on
     */
    private SparseRange(TreeSet<ContiguousRange> contiguousRanges)
    {
        assert !contiguousRanges.isEmpty();
        m_oSubRanges = contiguousRanges;
        m_lFrom      = m_oSubRanges.first().getFrom();
    }


    /**
     * Package Level Constructor based on two non-intersecting
     * {@link ContiguousRange}s.
     *
     * @param contiguousRange1 the first {@link ContiguousRange} to use
     * @param contiguousRange2 the second {@link ContiguousRange} to use
     */
    SparseRange(ContiguousRange contiguousRange1,
                ContiguousRange contiguousRange2)
    {
        if (contiguousRange1.isEmpty() && contiguousRange2.isEmpty())
        {
            m_lFrom      = Math.min(contiguousRange1.getFrom(), contiguousRange2.getFrom());
            m_oSubRanges = new TreeSet<ContiguousRange>();
        }
        else
        {
            if (contiguousRange1.isEmpty())
            {
                m_lFrom      = contiguousRange2.getFrom();
                m_oSubRanges = new TreeSet<ContiguousRange>();
                m_oSubRanges.add(contiguousRange2);
            }
            else
            {
                if (contiguousRange2.isEmpty())
                {
                    m_lFrom      = contiguousRange1.getFrom();
                    m_oSubRanges = new TreeSet<ContiguousRange>();
                    m_oSubRanges.add(contiguousRange1);
                }
                else
                {
                    m_oSubRanges = new TreeSet<ContiguousRange>();

                    // If the two ranges we are adding are adjacent - we should make a union out of them
                    if (contiguousRange1.isAdjacent(contiguousRange2) || contiguousRange1.intersects(contiguousRange2))
                    {
                        m_oSubRanges.add((ContiguousRange) contiguousRange1.union(contiguousRange2));
                    }
                    else
                    {
                        m_oSubRanges.add(contiguousRange1);
                        m_oSubRanges.add(contiguousRange2);
                    }

                    m_lFrom = m_oSubRanges.first().getFrom();
                }
            }
        }
    }


    /**
    * {@inheritDoc}
    */
    public long getFrom()
    {
        return isEmpty() ? m_lFrom : m_oSubRanges.first().getFrom();
    }


    /**
    * {@inheritDoc}
    */
    public long getTo()
    {
        return isEmpty() ? m_lFrom - 1 : m_oSubRanges.last().getTo();
    }


    /**
    * {@inheritDoc}
    */
    public long size()
    {
        long size = 0;

        for (ContiguousRange range : m_oSubRanges)
        {
            size = size + range.size();
        }

        return size;
    }


    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
    {
        return m_oSubRanges.isEmpty();
    }


    /**
    * {@inheritDoc}
    */
    public boolean isSingleton()
    {
        return m_oSubRanges.size() == 1 && m_oSubRanges.first().isSingleton();
    }


    /**
    * {@inheritDoc}
    */
    public boolean contains(long value)
    {
        for (ContiguousRange range : m_oSubRanges)
        {
            if (range.contains(value))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Test whether a {@link Range}ge is adjacent to any of the subranges.
     *
     * @param other the other {@link Range} to test against
     *
     * @return whether a {@link Range}e is adjacent to any of the subranges
     */
    private boolean testAdjacency(Range other)
    {
        for (ContiguousRange range : m_oSubRanges)
        {
            if (other.isAdjacent(range))
            {
                return true;
            }
        }

        return false;
    }


    /**
    * {@inheritDoc}
    */
    public boolean isAdjacent(Range other)
    {
        return isEmpty() || other.isEmpty() || other instanceof InfiniteRange || testAdjacency(other);
    }


    /**
    * {@inheritDoc}
    */
    public boolean intersects(Range other)
    {
        return isEmpty() || other.isEmpty() || other instanceof InfiniteRange || testIntersection(other);
    }


    /**
     * Tests the intersection of a SparseRange with a ContigousRange and another SparseRange.
     *
     * @param other is the other Range
     *
     * @return true if intersecting
     */
    private boolean testIntersection(Range other)
    {
        if (other instanceof ContiguousRange)
        {
            return (contains(other.getFrom()) || contains(other.getTo()) || other.contains(getFrom())
                    || other.contains(getTo()));
        }
        else
        {
            if (other instanceof SparseRange)
            {
                for (ContiguousRange range : m_oSubRanges)
                {
                    if (other.intersects(range))
                    {
                        return true;
                    }
                }

                return false;
            }
            else
            {
                return false;
            }
        }
    }


    /**
    * {@inheritDoc}
    */
    public Range add(long value)
    {
        return union(new ContiguousRange(value, value));
    }


    /**
    * {@inheritDoc}
    */
    public Range remove(long value)
    {
        TreeSet<ContiguousRange> resultingSubRanges = new TreeSet<ContiguousRange>();

        for (ContiguousRange range : m_oSubRanges)
        {
            if (range.contains(value))
            {
                Range resultingRange = range.remove(value);

                if (resultingRange.isEmpty())
                {
                    // nothing to do here... as the resulting range is empty
                }
                else
                {
                    if (resultingRange instanceof ContiguousRange)
                    {
                        resultingSubRanges.add((ContiguousRange) resultingRange);
                    }
                    else
                    {
                        resultingSubRanges.add(((SparseRange) resultingRange).m_oSubRanges.first());
                        resultingSubRanges.add(((SparseRange) resultingRange).m_oSubRanges.last());
                    }
                }
            }
            else
            {
                resultingSubRanges.add(range);
            }
        }

        if (resultingSubRanges.isEmpty())
        {
            return new ContiguousRange(value + 1);
        }
        else
        {
            if (resultingSubRanges.size() == 1)
            {
                return resultingSubRanges.first();
            }
            else
            {
                return new SparseRange(resultingSubRanges);
            }
        }
    }


    /**
    * {@inheritDoc}
    */
    public Range union(Range other)
    {
        if (other.isEmpty())
        {
            return this;
        }
        else
        {
            if (isEmpty())
            {
                return other;
            }
            else
            {
                if (other instanceof InfiniteRange)
                {
                    return other;
                }
                else
                {
                    Iterator<ContiguousRange> leftSubRanges = m_oSubRanges.iterator();
                    Iterator<ContiguousRange> rightSubRanges;

                    if (other instanceof ContiguousRange)
                    {
                        TreeSet<ContiguousRange> temp = new TreeSet<ContiguousRange>();

                        temp.add((ContiguousRange) other);
                        rightSubRanges = temp.iterator();
                    }
                    else
                    {
                        rightSubRanges = ((SparseRange) other).m_oSubRanges.iterator();
                    }

                    // merge the two sets of sub ranges
                    TreeSet<ContiguousRange> resultingSubRanges = new TreeSet<ContiguousRange>();
                    ContiguousRange          left               = null;
                    ContiguousRange          right              = null;
                    ContiguousRange          current            = leftSubRanges.next();

                    while (leftSubRanges.hasNext() || rightSubRanges.hasNext())
                    {
                        if (left == null && leftSubRanges.hasNext())
                        {
                            left = leftSubRanges.next();
                        }

                        if (right == null && rightSubRanges.hasNext())
                        {
                            right = rightSubRanges.next();
                        }

                        if (left != null && (current.intersects(left) || current.isAdjacent(left)))
                        {
                            current = (ContiguousRange) current.union(left);
                            left    = null;
                        }
                        else
                        {
                            if (right != null && (current.intersects(right) || current.isAdjacent(right)))
                            {
                                current = (ContiguousRange) current.union(right);
                                right   = null;

                                // the resulting current can still be either adjacent to or intersect left
                                if (left != null && (current.intersects(left) || current.isAdjacent(left)))
                                {
                                    current = (ContiguousRange) current.union(left);
                                    left    = null;
                                }

                            }
                            else
                            {
                                resultingSubRanges.add(current);

                                if (left == null)
                                {
                                    current = right;
                                    right   = null;
                                }
                                else
                                {
                                    current = left;
                                    left    = null;
                                }
                            }
                        }
                    }

                    if (left != null)
                    {
                        resultingSubRanges.add(left);
                    }

                    if (right != null)
                    {
                        resultingSubRanges.add(right);
                    }

                    if (current != null)
                    {
                        resultingSubRanges.add(current);
                    }

                    // create the resulting range
                    if (resultingSubRanges.isEmpty())
                    {
                        return Ranges.EMPTY;
                    }
                    else
                    {
                        if (resultingSubRanges.size() == 1)
                        {
                            return resultingSubRanges.first();
                        }
                        else
                        {
                            return new SparseRange(resultingSubRanges);
                        }
                    }
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Long> iterator()
    {
        return new RangeIterator(this);
    }


    /**
     * Iterates over all subranges and checks for equality.
     * @param other the range to compare for equality.
     * @return true if equal
     */
    private boolean iterateForEquality(Range other)
    {
        if (other instanceof ContiguousRange)
        {
            // If the other range is a ContiguousRange then we are equal if there is one subrange
            // that is equal to the other range
            ContiguousRange           otherrange    = (ContiguousRange) other;
            Iterator<ContiguousRange> thisSubRanges = m_oSubRanges.iterator();

            if (thisSubRanges.hasNext())
            {
                Range range = thisSubRanges.next();

                if (!range.equals(otherrange))
                {
                    return false;
                }

                return (!thisSubRanges.hasNext());    // If we don't have any more subranges we are equal
            }
        }

        if (other instanceof SparseRange)
        {
            SparseRange               othersparse    = (SparseRange) other;
            Iterator<ContiguousRange> thisSubRanges  = m_oSubRanges.iterator();
            Iterator<ContiguousRange> otherSubRanges = othersparse.m_oSubRanges.iterator();

            while (thisSubRanges.hasNext() && otherSubRanges.hasNext())
            {
                if (!thisSubRanges.next().equals(otherSubRanges.next()))
                {
                    return false;
                }
            }

            return (thisSubRanges.hasNext() == otherSubRanges.hasNext());    // we must still check if only one range ran out
        }
        else
        {
            if (other instanceof InfiniteRange)
            {
                return false;
            }
            else
            {
                return false;    // we don't support any other Ranges
            }
        }
    }


    /**
    * {@inheritDoc}
    */
    public boolean equals(Object object)
    {
        if (object != null && object instanceof Range)
        {
            Range other = (Range) object;

            return iterateForEquality(other);
        }
        else
        {
            return false;
        }
    }


    /**
    * {@inheritDoc}
    */
    public String toString()
    {
        return isEmpty() ? "SparseRange[]" : String.format("SparseRange[%s]", m_oSubRanges);
    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in) throws IOException
    {
        m_lFrom      = ExternalizableHelper.readLong(in);
        m_oSubRanges = new TreeSet<ContiguousRange>();
        ExternalizableHelper.readCollection(in, m_oSubRanges, getClass().getClassLoader());
    }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, m_lFrom);
        ExternalizableHelper.writeCollection(out, m_oSubRanges);
    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader reader) throws IOException
    {
        m_lFrom = reader.readLong(0);
        reader.readCollection(1, m_oSubRanges);
    }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, m_lFrom);
        writer.writeCollection(1, m_oSubRanges);
    }


    /**
     * An implementation of an {@link Iterator} so that we
     * can iterate over the values in a {@link SparseRange}.
     */
    static class RangeIterator implements Iterator<Long>
    {
        /**
         * An {@link Iterator} over the {@link ContiguousRange}s in the {@link SparseRange}.
         */
        private Iterator<ContiguousRange> ranges;

        /**
         * An {@link Iterator} over the values in the current {@link ContiguousRange}.
         */
        private Iterator<Long> iterator;


        /**
         * Standard Constructor.
         *
         * @param sparseRange the {@link SparseRange} to iterate over
         */
        RangeIterator(SparseRange sparseRange)
        {
            ranges = sparseRange.m_oSubRanges.iterator();

            if (ranges.hasNext())
            {
                iterator = ranges.next().iterator();
            }
            else
            {
                iterator = null;
            }
        }


        /**
        * {@inheritDoc}
        */
        public boolean hasNext()
        {
            return (iterator != null && iterator.hasNext()) || (ranges.hasNext());
        }


        /**
        * {@inheritDoc}
        */
        public Long next()
        {
            if (!iterator.hasNext())
            {
                iterator = ranges.next().iterator();
            }

            return iterator.next();
        }


        /**
        * {@inheritDoc}
        */
        public void remove()
        {
            throw new UnsupportedOperationException("Can't remove values from Range implementations as they are immutable");
        }
    }
}
