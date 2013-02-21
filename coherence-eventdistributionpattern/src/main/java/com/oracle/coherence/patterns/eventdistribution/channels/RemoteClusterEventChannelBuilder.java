/*
 * File: RemoteClusterEventChannelBuilder.java
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

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.InvocationService;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link RemoteClusterEventChannelBuilder} is an {@link EventChannelBuilder} for
 * {@link RemoteClusterEventChannel}s that may be configured through dependency injection.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class RemoteClusterEventChannelBuilder extends AbstractInterClusterEventChannelBuilder
{
    /**
     * The name of the remote {@link InvocationService} that we'll use to distribute {@link Event}s
     * to the remote cluster.
     */
    private String remoteInvocationServiceName;

    /**
     * The {@link EventChannelBuilder} that can produce an {@link EventChannel} that is capable of distributing
     * {@link Event}s in the remote cluster.
     */
    private EventChannelBuilder remoteEventChannelBuilder;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public RemoteClusterEventChannelBuilder()
    {
        super();
    }


    /**
     * Set the service name used for doing remote invocation.
     */
    @Property("remote-invocation-service-name")
    public void setRemoteInvocationServiceName(String remoteInvocationServiceName)
    {
        this.remoteInvocationServiceName = remoteInvocationServiceName;
    }


    /**
     * Get the service name used for doing remote invocation.
     */
    public String getRemoteInvocationServiceName()
    {
        return this.remoteInvocationServiceName;
    }


    /**
     * Set the {@link EventChannelBuilder} used for doing remote distribution.
     */
    @Property("remote-channel-scheme")
    @Mandatory
    public void setRemoteEventChannelBuilder(EventChannelBuilder remoteEventChannelBuilder)
    {
        this.remoteEventChannelBuilder = remoteEventChannelBuilder;
    }


    /**
     * Get the {@link EventChannelBuilder} that we can use to realize an {@link EventChannel} for remote
     * {@link Event} distribution.
     */
    public EventChannelBuilder getRemoteEventChannelBuilder()
    {
        return remoteEventChannelBuilder;
    }


    /**
     * {@inheritDoc}
     */
    public EventChannel realize(ParameterProvider parameterProvider)
    {
        return new RemoteClusterEventChannel(getDistributionRole(),
                                             remoteInvocationServiceName,
                                             remoteEventChannelBuilder,
                                             parameterProvider);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.remoteInvocationServiceName = ExternalizableHelper.readSafeUTF(in);
        this.remoteEventChannelBuilder   = (EventChannelBuilder) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeSafeUTF(out, remoteInvocationServiceName);
        ExternalizableHelper.writeObject(out, remoteEventChannelBuilder);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.remoteInvocationServiceName = reader.readString(201);
        this.remoteEventChannelBuilder   = (EventChannelBuilder) reader.readObject(202);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeString(201, remoteInvocationServiceName);
        writer.writeObject(202, remoteEventChannelBuilder);
    }
}
