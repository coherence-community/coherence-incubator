/*
 * File: ReflectiveBuilder.java
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
 * A {@link ReflectiveBuilder} enables runtime type information to be determined about the instance of objects
 * a {@link Builder} may produce.
 * <p>
 * This interface essentially provides us with the ability to determine the actual type of objects
 * a {@link Builder} may realize at runtime, without needing to realize an object.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Mark Johnson
 * @author Brian Oliver
 */
public interface ReflectiveBuilder<T> extends Builder<T>
{
    /**
     * Returns if the implementing class (of builder) will realize instances of the specified {@link Class}.
     *
     * @param clazz             The {@link Class} that we expect may be realized by the {@link ReflectiveBuilder}
     * @param parameterProvider The {@link ParameterProvider} providing
     *                          {@link com.oracle.coherence.configuration.parameters.Parameter}s possibly used by the builder.
     * @return <code>true</code> if the specified {@link Class} may be realized by the {@link ReflectiveBuilder}.
     */
    public boolean realizesClassOf(Class<?>          clazz,
                                   ParameterProvider parameterProvider);
}
