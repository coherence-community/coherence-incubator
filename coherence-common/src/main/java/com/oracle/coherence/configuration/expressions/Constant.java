/*
 * File: Constant.java
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

package com.oracle.coherence.configuration.expressions;

import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Constant} represents a simple {@link Value} as an {@link Expression}.
 * <p>
 * {@link Constant}s are useful {@link Expression} substitutes when only a single {@link Value} is
 * required and you don't want to create an entire {@link Expression}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Constant implements Expression, ExternalizableLite, PortableObject
{
    /**
     * The {@link Value} of the constant.
     */
    private Value value;


    /**
     * Standard Constructor (required for {@link ExternalizableLite} and {@link PortableObject}).
     */
    public Constant()
    {
        // deliberately empty
    }


    /**
     * Standard Constructor.
     *
     * @param <T> the type of the constant
     * @param value The value of the constant
     */
    public <T>Constant(T value)
    {
        this.value = new Value(value);
    }


    /**
     * Standard Constructor.
     *
     * @param value The {@link Value} of the constant
     */
    public Constant(Value value)
    {
        this.value = value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Value evaluate(ParameterProvider parameterProvider)
    {
        return value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("Constant{value=%s}", value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSerializable()
    {
        return value == null || value.isSerializable();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.value = (Value) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.value = (Value) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, value);
    }
}
