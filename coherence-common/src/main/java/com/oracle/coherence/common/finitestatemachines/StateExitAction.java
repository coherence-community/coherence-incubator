/*
 * File: StateExitAction.java
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
 * A {@link StateExitAction} encapsulates the actions to be performed by
 * a {@link FiniteStateMachine} when leaving a known state.
 * <p>
 * A {@link StateExitAction} for a particular state will be executed
 * prior to a {@link FiniteStateMachine} entering a new state.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of the state
 *
 * @author Brian Oliver
 */
public interface StateExitAction<S extends Enum<S>>
{
    /**
     * Performs the necessary actions when a {@link FiniteStateMachine}
     * exits a particular state.
     *
     * @param state    the state of the {@link FiniteStateMachine} prior to
     *                 changing state
     * @param event    the {@link Event} that triggered the action
     * @param context  the {@link ExecutionContext} for the action
     */
    public void onExitState(S                state,
                            Event<S>         event,
                            ExecutionContext context);
}
