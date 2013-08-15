/*
 * File: ParallelLocalCacheEventChannelBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;

import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.common.expression.SerializableLiteralExpression;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.channels.AbstractEventChannelBuilder;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ResourceRegistry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannelBuilder} is a {@link com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder} for {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.LocalCacheEventChannel}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
@SuppressWarnings("serial")
public class ParallelLocalCacheEventChannelBuilder extends AbstractEventChannelBuilder
{
    /**
     * The name of the invocation service to use to publish to
     */
    public static final String INVOCATION_SVC = "PublishingInvocationService";


    /**
     * The name of the local cache to which the {@link com.oracle.coherence.patterns.eventdistribution.EventChannel} will apply {@link com.oracle.coherence.common.events.EntryEvent}.
     */
    private Expression<String> targetCacheName;

    /**
     * The {@link ParameterizedBuilder} that can realize a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver} when required.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder = null;

    /**
     * The name of the Invocation Service to use.
     */
    private Expression<String> invocationServiceName;


    /**
     * Required for {@link com.tangosol.io.ExternalizableLite} and {@link com.tangosol.io.pof.PortableObject}.
     */
    public ParallelLocalCacheEventChannelBuilder()
    {
        super();

        invocationServiceName = new SerializableLiteralExpression<String>(INVOCATION_SVC);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventChannel realize(ParameterResolver parameterResolver,
            ClassLoader classLoader, ParameterList parameters)
    {
        ResourceRegistry registry = CacheFactory.getConfigurableCacheFactory().getResourceRegistry();

        String targetCacheName       = getTargetCacheName().evaluate(parameterResolver);
        String invocationServiceName = getInvocationServiceName().evaluate(parameterResolver);

        LocalCacheEventChannelBuilder localEventChannelBuilder = new LocalCacheEventChannelBuilder();

        localEventChannelBuilder.setConflictResolverBuilder(getConflictResolverBuilder());
        localEventChannelBuilder.setTargetCacheName(getTargetCacheName());

        return new ParallelLocalCacheEventChannel(targetCacheName,
                                                  invocationServiceName,
                                                  localEventChannelBuilder,
                                                  registry,
                                                  parameterResolver);
    }


    /**
     * Return the target cache name expression.
     *
     * @return the target cache name expression
     */
    public Expression<String> getTargetCacheName()
    {
        return targetCacheName;
    }


    /**
     * Set the target cache name expression.
     *
     * @param targetCacheName  the target cache name expression
     */
    @Injectable
    public void setTargetCacheName(Expression targetCacheName)
    {
        this.targetCacheName = SerializableExpressionHelper.ensureSerializable(targetCacheName);
    }


    /**
     * Return the conflict resolver builder.
     *
     * @return  the conflict resolver builder
     */
    public ParameterizedBuilder<ConflictResolver> getConflictResolverBuilder()
    {
        return conflictResolverBuilder;
    }


    /**
     * Set the conflict resolver builder.
     *
     * @param conflictResolverBuilder  the conflict resolver builder
     */
    @Injectable("conflict-resolver-scheme")
    public void setConflictResolverBuilder(ParameterizedBuilder<ConflictResolver> conflictResolverBuilder)
    {
        this.conflictResolverBuilder = conflictResolverBuilder;
    }


    /**
     * Return the InvocationService name expression.
     *
     * @return the Invocation Service name expression
     */
    public Expression<String> getInvocationServiceName()
    {
        return invocationServiceName;
    }


    /**
     * Set the InvocationService name expression.
     *
     * @param serviceName  the Invocation Service name expression
     */
    @Injectable("remote-invocation-service-scheme")
    public void setInvocationServiceName(Expression serviceName)
    {
        invocationServiceName = SerializableExpressionHelper
                .ensureSerializable(serviceName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.targetCacheName         = (Expression) ExternalizableHelper.readObject(in);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, targetCacheName);
        ExternalizableHelper.writeObject(out, conflictResolverBuilder);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.targetCacheName         = (Expression) reader.readObject(100);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) reader.readObject(101);
        this.invocationServiceName   = (Expression) reader.readObject(102);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(100, targetCacheName);
        writer.writeObject(101, conflictResolverBuilder);
        writer.writeObject(102, invocationServiceName);
    }
}
