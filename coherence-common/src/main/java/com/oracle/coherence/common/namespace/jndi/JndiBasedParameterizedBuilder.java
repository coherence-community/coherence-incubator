/*
 * File: JndiBasedParameterizedBuilder.java
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

package com.oracle.coherence.common.namespace.jndi;


import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.common.expression.SerializableLiteralExpression;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder.ReflectionSupport;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A {@link JndiBasedParameterizedBuilder} is a {@link ParameterizedBuilder} that uses JNDI to resolve resources.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class JndiBasedParameterizedBuilder
        implements ParameterizedBuilder<Object>, ReflectionSupport , PortableObject, ExternalizableLite
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(JndiBasedParameterizedBuilder.class.getName());

    /**
     * The name of the resource to locate.
     */
    private Expression<String> exprResourceName;

    /**
     * The parameters.
     */
    private Hashtable<String, Object> params;

    /**
     * Constructs a {@link JndiBasedParameterizedBuilder}.
     */
    public JndiBasedParameterizedBuilder()
    {
        exprResourceName = new SerializableLiteralExpression<String>("");
        params = new Hashtable<String, Object>();
    }

    @Injectable
    public void setResourceName(Expression exprResourceName)
    {
        this.exprResourceName = SerializableExpressionHelper
                .ensureSerializable(exprResourceName);
    }

    public void setParams(Hashtable<String, Object> params)
    {
        this.params = params;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizes(Class<?> clazz,
                            ParameterResolver parameterResolver,
                            ClassLoader classLoader)
    {
        return clazz.isAssignableFrom(realize(parameterResolver, classLoader, null).getClass());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object realize(ParameterResolver parameterResolver,
                          ClassLoader classLoader,
                          ParameterList parameterList)
    {
        InitialContext initialContext;

        try
        {
            // determine the resource name to lookup
            String resourceName = exprResourceName.evaluate(parameterResolver);

            initialContext = new InitialContext(params);

            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE,
                        "Looking up {0} using JNDI with the environment {1}",
                        new Object[] {resourceName, params});
            }

            // find the resource using JNDI
            Object resource = initialContext.lookup(resourceName);

            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Found {0} using JNDI", resource);
            }

            return resource;
        }
        catch (NamingException e)
        {
            throw Base.ensureRuntimeException(e,
                "Unable to resolve the JNDI resource: " +  exprResourceName.evaluate(parameterResolver));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("%s{resourceName=%s, bldr=%s}",
                             this.getClass().getName(),
                             exprResourceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        exprResourceName  = (Expression) ExternalizableHelper.readObject(in,
                this.getClass().getClassLoader());

        params = new Hashtable<String, Object>();
        ExternalizableHelper.readMap(in, params,
                params.getClass().getClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, exprResourceName);
        ExternalizableHelper.writeMap(out, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        exprResourceName  = (Expression) reader.readObject(1);

        params = new Hashtable<String, Object>();
        reader.readMap(2, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, exprResourceName);
        writer.writeMap(2, params);
    }

}
