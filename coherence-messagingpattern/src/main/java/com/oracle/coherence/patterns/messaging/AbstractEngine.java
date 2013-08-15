/*
 * File: AbstractEngine.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.messaging;

import com.oracle.coherence.common.finitestatemachines.Event;
import com.oracle.coherence.common.finitestatemachines.ExecutionContext;
import com.oracle.coherence.common.finitestatemachines.Instruction;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine;
import com.oracle.coherence.common.finitestatemachines.annotations.OnEnterState;
import com.oracle.coherence.common.finitestatemachines.annotations.Transition;
import com.oracle.coherence.common.finitestatemachines.annotations.Transitions;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.common.base.Disposable;

/**
 * The base Engine class that contains a Finite State Machine (FSM) which is responsible
 * for managing process flow.
 *
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *
 * @author David Rowlands
 */
abstract class AbstractEngine implements Disposable
{
    // ----- enum State -----------------------------------------------------

    enum State
    {
        /**
         * The idle state.
         */
        IDLE,

        /**
         * Indicates that the queue processing is in running state.
         */
        RUNNING
    }


    // NOTE: Strings are needed for annotation
    private static final String                    STATE_IDLE    = "IDLE";
    private static final String                    STATE_RUNNING = "RUNNING";

    protected volatile Identifier                  destinationIdentifier;
    protected NonBlockingFiniteStateMachine<State> m_fsm = null;


    /**
     * Protected Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    protected AbstractEngine(Identifier destinationIdentifier)
    {
        this.destinationIdentifier = destinationIdentifier;
    }


    /**
     * Returns a {@link Identifier} for referring to the {@link Destination}
     * @return destination identifier
     */
    public Identifier getDestinationIdentifier()
    {
        return destinationIdentifier;
    }


    /*
     * Called when FSM state transitions to idle state.
     * @return Instruction State for FSM upon completion of transition to idle state.
     */
    Instruction onIdle()
    {
        return Instruction.NOTHING;
    }


    /*
     * Abstract method Called when FSM state transitions to running state.
     * @return Instruction  State for FSM upon completion of transition to running state.
     */
    abstract Instruction onRunning(Event<State> event);


    /**
     * Shutdown the FSM as part of cleanup
     */
    public void dispose()
    {
        if (m_fsm != null)
        {
            if (!m_fsm.hasPendingEvents())
            {
                m_fsm.stop();
            }
        }
    }


    // ----- inner class Model (finite state machine model) -----------------

    /**
     * Model for basic idle/running FSM.
     */
    @Transitions({@Transition(
        fromStates = {STATE_IDLE},
        toState    = STATE_RUNNING
    ) , @Transition(
        fromStates = {STATE_RUNNING},
        toState    = STATE_IDLE
    ) , })
    public class Model
    {
        /**
         * A callback when the FSM enters idle state.
         */
        @OnEnterState(STATE_IDLE)
        public Instruction enterIdle(State            previousState,
                                     State            newState,
                                     Event<State>     event,
                                     ExecutionContext context)
        {
            return onIdle();
        }


        /**
         * A callback when the FSM enters running state.
         */
        @OnEnterState(STATE_RUNNING)
        public Instruction enterRunning(State            previousState,
                                        State            newState,
                                        Event<State>     event,
                                        ExecutionContext context)
        {
            return onRunning(event);
        }
    }


    // ----- inner class StateEvent -----------------------------------------

    /**
     * The events processed by the state machine.
     */
    public static class StateEvent implements Event<State>
    {
        // ----- data members ---------------------------------------------------

        /**
         * The desired state the event is attempting to reach.
         */
        private final State f_desiredState;


        /**
         * Constructs an AbstractStateEvent.
         *
         * @param desiredState the desired state after the event
         */
        public StateEvent(State desiredState)
        {
            f_desiredState = desiredState;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public State getDesiredState(State            currentState,
                                     ExecutionContext context)
        {
            return f_desiredState;
        }
    }
}
