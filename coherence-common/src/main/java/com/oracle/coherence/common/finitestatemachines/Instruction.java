/*
 * File: Instruction.java
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
 * A marker interface for {@link Instruction}s to {@link FiniteStateMachine}s.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface Instruction
{
    /**
     * The {@link Instruction} for a {@link FiniteStateMachine} to do nothing.
     */
    public final static Instruction NOTHING = new Instruction()
    {
    } ;

    /**
     * The {@link Instruction} for a {@link FiniteStateMachine} to stop.
     */
    public final static Instruction STOP = new Instruction()
    {
    } ;


    /**
     * An {@link Instruction} for a {@link FiniteStateMachine} to process
     * an {@link Event}.
     *
     * (immediately on the thread that created the {@link Instruction}).
     */
    public final class ProcessEvent<S extends Enum<S>> implements Instruction
    {
        /**
         * The {@link Event} to process.
         */
        private Event<S> m_event;


        /**
         * Constructs a {@link ProcessEvent} {@link Instruction}.
         *
         * @param event  the {@link Event} to process
         */
        public ProcessEvent(Event<S> event)
        {
            m_event = event;
        }


        /**
         * Obtain the {@link Event} to process.
         *
         * @return  the {@link Event} to process
         */
        public Event<S> getEvent()
        {
            return m_event;
        }
    }


    /**
     * An {@link Instruction} for a {@link FiniteStateMachine} to
     * {@link TransitionTo} another state.
     * (immediately on the thread that created the {@link Instruction}).
     */
    public final class TransitionTo<S extends Enum<S>> implements Instruction, Event<S>
    {
        /**
         * The desired state
         */
        private S m_desiredState;


        /**
         * Constructs a {@link TransitionTo}.
         *
         * @param desiredState  the desired state to which to transition
         */
        public TransitionTo(S desiredState)
        {
            m_desiredState = desiredState;
        }


        /**
         * {@inheritDoc}
         */
        public S getDesiredState(S                currentState,
                                 ExecutionContext context)
        {
            return m_desiredState;
        }
    }
}
