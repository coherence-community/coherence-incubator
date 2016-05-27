/*
 * File: ContextsManager.java
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

package com.oracle.coherence.patterns.command;

import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.util.ValueExtractor;

/**
 * A {@link ContextsManager} provides the mechanism by which
 * we may register and manage {@link Context}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ContextsManager
{
    /**
     * Registers a {@link Context} with the specified {@link Identifier}
     * and {@link ContextConfiguration}.
     * <p>
     * The returned {@link Identifier} may then be used to
     * manage the {@link Context}, including submitting {@link Command}s
     * for execution.
     *
     * @param identifier
     * @param context
     * @param contextConfiguration
     */
    public Identifier registerContext(Identifier           identifier,
                                      Context              context,
                                      ContextConfiguration contextConfiguration);


    /**
     * Registers a {@link Context} with the specified {@link Identifier}
     * using a {@link DefaultContextConfiguration}.
     * <p>
     * The returned {@link Identifier} may then be used to
     * manage the {@link Context}, including submitting {@link Command}s
     * for execution.
     *
     * @param identifier
     * @param context
     */
    public Identifier registerContext(Identifier identifier,
                                      Context    context);


    /**
     * Registers a {@link Context} with the specified contextName
     * and {@link ContextConfiguration}.
     * <p>
     * The returned {@link Identifier} may then be used to
     * manage the {@link Context}, including submitting {@link Command}s
     * for execution.
     *
     * @param contextName
     * @param context
     * @param contextConfiguration
     */
    public Identifier registerContext(String               contextName,
                                      Context              context,
                                      ContextConfiguration contextConfiguration);


    /**
     * Registers a {@link Context} with the specified contextName
     * using a {@link DefaultContextConfiguration}.
     *
     * The returned {@link Identifier} may then be used to
     * manage the {@link Context}, including submitting {@link Command}s
     * for execution.
     *
     * @param contextName
     * @param context
     */
    public Identifier registerContext(String  contextName,
                                      Context context);


    /**
     * Registers a {@link Context} with a specified {@link ContextConfiguration}.
     * <p>
     * NOTE: A unique {@link Identifier} will be generated for the {@link Context}.
     * <p>
     * The returned {@link Identifier} may then be used to
     * manage the {@link Context}, including submitting {@link Command}s
     * for execution.
     *
     * @param context
     * @param contextConfiguration
     */
    public Identifier registerContext(Context              context,
                                      ContextConfiguration contextConfiguration);


    /**
     * Registers a {@link Context} using a {@link DefaultContextConfiguration}.
     * <p>
     * NOTE: A unique {@link Identifier} will be generated for the {@link Context}.
     * <p>
     * The returned {@link Identifier} may then be used to
     * manage the {@link Context}, including submitting {@link Command}s
     * for execution.
     *
     * @param context
     */
    public Identifier registerContext(Context context);


    /**
     * Returns the most recent updated version of the {@link Context} with the
     * specified {@link Identifier}.
     *
     * @param identifier
     */
    public Context getContext(Identifier identifier);


    /**
     * Extracts a value from the most recently updated version of the
     * {@link Context} with the provided {@link ValueExtractor}.
     *
     * @param identifier
     * @param valueExtractor
     */
    public Object extractValueFromContext(Identifier     identifier,
                                          ValueExtractor valueExtractor);
}
