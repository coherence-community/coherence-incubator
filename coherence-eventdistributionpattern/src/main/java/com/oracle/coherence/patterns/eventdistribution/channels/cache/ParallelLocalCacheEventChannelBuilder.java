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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Constant;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.channels.AbstractEventChannelBuilder;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;

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
    private Expression targetCacheName;

    /**
     * The {@link com.oracle.coherence.common.builders.ParameterizedBuilder} that can realize a {@link com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver} when required.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder = null;

    /**
     * The name of the Invocation Service to use.
     */
    private Expression invocationServiceName;


    /**
     * Required for {@link com.tangosol.io.ExternalizableLite} and {@link com.tangosol.io.pof.PortableObject}.
     */
    public ParallelLocalCacheEventChannelBuilder()
    {
        super();
        invocationServiceName = new Constant(INVOCATION_SVC);
    }


    /**
     * {@inheritDoc}
     */
    public EventChannel realize(ParameterProvider parameterProvider)
    {
        String                        targetCacheName = getTargetCacheName().evaluate(parameterProvider).getString();
        String invocationServiceName = getInvocationServiceName().evaluate(parameterProvider).getString();

        LocalCacheEventChannelBuilder localEventChannelBuilder = new LocalCacheEventChannelBuilder();

        localEventChannelBuilder.setConflictResolverBuilder(getConflictResolverBuilder());
        localEventChannelBuilder.setTargetCacheName(getTargetCacheName());

        return new ParallelLocalCacheEventChannel(targetCacheName,
                                                  invocationServiceName,
                                                  localEventChannelBuilder,
                                                  parameterProvider);
    }


    /**
     * Method description
     *
     * @return
     */
    public Expression getTargetCacheName()
    {
        return targetCacheName;
    }


    /**
     * Method description
     *
     * @param targetCacheName
     */
    @Property("target-cache-name")
    @Mandatory
    @Type(Expression.class)
    @SubType(String.class)
    public void setTargetCacheName(Expression targetCacheName)
    {
        this.targetCacheName = targetCacheName;
    }


    /**
     * Method description
     *
     * @return the Conflict Resolver Builder
     */
    public ParameterizedBuilder<ConflictResolver> getConflictResolverBuilder()
    {
        return conflictResolverBuilder;
    }


    /**
     * Method description
     *
     * @param conflictResolverBuilder
     */
    @Property("conflict-resolver-scheme")
    @Type(ParameterizedBuilder.class)
    @SubType(ConflictResolver.class)
    public void setConflictResolverBuilder(ParameterizedBuilder<ConflictResolver> conflictResolverBuilder)
    {
        this.conflictResolverBuilder = conflictResolverBuilder;
    }


    /**
     * Method description
     *
     * @return the Invocation Service name
     */
    public Expression getInvocationServiceName()
    {
        return invocationServiceName;
    }


    /**
     * Method description
     *
     * @param serviceName The name of the service to use
     */
    @Property("invocation-service-name")
    @Type(Expression.class)
    @SubType(String.class)
    public void setInvocationServiceName(Expression serviceName)
    {
        invocationServiceName = serviceName;
    }


    /**
     * Method description
     *
     * @param in
     *
     * @throws java.io.IOException
     */
    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.targetCacheName         = (Expression) ExternalizableHelper.readObject(in);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) ExternalizableHelper.readObject(in);
    }


    /**
     * Method description
     *
     * @param out
     *
     * @throws java.io.IOException
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, targetCacheName);
        ExternalizableHelper.writeObject(out, conflictResolverBuilder);
    }


    /**
     * Method description
     *
     * @param reader
     *
     * @throws java.io.IOException
     */
    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.targetCacheName         = (Expression) reader.readObject(100);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) reader.readObject(101);
        this.invocationServiceName   = (Expression) reader.readObject(102);
    }


    /**
     * Method description
     *
     * @param writer
     *
     * @throws java.io.IOException
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
