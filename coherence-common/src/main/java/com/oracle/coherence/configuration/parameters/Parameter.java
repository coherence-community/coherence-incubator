/*
 * File: Parameter.java
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

package com.oracle.coherence.configuration.parameters;

import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.expressions.Constant;
import com.oracle.coherence.configuration.expressions.Expression;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Parameter} represents an optionally named and optionally typed {@link Expression}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Parameter implements Expression, ExternalizableLite, PortableObject
{
    /**
     * The name of the {@link Parameter}.
     */
    private String name;

    /**
     * (optional)The expected type (as a fully qualified class name) of the
     * {@link Parameter} when evaluated.
     *
     * NOTE: when <code>null</code> a type has not been specified.
     */
    private String type;

    /**
     * The {@link Expression} of the {@link Parameter} that when evaluated will produce
     * a {@link Value}.
     */
    private Expression expression;


    /**
     * Standard Constructor (required for {@link ExternalizableLite} and {@link PortableObject}).
     */
    public Parameter()
    {
        // deliberately empty
    }


    /**
     * Standard Constructor (for a {@link Boolean} parameter).
     *
     * @param name The name of the parameter
     * @param value The value for the parameter
     */
    public Parameter(String  name,
                     boolean value)
    {
        this.name       = name;
        this.type       = Boolean.TYPE.getName();
        this.expression = new Constant(value);
    }


    /**
     * Standard Constructor (with no explicitly declared type).
     *
     * @param name The name of the {@link Parameter}
     * @param expression The {@link Expression} for the {@link Parameter}
     */
    public Parameter(String     name,
                     Expression expression)
    {
        this.name       = name;
        this.type       = null;
        this.expression = expression;
    }


    /**
     * Standard Constructor (for an {@link Object} parameter).
     *
     * @param name The name of the parameter
     * @param value The value for the parameter
     */
    public Parameter(String name,
                     Object value)
    {
        this.name       = name;
        this.type       = null;
        this.expression = new Constant(value);
    }


    /**
     * Standard Constructor (for a {@link String} parameter).
     *
     * @param name The name of the parameter
     * @param value The value for the parameter
     */
    public Parameter(String name,
                     String value)
    {
        this.name       = name;
        this.type       = String.class.getName();
        this.expression = new Constant(value);
    }


    /**
     * Standard Constructor.
     *
     * @param name The name of the {@link Parameter}
     * @param type The expected type of the {@link Parameter}
     * @param expression The {@link Expression} for the {@link Parameter}
     */
    public Parameter(String     name,
                     String     type,
                     Expression expression)
    {
        this.name       = name;
        this.type       = type;
        this.expression = expression;
    }


    /**
     * Standard Constructor (for a {@link Value} parameter).
     *
     * @param name The name of the parameter
     * @param type The expected type of the parameter (may be null if unknown)
     * @param value The value for the parameter
     */
    public Parameter(String name,
                     String type,
                     Value  value)
    {
        this.name       = name;
        this.type       = type;
        this.expression = new Constant(value);
    }


    /**
     * Returns the name of the {@link Parameter}.
     *
     * @return A {@link String} representing the name of the {@link Parameter}
     */
    public String getName()
    {
        return name;
    }


    /**
     * Determines the expected type of the {@link Parameter} (as a fully qualified class name).
     *
     * @return A {@link String} representing the type of the {@link Parameter}
     */
    public String getType()
    {
        return type;
    }


    /**
     * Determines if an expected/actual type of the {@link Parameter} has been specified/is known.
     *
     * @return <code>true</code> if the type of the {@link Parameter} is known/specified.  ie: {@link #getType()}
     *         is not <code>null</code>, otherwise returns <code>false</code>.
     */
    public boolean isStronglyTyped()
    {
        return type != null;
    }


    /**
     * Determines if all of the members of the {@link Parameter} are serializable.
     *
     * @return <code>true</code> if all of the members of the {@link Parameter} are serializable, <code>false</code>
     *         otherwise.
     */
    public boolean isSerializable()
    {
        return expression.isSerializable();
    }


    /**
     * Determines the {@link Expression} for the {@link Parameter}.
     *
     * @return The {@link Expression} for the {@link Parameter}
     */
    public Expression getExpression()
    {
        return expression;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Value evaluate(ParameterProvider parameterProvider)
    {
        return expression.evaluate(parameterProvider);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.name       = ExternalizableHelper.readSafeUTF(in);
        this.type       = ExternalizableHelper.readSafeUTF(in);
        this.expression = (Expression) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, name);
        ExternalizableHelper.writeSafeUTF(out, type);
        ExternalizableHelper.writeObject(out, expression);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.name       = reader.readString(1);
        this.type       = reader.readString(2);
        this.expression = (Expression) reader.readObject(3);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(1, name);
        writer.writeString(2, type);
        writer.writeObject(3, expression);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        String result = "";

        if (name != null)
        {
            result += "name=" + getName();
        }

        if (type != null)
        {
            result += (result.isEmpty() ? "" : ", ") + "type=" + getType();
        }

        result += (result.isEmpty() ? "" : ", ") + "expression=" + getExpression();

        return "Parameter{" + result + "}";
    }
}
