/*
 * File: EventDispatcher.java
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
import com.oracle.coherence.environment.Environment;
import com.tangosol.util.Filter;

/**
 * An {@link EventDispatcher} is responsible dispatching {@link Event}s to {@link EventProcessor}s for processing.
 * <p>
 * <strong>IMPORTANT:</strong> In general, {@link Event} dispatching and processing of said {@link Event}s via an
 * {@link EventDispatcher} always occurs on the thread that requested the dispatch.  This means that should a
 * Coherence owned thread request an {@link Event} to be dispatched and thus processed, said processing will occur
 * using/on the Coherence owned thread (unless the {@link EventProcessor} is asynchronous or {@link #dispatchEventLater(Event)}
 * was used for dispatching).
 * <p>
 * In such situations, where a Coherence owned thread performs the dispatching and thus execution of
 * {@link EventProcessor}s, care should be taken to ensure said {@link EventProcessor}s don't make re-entrant calls onto
 * Coherence APIs, specifically onto the same Coherence Service that owns the said thread.  This typically means
 * avoiding calls to CacheFactory.getCache(...) for caches managed by the said serivce thread.
 * <p>
 * Should an {@link EventProcessor} require access to a cache, and it can't be guaranteed that is safe to do so, the
 * {@link EventProcessor} should be made "asynchronous".
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventDispatcher
{
    /**
     * Returns the {@link Environment} in which the {@link EventDispatcher} is operating.
     *
     * @return An {@link Environment}.
     */
    public Environment getEnvironment();


    /**
     * Dispatches the specified {@link Event} to be processed by the specified and other appropriate
     * {@link EventProcessor}s <strong>using the calling thread.</strong>.
     *
     * @param <E> The type of {@link Event}
     * @param event The {@link Event} to be processed by the specified {@link EventProcessor}.
     * @param eventProcessor The {@link EventProcessor} to process the specified {@link Event}
     */
    public <E extends Event> void dispatchEvent(E                 event,
                                                EventProcessor<E> eventProcessor);


    /**
     * Dispatches the specified {@link Event} to be processed by the appropriate {@link EventProcessor}s
     * <strong>using the calling thread.</strong>.
     *
     * @param <E>   The type of {@link Event}
     * @param event The {@link Event} to be processed by the {@link EventProcessor}s.
     */
    public <E extends Event> void dispatchEvent(E event);


    /**
     * Requests that the specified {@link Event} to be dispatched and processed asynchronously
     * <strong>using an alternative thread (not the calling thread)</strong>.
     * <p>
     * <strong>NOTE:</strong> Use of this method is <strong>strongly discouraged</strong>. It is reserved only for use
     * in circumstances where Coherence events <strong>must always</strong> be processed on a non-Coherence thread.
     * There is no guarantee as to when the specified {@link Event} will be dispatched and processed, only that it
     * will occur at sometime in the future.
     *
     * @param <E>   The type of {@link Event}
     * @param event The {@link Event} to be processed by the {@link EventProcessor}s.
     */
    public <E extends Event> void dispatchEventLater(E event);


    /**
     * Registers the specified {@link EventProcessor} and associated {@link Filter} to be
     * used to select and process {@link Event}s dispatched by the {@link #dispatchEvent(Event)}
     * method.
     *
     * @param <E> The type of {@link Event}
     * @param filter The {@link Filter} to use to filter {@link Event}s prior to being executed
     *               by the {@link EventProcessor}.
     *
     *               NOTE: {@link Filter}s must implement hashCode() and equals(Object) methods correctly.
     * @param eventProcessor The {@link EventProcessor} to register.
     *
     * @return true If the specified {@link EventProcessor} was successfully registered,
     *         false if one is already registered.
     */
    public <E extends Event> boolean registerEventProcessor(Filter            filter,
                                                            EventProcessor<E> eventProcessor);


    /**
     * Unregisters the specified {@link EventProcessor} and associated {@link Filter} for processing
     * {@link Event}s.
     *
     * @param <E> The type of {@link Event}
     * @param filter The {@link Filter} use to register the {@link EventProcessor}.
     *               NOTE: {@link Filter}s must implement hashCode() and equals(Object) methods correctly
     * @param eventProcessor The {@link EventProcessor} that was registered.
     */
    public <E extends Event> void unregisterEventProcessor(Filter            filter,
                                                           EventProcessor<E> eventProcessor);
}
