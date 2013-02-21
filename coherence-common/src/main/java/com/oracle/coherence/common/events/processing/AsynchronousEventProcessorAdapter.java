/*
 * File: AsynchronousEventProcessorAdapter.java
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

/**
 * An {@link AsynchronousEventProcessorAdapter} adapts another {@link EventProcessor} implementation
 * to ensure that it is processed asynchronously (on another thread other than the one that raise the said
 * {@link Event}).
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <E> the {link Event} type
 *
 * @author Brian Oliver
 */
public class AsynchronousEventProcessorAdapter<E extends Event> extends AbstractAsynchronousEventProcessor<E>
{
    /**
     * The {@link EventProcessor} being adapted.
     */
    private EventProcessor<E> eventProcessor;


    /**
     * Standard Constructor.
     *
     * @param eventProcessor The {@link EventProcessor} to adapt.
     */
    public AsynchronousEventProcessorAdapter(EventProcessor<E> eventProcessor)
    {
        this.eventProcessor = eventProcessor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void processLater(EventDispatcher eventDispatcher,
                             E               event)
    {
        eventProcessor.process(eventDispatcher, event);
    }
}
