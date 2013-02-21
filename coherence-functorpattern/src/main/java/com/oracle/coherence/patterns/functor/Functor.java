/*
 * File: Functor.java
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

package com.oracle.coherence.patterns.functor;

import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;

/**
 * A {@link Functor} represents some action (and required parameters)
 * to be performed asynchronously against a {@link Context} (of type C) that
 * returns some value of (type T).
 * <p>
 * {@link Functor}s are related to {@link Command}s, with the exception that
 * {@link Functor}s may return a value to the submitter where as {@link Command}s
 * may not.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <C> The type of the {@link Context}
 * @param <T> The return type from the execution of the {@link Functor}
 *
 * @author Brian Oliver
 *
 * @see Context
 * @see Command
 */
public interface Functor<C extends Context, T>
{
    /**
     * Executes the {@link Functor} using the provided
     * {@link ExecutionEnvironment} returning a value of type T.
     *
     * @param executionEnvironment The environment in which the {@link Command}
     *                             is being executed.
     */
    public T execute(ExecutionEnvironment<C> executionEnvironment);
}
