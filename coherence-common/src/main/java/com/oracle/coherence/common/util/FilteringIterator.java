/*
 * File: FilteringIterator.java
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

package com.oracle.coherence.common.util;

import com.tangosol.util.Filter;

import java.util.Iterator;

/**
 * A {@link FilteringIterator} will (lazily) filter the results of a provided
 * {@link Iterable} with a provided {@link Filter}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @param <T> The type of values being iterated
 */
public class FilteringIterator<T> implements Iterator<T>
{
    /**
     * The {@link Filter} to apply to each element of the wrapped {@link Iterator}.
     */
    private Filter filter;

    /**
     * The underlying {@link Iterator} being filtered.
     */
    private Iterator<T> iterator;

    /**
     * The next value to return from the {@link Iterator}.
     * (when <code>null</code> the iteration is over).
     */
    private T next;


    /**
     * Standard Constructor.
     *
     * @param filter The filter to filter out values in the {@link Iterator}.
     * @param iterator The underlying {@link Iterator} of values being filtered.
     */
    public FilteringIterator(Filter      filter,
                             Iterator<T> iterator)
    {
        this.filter   = filter;
        this.iterator = iterator;
        this.next     = seekNext();
    }


    /**
     * Attempts to find the next value in the underlying {@link Iterator}
     * that satisfies the provided {@link Filter}.
     *
     * @return The next value satisfying the {@link Filter}, or <code>null</code> if none available.
     */
    private T seekNext()
    {
        T next = null;

        while (iterator != null && iterator.hasNext() && next == null)
        {
            next = iterator.next();

            if (filter != null &&!filter.evaluate(next))
            {
                next = null;
            }
        }

        return next;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
        return next != null;
    }


    /**
     * {@inheritDoc}
     */
    public T next()
    {
        T result = next;

        this.next = seekNext();

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void remove()
    {
        throw new UnsupportedOperationException("Can't remove() from a FilteringIterator");
    }
}
