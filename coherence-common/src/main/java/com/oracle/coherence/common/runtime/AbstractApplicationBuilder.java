/*
 * File: AbstractApplicationBuilder.java
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
import com.tangosol.util.UUID;

import java.io.IOException;
import java.util.ArrayList;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link AbstractApplicationBuilder} is a base implementation of an {@link ApplicationBuilder}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public abstract class AbstractApplicationBuilder<A extends Application, S extends ApplicationSchema<A, S>,
                                                 B extends ApplicationBuilder<A, S, B>>
    implements ApplicationBuilder<A, S, B>
{
    /**
     * The collection of {@link LifecycleEvent} {@link EventProcessor}s to apply to {@link Application} events.
     */
    protected ArrayList<EventProcessor<LifecycleEvent<A>>> m_lifecycleEventProcessors;


    /**
     * Constructs an {@link AbstractApplicationBuilder} with default {@link ApplicationSchema}.
     */
    public AbstractApplicationBuilder()
    {
        m_lifecycleEventProcessors = new ArrayList<EventProcessor<LifecycleEvent<A>>>();
    }


    /**
     * {@inheritDoc}
     */
    public A realize(S schema) throws IOException
    {
        return realize(schema, new UUID().toString(), new NullApplicationConsole());
    }


    /**
     * {@inheritDoc}
     */
    public A realize(S      schema,
                     String name) throws IOException
    {
        return realize(schema, name, new SystemApplicationConsole());
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public B addApplicationLifecycleProcessor(EventProcessor<LifecycleEvent<A>> processor)
    {
        m_lifecycleEventProcessors.add(processor);

        return (B) this;
    }
}
