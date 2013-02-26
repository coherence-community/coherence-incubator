/*
 * File: LifecycleAwareEvent.java
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

package com.oracle.coherence.common.finitestatemachines;

/**
 * A {@link LifecycleAwareEvent} is a specialized {@link Event} that
 * receives callbacks during the processing of the {@link Event} by
 * a {@link FiniteStateMachine}.
 *
 * @param <S>  the type of state of a {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 */
public interface LifecycleAwareEvent<S extends Enum<S>> extends Event<S>
{
    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * is initially about to accepted for processing.
     *
     * @param context  the {@link ExecutionContext} for the {@link Event}
     *
     * @return <code>true</code> if the {@link Event} should be accepted, or
     *         <code>false</code> if the {@link FiniteStateMachine} should
     *         ignore the {@link LifecycleAwareEvent}
     */
    public boolean onAccept(ExecutionContext context);


    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * is about to be processed.
     *
     * @param context  the {@link ExecutionContext} for the {@link Event}
     */
    public void onProcessing(ExecutionContext context);


    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * has been processed.
     *
     * @param context  the {@link ExecutionContext} for the {@link Event}
     */
    public void onProcessed(ExecutionContext context);
}
