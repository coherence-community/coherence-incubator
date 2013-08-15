/*
 * File: SerializableLiteralExpression.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.common.expression;


import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A SerializableLiteralExpression a serializable literal (aka: constant) {@link Expression}.
 *
 * @param T the type of the literal constant
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableLiteralExpression<T> implements Expression<T>, ExternalizableLite, PortableObject
{
    /**
     * The value of the constant.
     */
    private T value;


    /**
     * Default constructor needed for serialization.
     */
    public SerializableLiteralExpression()
    {
    }


    /**
     * Construct a {@link com.tangosol.config.expression.LiteralExpression}.
     *
     * @param value the value of the constant
     */
    public SerializableLiteralExpression(T value)
    {
        this.value = value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public T evaluate(ParameterResolver resolver)
    {
        return value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("SerializableLiteralExpression{value=%s}", value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        value = (T) ExternalizableHelper.readObject(in);
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
        value = (T) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, value);
    }
}
