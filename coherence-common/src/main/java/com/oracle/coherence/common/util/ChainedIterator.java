/*
 * File: ChainedIterator.java
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A {@link ChainedIterator} makes an ordered collection of other {@link Iterator}s appear and operate
 * as a single {@link Iterator}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ChainedIterator<E> implements Iterator<E>
{
    private LinkedList<Iterator<E>> iterators;


    /**
     * Standard Constructor.
     */
    public ChainedIterator()
    {
        iterators = new LinkedList<Iterator<E>>();
    }


    /**
     * Standard Constructor (using var args)
     *
     * @param iterators An array of {@link Iterator}s.
     */
    public ChainedIterator(Iterator<E>... iterators)
    {
        this.iterators.addAll(Arrays.asList(iterators));
    }


    /**
     * Adds the specified {@link Iterator} to the {@link ChainedIterator} (as the last)
     *
     * @param iterator An {@link Iterator}
     *
     * @return A {@link ChainedIterator}.
     */
    public ChainedIterator<E> addIterator(Iterator<E> iterator)
    {
        this.iterators.addLast(iterator);

        return this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext()
    {
        while (iterators.size() > 0 &&!iterators.get(0).hasNext())
        {
            iterators.removeFirst();
        }

        return iterators.size() > 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public E next()
    {
        if (hasNext())
        {
            return iterators.get(0).next();
        }
        else
        {
            throw new IndexOutOfBoundsException("Attempted to iterator past the end of an ChainedIterator");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Can't remove from an ChainedIterator");
    }
}
