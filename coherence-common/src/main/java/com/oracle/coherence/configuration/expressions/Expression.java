/*
 * File: Expression.java
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

package com.oracle.coherence.configuration.expressions;

import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.parameters.ParameterProvider;

/**
 * A {@link Expression} represents a user-defined expression that may be evaluated at
 * runtime to produce a {@link Value}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface Expression
{
    /**
     * Evaluates the expression to produce a resulting {@link Value}.
     *
     * @param parameterProvider The {@link ParameterProvider} that may be used to determine parameters occurring in
     *                          the {@link Expression}
     *
     * @return The {@link Value} as a result of evaluating the expression
     */
    public Value evaluate(ParameterProvider parameterProvider);


    /**
     * Determines if the {@link Expression} is serializable.
     *
     * @return <code>true</code> if the expression can be serialized, <code>false</code> otherwise.
     */
    public boolean isSerializable();
}
