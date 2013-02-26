/*
 * File: AbstractAsynchronousEventProcessor.java
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

package com.oracle.coherence.common.events.processing;

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.environment.Environment;
import com.tangosol.net.CacheFactory;

import java.util.concurrent.ExecutorService;

/**
 * An {@link AbstractAsynchronousEventProcessor} provides an abstract implementation of an {@link EventProcessor}
 * that will aysnchronously process an event (on another thread), instead of the dispatching thread.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <E> the type of the {@link Event}
 *
 * @author Brian Oliver
 */
public abstract class AbstractAsynchronousEventProcessor<E extends Event> implements EventProcessor<E>
{
    /**
     * {@inheritDoc}
     */
    public final void process(final EventDispatcher eventDispatcher,
                              final E               event)
    {
        // schedule the event to be processed later on another thread provided by the executor service
        Environment environment = (Environment) CacheFactory.getConfigurableCacheFactory();

        environment.getResource(ExecutorService.class).execute(new Runnable()
        {
            public void run()
            {
                processLater(eventDispatcher, event);
            }
        });
    }


    /**
     * Process the {@link Event} raised by the specified {@link EventDispatcher}.
     * <p>
     * NOTE: This method is invoked by a thread different to the thread that raised the {@link Event}.
     * The raising thread does not block, and thus this allows asynchronous processing of events.
     *
     * @param eventDispatcher The {@link EventDispatcher} that raised the {@link Event}
     * @param event The {@link Event}.
     */
    public abstract void processLater(EventDispatcher eventDispatcher,
                                      E               event);
}
