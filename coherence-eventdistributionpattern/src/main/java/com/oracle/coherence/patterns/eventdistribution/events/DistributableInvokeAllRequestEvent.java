/*
 * File: DistributableInvokeAllRequestEvent.java
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

package com.oracle.coherence.patterns.eventdistribution.events;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap.EntryProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link DistributableInvokeAllRequestEvent} represents a request to perform an invokeAll
 * on a cache.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class DistributableInvokeAllRequestEvent implements DistributableCacheInvocationRequestEvent
{
    /**
     * The {@link Filter} on which to invoke the {@link EntryProcessor}.
     */
    private Filter filter;

    /**
     * The {@link EntryProcessor} to invoke.
     */
    private EntryProcessor entryProcessor;


    /**
     * Standard Constructor (required for {@link PortableObject} and {@link ExternalizableLite}).
     */
    public DistributableInvokeAllRequestEvent()
    {
        // deliberately empty
    }


    /**
     * Standard Constructor.
     *
     * @param filter         The {@link Filter} to use with the invokeAll.
     * @param entryProcessor The {@link EntryProcessor} to invoke.
     */
    public DistributableInvokeAllRequestEvent(Filter         filter,
                                              EntryProcessor entryProcessor)
    {
        this.filter         = filter;
        this.entryProcessor = entryProcessor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(NamedCache namedCache)
    {
        namedCache.invokeAll(filter, entryProcessor);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.filter         = (Filter) ExternalizableHelper.readObject(in);
        this.entryProcessor = (EntryProcessor) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, filter);
        ExternalizableHelper.writeObject(out, entryProcessor);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.filter         = (Filter) reader.readObject(1);
        this.entryProcessor = (EntryProcessor) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, filter);
        writer.writeObject(2, entryProcessor);
    }
}
