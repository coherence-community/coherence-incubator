/*
 * File: SerializableSimpleParameterList.java
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
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A {@link SerializableSimpleParameterList} is a simple implementation of
 * {@link ParameterList}.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableSimpleParameterList implements ParameterList, ExternalizableLite, PortableObject
{
    /**
     * The {@link Parameter} list.
     */
    private ArrayList<Parameter> listParameters;


    /**
     * Constructs an empty {@link SerializableSimpleParameterList}.
     */
    public SerializableSimpleParameterList()
    {
        listParameters = new ArrayList<Parameter>(5);
    }


    /**
     * Constructs a {@link SerializableSimpleParameterList}
     * based on the specified array of objects, each object becoming it's own
     * {@link Parameter} in the resulting list.
     *
     * @param aObjects the objects to be considered as parameters
     */
    public SerializableSimpleParameterList(Object... aObjects)
    {
        this();

        if (aObjects != null)
        {
            for (Object o : aObjects)
            {
                add(o);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Parameter parameter)
    {
        listParameters.add(parameter);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return listParameters.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return listParameters.size();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Parameter> iterator()
    {
        return listParameters.iterator();
    }


    /**
     * Adds the specified object to the end of the {@link ParameterList} as
     * an anonymous {@link Parameter}.
     *
     * @param o the object to add as a {@link Parameter}
     */
    public void add(Object o)
    {
        if (o instanceof SerializableParameter)
        {
            add((Parameter) o);
        }
        else
        {
            add(new SerializableParameter("param-" + Integer.toString(size() + 1), o));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.listParameters = new ArrayList<Parameter>();
        ExternalizableHelper.readCollection(in, listParameters, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeCollection(out, listParameters);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.listParameters = new ArrayList<Parameter>();
        reader.readCollection(1, listParameters);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeCollection(1, listParameters);
    }
}
