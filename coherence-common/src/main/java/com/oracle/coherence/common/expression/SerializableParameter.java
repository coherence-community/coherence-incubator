/*
 * File: SerializableParameter.java
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
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SerializableParameter} represents an optionally named and optionally
 * explicitly typed {@link Expression}.
 * <p/>
 * <strong>Terminology:</strong>
 * <p/>
 * A {@link SerializableParameter} that doesn't have a name, or when the name is
 * irrelevant, is referred to as an <strong>Actual {@link
 * SerializableParameter}</strong>.  That is, only the value of the Parameter is of any
 * importance.
 * <p/>
 * A {@link Parameter} with a name and/or when the name is of importance, is referred to
 * as a <strong>Formal {@link Parameter}</strong> or <strong>Argument</strong>.
 * <p/>
 * NOTE: This class is used to represent Actual and Formal {@link Parameter}s.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableParameter extends Parameter implements ExternalizableLite, PortableObject

{
    /**
     * The name of the {@link com.tangosol.config.expression.Parameter}.
     * <p/>
     * NOTE: this will be a UUID if a name hasben been specified.
     */
    private String name;

    /**
     * (optional) The expected type of value of the {@link
     * com.tangosol.config.expression.Parameter}.
     * <p/>
     * NOTE: when <code>null</code> a type has not been specified.
     */
    private Class<?> clzType = Object.class;

    /**
     * The {@link Expression} representing the value of the {@link
     * com.tangosol.config.expression.Parameter}.
     */
    private Expression<?> expression;


    /**
     * Default constructor needed for serialization.
     */
    public SerializableParameter()
    {
        super(null, null);
    }


    /**
     * Construct an implicitly typed {@link SerializableParameter} based on an {@link Expression}.
     *
     * @param name       the name of the {@link com.tangosol.config.expression.Parameter}
     * @param expression the {@link Expression} for the {@link
     *                   com.tangosol.config.expression.Parameter}
     */
    public SerializableParameter(String        name,
                                 Expression<?> expression)
    {
        super(name, expression);

        this.name       = name;
        this.expression = SerializableExpressionHelper.ensureSerializable(
                expression);
    }


    /**
     * Construct an implicitly typed {@link SerializableParameter} based on an
     * Object.
     *
     * @param name   the name of the {@link com.tangosol.config.expression.Parameter}
     * @param oValue the value for the {@link com.tangosol.config.expression.Parameter}
     */
    public SerializableParameter(String name,
                                 Object oValue)
    {
        super(name, oValue);

        this.name       = name;
        this.expression = new SerializableLiteralExpression<Object>(oValue);
    }


    /**
     * Construct a {@link SerializableParameter} given a class type and Expression.
     *
     * @param name       the name of the {@link com.tangosol.config.expression.Parameter}
     * @param clzType    the expected type of the {@link com.tangosol.config.expression.Parameter}
     * @param expression the {@link Expression} for the {@link
     *                   com.tangosol.config.expression.Parameter}
     */
    public SerializableParameter(String        name,
                                 Class<?>      clzType,
                                 Expression<?> expression)
    {
        super(name, clzType, expression);

        this.name       = name;
        this.expression = SerializableExpressionHelper.ensureSerializable(
                expression);
    }


    /**
     * Construct an explicitly typed {@link SerializableParameter} based on a
     * class type and a {@link Value}.
     *
     * @param name    the name of the {@link com.tangosol.config.expression.Parameter}
     * @param clzType the expected type of the {@link com.tangosol.config.expression.Parameter}
     * @param oValue  the value for the {@link com.tangosol.config.expression.Parameter}
     */
    public SerializableParameter(String   name,
                                 Class<?> clzType,
                                 Object   oValue)
    {
        super(name, clzType, oValue);

        this.name       = name;
        this.clzType    = clzType;
        this.expression = new SerializableLiteralExpression<Object>(oValue);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Value evaluate(ParameterResolver resolver)
    {
        // evaluate whatever the expression is
        Object oValue = expression.evaluate(resolver);

        // make sure the result is a value
        Value value = oValue instanceof Value ? (Value) oValue : new Value(oValue);

        // when the parameter is explicitly typed, attempt to coerce the type into that which is specified
        if (isExplicitlyTyped())
        {
            return new Value(value.as(clzType));
        }
        else
        {
            return value;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        String sResult = "";

        if (name != null)
        {
            sResult += "name=" + name;
        }

        if (clzType != null)
        {
            sResult += (sResult.isEmpty() ? "" : ", ") + "type=" + clzType;
        }

        sResult += (sResult.isEmpty() ? "" : ", ") + "expression=" + expression;

        return "SerializableParameter{" + sResult + "}";
    }


    /**
     * Obtains the name of the {@link com.tangosol.config.expression.Parameter}.
     *
     * @return a {@link String} representing the name of the {@link
     *         com.tangosol.config.expression.Parameter}
     */
    public String getName()
    {
        return name;
    }


    /**
     * Obtains the explicitly specified type of the {@link
     * com.tangosol.config.expression.Parameter}.
     *
     * @return a {@link Class} representing the type of the {@link
     *         com.tangosol.config.expression.Parameter}
     */
    public Class<?> getExplicitType()
    {
        return clzType;
    }


    /**
     * Obtains if an expected/actual type of the {@link
     * com.tangosol.config.expression.Parameter} has been specified/is
     * known.
     *
     * @return <code>true</code> if the type of the {@link
     *         com.tangosol.config.expression.Parameter} is known/specified.
     *         ie: {@link #getExplicitType()} is not <code>null</code>,
     *         otherwise returns <code>false</code>.
     */
    public boolean isExplicitlyTyped()
    {
        return clzType != null;
    }


    /**
     * Obtains the {@link Expression} for the {@link com.tangosol.config.expression.Parameter}.
     *
     * @return the {@link Expression} for the {@link com.tangosol.config.expression.Parameter}
     */
    @Override
    public Expression<?> getExpression()
    {
        return expression;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.name       = ExternalizableHelper.readSafeUTF(in);
        this.clzType    = getClass(ExternalizableHelper.readSafeUTF(in));
        this.expression = (Expression) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        if (name.equals("class-loader"))
        {
            // Remove the class loader since it is not serializable
            name       = "NonSerializableObject";
            clzType    = String.class;
            expression = new SerializableLiteralExpression<String>("");
        }

        ExternalizableHelper.writeSafeUTF(out, name);
        ExternalizableHelper.writeSafeUTF(out, clzType.getName());
        ExternalizableHelper.writeObject(out, expression);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.name       = reader.readString(1);
        this.clzType    = getClass(reader.readString(2));
        this.expression = (Expression) reader.readObject(3);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        if (name.equals("class-loader"))
        {
            // Remove the class loader since it is not serializable
            name       = "NonSerializableObject";
            clzType    = String.class;
            expression = new SerializableLiteralExpression<String>("");
        }

        writer.writeString(1, name);
        writer.writeString(2, clzType.getName());
        writer.writeObject(3, expression);
    }


    /**
     * Return the class given the name of the class.
     *
     * @param clzName the class name
     *
     * @return the class represented by the name
     */
    Class<?> getClass(String clzName)
    {
        try
        {
            return (Class<?>) Class.forName(clzName);
        }
        catch (Exception e)
        {
            throw Base.ensureRuntimeException(e);
        }
    }
}
