/*
 * File: FilteringEventIteratorTransformer.java
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
import com.oracle.coherence.common.util.FilteringIterator;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.tangosol.util.Filter;

import java.util.Iterator;

/**
 * A {@link FilteringEventIteratorTransformer} applies a {@link Filter} to batch of
 * {@link Event}s, returning only those that satisfy the {@link Filter} in the resulting batch.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class FilteringEventIteratorTransformer implements EventIteratorTransformer
{
    /**
     * The {@link Filter} that will be applied to each {@link Event} in the batch.  Only
     * those {@link Event}s satisfying the {@link Filter} will be in the returned batch.
     */
    private Filter filter;


    /**
     * Standard Constructor.
     */
    public FilteringEventIteratorTransformer(Filter filter)
    {
        this.filter = filter;
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Event> transform(Iterator<Event> events)
    {
        return new FilteringIterator<Event>(filter, events);
    }
}
