/*
 * File: SimpleModel.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SimpleModel} is a basic implementation of a {@link Model} for a {@link FiniteStateMachine}.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of state
 *
 * @author Brian Oliver
 */
public class SimpleModel<S extends Enum<S>> implements Model<S>
{
    /**
     * The {@link Class} of the state of the {@link Model}.
     */
    private Class<S> m_stateClass;

    /**
     * The array of possible states of a {@link FiniteStateMachine}.
     */
    private S[] m_states;

    /**
     * A list of possible transitions for the {@link FiniteStateMachine}.
     */
    private ArrayList<Transition<S>> m_transitions;

    /**
     * A map of states to their {@link StateEntryAction}.
     */
    private HashMap<S, StateEntryAction<S>> m_stateEntryActions;

    /**
     * A map of states to their {@link StateExitAction}.
     */
    private HashMap<S, StateExitAction<S>> m_stateExitActions;


    /**
     * Constructs a {@link SimpleModel} given an array of the possible states
     * of a {@link FiniteStateMachine}
     *
     * @param states   the array of possible states
     */
    public SimpleModel(Class<S> stateClass)
    {
        m_stateClass        = stateClass;
        m_states            = stateClass.getEnumConstants();
        m_transitions       = new ArrayList<Transition<S>>();
        m_stateEntryActions = new HashMap<S, StateEntryAction<S>>();
        m_stateExitActions  = new HashMap<S, StateExitAction<S>>();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<S> getStateClass()
    {
        return m_stateClass;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public S[] getStates()
    {
        return m_states;
    }


    /**
     * Obtains the state with the specified name from the {@link SimpleModel}.
     *
     * @param name  the name of the state to obtain
     * @return  the state with the specified name or <code>null</code> if no
     *          such state is known by the {@link SimpleModel}
     */
    public S getState(String name)
    {
        // prepare the name
        name = name.trim().toUpperCase();

        for (S state : m_states)
        {
            if (state.name().toUpperCase().equals(name))
            {
                return state;
            }
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<S, StateEntryAction<S>> getStateEntryActions()
    {
        return m_stateEntryActions;
    }


    /**
     * Adds/overrides the {@link StateEntryAction} for the specified state in
     * the {@link Model}.
     *
     * @param state             the state
     * @param stateEntryAction  the {@link StateEntryAction} for the state
     */
    public void addStateEntryAction(S                   state,
                                    StateEntryAction<S> stateEntryAction)
    {
        m_stateEntryActions.put(state, stateEntryAction);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<S, StateExitAction<S>> getStateExitActions()
    {
        return m_stateExitActions;
    }


    /**
     * Adds/overrides the {@link StateExitAction} for the specified state in
     * the {@link Model}.
     *
     * @param state             the state
     * @param stateExitAction  the {@link StateExitAction} for the state
     */
    public void addStateExitAction(S                  state,
                                   StateExitAction<S> stateExitAction)
    {
        m_stateExitActions.put(state, stateExitAction);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Transition<S>> getTransitions()
    {
        return m_transitions;
    }


    /**
     * Adds the specified {@link Transition} to the {@link Model}.
     *
     * @param transition  the {@link Transition} to add
     */
    public void addTransition(Transition<S> transition)
    {
        m_transitions.add(transition);
    }
}
