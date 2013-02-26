/*
 * File: FunctorFuture.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.functor.Functor;
import com.oracle.coherence.patterns.functor.FunctorSubmitter;

/**
 * A {@link FunctorFuture} is used to asynchronously capture
 * and return {@link FunctorResult}s back to the submitter of
 * one or more {@link Functor}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface FunctorFuture
{
    /**
     * Returns the {@link Identifier} of the {@link FunctorSubmitter}
     * that produced this {@link FunctorFuture}.
     */
    public Identifier getFunctorSubmitterIdentifier();


    /**
     * Returns the {@link Identifier} of the {@link FunctorResult}
     * that will be produced for this {@link FunctorFuture}.
     */
    public Identifier getFunctorResultIdentifier();


    /**
     * Accepts the specified {@link FunctorResult}.
     */
    public void acceptFunctorResult(FunctorResult functorResult);
}
