/*
 * File: ExampleEventFilter.java
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

import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryInsertedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryRemovedEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryUpdatedEvent;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map.Entry;

/**
 * A simple example of an {@link DistributableEntryEvent} {@link Filter}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author  Brian Oliver
 */
@SuppressWarnings("serial")
public class ExampleEventFilter implements Filter, Serializable, PortableObject
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean evaluate(Object object)
    {
        if (object instanceof String)
        {
            return containsVowel((String) object);
        }
        else if (object instanceof Entry && ((Entry) object).getValue() instanceof String)
        {
            return containsVowel((String) ((Entry) object).getValue());
        }
        else if (object instanceof DistributableEntryInsertedEvent || object instanceof DistributableEntryUpdatedEvent)
        {
            DistributableEntryEvent event = (DistributableEntryEvent) object;
            DistributableEntry      entry = event.getEntry();
            Object                  value = entry.getValue();

            return value instanceof String ? containsVowel((String) value) : false;
        }
        else if (object instanceof DistributableEntryRemovedEvent)
        {
            return true;
        }
        else
        {
            throw new IllegalArgumentException("Unknown Event Type to Filter: " + object);
        }
    }


    /**
     * Determines if a specified {@link String} contains at least one vowel.
     *
     * @param s  the string
     * @return if the string contains at least one vowel
     */
    private boolean containsVowel(String s)
    {
        return s.contains("a") || s.contains("e") || s.contains("i") || s.contains("o") || s.contains("u");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        // skip: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        // skip: deliberately empty
    }
}
