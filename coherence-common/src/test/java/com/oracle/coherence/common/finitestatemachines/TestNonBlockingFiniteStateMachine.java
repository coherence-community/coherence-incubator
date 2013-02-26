/*
 * File: TestNonBlockingFiniteStateMachine.java
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

import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine.CoalescedEvent;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine.CoalescedEvent.Process;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine.SubsequentEvent;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.tools.deferred.DeferredAssert;
import junit.framework.Assert;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.tools.deferred.DeferredHelper.eventually;
import static com.oracle.tools.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for the {@link NonBlockingFiniteStateMachine}
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class TestNonBlockingFiniteStateMachine
{
    /**
     * A simple test of transitions for a {@link LightSwitch}.
     */
    @Test
    public void testSimpleLightSwitchMachine()
    {
        final AtomicInteger count = new AtomicInteger(0);

        // build the finite state machine model
        SimpleModel<Light> model = new SimpleModel<Light>(Light.class);

        model.addTransition(new Transition<Light>("Turn On", Light.OFF, Light.ON, new TraceAction<Light>()));
        model.addTransition(new Transition<Light>("Turn Off", Light.ON, Light.OFF, new TraceAction<Light>()));
        model.addTransition(new Transition<Light>("Break It",
                                                  EnumSet.of(Light.ON, Light.OFF),
                                                  Light.BROKEN,
                                                  new TraceAction<Light>()));

        model.addStateEntryAction(Light.ON, new StateEntryAction<Light>()
        {
            @Override
            public Instruction onEnterState(Light            previousState,
                                            Light            newState,
                                            Event<Light>     event,
                                            ExecutionContext context)
            {
                System.out.println("The light is on.");
                count.incrementAndGet();

                return Instruction.NOTHING;
            }
        });

        model.addStateEntryAction(Light.OFF, new StateEntryAction<Light>()
        {
            @Override
            public Instruction onEnterState(Light            previousState,
                                            Light            newState,
                                            Event<Light>     event,
                                            ExecutionContext context)
            {
                System.out.println("The light is off.");

                return Instruction.NOTHING;
            }
        });

        model.addStateEntryAction(Light.BROKEN, new StateEntryAction<Light>()
        {
            @Override
            public Instruction onEnterState(Light            previousState,
                                            Light            newState,
                                            Event<Light>     event,
                                            ExecutionContext context)
            {
                System.out.println("The light is broken.");

                return Instruction.NOTHING;
            }
        });

        ScheduledExecutorService executorService = ExecutorServiceFactory.newSingleThreadScheduledExecutor();

        // build the finite state machine from the model
        NonBlockingFiniteStateMachine<Light> machine = new NonBlockingFiniteStateMachine<Light>("Light Bulb",
                                                                                                model,
                                                                                                Light.OFF,
                                                                                                executorService,
                                                                                                true);

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

        Assert.assertEquals(2, count.get());

        Assert.assertEquals(Light.BROKEN, machine.getState());

        Assert.assertEquals(5, machine.getTransitionCount());
    }


    /**
     * Ensure that a). a scheduled {@link SubsequentEvent} is processed when
     * no interleaving of other events occur, and b). a scheduled
     * {@link SubsequentEvent} is not processed when interleaving of other
     * events does occur.
     */
    @Test
    public void testSubsequentEventProcessing()
    {
        SimpleModel<Motor> model = new SimpleModel<Motor>(Motor.class);

        model.addTransition(new Transition<Motor>("Turn On", Motor.STOPPED, Motor.RUNNING, new TraceAction<Motor>()));
        model.addTransition(new Transition<Motor>("Turn Off", Motor.RUNNING, Motor.STOPPED, new TraceAction<Motor>()));

        NonBlockingFiniteStateMachine<Motor> machine =
            new NonBlockingFiniteStateMachine<Motor>("Subsequent Testing Model",
                                                     model,
                                                     Motor.STOPPED,
                                                     ExecutorServiceFactory.newSingleThreadScheduledExecutor(),
                                                     false);

        machine.processLater(new SubsequentEvent<Motor>(MotorEvent.TURN_ON), 1, TimeUnit.SECONDS);

        // ensure that a subsequent event is executed when there's no interleaving
        DeferredAssert.assertThat(eventually(invoking(machine).getTransitionCount()), is(1L));
        Assert.assertEquals(machine.getState(), Motor.RUNNING);

        // ensure that a subsequent event is not executed when there's interleaving
        TrackingEvent<Motor> event = new TrackingEvent<Motor>(new SubsequentEvent<Motor>(MotorEvent.TURN_ON));

        machine.processLater(event, 1, TimeUnit.SECONDS);

        machine.process(MotorEvent.TURN_OFF);
        machine.process(MotorEvent.TURN_ON);
        machine.process(MotorEvent.TURN_OFF);

        DeferredAssert.assertThat(eventually(invoking(machine).getTransitionCount()), is(4L));
        Assert.assertEquals(Motor.STOPPED, machine.getState());

        Assert.assertEquals(true, event.accepted());
        Assert.assertEquals(false, event.evaluated());
        Assert.assertEquals(false, event.processed());
    }


    /**
     * Ensure that only a single {@link CoalescedEvent} is processed.
     */
    @Test
    public void testCoalescedEventProcessing()
    {
        SimpleModel<Motor> model = new SimpleModel<Motor>(Motor.class);

        model.addTransition(new Transition<Motor>("Turn On", Motor.STOPPED, Motor.RUNNING, new TraceAction<Motor>()));
        model.addTransition(new Transition<Motor>("Turn Off", Motor.RUNNING, Motor.STOPPED, new TraceAction<Motor>()));

        NonBlockingFiniteStateMachine<Motor> machine =
            new NonBlockingFiniteStateMachine<Motor>("Subsequent Testing Model",
                                                     model,
                                                     Motor.STOPPED,
                                                     ExecutorServiceFactory.newSingleThreadScheduledExecutor(),
                                                     false);

        // ensure that the first submitted coalesced event is processed
        // and the others are not
        TrackingEvent<Motor> event1 = new TrackingEvent<Motor>(MotorEvent.TURN_ON);
        TrackingEvent<Motor> event2 = new TrackingEvent<Motor>(MotorEvent.TURN_ON);
        TrackingEvent<Motor> event3 = new TrackingEvent<Motor>(MotorEvent.TURN_ON);

        machine.processLater(new CoalescedEvent<Motor>(event1, Process.FIRST), 3, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<Motor>(event2, Process.FIRST), 2, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<Motor>(event3, Process.FIRST), 1, TimeUnit.SECONDS);

        DeferredAssert.assertThat(eventually(invoking(machine).getTransitionCount()), is(1L));
        Assert.assertEquals(machine.getState(), Motor.RUNNING);

        Assert.assertEquals(true, event1.accepted());
        DeferredAssert.assertThat(eventually(invoking(event1).evaluated()), is(true));
        Assert.assertEquals(true, event1.processed());

        Assert.assertEquals(true, event2.accepted());
        DeferredAssert.assertThat(eventually(invoking(event2).evaluated()), is(false));
        Assert.assertEquals(false, event2.processed());

        Assert.assertEquals(true, event3.accepted());
        DeferredAssert.assertThat(eventually(invoking(event3).evaluated()), is(false));
        Assert.assertEquals(false, event3.processed());

        // ensure that the most recently submitted coalesced event is processed
        // and the others are not
        event1 = new TrackingEvent<Motor>(MotorEvent.TURN_OFF);
        event2 = new TrackingEvent<Motor>(MotorEvent.TURN_OFF);
        event3 = new TrackingEvent<Motor>(MotorEvent.TURN_OFF);

        machine.processLater(new CoalescedEvent<Motor>(event1, Process.MOST_RECENT), 3, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<Motor>(event2, Process.MOST_RECENT), 2, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<Motor>(event3, Process.MOST_RECENT), 1, TimeUnit.SECONDS);

        DeferredAssert.assertThat(eventually(invoking(machine).getTransitionCount()), is(2L));
        Assert.assertEquals(machine.getState(), Motor.STOPPED);

        Assert.assertEquals(true, event3.accepted());
        DeferredAssert.assertThat(eventually(invoking(event3).evaluated()), is(true));
        Assert.assertEquals(true, event3.processed());

        Assert.assertEquals(true, event2.accepted());
        DeferredAssert.assertThat(eventually(invoking(event2).evaluated()), is(false));
        Assert.assertEquals(false, event2.processed());

        Assert.assertEquals(true, event1.accepted());
        DeferredAssert.assertThat(eventually(invoking(event1).evaluated()), is(false));
        Assert.assertEquals(false, event1.processed());
    }


    /**
     * Ensure that it is possible to transition from a state back to itself,
     * including executing appropriate actions.
     */
    @Test
    public void testSelfTransitions()
    {
        SimpleModel<Singularity>              model            = new SimpleModel<Singularity>(Singularity.class);

        TransitionTrackingAction<Singularity> actionTransition = new TransitionTrackingAction<Singularity>();

        model.addTransition(new Transition<Singularity>("Process",
                                                        Singularity.PROCESSING,
                                                        Singularity.PROCESSING,
                                                        actionTransition));

        StateEntryTrackingAction<Singularity> actionEntry = new StateEntryTrackingAction<Singularity>();

        model.addStateEntryAction(Singularity.PROCESSING, actionEntry);

        StateExitTrackingAction<Singularity> actionExit = new StateExitTrackingAction<Singularity>();

        model.addStateExitAction(Singularity.PROCESSING, actionExit);

        NonBlockingFiniteStateMachine<Singularity> machine =
            new NonBlockingFiniteStateMachine<Singularity>("Singularity Model",
                                                           model,
                                                           Singularity.PROCESSING,
                                                           ExecutorServiceFactory.newSingleThreadScheduledExecutor(),
                                                           false);

        Assert.assertEquals(0, actionTransition.getExecutionCount());
        Assert.assertEquals(0, actionExit.getExecutionCount());
        Assert.assertEquals(1, actionEntry.getExecutionCount());

        for (int i = 0; i < 10; i++)
        {
            machine.process(new Instruction.TransitionTo<Singularity>(Singularity.PROCESSING));
        }

        DeferredAssert.assertThat(eventually(invoking(actionTransition).getExecutionCount()), is(10L));
        DeferredAssert.assertThat(eventually(invoking(actionExit).getExecutionCount()), is(10L));
        DeferredAssert.assertThat(eventually(invoking(actionEntry).getExecutionCount()), is(11L));
    }
}
