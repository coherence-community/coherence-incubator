/*
 * File: DistributableEntryUpdatedEvent.java
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

import com.oracle.coherence.common.events.EntryUpdatedEvent;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link DistributableEntryUpdatedEvent} is an {@link EntryUpdatedEvent} implementation specifically designed
 * for distribution using an {@link EventDistributor}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class DistributableEntryUpdatedEvent extends DistributableEntryEvent
    implements EntryUpdatedEvent<DistributableEntry>
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject} support.
     */
    public DistributableEntryUpdatedEvent()
    {
        super();
    }


    /**
     * Standard Constructor.
     */
    public DistributableEntryUpdatedEvent(String             cacheName,
                                          DistributableEntry distributableEntry)
    {
        super(cacheName, distributableEntry);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DistributableEntry getOriginalEntry()
    {
        throw new UnsupportedOperationException("The OriginalEntry is available as part of the getEntry()");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
    }
}
