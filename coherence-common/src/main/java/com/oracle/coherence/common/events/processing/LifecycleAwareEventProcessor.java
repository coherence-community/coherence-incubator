/*
 * File: LifecycleAwareEventProcessor.java
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

import com.oracle.coherence.common.events.dispatching.EventDispatcher;

/**
 * A {@link LifecycleAwareEventProcessor} is a friend interface of {@link EventProcessor}s that enables those
 * that are registered with an {@link EventDispatcher} to receive callbacks/notifications during their registration
 * lifecycle.  This allows {@link EventProcessor}s to perform necessary initialization and cleanup during according to
 * their lifecycle.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface LifecycleAwareEventProcessor
{
    /**
     * Called by the {@link EventDispatcher} before the {@link EventProcessor} is registered.
     *
     * @param eventDispatcher The {@link EventDispatcher}
     */
    public void onBeforeRegistered(EventDispatcher eventDispatcher);


    /**
     * Called by the {@link EventDispatcher} after the {@link EventProcessor} was registered.
     *
     * @param eventDispatcher The {@link EventDispatcher}
     */
    public void onAfterRegistered(EventDispatcher eventDispatcher);


    /**
     * Called by the {@link EventDispatcher} after the {@link EventProcessor} was unregistered.
     *
     * @param eventDispatcher The {@link EventDispatcher}
     */
    public void onAfterUnregistered(EventDispatcher eventDispatcher);
}
