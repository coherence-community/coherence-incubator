/*
 * File: TestNonBlockingFiniteStateMachineWithAnnotationDrivenModel.java
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

import com.oracle.coherence.common.finitestatemachines.annotations.OnEnterState;
import com.oracle.coherence.common.finitestatemachines.annotations.OnTransition;
import com.oracle.coherence.common.finitestatemachines.annotations.Transition;
import com.oracle.coherence.common.finitestatemachines.annotations.Transitions;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import junit.framework.Assert;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for the {@link NonBlockingFiniteStateMachine}
 *  <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class TestNonBlockingFiniteStateMachineWithAnnotationDrivenModel
{
    /**
     * A simple test of transitions for a {@link Light}.
     */
    @Test
    public void testSimpleLightSwitchMachine()
    {
        MyFiniteStateMachineModel myModel = new MyFiniteStateMachineModel();

        // build the finite state machine model
        AnnotationDrivenModel<Light> model           = new AnnotationDrivenModel<Light>(Light.class, myModel);

        ScheduledExecutorService     executorService = ExecutorServiceFactory.newSingleThreadScheduledExecutor();

        // build the finite state machine from the model
        NonBlockingFiniteStateMachine<Light> machine = new NonBlockingFiniteStateMachine<Light>("Light Bulb",
                                                                                                model,
                                                                                                Light.OFF,
                                                                                                executorService,
                                                                                                true);

        Assert.assertEquals(Light.OFF, machine.getState());

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.TOGGLE_TOO_FAST);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        Assert.assertTrue(machine.quiesceThenStop());

        Assert.assertFalse(machine.quiesceThenStop());

        Assert.assertFalse(machine.stop());

        Assert.assertEquals(2, myModel.getCount());

        Assert.assertEquals(Light.BROKEN, machine.getState());

        Assert.assertEquals(5, machine.getTransitionCount());
    }


    /**
     * An example of a simple annotated {@link Transitions}.
     */
    @Transitions({@Transition(
        name       = "Turning On",
        fromStates = "OFF",
        toState    = "ON"
    ) , @Transition(
        name       = "Turning Off",
        fromStates = "ON",
        toState    = "OFF"
    ) , @Transition(
        name       = "Broken",
        fromStates = {"ON", "OFF"},
        toState    = "BROKEN"
    ) })
    public static class MyFiniteStateMachineModel
    {
        /**
         * A counter for the number of times a {@link Light} has been turned on
         */
        private final AtomicInteger m_count = new AtomicInteger(0);


        /**
         * Obtains the number of times the {@link MyFiniteStateMachineModel} has been
         * turned on.
         *
         * @return  the count of times turned on
         */
        public long getCount()
        {
            return m_count.get();
        }


        /**
         * A callback when a {@link Transitions} transitions from
         * {@link Light#OFF} to {@link Light#ON}.
         */
        @OnTransition(
            fromStates = "OFF",
            toStates   = "ON"
        )
        public void onTurningOn(String           sTransitionName,
                                Light            stateFrom,
                                Light            stateTo,
                                Event<Light>     event,
                                ExecutionContext context) throws RollbackTransitionException
        {
            System.out.printf("(executing '%s' transition #%d from %s to %s due to %s event)\n\n",
                              sTransitionName,
                              context.getTransitionCount() + 1,
                              stateFrom,
                              stateTo,
                              event);
        }


        /**
         * A callback when a {@link Transitions} transitions from
         * {@link Light#ON} to {@link Light#OFF}.
         */
        @OnTransition(
            fromStates = "ON",
            toStates   = "OFF"
        )
        public void onTurningOff(String           sTransitionName,
                                 Light            stateFrom,
                                 Light            stateTo,
                                 Event<Light>     event,
                                 ExecutionContext context) throws RollbackTransitionException
        {
            System.out.printf("(executing '%s' transition #%d from %s to %s due to %s event)\n\n",
                              sTransitionName,
                              context.getTransitionCount() + 1,
                              stateFrom,
                              stateTo,
                              event);
        }


        /**
         * A callback when a {@link Transitions} transitions from
         * {@link Light#ON} or {@link Light#OFF}
         * to {@link Light#BROKEN}.
         */
        @OnTransition(
            fromStates = {"ON", "OFF"},
            toStates   = "BROKEN"
        )
        public void onBreakingTransition(String           sTransitionName,
                                         Light            stateFrom,
                                         Light            stateTo,
                                         Event<Light>     event,
                                         ExecutionContext context) throws RollbackTransitionException
        {
            System.out.printf("(executing '%s' transition #%d from %s to %s due to %s event)\n\n",
                              sTransitionName,
                              context.getTransitionCount() + 1,
                              stateFrom,
                              stateTo,
                              event);
        }


        /**
         * A callback when the {@link Light} state of a
         * {@link Transitions} becomes {@link Light#ON}
         */
        @OnEnterState("ON")
        public Instruction turnedOn(Light            previousState,
                                    Light            newState,
                                    Event<Light>     event,
                                    ExecutionContext context)
        {
            System.out.println("The light is on.");
            m_count.incrementAndGet();

            return Instruction.NOTHING;
        }


        /**
         * A callback when the {@link Light} state of a
         * {@link Transitions} becomes {@link Light#OFF}
         */
        @OnEnterState("OFF")
        public Instruction turnedOff(Light            previousState,
                                     Light            newState,
                                     Event<Light>     event,
                                     ExecutionContext context)
        {
            System.out.println("The light is off.");

            return Instruction.NOTHING;
        }


        /**
         * A callback when the {@link Light} state of a
         * {@link Transitions} becomes {@link Light#BROKEN}
         */
        @OnEnterState("BROKEN")
        public Instruction broken(Light            previousState,
                                  Light            newState,
                                  Event<Light>     event,
                                  ExecutionContext context)
        {
            System.out.println("The light is broken.");

            return Instruction.NOTHING;
        }
    }
}
