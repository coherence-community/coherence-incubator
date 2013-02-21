/*
 * File: NextValueFunctor.java
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

import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.oracle.coherence.patterns.functor.Functor;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

/**
 * A {@link NextValueFunctor} will increment the {@link Counter} by one.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
@SuppressWarnings("serial")
public class NextValueFunctor implements Functor<Counter, Long>, Serializable, PortableObject
{
    /**
     * Empty required by PortableObject
     */
    public NextValueFunctor()
    {
    }


    /**
     * Increment the counter by 1.
     *
     * @param executionEnvironment the ExecutionEnvironment which is represented as a {@link Counter}.
     */
    public Long execute(ExecutionEnvironment<Counter> executionEnvironment)
    {
        Counter counter = executionEnvironment.getContext();
        long    next    = counter.next();

        executionEnvironment.setContext(counter);

        return next;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
    }
}
