/*
 * File: SerializableResolvableParameterList.java
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


import com.tangosol.coherence.config.ParameterList;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A {@link SerializableResolvableParameterList} is a {@link ParameterList}
 * implementation that additionally supports name-based {@link Parameter}
 * resolution as defined by the {@link ParameterResolver} interface.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableResolvableParameterList implements ParameterList,
                                                            ParameterResolver,
                                                            ExternalizableLite,
                                                            PortableObject
{
    /**
     * The {@link Parameter} map.
     * <p/>
     * NOTE: It's important that this is a {@link LinkedHashMap} as we expect
     * to be able to iterate over the {@link Parameter}s in the order in
     * which they were added to the {@link com.tangosol.coherence.config.ResolvableParameterList}.
     *  This is to support implementation of the {@link ParameterList}
     * interface.
     */
    private LinkedHashMap<String, Parameter> mapParameters = new LinkedHashMap<String, Parameter>();


    /**
     * Constructs an empty {@link SerializableResolvableParameterList}.
     */
    public SerializableResolvableParameterList()
    {
    }


    /**
     * Constructs a {@link SerializableResolvableParameterList} based on a {@link ParameterList}.
     *
     * @param listParameters the {@link ParameterList} from which {@link
     *                       Parameter}s should be drawn
     */
    public SerializableResolvableParameterList(ParameterList listParameters)
    {
        this();

        for (Parameter parameter : listParameters)
        {
            add(new SerializableParameter(parameter.getName(), parameter.getExplicitType(), parameter.getExpression()));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
    {
        return mapParameters.get(sName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Parameter parameter)
    {
        mapParameters.put(parameter.getName(), parameter);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return mapParameters.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return mapParameters.size();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Parameter> iterator()
    {
        return mapParameters.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "{" + mapParameters.toString() + "}";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.mapParameters = new LinkedHashMap<String, Parameter>();
        ExternalizableHelper.readMap(in, mapParameters, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeMap(out, mapParameters);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.mapParameters = new LinkedHashMap<String, Parameter>();
        reader.readMap(1, mapParameters);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeMap(1, mapParameters);
    }
}

