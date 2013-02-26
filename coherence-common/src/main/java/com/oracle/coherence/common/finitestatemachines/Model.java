/*
 * File: Model.java
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

import java.util.Map;

/**
 * A {@link Model} represents the definition of a {@link FiniteStateMachine},
 * the set of known states, {@link Transition}s between said states and
 * {@link StateEntryAction}s / {@link StateExitAction}s to be performed when
 * said states are changed.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of state of the {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 */
public interface Model<S extends Enum<S>>
{
    /**
     * Obtains the {@link Transition}s defined by the {@link Model}.
     *
     * @return the {@link Transition}s defined by the {@link Model}
     */
    public Iterable<Transition<S>> getTransitions();


    /**
     * Obtains the {@link Class} of the state of the {@link Model}.
     *
     * @return  the {@link Class} of the state
     */
    public Class<S> getStateClass();


    /**
     * Obtains the valid states defined by the {@link Model}.
     *
     * @return  the valid states of the {@link Model}
     */
    public S[] getStates();


    /**
     * Obtains the {@link StateEntryAction}s defined for the states in the
     * {@link Model}.
     *
     * @return  the defined {@link StateEntryAction}s defined for the states in
     *          the {@link Model}
     */
    public Map<S, StateEntryAction<S>> getStateEntryActions();


    /**
     * Obtains the {@link StateExitAction}s defined for the states in the
     * {@link Model}.
     *
     * @return  the defined {@link StateExitAction}s defined for the states in
     *          the {@link Model}
     */
    public Map<S, StateExitAction<S>> getStateExitActions();
}
