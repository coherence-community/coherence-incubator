/*
 * File: BinaryEntryStoreEventChannelBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.expressions.MacroParameterExpression;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link BinaryEntryStoreEventChannelBuilder} is an {@link EventChannelBuilder} for {@link BinaryEntryStoreEventChannel}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class BinaryEntryStoreEventChannelBuilder extends AbstractEventChannelBuilder
{
    /**
     * The name of the Cache that may be used to provide a BackingMapManagerContext
     * (for (de)serializating Events when required.
     */
    private Expression exprContextCacheName;

    /**
     * The {@link ParameterizedBuilder} for a {@link CacheStore}.
     */
    private ParameterizedBuilder<BinaryEntryStore> builder;


    /**
     * Standard Constructor.
     */
    public BinaryEntryStoreEventChannelBuilder()
    {
        this.exprContextCacheName = new MacroParameterExpression("{cache-name}");
        this.builder              = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventChannel realize(ParameterProvider parameterProvider)
    {
        return new BinaryEntryStoreEventChannel(exprContextCacheName.evaluate(parameterProvider).getString(),
                                                builder.realize(parameterProvider));
    }


    /**
     * Sets the name of the cache that can provide a BackingMapManagerContext for (de)serializing
     * Events.
     *
     * @param exprContextCacheName
     */
    @Property("context-cache-name")
    @Type(Expression.class)
    public void setContextCacheName(Expression exprContextCacheName)
    {
        this.exprContextCacheName = exprContextCacheName;
    }


    /**
     * Sets the {@link ParameterizedBuilder} to realize the {@link CacheStore}.
     *
     * @param builder
     */
    @Property("binaryentry-store-scheme")
    @Mandatory
    @Type(ParameterizedBuilder.class)
    public void setCacheStoreBuilder(ParameterizedBuilder<BinaryEntryStore> builder)
    {
        this.builder = builder;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.builder = (ParameterizedBuilder<BinaryEntryStore>) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, builder);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.builder = (ParameterizedBuilder<BinaryEntryStore>) reader.readObject(100);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(100, builder);
    }
}
