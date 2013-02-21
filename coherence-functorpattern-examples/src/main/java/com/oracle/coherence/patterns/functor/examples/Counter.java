/*
 * File: Counter.java
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

package com.oracle.coherence.patterns.functor.examples;

import com.oracle.coherence.patterns.command.Context;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

/**
 * A {@link Counter} for the {@link FunctorPatternExample}. It is incremented
 * by one each time the {@link NextValueFunctor} executes.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class Counter implements Context, Serializable, PortableObject
{
    private static final long serialVersionUID = 3245746282404451450L;

    /**
     * The next value
     */
    private long next;


    /**
     * Empty constructor required by PortableObject
     */
    public Counter()
    {
    }


    /**
     * Construct a new Counter.
     *
     * @param initialValue the initial value
     */
    public Counter(long initialValue)
    {
        this.next = initialValue;
    }


    /**
     * Increment the {@link Counter}.
     *
     * @return the next value
     */
    public long next()
    {
        return next++;
    }


    /**
     * Return a string representation of this {@link Counter}.
     */
    public String toString()
    {
        return String.format("Counter{next=%d}", next);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        next = reader.readLong(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, next);
    }
}
