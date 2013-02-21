/*
 * File: FunctorResult.java
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

package com.oracle.coherence.patterns.functor.internal;

import com.oracle.coherence.patterns.functor.Functor;

/**
 * A {@link FunctorResult} represents the result of executing
 * one or more {@link Functor}s.
 * <p>
 * For some types of {@link Functor} execution(s), a single value
 * may be returned.  For others, like streaming {@link Functor}s,
 * multiple values may be returned.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface FunctorResult
{
    /**
     * The Coherence Cache that will hold {@link Functor} execution results.
     */
    public static final String CACHENAME = "coherence.functorpattern.results";


    /**
     * Returns if the execution has been completed and the
     * associated result is completed.
     *
     * NOTE: The execution of some functor(s) may return (or stream back)
     * multiple results. The purpose of this method is to determine when
     * there are no more results to be returned.
     */
    public boolean isComplete();
}
