/*
 * File: SequenceGenerator.java
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

package com.oracle.coherence.common.sequencegenerators;

import com.oracle.coherence.common.ranges.Range;

/**
 * A {@link SequenceGenerator} may be used to generate one-or-more
 * non-overlapping monotonically increasing long numbers.
 * <p>
 * NOTE: All {@link SequenceGenerator} implementations <strong>must</strong>
 * be thread-safe. An individual {@link SequenceGenerator} must be capable of
 * being used by multiple-threads simultaneously without corrupting the underlying
 * sequence.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface SequenceGenerator
{
    /**
     * Creates and returns the next long from the sequence.
     *
     * @return The next long from a sequence of longs.
     */
    public long next();


    /**
     * Creates and returns a {@link Range} of monotonically increasing numbers
     * from the sequence, the resulting {@link Range} being of length sequenceSize.
     *
     * @param sequenceSize The number of values in the resulting {@link Range}.
     *
     * @return A {@link Range} of longs with the specified size.
     */
    public Range next(long sequenceSize);
}
