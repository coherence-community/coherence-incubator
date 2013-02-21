/*
 * File: CreateRemoteObjectProcessor.java
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

package com.oracle.coherence.common.processors;

import com.oracle.coherence.common.util.ReflectionHelper;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link CreateRemoteObjectProcessor} will create an object of the supplied
 * class, matching it to a constructor that matches the supplied parameters.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class CreateRemoteObjectProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The logger to use.
     */
    private static final Logger logger = Logger.getLogger(CreateRemoteObjectProcessor.class.getName());

    /**
     * The class name of the class we will instantiate.
     */
    private String className;

    /**
     * The parameters to pass to the constructor.
     */
    protected Object parameters[];


    /**
     * Default constructor.
     */
    public CreateRemoteObjectProcessor()
    {
    }


    /**
     * Standard constructor.
     *
     * @param classname  the name of the class to instantiate.
     * @param parameters array of constructor parameters
     */
    public CreateRemoteObjectProcessor(String    classname,
                                       Object... parameters)
    {
        className       = classname;
        this.parameters = parameters;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public Object process(Entry entry)
    {
        if (entry.isPresent())
        {
            return new IllegalStateException("An object with the key " + entry.getKey() + " already exists");
        }
        else
        {
            Class clazz;

            try
            {
                clazz = Class.forName(className);
            }
            catch (ClassNotFoundException e)
            {
                logger.log(Level.SEVERE, "Exception in process method ", e);

                throw Base.ensureRuntimeException(e);
            }

            try
            {
                Class[] parameterTypes = null;

                if (parameters != null)
                {
                    parameterTypes = new Class[parameters.length];

                    for (int i = 0; i < parameters.length; i++)
                    {
                        parameterTypes[i] = parameters[i].getClass();
                    }
                }

                Constructor con  = ReflectionHelper.getCompatibleConstructor(clazz, parameterTypes);

                Object      inst = con.newInstance(parameters);

                entry.setValue(inst);

                return null;
            }
            catch (InstantiationException e)
            {
                return e;
            }
            catch (IllegalAccessException e)
            {
                return e;
            }
            catch (Exception e)
            {
                return e;
            }
        }
    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(final DataInput in) throws IOException
    {
        this.className = ExternalizableHelper.readUTF(in);

        int      cParams = ExternalizableHelper.readInt(in);
        Object[] aoParam = cParams == 0 ? null : new Object[cParams];

        for (int i = 0; i < cParams; i++)
        {
            aoParam[i] = ExternalizableHelper.readObject(in);
        }

        parameters = aoParam;

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeUTF(out, className);

        Object[] aoParam = parameters;
        int      cParams = aoParam == null ? 0 : aoParam.length;

        ExternalizableHelper.writeInt(out, cParams);

        for (int i = 0; i < cParams; i++)
        {
            ExternalizableHelper.writeObject(out, aoParam[i]);
        }

    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(final PofReader oReader) throws IOException
    {
        this.className  = oReader.readString(0);
        this.parameters = oReader.readObjectArray(1, null);
    }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(final PofWriter oWriter) throws IOException
    {
        oWriter.writeString(0, className);
        oWriter.writeObjectArray(1, parameters);
    }
}
