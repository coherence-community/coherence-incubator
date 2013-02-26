/*
 * File: ChainedEventIteratorTransformer.java
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

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link ChainedEventIteratorTransformer} chains a collection of {@link EventIteratorTransformer}s together
 * so they may be used as a single {@link EventIteratorTransformer}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ChainedEventIteratorTransformer implements EventIteratorTransformer
{
    /**
     * The list of {@link EventIteratorTransformer}s to apply in order to {@link Event}s.
     */
    private final ArrayList<EventIteratorTransformer> transformers;


    /**
     * Standard Constructor.
     */
    public ChainedEventIteratorTransformer()
    {
        this.transformers = new ArrayList<EventIteratorTransformer>();
    }


    /**
     * Standard Constructor.
     *
     * @param transformers The {@link EventIteratorTransformer}s to chain together.
     */
    public ChainedEventIteratorTransformer(EventIteratorTransformer... transformers)
    {
        this.transformers = new ArrayList<EventIteratorTransformer>(Arrays.asList(transformers));
    }


    /**
     * Standard Constructor.
     *
     * @param transformers The {@link EventIteratorTransformer}s to chain together.
     */
    public ChainedEventIteratorTransformer(List<EventIteratorTransformer> transformers)
    {
        this.transformers = new ArrayList<EventIteratorTransformer>();
        this.transformers.addAll(transformers);
    }


    /**
     * {@inheritDoc}
     */

    public Iterator<Event> transform(Iterator<Event> events)
    {
        // process the result with the EventsTransformers
        for (EventIteratorTransformer transformer : transformers)
        {
            events = transformer.transform(events);
        }

        return events;
    }
}
