/*
 * File: ChainedEventIteratorTransformerBuilder.java
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
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A {@link ChainedEventIteratorTransformerBuilder} is a  builder for {@link ChainedEventIteratorTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class ChainedEventIteratorTransformerBuilder extends AbstractEventIteratorTransformerBuilder
{
    /**
     * The list of {@link ParameterizedBuilder}s of {@link EventIteratorTransformer}s to be chained/applied in order.
     */
    private ArrayList<ParameterizedBuilder<EventIteratorTransformer>> transformerBuilders;


    /**
     * Standard Constructor (required for {@link PortableObject} and {@link ExternalizableLite}).
     */
    public ChainedEventIteratorTransformerBuilder()
    {
        this.transformerBuilders = new ArrayList<ParameterizedBuilder<EventIteratorTransformer>>();
    }


    /**
     * Adds the specified {@link EventIteratorTransformer} builder to the {@link ChainedEventIteratorTransformerBuilder}.
     *
     * @param builder The {@link ParameterizedBuilder} for the {@link EventIteratorTransformer}.
     */
    public void addEventsTransformerBuilder(ParameterizedBuilder<EventIteratorTransformer> builder)
    {
        transformerBuilders.add(builder);
    }


    /**
     * {@inheritDoc}
     */
    public EventIteratorTransformer realize(ParameterProvider parameterProvider)
    {
        // construct a list of EventsTransformers from their builders
        ArrayList<EventIteratorTransformer> transformers = new ArrayList<EventIteratorTransformer>();

        for (ParameterizedBuilder<EventIteratorTransformer> builder : transformerBuilders)
        {
            transformers.add(builder.realize(parameterProvider));
        }

        return new ChainedEventIteratorTransformer(transformers);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.transformerBuilders = new ArrayList<ParameterizedBuilder<EventIteratorTransformer>>();
        ExternalizableHelper.readCollection(in, transformerBuilders, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeCollection(out, transformerBuilders);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.transformerBuilders = new ArrayList<ParameterizedBuilder<EventIteratorTransformer>>();
        reader.readCollection(100, transformerBuilders);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeCollection(100, transformerBuilders);
    }
}
