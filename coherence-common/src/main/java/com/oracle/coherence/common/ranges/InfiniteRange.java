/*
 * File: InfiniteRange.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

/**
 * An {@link InfiniteRange} represents the range of all possible long integer values.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class InfiniteRange implements Range, ExternalizableLite, PortableObject, Iterable<Long>, Comparable<Range>
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public InfiniteRange()
    {
        // nothing to do here
    }


    /**
     * {@inheritDoc}
     */
    public long getFrom()
    {
        return Long.MIN_VALUE;
    }


    /**
     * {@inheritDoc}
     */
    public long getTo()
    {
        return Long.MAX_VALUE;
    }


    /**
     * {@inheritDoc}
     */
    public Range add(long value)
    {
        return this;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains(long value)
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean intersects(Range other)
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAdjacent(Range other)
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isSingleton()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public long size()
    {
        return Long.MAX_VALUE;
    }


    /**
     * {@inheritDoc}
     */
    public Range union(Range other)
    {
        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Range remove(long value)
    {
        throw new UnsupportedOperationException("Can't remove a value from an InfiniteRange");
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Long> iterator()
    {
        throw new UnsupportedOperationException("Can't create an Iterator over an InfiniteRange");
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(Range other)
    {
        return -1;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "InfiniteRange[...]";
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        // nothing to do here
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        // nothing to do here
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        // nothing to do here
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        // nothing to do here
    }
}
