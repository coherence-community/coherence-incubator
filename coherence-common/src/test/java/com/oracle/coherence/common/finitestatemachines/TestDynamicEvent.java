/*
 * File: TestDynamicEvent.java
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

import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import junit.framework.Assert;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Unit tests for the {@link NonBlockingFiniteStateMachine}
 *  <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class TestDynamicEvent
{
    /**
     * A {@link LightEvent} captures the {@link Event}s that can happen to
     * a {@link Light}.
     */
    private enum SwitchEvent implements Event<Brightness>
    {
        TURN;

        /**
         * {@inheritDoc}
         */
        @Override
        public Brightness getDesiredState(Brightness       currentState,
                                          ExecutionContext context)
        {
            // each 'turn' event increases the brightness.  when a turn
            // from 'bright' brightness occurs, the brightness becomes 'off'

            int cBrightness = Brightness.values().length;

            return Brightness.values()[(currentState.ordinal() + 1) % cBrightness];
        }
    }


    /**
     * A {@link Light} that provides several different distinct levels of
     * brightness.
     */
    private enum Brightness
    {
        OFF,
        DULL,
        MEDIUM,
        BRIGHT
    }


    /**
     * A simple test of transitions for a {@link Light}.
     */
    @Test
    public void testSimpleLightSwitchMachine()
    {
        // build the finite state machine model
        SimpleModel<Brightness> model = new SimpleModel<Brightness>(Brightness.class);

        model.addTransition(new Transition<Brightness>("Dull",
                                                       Brightness.OFF,
                                                       Brightness.DULL,
                                                       new TraceAction<Brightness>()));
        model.addTransition(new Transition<Brightness>("Medium",
                                                       Brightness.DULL,
                                                       Brightness.MEDIUM,
                                                       new TraceAction<Brightness>()));
        model.addTransition(new Transition<Brightness>("Bright",
                                                       Brightness.MEDIUM,
                                                       Brightness.BRIGHT,
                                                       new TraceAction<Brightness>()));
        model.addTransition(new Transition<Brightness>("Off",
                                                       Brightness.BRIGHT,
                                                       Brightness.OFF,
                                                       new TraceAction<Brightness>()));

        model.addStateEntryAction(Brightness.BRIGHT, new StateEntryAction<Brightness>()
        {
            @Override
            public Instruction onEnterState(Brightness        exitingState,
                                            Brightness        enteringState,
                                            Event<Brightness> event,
                                            ExecutionContext  context)
            {
                System.out.println("Oh man.. that's bright!!!");

                return Instruction.NOTHING;
            }
        });

        ScheduledExecutorService executorService = ExecutorServiceFactory.newSingleThreadScheduledExecutor();

        // build the finite state machine from the model
        NonBlockingFiniteStateMachine<Brightness> machine =
            new NonBlockingFiniteStateMachine<Brightness>("Light Switch",
                                                          model,
                                                          Brightness.OFF,
                                                          executorService,
                                                          true);

        Assert.assertEquals(Brightness.OFF, machine.getState());

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        Assert.assertTrue(machine.quiesceThenStop());
    }
}
