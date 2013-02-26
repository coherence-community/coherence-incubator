/*
 * File: Range.java
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

package com.oracle.coherence.common.ranges;

import java.util.Iterator;

/**
 * A {@link Range} represents an immutable sequence of increasing long integer values.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface Range extends Iterable<Long>
{
    /**
     * Returns the start of the {@link Range}.
     *
     * @return the start of the {@link Range}
     */
    public long getFrom();


    /**
     * Returns the end of the {@link Range}.
     *
     * @return the end of the {@link Range}
     */
    public long getTo();


    /**
     * Returns the number of values in the {@link Range}.
     *
     * @return the number of values in the {@link Range}
     */
    public long size();


    /**
     * Returns if the {@link Range} is empty.
     *
     * @return if the {@link Range} is empty
     */
    public boolean isEmpty();


    /**
     * Returns if the {@link Range} is a singleton range.  That is
     * the number of values in the range is a single value, starting at
     * 'x' and finishing at 'x'.
     *
     * @return if the {@link Range} is a singleton range
     */
    public boolean isSingleton();


    /**
     * Returns if the {@link Range} contains the specified value.
     *
     * @param value the value to look for
     *
     * @return if the {@link Range} contains the specified value
     */
    public boolean contains(long value);


    /**
     * Returns if this {@link Range} intersects with another.
     *
     * @param other the other {@link Range}nge to check
     *
     * @return if this {@link Range} intersects with another
     */
    public boolean intersects(Range other);


    /**
     * Returns if this {@link Range} is adjacent to another.
     *
     * @param other the other {@link Range} to check
     *
     * @return if this {@link Range} is adjacent to another
     */
    public boolean isAdjacent(Range other);


    /**
     * Creates and returns a new {@link Range} representing the union of
     * this and another {@link Range}.
     *
     * @param other the other {@link Range}
     *
     * @return a new {@link Range} representing the union of
     * this and another {@link Range}
     */
    public Range union(Range other);


    /**
     * Creates and returns a new {@link Range} based on the
     * current {@link Range} but with the specified value added.
     *
     * @param value the value to add
     *
     * @return a new {@link Range} based on the current {@link Range} with the specified value added
     */
    public Range add(long value);


    /**
     * Creates and returns a new {@link Range} based on this {@link Range}
     * but with the specified value removed.
     *
     * @param value the value to remove
     *
     * @return returns a new {@link Range} based on this {@link Range} with the specified value removed
     */
    public Range remove(long value);


    /**
     * Returns an {@link Iterator} over the values in a {@link Range},
     * in order from lowest value to the highest value inclusive.
     *
     * @return an {@link Iterator} over the values in a {@link Range}
     */
    public Iterator<Long> iterator();
}
