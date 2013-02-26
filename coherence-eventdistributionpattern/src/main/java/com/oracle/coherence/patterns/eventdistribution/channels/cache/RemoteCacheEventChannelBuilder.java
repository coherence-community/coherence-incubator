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
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.AbstractInterClusterEventChannelBuilder;
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
    private Expression targetCacheName;

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
     * @return
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
     * @return
     */
    public String getRemoteCacheServiceName()
    {
        return remoteCacheServiceName;
    }


    /**
     * Method description
     *
     * @param remoteCacheServiceName
     */
    @Property("remote-cache-service-name")
    @Mandatory
    public void setRemoteCacheServiceName(String remoteCacheServiceName)
    {
        this.remoteCacheServiceName = remoteCacheServiceName;
    }


    /**
     * {@inheritDoc}
     */
    public EventChannel realize(ParameterProvider parameterProvider)
    {
        String targetCacheName = getTargetCacheName().evaluate(parameterProvider).getString();

        return new RemoteCacheEventChannel(getDistributionRole(),
                                           targetCacheName,
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
