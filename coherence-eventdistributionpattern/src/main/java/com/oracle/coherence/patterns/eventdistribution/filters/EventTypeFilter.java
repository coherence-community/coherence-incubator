/*
 * File: EventTypeFilter.java
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
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.EntryUpdatedEvent;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * An {@link EventTypeFilter} is a specialized {@link Filter} that checks
 * the type of {@link EntryEvent}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class EventTypeFilter implements Filter, Serializable, PortableObject
{
    /**
     * The type of {@link EntryEvent}s.
     */
    public enum EventType {INSERTED,
                           UPDATED,
                           REMOVED;}


    /**
     * The set of {@link EventType}s that satisfy the {@link EventTypeFilter}.
     */
    private EnumSet<EventType> m_setEventTypes;


    /**
     * Constructs an {@link EventTypeFilter}.
     * <p>
     * Required for {@link Serializable} and {@link PortableObject}.
     */
    public EventTypeFilter()
    {
        m_setEventTypes = EnumSet.noneOf(EventType.class);
    }


    /**
     * Construct an {@link EventTypeFilter}.
     *
     * @param eventTypes  the type of {@link EntryEvent}s that satisfy the
     *                    {@link EventTypeFilter}
     */
    public EventTypeFilter(EnumSet<EventType> setEventTypes)
    {
        m_setEventTypes = EnumSet.copyOf(setEventTypes);
    }


    /**
     * Construct an {@link EventTypeFilter}.
     *
     * @param eventTypes  the type of {@link EntryEvent}s that satisfy the
     *                    {@link EventTypeFilter}
     */
    public EventTypeFilter(EventType... eventTypes)
    {
        m_setEventTypes = EnumSet.copyOf(Arrays.asList(eventTypes));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(Object oValue)
    {
        return oValue instanceof EntryInsertedEvent && m_setEventTypes.contains(EventType.INSERTED)
               || oValue instanceof EntryUpdatedEvent && m_setEventTypes.contains(EventType.UPDATED)
               || oValue instanceof EntryRemovedEvent && m_setEventTypes.contains(EventType.REMOVED);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        int size = reader.readInt(0);

        m_setEventTypes = EnumSet.noneOf(EventType.class);

        for (int i = 0; i < size; i++)
        {
            m_setEventTypes.add(EventType.values()[reader.readInt(i + 1)]);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeInt(0, m_setEventTypes.size());

        int i = 1;

        for (EventType eventType : m_setEventTypes)
        {
            writer.writeInt(i++, eventType.ordinal());
        }
    }
}
