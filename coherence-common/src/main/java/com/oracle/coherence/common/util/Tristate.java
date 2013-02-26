/*
 * File: Tristate.java
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

package com.oracle.coherence.common.util;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Tristate} is an enumeration that represents three values, TRUE, FALSE, UNDEFINED.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Tristate implements ExternalizableLite, PortableObject
{
    /**
     * The undefined value for a {@link Tristate}.
     */
    public static final Tristate UNDEFINED = new Tristate((byte) -1);

    /**
     * The false value for a {@link Tristate}.
     */
    public static final Tristate FALSE = new Tristate((byte) 0);

    /**
     * The true value for a {@link Tristate}.
     */
    public static final Tristate TRUE = new Tristate((byte) 1);

    /**
     * The value of a {@link Tristate}.
     */
    private byte value;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public Tristate()
    {
    }


    /**
     * Standard Constructor to create a {@link Tristate} from a {@link Boolean}.
     *
     * @param value The boolean value for initialization.
     */
    public Tristate(boolean value)
    {
        this.value = value ? (byte) 1 : (byte) 0;
    }


    /**
     * Private Constructor: You should use the statics when a specific value is required.
     *
     * @param value The value of the {@link Tristate}
     */
    private Tristate(byte value)
    {
        this.value = value;
    }


    /**
     * Returns if the {@link Tristate} is TRUE.
     *
     * @return <code>true</code> if the {@link Tristate} value is TRUE.
     */
    public boolean isTrue()
    {
        return value == (byte) 1;
    }


    /**
     * Returns if the {@link Tristate} is FALSE.
     *
     * @return <code>true</code> if the {@link Tristate} value is FALSE.
     */
    public boolean isFalse()
    {
        return value == (byte) 0;
    }


    /**
     * Returns if the {@link Tristate} is UNDEFINED.
     *
     * @return <code>true</code> if the {@link Tristate} value is UNDEFINED.
     */
    public boolean isUndefined()
    {
        return value == (byte) -1;
    }


    /**
     * Returns the negation (not) of the {@link Tristate}.
     *
     * <code>
     *   not(True)      = False
     *   not(False)     = True
     *   not(Undefined) = Undefined
     * </code>
     *
     * @return The negation (not) of a {@link Tristate}.
     */
    public Tristate not()
    {
        if (isUndefined())
        {
            return this;
        }
        else
        {
            return isTrue() ? Tristate.FALSE : Tristate.TRUE;
        }
    }


    /**
     * Returns the disjunction (or) of this {@link Tristate} and another.
     *
     * <code>
     *   True or True           = True
     *   True or False          = True
     *   True or Undefined      = True
     *   False or True          = True
     *   False or False         = False
     *   False or Undefined     = False
     *   Undefined or True      = True
     *   Undefined or False     = False
     *   Undefined or Undefined = Undefined
     * </code>
     *
     * @param other The other {@link Tristate} with which to perform the disjunction
     *
     * @return The disjunction (or) of this and another {@link Tristate}.
     */
    public Tristate or(Tristate other)
    {
        if (isTrue())
        {
            return this;
        }
        else if (isFalse())
        {
            return other.isTrue() ? other : Tristate.FALSE;
        }
        else
        {
            return other;
        }
    }


    /**
     * Returns the conjunction (and) of this {@link Tristate} and another.
     * <p>
     * <code>
     *   True and True           = True
     *   True and False          = False
     *   True and Undefined      = Undefined
     *   False and True          = False
     *   False and False         = False
     *   False and Undefined     = Undefined
     *   Undefined and True      = Undefined
     *   Undefined and False     = Undefined
     *   Undefined and Undefined = Undefined
     * </code>
     *
     * @param other The other {@link Tristate} with which to perform the conjunction
     *
     * @return The conjunction (and) of this and another {@link Tristate}.
     */
    public Tristate and(Tristate other)
    {
        if (isTrue())
        {
            return other;
        }
        else if (isFalse())
        {
            return other.isUndefined() ? other : Tristate.FALSE;
        }
        else
        {
            return Tristate.UNDEFINED;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.value = in.readByte();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        out.writeByte(value);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.value = reader.readByte(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeByte(1, value);
    }


    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return value;
    }


    /**
     * {@inheritDoc}
     */
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

        if (!(obj instanceof Tristate))
        {
            return false;
        }

        Tristate other = (Tristate) obj;

        if (value != other.value)
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Tristate{%s}", value == 0 ? "FALSE" : (value == 1 ? "TRUE" : "UNDEFINED"));
    }
}
