/*
 * File: Triple.java
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

/**
 * An immutable sequence of three type-safe serializable values.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <X> The type of the first value of the {@link Triple}
 * @param <Y> The type of the second value of the {@link Triple}
 * @param <Z> The type of the third value of the {@link Triple}
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Triple<X, Y, Z> implements Tuple
{
    /**
     * The first value of the {@link Triple}.
     */
    private X x;

    /**
     * The second value of the {@link Triple}.
     */
    private Y y;

    /**
     * The third value of the {@link Triple}.
     */
    private Z z;


    /**
     * Required for ExternalizableLite and PortableObject.
     */
    public Triple()
    {
        // for ExternalizableLite
    }


    /**
     * Standard Constructor.
     *
     * @param x The first value of the {@link Triple}
     * @param y The second value of the {@link Triple}
     * @param z The third value of the {@link Triple}
     */
    public Triple(X x,
                  Y y,
                  Z z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
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
        else if (index == 2)
        {
            return z;
        }
        else
        {
            throw new IndexOutOfBoundsException(String.format("%d is an illegal index for a Triple", index));
        }
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return 3;
    }


    /**
     * Returns the first value of the {@link Triple}.
     *
     * @return The first value of the {@link Triple}
     */
    public X getX()
    {
        return x;
    }


    /**
     * Returns the second value of the {@link Triple}.
     *
     * @return The second value of the {@link Triple}
     */
    public Y getY()
    {
        return y;
    }


    /**
     * Returns the third value of the {@link Triple}.
     *
     * @return The third value of the {@link Triple}
     */
    public Z getZ()
    {
        return z;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.x = (X) ExternalizableHelper.readObject(in);
        this.y = (Y) ExternalizableHelper.readObject(in);
        this.z = (Z) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, x);
        ExternalizableHelper.writeObject(out, y);
        ExternalizableHelper.writeObject(out, z);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        x = (X) reader.readObject(0);
        y = (Y) reader.readObject(1);
        z = (Z) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, x);
        writer.writeObject(1, y);
        writer.writeObject(2, z);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("Triple<%s, %s, %s>",
                             x == null ? "null" : x.toString(),
                             y == null ? "null" : y.toString(),
                             z == null ? "null" : z.toString());
    }
}
