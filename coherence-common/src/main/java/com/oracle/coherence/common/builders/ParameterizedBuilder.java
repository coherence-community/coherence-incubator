/*
 * File: ParameterizedBuilder.java
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

package com.oracle.coherence.common.builders;

import com.oracle.coherence.configuration.parameters.ParameterProvider;

/**
 * An {@link ParameterizedBuilder} is a {@link ReflectiveBuilder} that requires runtime
 * {@link com.oracle.coherence.configuration.parameters.Parameter}s, supplied by a {@link ParameterProvider},
 * in order to realize an object.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <T> The class of object that will be produced by the {@link ParameterizedBuilder}
 *
 * @author Brian Oliver
 */
public interface ParameterizedBuilder<T> extends ReflectiveBuilder<T>
{
    /**
     * Realizes an instance of the specified type of class.
     *
     * @param parameterProvider The {@link ParameterProvider} that provides
     *                          {@link com.oracle.coherence.configuration.parameters.Parameter}s.
     *
     * @return An instance of <T>
     */
    public T realize(ParameterProvider parameterProvider);
}
