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
     * is about to accepted for processing.
     * <p/>
     * This callback allows the {@link LifecycleAwareEvent} to establish internal
     * state and/or reject being accepted for processing based on the provided
     * {@link ExecutionContext}.
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
     * is about to be processed, specifically before the Transition or Entry
     * Actions are executed.
     *
     * @param exitingState  the State the {@link FiniteStateMachine} is about to exit
     * @param context       the {@link ExecutionContext} for the {@link Event}
     */
    public void onProcessing(S                exitingState,
                             ExecutionContext context);


    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * has been processed, specifically after the previous State Exit, Transition
     * and new State Entry Actions.
     *
     * @param enteredState  the State the {@link FiniteStateMachine} entered as
     *                      a result of the {@link LifecycleAwareEvent}
     * @param context       the {@link ExecutionContext} for the {@link Event}
     */
    public void onProcessed(S                enteredState,
                            ExecutionContext context);


    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * did not complete due to a failure.  Possible failures include: could not
     * find a valid transition to the required state, the desired state being
     * null of if an exception occurred.
     *
     * @param currentState  the State the {@link FiniteStateMachine} after the
     *                      failure
     * @param context       the {@link ExecutionContext} for the {@link Event}
     * @param exception     the {@link Exception} that may have caused the failure
     *                      (this will be null in cases where a transition
     *                       was illegal / not valid)
     */
    public void onFailure(S                currentState,
                          ExecutionContext context,
                          Exception        exception);
}
