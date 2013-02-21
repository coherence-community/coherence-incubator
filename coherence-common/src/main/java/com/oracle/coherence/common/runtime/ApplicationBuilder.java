/*
 * File: ApplicationBuilder.java
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

package com.oracle.coherence.common.runtime;

import com.oracle.coherence.common.events.lifecycle.LifecycleEvent;
import com.oracle.coherence.common.events.processing.EventProcessor;

import java.io.IOException;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link ApplicationBuilder} is responsible the creation of {@link Application}s based on {@link ApplicationSchema}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A> The type of the {@link Application}s the {@link ApplicationBuilder} will realize.
 * @param <S> The type of the {@link ApplicationSchema} for the {@link Application}s.
 * @param <B> The type of the {@link ApplicationBuilder} (to support fluent-style setters with sub-classes of
 *            {@link ApplicationBuilder}).
 *
 * @author Brian Oliver
 */
@Deprecated
public interface ApplicationBuilder<A extends Application, S extends ApplicationSchema<A, S>,
                                    B extends ApplicationBuilder<A, S, B>>
{
    /**
     * Realizes an instance of an {@link Application}.
     *
     * @param schema    The {@link ApplicationSchema} to use for realizing the {@link Application}.
     * @param name      The name of the application.
     * @param console   The {@link ApplicationConsole} that will be used for I/O by the
     *                  realized {@link Application}. This may be <code>null</code> if not required.
     *
     * @return An {@link Application} representing the application realized by the {@link ApplicationBuilder}.
     *
     * @throws IOException thrown if a problem occurs while starting the application.
     */
    public A realize(S                  schema,
                     String             name,
                     ApplicationConsole console) throws IOException;


    /**
     * Realizes an instance of an {@link Application} (using a {@link SystemApplicationConsole}).
     *
     * @param schema    The {@link ApplicationSchema} to use for realizing the {@link Application}.
     * @param name      The name of the application.
     *
     * @return An {@link Application} representing the application realized by the {@link ApplicationBuilder}.
     *
     * @throws IOException thrown if a problem occurs while starting the application.
     */
    public A realize(S      schema,
                     String name) throws IOException;


    /**
     * Realizes an instance of an {@link Application} (without a name and using a {@link NullApplicationConsole}).
     *
     * @param schema    The {@link ApplicationSchema} to use for realizing the {@link Application}.
     *
     * @return An {@link Application} representing the application realized by the {@link ApplicationBuilder}.
     *
     * @throws IOException thrown if a problem occurs while starting the application.
     */
    public A realize(S schema) throws IOException;


    /**
     * Adds a {@link LifecycleEvent} {@link EventProcessor} to the {@link ApplicationBuilder} allowing
     * developers to intercept and react to {@link LifecycleEvent}s during the process of building, starting and
     * stopping {@link Application}s.
     *
     * @param processor The {@link LifecycleEvent} {@link EventProcessor}.
     *
     * @return The {@link ApplicationBuilder}.
     */
    public B addApplicationLifecycleProcessor(EventProcessor<LifecycleEvent<A>> processor);
}
