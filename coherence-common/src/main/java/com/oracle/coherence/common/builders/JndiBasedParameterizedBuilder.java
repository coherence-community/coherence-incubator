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

package com.oracle.coherence.common.builders;

import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link JndiBasedParameterizedBuilder} is a {@link ParameterizedBuilder} that uses JNDI to resolve resources.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class JndiBasedParameterizedBuilder implements ParameterizedBuilder<Object>, PortableObject, ExternalizableLite
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(JndiBasedParameterizedBuilder.class.getName());

    /**
     * The name of the resource to locate.
     */
    private Expression m_exprResourceName;

    /**
     * The {@link Parameter}s that will be used to construct an {@link InitialContext}.
     */
    private ParameterProvider m_ParameterProvider;


    /**
     * Standard Constructor.
     */
    public JndiBasedParameterizedBuilder()
    {
        m_ParameterProvider = SystemPropertyParameterProvider.INSTANCE;
    }


    /**
     * Determine the {@link Expression} representing the JNDI resource name to lookup.
     *
     * @return {@link Expression}
     */
    public Expression getResourceNameExpression()
    {
        return m_exprResourceName;
    }


    /**
     * Sets the {@link Expression} representing the JNDI resource name to lookup.
     *
     * @param exprResourceName The {@link Expression} representing the resouce name.
     */
    @Property("resource-name")
    @Mandatory
    @Type(Expression.class)
    @SubType(String.class)
    public void setResourceNameExpression(Expression exprResourceName)
    {
        this.m_exprResourceName = exprResourceName;
    }


    /**
     * Determines the {@link ParameterProvider} for constructing the {@link InitialContext} environment.
     *
     * @return {@link ParameterProvider}
     */
    public ParameterProvider getParameterProvider()
    {
        return m_ParameterProvider;
    }


    /**
     * Sets the {@link ParameterProvider} for constructing the {@link InitialContext} environment.
     *
     * @param parameterProvider
     */
    @Property("init-params")
    @Type(ParameterProvider.class)
    public void setParameterProvider(ParameterProvider parameterProvider)
    {
        this.m_ParameterProvider = parameterProvider;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizesClassOf(Class<?>          clazz,
                                   ParameterProvider parameterProvider)
    {
        return clazz.isAssignableFrom(realize(parameterProvider).getClass());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object realize(ParameterProvider parameterProvider)
    {
        InitialContext initialContext;

        try
        {
            // determine the resource name to lookup
            String resourceName = m_exprResourceName.evaluate(parameterProvider).getString();

            // construct the initial context with an environment using the specified parameters
            Hashtable<String, Object> env = new Hashtable<String,
                                                          Object>(m_ParameterProvider.getParameters(parameterProvider));

            initialContext = new InitialContext(env);

            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE,
                           "Looking up {0} using JNDI with the environment {1}",
                           new Object[] {resourceName, env});
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
                                              "Unable to resolve the JNDI resource: " + m_exprResourceName.toString());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        m_exprResourceName  = (Expression) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
        m_ParameterProvider = (ParameterProvider) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, m_exprResourceName);
        ExternalizableHelper.writeObject(out, m_ParameterProvider);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        m_exprResourceName  = (Expression) reader.readObject(1);
        m_ParameterProvider = (ParameterProvider) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, m_exprResourceName);
        writer.writeObject(2, m_ParameterProvider);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("%s{resourceName=%s, parameters=%s}",
                             this.getClass().getName(),
                             m_exprResourceName,
                             m_ParameterProvider);
    }
}
