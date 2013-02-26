/*
 * File: Pair.java
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

package com.oracle.coherence.common.tuples;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * An immutable sequence of two type-safe serializable values.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <X> The type of the first value of the {@link Pair}
 * @param <Y> The type of the second value of the {@link Pair}
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Pair<X, Y> implements Tuple
{
    /**
     * The first value of the {@link Pair}.
     */
    private X x;

    /**
     * The second value of the {@link Pair}.
     */
    private Y y;


    /**
     * Required for ExternalizableLite and PortableObject.
     */
    public Pair()
    {
    }


    /**
     * Standard Constructor to create a {@link Pair} based on a Map.Entry.
     *
     * @param entry A Map.Entry from which to create a {@link Pair}
     */
    public Pair(Map.Entry<X, Y> entry)
    {
        this.x = entry.getKey();
        this.y = entry.getValue();
    }


    /**
     * Standard Constructor.
     *
     * @param x The first value of the {@link Pair}
     * @param y The second value of the {@link Pair}
     */
    public Pair(X x,
                Y y)
    {
        this.x = x;
        this.y = y;
    }


    /**
     * {@inheritDoc}
     */
    public Object get(int index) throws IndexOutOfBoundsException
    {
        if (index == 0)
        {
            return x;
        }
        else if (index == 1)
        {
            return y;
        }
        else
        {
            throw new IndexOutOfBoundsException(String.format("%d is an illegal index for a Pair", index));
        }
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return 2;
    }


    /**
     * Returns the first value of the {@link Pair}.
     *
     * @return The first value of the {@link Pair}
     */
    public X getX()
    {
        return x;
    }


    /**
     * Returns the second value of the {@link Pair}.
     *
     * @return The second value of the {@link Pair}
     */
    public Y getY()
    {
        return y;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.x = (X) ExternalizableHelper.readObject(in);
        this.y = (Y) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, x);
        ExternalizableHelper.writeObject(out, y);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.x = (X) reader.readObject(0);
        this.y = (Y) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, x);
        writer.writeObject(1, y);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("Pair<%s, %s>", x == null ? "null" : x.toString(), y == null ? "null" : y.toString());
    }
}
