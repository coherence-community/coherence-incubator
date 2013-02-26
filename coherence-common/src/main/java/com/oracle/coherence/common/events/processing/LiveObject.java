/*
 * File: LiveObject.java
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

import com.oracle.coherence.common.events.EntryEvent;

import java.util.Map.Entry;

/**
 * A {@link LiveObject} is a cache {@link java.util.Map.Entry} value that is capable of handling
 * {@link com.oracle.coherence.common.events.Event}s that occur on itself.
 * <p>
 * Essentially a {@link LiveObject} is a specialized {@link EventProcessor}, specifically for {@link EntryEvent}s.
 * <p>
 * Unlike the original LifecycleAwareCacheEntry class from Coherence Common 1.5 which where based on
 * {@link com.tangosol.util.MapEvent}s, {@link LiveObject}s are based on the new {@link EntryEvent}
 * interfaces/classes.
 * <p>
 * Note however, like Coherence Common 1.5, processing of the {@link com.oracle.coherence.common.events.Event} occurs
 * on the thread that raised the said {@link com.oracle.coherence.common.events.Event} and as such care should be taken
 * when processing the {@link com.oracle.coherence.common.events.Event} as this will occur on a Coherence
 * Service/Worker thread.  If there is any doubt as to the thread-safety of calls made by the
 * {@link EventProcessor#process(com.oracle.coherence.common.events.dispatching.EventDispatcher, Event)} method,
 * the said method should use an alternate thread for execution.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("rawtypes")
public interface LiveObject<E extends Entry> extends LifecycleAwareEntry<E>
{
    /**
     * This interface is deliberately empty as it inherits the signatures of the EventProcessor.
     */
}
