/*
 * File: SerializableNullParameterResolver.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SerializableNullParameterResolver} is a {@link ParameterResolver} that always
 * resolves named {@link Parameter}s to <code>null</code>.
 * <p>
 * <strong>IMPORTANT:</strong> Developers should only use this class when they
 * can't easily access the {@link ParameterResolver} provided by the CacheConfig
 * getDefaultParameterResolver() method.
 * <p>
 * In most circumstances this class is only ever used for:
 * a). testing and/or b) those very rare occasions that you need a
 * {@link ParameterResolver} and want all parameters resolved to <code>null</code>.
 * <p>
 * <strong>NOTE:</strong> This class does not provide a static INSTANCE
 * declaration by design. Developers are not meant to use this class very often
 * and hence we discourage this by not providing an INSTANCE declaration.
 * <p>
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableNullParameterResolver implements ParameterResolver, ExternalizableLite, PortableObject
{
    /**
     * Default constructor needed for serialization.
     */
    public SerializableNullParameterResolver()
    {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
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
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
    }
}

