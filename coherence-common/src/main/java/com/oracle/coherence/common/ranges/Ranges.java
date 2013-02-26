/*
 * File: Ranges.java
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

/**
 * A set of static methods to help with the construction of {@link Range}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class Ranges
{
    /**
     * A constant representing an empty {@link Range} starting at 0.
     */
    public static Range EMPTY = new ContiguousRange(0);

    /**
     * A constant representing an infinite {@link Range} of values.
     */
    public static Range INFINITE = new InfiniteRange();


    /**
     * No instances of a {@link Ranges} class is permitted.
     */
    private Ranges()
    {
    }


    /**
     * A helper method to construct an empty {@link Range} commencing
     * at a specified value.
     *
     * @param from the start of the range
     *
     * @return an empty {@link Range} commencing at a specified value
     */
    public static Range newEmptyRangeFrom(long from)
    {
        return new ContiguousRange(from);
    }


    /**
     * A helper method to construct a {@link Range} with only a single
     * value.
     *
     * @param value the value to have in the {@link Range}
     *
     * @return a {@link Range} with only a single
     */
    public static Range newSingletonRange(long value)
    {
        return new ContiguousRange(value, value);
    }


    /**
     * A helper method to construct a {@link Range} between (and including)
     * two values.
     *
     * @param from the start of the {@link Range}
     * @param to   the end of the {@link Range}
     *
     * @return a {@link Range} between (and including) two values.
     */
    public static Range newRange(long from,
                                 long to)
    {
        return new ContiguousRange(Math.min(from, to), Math.max(from, to));
    }
}
