/*
 * File: LifecycleEvent.java
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

package com.oracle.coherence.common.events.lifecycle;

import com.oracle.coherence.common.events.Event;

/**
 * A {@link LifecycleEvent} is use to capture {@link Event}s relating to the lifecycle
 * of a particular source object.
 * <p>
 * A good example of {@link LifecycleEvent}s are those that relate to monitoring the lifecycle
 * of a Coherence {@link com.tangosol.net.Service}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S> The source type
 *
 * @author Brian Oliver
 */
public interface LifecycleEvent<S> extends Event
{
    /**
     * <p>Returns the source of the {@link LifecycleEvent}. ie: the object on which the
     * {@link LifecycleEvent} was observed.</p>
     *
     * @return The source of the {@link LifecycleEvent}
     */
    public S getSource();
}
