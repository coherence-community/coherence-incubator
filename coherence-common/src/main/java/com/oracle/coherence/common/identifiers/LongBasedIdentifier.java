/*
 * File: LongBasedIdentifier.java
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

package com.oracle.coherence.common.identifiers;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Long}-based implementation of an {@link Identifier}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class LongBasedIdentifier implements Identifier, ExternalizableLite, PortableObject
{
    /**
     * The Long being used as an {@link Identifier}.
     */
    private long value;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public LongBasedIdentifier()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param value the {@link Long} to use as an identifier
     */
    private LongBasedIdentifier(long value)
    {
        this.value = value;
    }


    /**
     * A factory method to create {@link Long}-based {@link Identifier}s.
     *
     * @param value the {@link Long} to use as an identifier
     *
     * @return a {@link Long}-based {@link Identifier}.
     */
    public static Identifier newInstance(long value)
    {
        return new LongBasedIdentifier(value);
    }


    /**
     * Returns the internal value used for the identifier.
     *
     * @return the internal value used for the identifier
     */
    public long getlong()
    {
        return value;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Identifier{%d}", value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + (int) (value ^ (value >>> 32));

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        LongBasedIdentifier other = (LongBasedIdentifier) obj;

        if (value != other.value)
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.value = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, value);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.value = reader.readLong(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, value);
    }
}
