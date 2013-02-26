/*
 * File: DispatcherManager.java
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

package com.oracle.coherence.patterns.processing.friends;

import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;

/**
 * A {@link DispatcherManager} is responsible for registering and
 * unregistering {@link Dispatcher}s for the processing pattern.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public interface DispatcherManager extends DependentResource
{
    /**
     * Register a {@link Dispatcher} with the system.
     *
     * @param priority   the relative priority to other Dispatchers
     *                   NOTE: no two dispatchers can be registered with the same priority
     * @param dispatcher the {@link Dispatcher} to register
     */
    public void registerDispatcher(int        priority,
                                   Dispatcher dispatcher);


    /**
     * Unregister a {@link Dispatcher} with the system.
     *
     * @param priority   the priority of the {@link Dispatcher} to remove
     * @param dispatcher the {@link Dispatcher} to unregister
     */
    public void unregisterDispatcher(int        priority,
                                     Dispatcher dispatcher);
}
