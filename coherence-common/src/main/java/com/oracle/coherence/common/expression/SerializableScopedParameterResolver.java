/*
 * File: SerializableScopedParameterResolver.java
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

/**
 * A {@link SerializableScopedParameterResolver} is a {@link ParameterResolver} implementation
 * that provides the ability to "scope" {@link Parameter} definitions to either an
 * inner (wrapped) or outer {@link ParameterResolver}, such that those being
 * defined in the outer {@link ParameterResolver} hide those (of the same name)
 * in the inner (wrapped) {@link ParameterResolver}.
 * <p>
 * For example: Parameter "A" defined in the outer {@link ParameterResolver}
 * will override and thus "hide" Parameter "A" that is defined in the inner
 * {@link ParameterResolver}.
 * <p>
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableScopedParameterResolver implements ParameterResolver, ExternalizableLite, PortableObject
{
    // ----- data members ---------------------------------------------------

    /**
     * The inner (wrapped) {@link ParameterResolver}.
     */
    private ParameterResolver innerResolver;

    /**
     * The outer {@link ParameterResolver}.
     */
    private SerializableResolvableParameterList outerResolver;


    // ----- constructors ---------------------------------------------------

    /**
     * Constructor needed for serialization.
     */
    public SerializableScopedParameterResolver()
    {
    }


    /**
     * Construct a {@link SerializableScopedParameterResolver} given the specified inner
     * {@link ParameterResolver}.
     *
     * @param resolver  the inner {@link ParameterResolver}
     */
    public SerializableScopedParameterResolver(ParameterResolver resolver)
    {
        innerResolver = resolver;
        outerResolver = new SerializableResolvableParameterList();
    }


    // ----- ParameterResolver interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public com.tangosol.config.expression.Parameter resolve(String sName)
    {
        // see if this object has the parameter, if not use the inner resolver.
        com.tangosol.config.expression.Parameter param = outerResolver.resolve(sName);

        return param == null ? innerResolver.resolve(sName) : param;
    }


    // ----- ScopedParameterResolver methods --------------------------------

    /**
     * Adds the specified {@link com.tangosol.config.expression.Parameter} to the outer
     * {@link ParameterResolver}.
     *
     * @param parameter  the {@link com.tangosol.config.expression.Parameter} to add
     */
    public void add(Parameter parameter)
    {
        outerResolver.add(parameter);
    }


    // ----- logging support ------------------------------------------------

    /**
     * Return a human-readable String representation of this class.
     *
     * @return the description of this class
     */
    public String toString()
    {
        return "Outer Resolver=" + outerResolver + ", Inner Resolver=" + innerResolver;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.innerResolver = (ParameterResolver) ExternalizableHelper.readObject(in);
        this.outerResolver = (SerializableResolvableParameterList) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, innerResolver);
        ExternalizableHelper.writeObject(out, outerResolver);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.innerResolver = (ParameterResolver) reader.readObject(1);
        this.outerResolver = (SerializableResolvableParameterList) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, innerResolver);
        writer.writeObject(2, outerResolver);
    }
}

