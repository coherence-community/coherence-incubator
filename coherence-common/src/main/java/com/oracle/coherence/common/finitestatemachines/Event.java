/*
 * File: Event.java
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
 * An {@link Event} captures the information that may trigger a
 * {@link Transition} in a {@link FiniteStateMachine} from one state to
 * another.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of the state of the {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 */
public interface Event<S extends Enum<S>>
{
    /**
     * Determines the desired state of the {@link FiniteStateMachine}
     * for the {@link Event} given the current state of the
     * {@link FiniteStateMachine}.
     *
     * @param state    the current state of the {@link FiniteStateMachine}
     * @param context  the {@link ExecutionContext} for the {@link Event}
     *
     * @return  the desired state of the {@link FiniteStateMachine} or
     *          <code>null</code> if no transition is required
     */
    public S getDesiredState(S                state,
                             ExecutionContext context);
}
