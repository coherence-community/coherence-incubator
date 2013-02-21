/*
 * File: SimpleEventDispatcher.java
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

package com.oracle.coherence.common.events.dispatching;

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.events.processing.LifecycleAwareEventProcessor;
import com.oracle.coherence.environment.Environment;
import com.tangosol.util.Filter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple implementation of an {@link EventDispatcher}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class SimpleEventDispatcher implements EventDispatcher
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(SimpleEventDispatcher.class.getName());

    /**
     * The {@link Environment} in which the {@link EventDispatcher} is operating.
     */
    private Environment environment;

    /**
     * The currently registered {@link EventProcessor}s that will be used to
     * process {@link Event}s.
     */
    private ConcurrentHashMap<Filter, CopyOnWriteArraySet<EventProcessor<?>>> eventProcessors;


    /**
     * Standard Constructor.
     *
     * @param environment the {@link Environment} to use
     */
    public SimpleEventDispatcher(Environment environment)
    {
        this.environment     = environment;
        this.eventProcessors = new ConcurrentHashMap<Filter, CopyOnWriteArraySet<EventProcessor<?>>>();
    }


    /**
     * {@inheritDoc}
     */
    public Environment getEnvironment()
    {
        return environment;
    }


    /**
     * {@inheritDoc}
     */
    public <E extends Event> void dispatchEvent(E                 event,
                                                EventProcessor<E> eventProcessor)
    {
        // have the specified event processed with the specified event processor
        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "[Commenced] Processing {0} with {1}", new Object[] {event, eventProcessor});
        }

        ((EventProcessor<E>) eventProcessor).process(this, event);

        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "[Completed] Processing {0} with {1}", new Object[] {event, eventProcessor});
        }

        // dispatch the event to other event processors
        dispatchEvent(event);
    }


    /**
     * {@inheritDoc}
     */
    public <E extends Event> void dispatchEventLater(final E event)
    {
        // schedule the event to be dispatched later using our execution service
        getEnvironment().getResource(ExecutorService.class).execute(new Runnable()
        {
            public void run()
            {
                dispatchEvent(event);
            }
        });

    }


    /**
     * {@inheritDoc}
     */
    public <E extends Event> boolean registerEventProcessor(Filter            filter,
                                                            EventProcessor<E> eventProcessor)
    {
        // while the underlying data-structures are thread-safe, we need to guard against
        // two threads that attempt to add and remove the same filter and event processor simultaneously
        // (as two data-structures are involved, we could get a race)
        synchronized (eventProcessors)
        {
            Set<EventProcessor<?>> eventProcessorSet = eventProcessors.putIfAbsent(filter,
                                                                                   new CopyOnWriteArraySet<EventProcessor<?>>());

            if (eventProcessorSet == null)
            {
                eventProcessorSet = eventProcessors.get(filter);
            }

            boolean isAlreadyRegistered = eventProcessorSet.contains(eventProcessor);

            if (!isAlreadyRegistered)
            {
                if (eventProcessor instanceof LifecycleAwareEventProcessor)
                {
                    ((LifecycleAwareEventProcessor) eventProcessor).onBeforeRegistered(this);
                }

                eventProcessorSet.add(eventProcessor);

                if (eventProcessor instanceof LifecycleAwareEventProcessor)
                {
                    ((LifecycleAwareEventProcessor) eventProcessor).onAfterRegistered(this);
                }
            }

            return !isAlreadyRegistered;
        }
    }


    /**
     * {@inheritDoc}
     */
    public <E extends Event> void unregisterEventProcessor(Filter            filter,
                                                           EventProcessor<E> eventProcessor)
    {
        // while the underlying data-structures are thread-safe, we need to guard against
        // two threads that attempt to add and remove the same filter and event processor simultaneously
        // (as two data-structures are involved, we could get a race)
        synchronized (eventProcessors)
        {
            Set<EventProcessor<?>> eventProcessorSet = eventProcessors.get(filter);

            if (eventProcessorSet != null)
            {
                if (eventProcessorSet.remove(eventProcessor))
                {
                    if (eventProcessor instanceof LifecycleAwareEventProcessor)
                    {
                        ((LifecycleAwareEventProcessor) eventProcessor).onAfterUnregistered(this);
                    }
                }

                if (eventProcessorSet.isEmpty())
                {
                    eventProcessors.remove(filter);

                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <E extends Event> void dispatchEvent(E event)
    {
        for (Filter filter : eventProcessors.keySet())
        {
            if (filter.evaluate(event))
            {
                for (EventProcessor<?> eventProcessor : eventProcessors.get(filter))
                {
                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.log(Level.FINEST,
                                   "[Commenced] Processing {0} with {1}",
                                   new Object[] {event, eventProcessor});
                    }

                    ((EventProcessor<E>) eventProcessor).process(this, event);

                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.log(Level.FINEST,
                                   "[Completed] Processing {0} with {1}",
                                   new Object[] {event, eventProcessor});
                    }
                }
            }
        }
    }
}
