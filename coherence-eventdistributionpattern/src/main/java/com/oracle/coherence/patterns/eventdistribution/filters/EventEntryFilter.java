/*
 * File: EventEntryFilter.java
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

package com.oracle.coherence.patterns.eventdistribution.filters;

import com.oracle.coherence.common.events.EntryEvent;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.EntryFilter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map.Entry;

/**
 * An {@link EventEntryFilter} is a {@link Filter} that is designed to
 * evaluate an {@link EntryEvent} by extracting the {@link Entry} contained
 * in the said {@link EntryEvent} and applying an {@link EntryFilter} to it.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class EventEntryFilter implements Filter, Serializable, PortableObject
{
    /**
     * The {@link EntryFilter} to apply to the {@link Entry} in the {@link EntryEvent}.
     */
    protected EntryFilter m_filter;


    /**
     * Constructs an {@link EventEntryFilter}.
     * <p>
     * Required for {@link Serializable} and {@link PortableObject}.
     */
    public EventEntryFilter()
    {
        // skip: deliberately empty
    }


    /**
     * Constructs an {@link EventEntryFilter}.
     *
     * @param filter  the nested {@link EntryFilter}
     */
    public EventEntryFilter(EntryFilter filter)
    {
        m_filter = filter;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(Object object)
    {
        if (object instanceof EntryEvent)
        {
            EntryEvent<?> e = (EntryEvent<?>) object;

            return m_filter.evaluateEntry(e.getEntry());
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        m_filter = (EntryFilter) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, m_filter);
    }
}
