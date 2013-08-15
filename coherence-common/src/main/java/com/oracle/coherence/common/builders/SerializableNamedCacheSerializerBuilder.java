/*
 * File: SerializableNamedCacheSerializerBuilder.java
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

package com.oracle.coherence.common.builders;


import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.common.expression.SerializableLiteralExpression;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SerializableNamedCacheSerializerBuilder} is a {@link ParameterizedBuilder}
 * for a {@link Serializer} for a specific {@link NamedCache}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class SerializableNamedCacheSerializerBuilder implements ParameterizedBuilder<Serializer>,
                                                                ExternalizableLite,
                                                                PortableObject
{
    /**
     * An {@link Expression} that will yeild the name of the cache from which to determine the serializer.
     */
    private Expression<String> exprCacheName;


    /**
     * Required for serialization.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             bleLite} and {@link PortableObject}.
     */
    public SerializableNamedCacheSerializerBuilder()
    {
        // SKIP: deliberately empty
    }


    /**
     * Standard Constructor (using an expression).
     *
     * @param exprCacheName An {@link Expression} to determine the cache name
     */
    public SerializableNamedCacheSerializerBuilder(Expression exprCacheName)
    {
        exprCacheName = SerializableExpressionHelper.ensureSerializable(exprCacheName);
    }


    /**
     * Standard Constructor (using an explicit cache name).
     *
     * @param cacheName The name of the cache.
     */
    public SerializableNamedCacheSerializerBuilder(String cacheName)
    {
        exprCacheName = new SerializableLiteralExpression<String>(cacheName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Serializer realize(ParameterResolver parameterResolver,
                              ClassLoader       loader,
                              ParameterList     listParameters)
    {
        return CacheFactory.getCache(exprCacheName.evaluate(parameterResolver)).getCacheService().getSerializer();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        exprCacheName = (Expression) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, exprCacheName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        exprCacheName = (Expression) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, exprCacheName);
    }
}

