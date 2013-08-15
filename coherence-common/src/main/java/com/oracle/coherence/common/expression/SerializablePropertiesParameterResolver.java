/*
 * File: SerializablePropertiesParameterResolver.java
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


import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

/**
 * A {@link SerializablePropertiesParameterResolver} is a {@link
 * ParameterResolver} that is based on a {@link Properties} instance.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializablePropertiesParameterResolver implements ParameterResolver, ExternalizableLite, PortableObject
{
    /**
     * The {@link Properties} from which to resolve {@link
     * SerializableParameter}s.
     */
    private Properties properties;


    /**
     * Default constructor needed for serialization.
     */
    public SerializablePropertiesParameterResolver()
    {
    }


    /**
     * Construct a {@link SerializablePropertiesParameterResolver} using a
     * {@link Properties} collection.
     *
     * @param properties the {@link Properties} on which to base the {@link
     *                   SerializablePropertiesParameterResolver} (these are
     *                   not copied, but a reference is held to them)
     */
    public SerializablePropertiesParameterResolver(Properties properties)
    {
        this.properties = properties;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SerializableParameter resolve(String sName)
    {
        String sValue;

        try
        {
            sValue = properties.getProperty(sName);
        }
        catch (SecurityException e)
        {
            sValue = null;
        }

        if (sValue == null)
        {
            return null;
        }
        else
        {
            return new SerializableParameter(sName, sValue);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        properties = System.getProperties();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        properties = System.getProperties();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
    }
}

