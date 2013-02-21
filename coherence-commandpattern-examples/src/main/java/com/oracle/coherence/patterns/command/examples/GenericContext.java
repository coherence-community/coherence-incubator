/*
 * File: GenericContext.java
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

package com.oracle.coherence.patterns.command.examples;

import com.oracle.coherence.patterns.command.Context;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link GenericContext} provides a generic context implementation of a {@link Context}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <T> The object to wrap
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class GenericContext<T> implements Context, ExternalizableLite, PortableObject
{
    /**
     * The value wrapped as a context
     */
    private T value;


    /**
     * Empty constructor required by PortableObject
     */
    public GenericContext()
    {
    }


    /**
     * Construct a new GenericContext.
     *
     * @param value the value wrapped by this {@link GenericContext}
     */
    public GenericContext(T value)
    {
        this.value = value;
    }


    /**
     * Return the value wrapped by this {@link GenericContext}.
     *
     * @return the value wrapped by this {@link GenericContext}
     */
    public T getValue()
    {
        return value;
    }


    /**
     * Set the value wrapped by this {@link GenericContext}.
     *
     * @param value The value to set
     */
    public void setValue(T value)
    {
        this.value = value;
    }


    /**
     * Return a string representation of this example.
     */
    public String toString()
    {
        return String.format("GenericContext{value=%s}", value);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.value = (T) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, value);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.value = (T) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, value);
    }
}
