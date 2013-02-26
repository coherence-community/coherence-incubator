/*
 * File: Transition.java
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

import java.util.EnumSet;

/**
 * A {@link Transition} represents a transition in a {@link FiniteStateMachine}
 * from one or more possible states to a desired state.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of the state of the {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 */
public class Transition<S extends Enum<S>>
{
    /**
     * The name of the {@link Transition}.
     * <p>
     * This is used for display and logging purposes.
     */
    private String m_name;

    /**
     * The set of states from which this {@link Transition} may occur.
     */
    private EnumSet<S> m_statesFrom;

    /**
     * The state of the {@link FiniteStateMachine} once the {@link TransitionAction}
     * has been successfully performed.
     */
    private S m_stateTo;

    /**
     * The {@link TransitionAction} to be performed for the {@link Transition}.
     * <p>
     * This may be <code>null</code> if no action is to be performed.
     */
    private TransitionAction<S> m_action;


    /**
     * Constructs a {@link Transition} (without an {@link TransitionAction}).
     *
     * @param name        the name of the {@link Transition} (used for display and logging purposes)
     * @param statesFrom  the set of states from which this {@link Transition} may occur
     * @param stateTo     the state once the {@link TransitionAction} has been successfully performed
     */
    public Transition(String     name,
                      EnumSet<S> statesFrom,
                      S          stateTo)
    {
        this(name, statesFrom, stateTo, /* no action */ null);
    }


    /**
     * Constructs a {@link Transition} (without an {@link TransitionAction})
     *
     * @param name        the name of the {@link Transition} (used for display and logging purposes)
     * @param stateFrom   the state from which this {@link Transition} may occur
     * @param stateTo     the state once the {@link TransitionAction} has been successfully performed
     */
    public Transition(String name,
                      S      stateFrom,
                      S      stateTo)
    {
        this(name, stateFrom, stateTo, /* no action */ null);
    }


    /**
     * Constructs a {@link Transition}.
     *
     * @param name        the name of the {@link Transition} (used for display and logging purposes)
     * @param statesFrom  the set of states from which this {@link Transition} may occur
     * @param stateTo     the state once the {@link TransitionAction} has been successfully performed
     * @param action      the {@link TransitionAction} to be perform for the {@link Transition}
     */
    public Transition(String              name,
                      EnumSet<S>          statesFrom,
                      S                   stateTo,
                      TransitionAction<S> action)
    {
        m_name       = name;
        m_statesFrom = statesFrom;
        m_stateTo    = stateTo;
        m_action     = action;
    }


    /**
     * Constructs a {@link Transition}.
     *
     * @param name        the name of the {@link Transition} (used for display and logging purposes)
     * @param stateFrom   the state from which this {@link Transition} may occur
     * @param stateTo     the state once the {@link TransitionAction} has been successfully performed
     * @param action      the {@link TransitionAction} to be perform for the {@link Transition}
     */
    public Transition(String              name,
                      S                   stateFrom,
                      S                   stateTo,
                      TransitionAction<S> action)
    {
        m_name       = name;
        m_statesFrom = EnumSet.of(stateFrom);
        m_stateTo    = stateTo;
        m_action     = action;
    }


    /**
     * Obtains the name of the {@link Transition}.
     *
     * @return the name of the {@link Transition}
     */
    public String getName()
    {
        return m_name;
    }


    /**
     * Determines if the specified state is one of the possible starting states for the {@link Transition}.
     *
     * @param state  the state to check
     *
     * @return  <code>true</code> if the specified state is one of the starting states for the {@link Transition}
     */
    public boolean isStartingState(S state)
    {
        return m_statesFrom.contains(state);
    }


    /**
     * Obtains the {@link TransitionAction} to be performed for the {@link Transition}.
     *
     * @return the {@link TransitionAction} for the transition
     */
    public TransitionAction<S> getAction()
    {
        return m_action;
    }


    /**
     * Obtains the state to which the {@link FiniteStateMachine} will be in once this {@link Transition}
     * has occurred.
     *
     * @return  the state of the {@link FiniteStateMachine} after this {@link Transition} has occurred
     */
    public S getEndingState()
    {
        return m_stateTo;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("Transition{name=%s, from=%s, to=%s, action=%s}",
                             m_name,
                             m_statesFrom,
                             m_stateTo,
                             m_action);
    }
}
