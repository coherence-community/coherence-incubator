/*
 * File: EventProcessor.java
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
 * An {@link EventProcessor} provides an implementation that is capable of
 * "processing" an {@link Event}.
 * <p>
 * While it's possible for {@link EventProcessor} instances to be reused,
 * they themselves should be <strong>immutable</strong> or thread-safe so that
 * if an instance is scheduled for execution by multiple threads concurrently,
 * their state is not corrupted.
 * <p>
 * <strong>NOTE:</strong>{@link EventProcessor}s are <strong>always</strong>
 * on non-Coherence threads.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <E> The {@link com.oracle.coherence.common.events.Event} type
 *
 * @author Brian Oliver
 */
public interface EventProcessor<E extends Event>
{
    /**
     * Perform necessary processing of the provided {@link Event}.
     *
     * @param eventDispatcher
     *            The {@link EventDispatcher} that dispatched the specified
     *            {@link Event} for processing
     * @param event
     *            The {@link Event} to be processed
     */
    public void process(EventDispatcher eventDispatcher,
                        E               event);
}
