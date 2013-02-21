/*
 * File: StringBasedIdentifier.java
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
 * A {@link String}-based implementation of an {@link Identifier}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class StringBasedIdentifier implements Identifier, ExternalizableLite, PortableObject
{
    /**
     * The string being used as an {@link Identifier}.
     */
    private String string;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public StringBasedIdentifier()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param string the {@link String} to use as an identifier
     */
    private StringBasedIdentifier(String string)
    {
        this.string = string;
    }


    /**
     * A factory method to create {@link String}-based {@link Identifier}s.
     *
     * @param string the {@link String} to use as an identifier
     *
     * @return a {@link String}-based {@link Identifier}.
     */
    public static Identifier newInstance(String string)
    {
        return new StringBasedIdentifier(string);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((string == null) ? 0 : string.hashCode());

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

        final StringBasedIdentifier other = (StringBasedIdentifier) obj;

        if (string == null)
        {
            if (other.string != null)
            {
                return false;
            }
        }
        else if (!string.equals(other.string))
        {
            return false;
        }

        return true;
    }


    /**
     * Returns the internal value used for the identifier.
     *
     * @return the internal value used for the identifier
     */
    public String getString()
    {
        return string;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Identifier{%s}", string);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.string = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, string);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.string = reader.readString(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, string);
    }
}
