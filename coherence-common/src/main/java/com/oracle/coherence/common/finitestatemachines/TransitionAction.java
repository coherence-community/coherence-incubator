/*
 * File: TransitionAction.java
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
 * An {@link TransitionAction} encapsulates the actions to be performed as part
 * of the {@link Transition} from one state to another.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Transition
 *
 * @author Brian Oliver
 */
public interface TransitionAction<S extends Enum<S>>
{
    /**
     * Performs the necessary actions as part of a {@link Transition} from one
     * state to another, triggered by a specified {@link Event}.
     * <p>
     * Note: After executing this method the {@link FiniteStateMachine} will
     * automatically move to the state specified by the {@link Transition}.
     *
     * @param sTransitionName  the name of the transition
     * @param stateFrom        the state from which the transition is occurring
     * @param stateTo          the state to which the transition is occurring
     * @param event            the {@link Event} that triggered the {@link Transition}
     * @param context          the {@link ExecutionContext} for the action
     *
     * @throws RollbackTransitionException  if the {@link Transition} should be
     *                                      aborted and the {@link FiniteStateMachine}
     *                                      should be left in it's current state
     */
    public void onTransition(String           sName,
                             S                stateFrom,
                             S                stateTo,
                             Event<S>         event,
                             ExecutionContext context) throws RollbackTransitionException;
}
