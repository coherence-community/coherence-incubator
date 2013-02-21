/*
 * File: SimpleParameterProvider.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A {@link SimpleParameterProvider} is a simple map-based implementation of a {@link MutableParameterProvider}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SimpleParameterProvider implements MutableParameterProvider, ExternalizableLite, PortableObject
{
    /**
     * The {@link Parameter} map.
     */
    private LinkedHashMap<String, Parameter> parameters;


    /**
     * Standard Constructor.
     */
    public SimpleParameterProvider()
    {
        this.parameters = new LinkedHashMap<String, Parameter>();
    }


    /**
     * {@inheritDoc}
     */
    public void addParameter(Parameter parameter)
    {
        parameters.put(parameter.getName(), parameter);
    }


    /**
     * {@inheritDoc}
     */
    public Parameter getParameter(String name) throws ClassCastException
    {
        return parameters.get(name);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDefined(String name)
    {
        return parameters.containsKey(name);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return parameters.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return parameters.size();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Parameter> iterator()
    {
        return parameters.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ?> getParameters(ParameterProvider parameterProvider)
    {
        HashMap<String, Object> parameters = new HashMap<String, Object>();

        for (Parameter parameter : this)
        {
            Object value;

            if (parameter.isStronglyTyped())
            {
                // TODO: use the "getValue(...)" with the parameter type to ensure type-safety
                value = parameter.getExpression().evaluate(parameterProvider).getObject();
            }
            else
            {
                value = parameter.getExpression().evaluate(parameterProvider).getObject();
            }

            parameters.put(parameter.getName(), value);
        }

        return parameters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties(ParameterProvider parameterProvider)
    {
        Properties properties = new Properties();

        for (Parameter parameter : this)
        {
            properties.put(parameter.getName(), parameter.getExpression().evaluate(parameterProvider).getString());
        }

        return properties;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("SimpleParameterProvider{parameters=%s}", parameters);
    }


    /**
     * Returns a {@link Map} containing only those {@link Parameter}s that are serializable.
     *
     * @return A {@link Map}.
     */
    protected Map<String, Parameter> getSerializableParameters()
    {
        // only write out serializable, externalizable or portable objects
        LinkedHashMap<String, Parameter> serializableParameters = new LinkedHashMap<String, Parameter>();

        for (Entry<String, Parameter> entry : parameters.entrySet())
        {
            if (entry.getValue().isSerializable())
            {
                serializableParameters.put(entry.getKey(), entry.getValue());
            }
        }

        return serializableParameters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.parameters = new LinkedHashMap<String, Parameter>();
        ExternalizableHelper.readMap(in, parameters, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeMap(out, getSerializableParameters());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.parameters = new LinkedHashMap<String, Parameter>();
        reader.readMap(1, parameters);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeMap(1, getSerializableParameters());
    }
}
