/*
 * File: UppercaseEntryEventTransformer.java
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

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventTransformer;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;

/**
 * An {@link UppercaseEntryEventTransformer} transforms {@link EntryEvent}
 * {@link String}-based values to uppercase.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class UppercaseEntryEventTransformer implements EventTransformer
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Event transform(Event event)
    {
        if (event instanceof DistributableEntryEvent)
        {
            DistributableEntryEvent evtDistributable = (DistributableEntryEvent) event;

            Object                  oValue           = evtDistributable.getEntry().getValue();

            if (oValue != null && oValue instanceof String)
            {
                String sValue    = (String) oValue;
                String sNewValue = sValue.toUpperCase();

                evtDistributable.getEntry().setValue(sNewValue);

                return evtDistributable;
            }
            else
            {
                return event;
            }
        }
        else
        {
            return event;
        }
    }
}
