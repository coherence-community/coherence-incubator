/*
 * File: RemoteCacheEventChannelBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;


import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.AbstractInterClusterEventChannelBuilder;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.net.CacheService;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link RemoteCacheEventChannelBuilder} is an {@link EventChannelBuilder} for {@link RemoteCacheEventChannel}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class RemoteCacheEventChannelBuilder extends AbstractInterClusterEventChannelBuilder
{
    /**
     * The name of the cache to which the {@link EntryEvent}s will be distributed and applied.
     */
    private Expression<String> targetCacheName;

    /**
     * The {@link ParameterizedBuilder} that can realize a {@link ConflictResolver} when required.
     */
    private ParameterizedBuilder<ConflictResolver> conflictResolverBuilder = null;

    /**
     * The name of the remote {@link CacheService} that we'll use to attach to the remote cache.
     */
    private String remoteCacheServiceName;


    /**
     * Standard Constructor.
     */
    public RemoteCacheEventChannelBuilder()
    {
        super();
    }


    /**
     * Return the target cache name expression.
     *
     * @return  the target cache name expression
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
    public void setTargetCacheName(Expression<String> targetCacheName)
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
     * Return the remote cache service name.
     *
     * @return  the remote cache service name
     */
    public String getRemoteCacheServiceName()
    {
        return remoteCacheServiceName;
    }


    /**
     * Set the remote cache service name.
     *
     * @param remoteCacheServiceName  the remote cache service name
     */
    @Injectable("remote-cache-service-name")
    public void setRemoteCacheServiceName(String remoteCacheServiceName)
    {
        this.remoteCacheServiceName = remoteCacheServiceName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventChannel realize(ParameterResolver resolver,
                                ClassLoader       classLoader,
                                ParameterList     parameters)
    {
        return new RemoteCacheEventChannel(getDistributionRole(),
                                           getTargetCacheName().evaluate(resolver),
                                           getConflictResolverBuilder(),
                                           remoteCacheServiceName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.targetCacheName         = (Expression) ExternalizableHelper.readObject(in);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) ExternalizableHelper.readObject(in);
        this.remoteCacheServiceName  = ExternalizableHelper.readSafeUTF(in);
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
        ExternalizableHelper.writeSafeUTF(out, remoteCacheServiceName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.targetCacheName         = (Expression) reader.readObject(201);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) reader.readObject(202);
        this.remoteCacheServiceName  = reader.readString(203);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(201, targetCacheName);
        writer.writeObject(202, conflictResolverBuilder);
        writer.writeString(203, remoteCacheServiceName);
    }
}
