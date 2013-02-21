/*
 * File: AbortablePhasedEvent.java
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

package com.oracle.coherence.common.events;

/**
 * A {@link AbortablePhasedEvent} is a specialized {@link Event}, much like a
 * {@link PhasedEvent}, that represents a long running activity which has an
 * explicit starting (commenced) and finishing (completed) {@link Event}, but
 * that of which may also aborted.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface AbortablePhasedEvent extends Event
{
    /**
     * The valid {@link Phase}s for the {@link AbortablePhasedEvent}.
     */
    public enum Phase
    {
        /**
         * The event has commenced running.
         */
        Commenced,

        /**
         * The event has completed.
         */
        Completed,

        /**
         * The event was aborted.
         */
        Aborted
    }


    ;

    /**
     * Returns the {@link Phase} of the {@link AbortablePhasedEvent}.
     *
     * @return the {@link Phase}
     */
    public Phase getPhase();
}
