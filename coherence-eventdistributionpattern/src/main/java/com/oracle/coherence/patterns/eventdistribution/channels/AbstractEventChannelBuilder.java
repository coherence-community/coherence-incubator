/*
 * File: AbstractEventChannelBuilder.java
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

import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link AbstractEventChannelBuilder} is a base implementation of the {@link EventChannelBuilder}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Bob Hanckel
 */
@SuppressWarnings("serial")
public abstract class AbstractEventChannelBuilder implements EventChannelBuilder
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public AbstractEventChannelBuilder()
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizesClassOf(Class<?>          clazz,
                                   ParameterProvider parameterProvider)
    {
        return EventChannel.class.isAssignableFrom(clazz);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        // SKIP: deliberately empty
    }
}
