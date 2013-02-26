/*
 * File: DistributableEntryEvent.java
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

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link DistributableEntryEvent} is an abstract {@link EntryEvent} implementation specifically designed for
 * distribution with an {@link EventDistributor}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class DistributableEntryEvent extends DistributableCacheEvent implements EntryEvent<DistributableEntry>
{
    /**
     * Then {@link DistributableEntry} for the {@link EntryEvent}.
     */
    private DistributableEntry entry;


    /**
     * <p>Required for {@link ExternalizableLite} and {@link PortableObject} support.</p>
     */
    public DistributableEntryEvent()
    {
        // SKIP: deliberately empty
    }


    /**
     * Standard Constructor.
     *
     * @param cacheName         The name of the cache on which the {@link EntryEvent} occurred.
     * @param entry             The {@link DistributableEntry} on which the {@link EntryEvent} occurred.
     */
    public DistributableEntryEvent(String             cacheName,
                                   DistributableEntry entry)
    {
        super(cacheName);
        this.entry = entry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DistributableEntry getEntry()
    {
        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.entry = (DistributableEntry) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, entry);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.entry = (DistributableEntry) reader.readObject(101);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(101, entry);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("%s{cacheName=%s, entry=%s}", getClass().getName(), getCacheName(), getEntry());
    }
}
