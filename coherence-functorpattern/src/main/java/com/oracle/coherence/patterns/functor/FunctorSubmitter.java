/*
 * File: FunctorSubmitter.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.CommandSubmitter;
import com.oracle.coherence.patterns.command.Context;

import java.util.concurrent.Future;

/**
 * A {@link FunctorSubmitter} provides the mechanism by which
 * {@link Functor}s may be submitted for execution in a specified
 * identifiable {@link Context}.
 * <p>
 * A {@link FunctorSubmitter} is a specialized {@link CommandSubmitter}.
 * All {@link FunctorSubmitter}s are capable of also submitting
 * {@link Command}s for execution.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @see Functor
 * @see CommandSubmitter
 */
public interface FunctorSubmitter extends CommandSubmitter
{
    /**
     * Asynchronously submits the provided {@link Functor} for execution
     * against the {@link Context} with the specified {@link Identifier},
     * returning a {@link Future} that may be used to access the return value
     * from the {@link Functor}
     *
     * @param contextIdentifier
     * @param functor
     *
     * @throws IllegalArgumentException If no {@link Context} with the specified {@link Identifier} exists.
     */
    public <C extends Context, T> Future<T> submitFunctor(Identifier    contextIdentifier,
                                                          Functor<C, T> functor);


    /**
     * Asynchronously submits the provided {@link Functor} for execution
     * against the {@link Context} with the specified {@link Identifier},
     * returning a {@link Future} that may be used to access the return value
     * from the {@link Functor}
     * <p>
     * If the specified {@link Context} does not exist, but the allowSubmissionWhenContextDoesNotExist
     * parameter is <code>true</code>, the provided {@link Functor} will be queued for
     * execution when the {@link Context} is created.
     *
     * @param contextIdentifier
     * @param functor
     * @param allowSubmissionWhenContextDoesNotExist
     *
     * @throws IllegalArgumentException If no {@link Context} with the specified {@link Identifier} exists
     *                                  and allowSubmissionWhenContextDoesNotExist is <code>false</code>
     */
    public <C extends Context, T> Future<T> submitFunctor(Identifier    contextIdentifier,
                                                          Functor<C, T> functor,
                                                          boolean       allowSubmissionWhenContextDoesNotExist);
}
