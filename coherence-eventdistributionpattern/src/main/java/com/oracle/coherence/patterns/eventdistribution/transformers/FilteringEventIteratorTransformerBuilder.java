/*
 * File: FilteringEventIteratorTransformerBuilder.java
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
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link FilteringEventIteratorTransformerBuilder} is a {@link ParameterizedBuilder} for {@link FilteringEventIteratorTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class FilteringEventIteratorTransformerBuilder extends AbstractEventIteratorTransformerBuilder
{
    /**
     * The {@link Filter} to use with the {@link FilteringEventIteratorTransformer}.
     */
    private Filter filter;


    /**
     * Standard Constructor (required for {@link PortableObject} and {@link ExternalizableLite}).
     */
    public FilteringEventIteratorTransformerBuilder()
    {
        // deliberately empty
    }


    /**
     * Returns the {@link Filter} that will be used with the {@link FilteringEventIteratorTransformer}.
     *
     * @return The {@link Filter}
     */
    public Filter getFilter()
    {
        return filter;
    }


    /**
     * Sets the {@link Filter} that will be used with the {@link FilteringEventIteratorTransformerBuilder}.
     *
     * @param filter The {@link Filter}
     */
    @Property("filter")
    @Mandatory
    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }


    /**
     * {@inheritDoc}
     */
    public EventIteratorTransformer realize(ParameterProvider parameterProvider)
    {
        return new FilteringEventIteratorTransformer(filter);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.filter = (Filter) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, filter);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.filter = (Filter) reader.readObject(100);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(100, filter);
    }
}
