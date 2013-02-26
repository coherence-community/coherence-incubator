/*
 * File: MutatingEventIteratorTransformer.java
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
import com.oracle.coherence.common.util.UniformTransformer;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.EventTransformer;

import java.util.Iterator;

/**
 * An {@link MutatingEventIteratorTransformer} is an {@link EventIteratorTransformer} that simply applies an
 * {@link EventTransformer} to each {@link Event} in the provided {@link Iterator}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class MutatingEventIteratorTransformer implements EventIteratorTransformer
{
    /**
     * A {@link EventTransformer} that will return a mutated {@link Event} for a given {@link Event}.
     */
    private EventTransformer transformer;


    /**
     * Standard Constructor.
     *
     * @param transformer The {@link UniformTransformer} to be used to transform/mutate individual {@link Event}s.
     */
    public MutatingEventIteratorTransformer(EventTransformer transformer)
    {
        this.transformer = transformer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Event> transform(final Iterator<Event> events)
    {
        return new Iterator<Event>()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext()
            {
                return events.hasNext();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Event next()
            {
                return transformer.transform(events.next());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void remove()
            {
                throw new UnsupportedOperationException("Can't remove an Event from an EventIteratorTransformer");
            }

        };
    }
}
