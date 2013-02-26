/*
 * File: SystemPropertyParameterProvider.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A {@link SystemPropertyParameterProvider} provides a {@link ParameterProvider} based representation of
 * the current state of the defined Java System {@link Properties}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SystemPropertyParameterProvider implements ParameterProvider, ExternalizableLite, PortableObject
{
    /**
     * The default {@link SystemPropertyParameterProvider} instance.
     */
    public static final SystemPropertyParameterProvider INSTANCE = new SystemPropertyParameterProvider();


    /**
     * Standard Constructor.
     */
    public SystemPropertyParameterProvider()
    {
        // deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDefined(String name)
    {
        return System.getProperties().containsKey(name);
    }


    /**
     * {@inheritDoc}
     */
    public Parameter getParameter(String name)
    {
        if (isDefined(name))
        {
            return new Parameter(name, System.getProperty(name));
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return System.getProperties().isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return System.getProperties().size();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterator<Parameter> iterator()
    {
        final Enumeration<String> propertyNames = (Enumeration<String>) System.getProperties().propertyNames();

        return new Iterator<Parameter>()
        {
            /**
             * {@inheritDoc}
             */
            public boolean hasNext()
            {
                return propertyNames.hasMoreElements();
            }

            /**
             * {@inheritDoc}
             */
            public Parameter next()
            {
                String propertyName = propertyNames.nextElement();

                return new Parameter(propertyName, System.getProperty(propertyName));
            }

            /**
             * {@inheritDoc}
             */
            public void remove()
            {
                throw new UnsupportedOperationException("Can't remove a parameter from an SystemPropertyParameterProvider");
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ?> getParameters(ParameterProvider parameterProvider)
    {
        HashMap<String, Object> result = new HashMap<String, Object>();

        for (Entry<Object, Object> entry : System.getProperties().entrySet())
        {
            result.put(entry.getKey().toString(), entry.getValue());
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties(ParameterProvider parameterProvider)
    {
        return System.getProperties();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        // deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        // deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        // deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        // deliberately empty
    }
}
