/*
 * File: ParameterProvider.java
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

package com.oracle.coherence.configuration.parameters;

import com.oracle.coherence.configuration.expressions.Expression;

import java.util.Map;
import java.util.Properties;

/**
 * A {@link ParameterProvider} provides a mechanism to request and access an immutable collection of
 * {@link Parameter}s.
 * <p>
 * For {@link ParameterProvider}s that support mutation of the collection, refer to the
 * {@link MutableParameterProvider}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ParameterProvider extends Iterable<Parameter>
{
    /**
     * Returns the specified named {@link Parameter}.
     *
     * @param name The name of the {@link Parameter}
     * @return A {@link Parameter} or <code>null</code> if the {@link Parameter} is unknown to
     *         the {@link ParameterProvider}.
     */
    public Parameter getParameter(String name);


    /**
     * Determines if the specified named {@link Parameter} is known by the {@link ParameterProvider}.
     * @param name The name of the {@link Parameter}
     *
     * @return <code>true</code> if the parameter is known by the {@link ParameterProvider},
     *         <code>false</code> otherwise.
     */
    public boolean isDefined(String name);


    /**
     * Determines if the {@link ParameterProvider} has known {@link Parameter}s.
     *
     * @return <code>true</code> if the {@link ParameterProvider} knows of one or more {@link Parameter}s
     *         <code>false</code> otherwise
     */
    public boolean isEmpty();


    /**
     * Determines the number of {@link Parameter}s known to the {@link ParameterProvider}.
     *
     * @return The number of {@link Parameter}s known to the {@link ParameterProvider}
     */
    public int size();


    /**
     * Constructs a {@link Map} of all of the {@link Parameter}s, each coerced to the require type if they
     * are strongly typed.
     *
     * @param parameterProvider The {@link ParameterProvider} to use to resolve expressions used by {@link Parameter}s.
     *
     * @return {@link Map}
     */
    public Map<String, ?> getParameters(ParameterProvider parameterProvider);


    /**
     * Constructs a Java {@link Properties} implementation containing all of the {@link Parameter}s,
     * each coerced to the require type (if they are strongly typed).
     *
     * @param parameterProvider The {@link ParameterProvider} to use to resolve {@link Expression}s within {@link Parameter}s.
     *
     * @return {@link Properties}
     */
    public Properties getProperties(ParameterProvider parameterProvider);
}
