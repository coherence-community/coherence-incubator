/*
 * File: EmptyParameterProvider.java
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link EmptyParameterProvider} is an implementation of a {@link ParameterProvider} that
 * knows of no {@link Parameter}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class EmptyParameterProvider implements ParameterProvider, ExternalizableLite, PortableObject
{
    /**
     * A constant {@link EmptyParameterProvider}.
     */
    public static final EmptyParameterProvider INSTANCE = new EmptyParameterProvider();


    /**
     * Standard Constructor.
     */
    public EmptyParameterProvider()
    {
        // deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    public Parameter getParameter(String name) throws ClassCastException
    {
        // always returns null for a EmptyParameterProvider
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDefined(String name)
    {
        // always returns false for a EmptyParameterProvider
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Parameter> iterator()
    {
        return new Iterator<Parameter>()
        {
            /**
             * {@inheritDoc}
             */
            public boolean hasNext()
            {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Parameter next()
            {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            public void remove()
            {
                throw new UnsupportedOperationException("Can't remove a parameter from an EmptyParameterProvider");
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> getParameters(ParameterProvider parameterProvider)
    {
        return Collections.EMPTY_MAP;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties(ParameterProvider parameterProvider)
    {
        return new Properties();
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
