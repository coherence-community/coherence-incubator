/*
 * File: FiniteStateMachine.java
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
 * A {@link FiniteStateMachine} implements a general purpose finite-state-machine.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Model
 * @see Transition
 * @see TransitionAction
 * @see StateEntryAction
 *
 * @author Brian Oliver
 */
public interface FiniteStateMachine<S extends Enum<S>>
{
    /**
     * Obtains the name of the {@link FiniteStateMachine}.  This is primarily used for
     * display/logging/monitoring purposes.
     *
     * @return the name of the {@link FiniteStateMachine}
     */
    public String getName();


    /**
     * Request the {@link FiniteStateMachine} to process the specified {@link Event}.
     * <p>
     * Note: There's no guarantee that the {@link Event} will processed because:
     * <ol>
     *    <li>the {@link Transition} to be performed for the {@link Event}
     *        is invalid as the {@link FiniteStateMachine} is not the required
     *        starting state.
     *    <li>the {@link FiniteStateMachine} may have been stopped.
     *
     * @param event  the {@link Event} for the {@link FiniteStateMachine} to
     *               process
     */
    public void process(Event<S> event);


    /**
     * Obtains the current state of the {@link FiniteStateMachine}.
     * <p>
     * Note: After returning the current state there's no guaranteed that the state will be the same, simply
     * because a {@link Transition} may be executing asynchronously in the background on another thread.
     *
     * @return  the current state of the {@link FiniteStateMachine}
     */
    public S getState();


    /**
     * Obtains the number of transitions that have occurred in the {@link FiniteStateMachine}.
     * <p>
     * Note: After returning the count there's no guaranteed that the count will be the same on next request,
     * simply because a {@link Transition} may be executing asynchronously in the background on another thread.
     *
     * @return  the number of transitions that have occurred in the {@link FiniteStateMachine}
     */
    public long getTransitionCount();


    /**
     * Stops the {@link FiniteStateMachine} as soon as possible.
     * <p>
     * Note: Once stopped a {@link FiniteStateMachine} can't be restarted.  Instead a new {@link FiniteStateMachine}
     * should be created.
     *
     * @return <code>true</code> if the stop was successful, <code>false</code> it's already stopped.
     */
    public boolean stop();
}
