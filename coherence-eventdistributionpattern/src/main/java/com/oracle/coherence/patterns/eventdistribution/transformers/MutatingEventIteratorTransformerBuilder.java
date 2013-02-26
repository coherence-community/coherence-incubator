/*
 * File: MutatingEventIteratorTransformerBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.transformers;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.util.UniformTransformer;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.EventTransformer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link MutatingEventIteratorTransformerBuilder} is a {@link ParameterizedBuilder} for {@link MutatingEventIteratorTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class MutatingEventIteratorTransformerBuilder extends AbstractEventIteratorTransformerBuilder
{
    /**
     * The {@link ParameterizedBuilder} for the {@link EventTransformer} that the {@link MutatingEventIteratorTransformer}
     * will use to transform events.
     */
    private ParameterizedBuilder<EventTransformer> m_bldrEventTransformer;


    /**
     * Constructor a {@link MutatingEventIteratorTransformer}. (required for {@link PortableObject} and {@link ExternalizableLite}).
     */
    public MutatingEventIteratorTransformerBuilder()
    {
        // deliberately empty
    }


    /**
     * Sets the {@link ParameterizedBuilder} that will be used to realize an {@link EventTransformer}.
     *
     * @param bldrEventTransformer The {@link UniformTransformer} for an {@link Event}.
     */
    @Property("event-transformer")
    @Type(ParameterizedBuilder.class)
    @SubType(EventTransformer.class)
    @Mandatory
    public void setEventTransformerBuilder(ParameterizedBuilder<EventTransformer> bldrEventTransformer)
    {
        m_bldrEventTransformer = bldrEventTransformer;
    }


    /**
     * {@inheritDoc}
     */
    public EventIteratorTransformer realize(ParameterProvider parameterProvider)
    {
        return new MutatingEventIteratorTransformer(m_bldrEventTransformer.realize(parameterProvider));
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        m_bldrEventTransformer = (ParameterizedBuilder<EventTransformer>) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, m_bldrEventTransformer);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        m_bldrEventTransformer = (ParameterizedBuilder<EventTransformer>) reader.readObject(100);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(100, m_bldrEventTransformer);
    }
}
