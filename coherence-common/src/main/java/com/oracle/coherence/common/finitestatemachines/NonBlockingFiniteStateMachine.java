/*
 * File: NonBlockingFiniteStateMachine.java
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

import com.oracle.coherence.common.finitestatemachines.Instruction.ProcessEvent;
import com.oracle.coherence.common.finitestatemachines.Instruction.TransitionTo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link NonBlockingFiniteStateMachine} is a specialized
 * {@link FiniteStateMachine} implementation that performs transitions
 * asynchronously to the threads that requests state changes.  That is, threads
 * that request state transitions are never blocked.  Instead their requests are
 * queued for a single thread to later perform the appropriate transition to
 * the requested state.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class NonBlockingFiniteStateMachine<S extends Enum<S>> implements FiniteStateMachine<S>, ExecutionContext
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(NonBlockingFiniteStateMachine.class.getName());

    /**
     * The name of the {@link NonBlockingFiniteStateMachine}.
     */
    private String m_name;

    /**
     * The state of the {@link FiniteStateMachine}.
     */
    private volatile S m_state;

    /**
     * The {@link Transition} table (by starting and ending states).
     */
    private EnumMap<S, EnumMap<S, Transition<S>>> m_transitions;

    /**
     * The {@link StateEntryAction} table (by state)
     */
    private EnumMap<S, StateEntryAction<S>> m_stateEntryActions;

    /**
     * The {@link StateExitAction} table (by state)
     */
    private EnumMap<S, StateExitAction<S>> m_stateExitActions;

    /**
     * The number of transitions that have occurred in the {@link FiniteStateMachine}.
     */
    private AtomicLong m_transitionCount;

    /**
     * Is the {@link FiniteStateMachine} accepting {@link Event}s to
     * trigger {@link Transition}s?
     * <p>
     * This flag allows us to stop the {@link FiniteStateMachine} from accepting
     * {@link Event}s (that may cause {@link Transition}s), but allows the
     * {@link FiniteStateMachine} to continue processing previously accepted
     * {@link Event}s.
     */
    private AtomicBoolean m_isAcceptingEvents;

    /**
     * Is the {@link FiniteStateMachine} allowed to perform {@link Transition}s?
     * <p>
     * This flag determines if the {@link FiniteStateMachine} is operational.
     * Once it can no longer perform {@link Transition}s, the
     * {@link FiniteStateMachine} is "dead" and can no longer be used.
     */
    private AtomicBoolean m_canPerformTransitions;

    /**
     * The number of pending, ie: queued, {@link Event}s to be processed.
     */
    private AtomicInteger m_pendingEventCount;

    /**
     * A {@link ScheduledExecutorService} that will be used to schedule
     * {@link Transition}s for the {@link FiniteStateMachine}.
     * <p>
     * Note: Only threads on this {@link ScheduledExecutorService} may apply a
     * {@link Transition}.
     */
    private ScheduledExecutorService m_executorService;

    /**
     * When <code>true</code> {@link RuntimeException}s be ignored
     * (will not stop the {@link FiniteStateMachine}).
     * <p>
     * When <code>false</code> {@link RuntimeException}s be will immediately stop
     * the {@link FiniteStateMachine}.
     */
    private boolean m_ignoreRuntimeExceptions;


    /**
     * Construct an {@link NonBlockingFiniteStateMachine} given a {@link Model}
     *
     * @param name                     the name of the {@link NonBlockingFiniteStateMachine}
     * @param model                    the {@link Model} of the {@link NonBlockingFiniteStateMachine}
     * @param initialState             the initial state
     * @param executorService          the {@link ScheduledExecutorService} to use for scheduling {@link Transition}s
     * @param ignoreRuntimeExceptions  when <code>true</code> {@link RuntimeException}s will be ignored,
     *                                 when <code>false</code> {@link RuntimeException}s will immediately stop
     *                                 the {@link NonBlockingFiniteStateMachine}
     */
    public NonBlockingFiniteStateMachine(String                   name,
                                         Model<S>                 model,
                                         S                        initialState,
                                         ScheduledExecutorService executorService,
                                         boolean                  ignoreRuntimeExceptions)
    {
        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.entering(getClass().getName(),
                            "Constructor",
                            new Object[] {name, model, executorService, ignoreRuntimeExceptions});
        }

        // TODO: we should prove that the model is valid
        // ie: no isolated/unreachable states
        // ie: no multiple paths from one state to another state, ensuring that two or more transitions from A to B are not defined
        // ie: that there are no cycles formed by potential "synchronous" state transitions,
        // ensuring that A and B don't have any cycles formed by synchronous state transitions between them,
        // and thus deadlocks in the finite state machine can't occur.

        m_name                    = name;
        m_isAcceptingEvents       = new AtomicBoolean(true);
        m_canPerformTransitions   = new AtomicBoolean(true);
        m_pendingEventCount       = new AtomicInteger(0);
        m_executorService         = executorService;
        m_ignoreRuntimeExceptions = ignoreRuntimeExceptions;
        m_transitionCount         = new AtomicLong(0);

        // build the transitions table based on the model
        S[] states = model.getStates();

        m_transitions = new EnumMap<S, EnumMap<S, Transition<S>>>(model.getStateClass());

        for (S stateFrom : states)
        {
            m_transitions.put(stateFrom, new EnumMap<S, Transition<S>>(model.getStateClass()));
        }

        for (Transition<S> transition : model.getTransitions())
        {
            for (S fromStates : states)
            {
                if (transition.isStartingState(fromStates))
                {
                    m_transitions.get(fromStates).put(transition.getEndingState(), transition);
                }
            }
        }

        // create the state entry and exit action tables based on the model
        m_stateEntryActions = new EnumMap<S, StateEntryAction<S>>(model.getStateClass());
        m_stateExitActions  = new EnumMap<S, StateExitAction<S>>(model.getStateClass());

        for (S state : states)
        {
            m_stateEntryActions.put(state, model.getStateEntryActions().get(state));
            m_stateExitActions.put(state, model.getStateExitActions().get(state));
        }

        // set the initial state
        m_state = null;
        processEvent(new TransitionTo<S>(initialState));

        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.exiting(getClass().getName(), "Constructor");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return m_name;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public S getState()
    {
        return m_state;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransitionCount()
    {
        return m_transitionCount.get();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stop()
    {
        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.entering(getClass().getName(), String.format("[%s]: stop", getName()));
        }

        boolean isStopped;

        if (m_isAcceptingEvents.compareAndSet(true, false))
        {
            m_canPerformTransitions.set(false);

            isStopped = true;
        }
        else
        {
            isStopped = false;
        }

        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.exiting(getClass().getName(), String.format("[%s]: stop", getName()));
        }

        return isStopped;
    }


    /**
     * Requests the {@link FiniteStateMachine} to stop accepting new {@link Event}s
     * to process, wait for any existing queued {@link Event}s to be processed
     * and then stop.
     * <p>
     * Note: Once stopped a {@link FiniteStateMachine} can't be restarted.
     * Instead a new {@link FiniteStateMachine} should be created.
     *
     * @return <code>true</code> if the {@link FiniteStateMachine} was stopped or
     *         <code>false</code> if it was already stopped
     */
    public boolean quiesceThenStop()
    {
        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.entering(getClass().getName(), String.format("[%s]: quiesceThenStop", getName()));
        }

        boolean isStopped;

        if (m_isAcceptingEvents.compareAndSet(true, false))
        {
            synchronized (this)
            {
                while (m_pendingEventCount.get() > 0)
                {
                    try
                    {
                        // wait for half a second to see if there are no more pending transitions
                        // (this non-infinite wait is to protect us against the possibility that we miss being notified)
                        wait(500);
                    }
                    catch (InterruptedException e)
                    {
                        if (LOGGER.isLoggable(Level.FINER))
                        {
                            LOGGER.finer(String
                                .format("[%s]: Thread interrupted while quiescing.  Will stop immediately.", m_name));
                        }

                        break;
                    }
                }
            }

            m_canPerformTransitions.set(false);

            isStopped = m_pendingEventCount.get() == 0;
        }
        else
        {
            isStopped = false;
        }

        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.exiting(getClass().getName(), String.format("[%s]: quiesceThenStop", getName()));
        }

        return isStopped;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Event<S> event)
    {
        processLater(event, 0, TimeUnit.SECONDS);
    }


    /**
     * Request the {@link FiniteStateMachine} to process the specified {@link Event}
     * as soon as possible.
     * <p>
     * Note: This method is semantically equivalent to {@link #process(Event)}.
     *
     * @param event     the {@link Event} for the {@link FiniteStateMachine} to
     *                  process
     */
    public void processLater(Event<S> event)
    {
        processLater(event, 0, TimeUnit.SECONDS);
    }


    /**
     * Request the {@link FiniteStateMachine} to process the specified {@link Event}
     * at some point in the future (represented as a duration to wait from
     * the moment the method is called).
     * <p>
     * Note: There's no guarantee that the {@link Event} will processed because:
     * <ol>
     *    <li>the {@link Transition} to be performed for the {@link Event}
     *        is invalid as the {@link FiniteStateMachine} is not the required
     *        starting state.
     *    <li>the {@link FiniteStateMachine} may have been stopped.
     *
     * @param event     the {@link Event} for the {@link FiniteStateMachine} to
     *                  process
     * @param duration  the amount of the {@link TimeUnit} to wait before the
     *                  {@link Event} is processed
     * @param timeUnit  the {@link TimeUnit}
     */
    public void processLater(Event<S> event,
                             long     duration,
                             TimeUnit timeUnit)
    {
        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.entering(getClass().getName(),
                            String.format("[%s]: processLater", getName()),
                            new Object[] {event, duration, timeUnit});
        }

        if (m_isAcceptingEvents.get())
        {
            final Event<S> preparedEvent = prepareEvent(event);

            if (preparedEvent == null)
            {
                if (LOGGER.isLoggable(Level.FINER))
                {
                    LOGGER.finer(String.format("[%s]: Ignoring event %s as it vetoed being prepared", m_name, event));
                }
            }
            else
            {
                m_executorService.schedule(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        processEvent(preparedEvent);
                    }

                }, duration, timeUnit);
            }
        }
        else
        {
            if (LOGGER.isLoggable(Level.FINER))
            {
                LOGGER.finer(String
                    .format("[%s]: Ignoring request to process the event %s in %d %s as the machine is no longer accepting new transitions",
                            m_name, event, duration, timeUnit));
            }
        }

        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.exiting(getClass().getName(), String.format("[%s]: processLater", getName()));
        }
    }


    /**
     * Prepares and {@link Event} to be accepted for processing.
     *
     * @param event  the {@link Event} to prepare
     *
     * @return  the prepared {@link Event} (or <code>null</code> if the event should not be processed)
     */
    private Event<S> prepareEvent(Event<S> event)
    {
        // assume the worst - no event is prepared
        Event<S> prepared = null;

        if (m_isAcceptingEvents.get())
        {
            // ensure lifecycle aware events are notified
            if (event instanceof LifecycleAwareEvent)
            {
                LifecycleAwareEvent<S> lifecycleAwareEvent = (LifecycleAwareEvent<S>) event;

                prepared = lifecycleAwareEvent.onAccept(this) ? lifecycleAwareEvent : null;
            }
            else
            {
                prepared = event;
            }
        }

        if (prepared != null)
        {
            // increase the number of events that are now pending
            m_pendingEventCount.incrementAndGet();
        }

        return prepared;
    }


    /**
     * Determines if there are any pending {@link Event}s for the
     * {@link FiniteStateMachine} to process.
     * <p>
     * Note: If the {@link FiniteStateMachine} can no longer process {@link Event}s
     * false will be returned.
     *
     * @return  <code>true</code> if there are pending {@link Event}s and
     *          {@link Event}s can be processed, <code>false</code> otherwise
     */
    public boolean hasPendingEvents()
    {
        return m_canPerformTransitions.get() && m_pendingEventCount.get() > 0;
    }


    /**
     * Processes the specified {@link Event}, causing the {@link FiniteStateMachine}
     * to {@link Transition} to a new state if required.
     *
     * @param event  the {@link Event} to process
     */
    @SuppressWarnings("unchecked")
    private void processEvent(Event<S> event)
    {
        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.entering(getClass().getName(), String.format("[%s]: processEvent", getName()), new Object[] {event});
        }

        // this is a totally defensive synchronized block to prevent multiple threads
        // (which should never happen) from performing a transition simultaneously
        synchronized (this)
        {
            // we keep processing events on this thread until we run out of events
            while (event != null && m_canPerformTransitions.get())
            {
                // notify the a lifecycle aware event of the commencement of processing
                if (event instanceof LifecycleAwareEvent)
                {
                    LifecycleAwareEvent<S> lifecycleAwareEvent = (LifecycleAwareEvent<S>) event;

                    lifecycleAwareEvent.onProcessing(this);
                }

                // determine the desired state from the event
                S       stateCurrent    = getState();
                S       stateDesired    = event.getDesiredState(stateCurrent, this);

                boolean fIsInitialState = stateCurrent == null;

                // as we're processing an event, decrease the counter of
                // pending events
                if (!fIsInitialState)
                {
                    m_pendingEventCount.decrementAndGet();
                }

                // if there's no desired state, we do nothing
                if (stateDesired == null)
                {
                    // do nothing for the event
                    if (LOGGER.isLoggable(Level.FINER))
                    {
                        LOGGER.finer(String.format("[%s]: Ignoring event %s as it produced a null desired state.",
                                                   m_name, event));
                    }

                    // no more events to process
                    event = null;
                }
                else
                {
                    // assume no transition will be made
                    Transition<S> transition = null;

                    // when we have a current and desired state, we can
                    // perform a transition
                    if (!fIsInitialState)
                    {
                        // determine the appropriate transition from the
                        // current state to the desired state (using the transition table)
                        transition = m_transitions.get(stateCurrent).get(stateDesired);

                        if (transition == null)
                        {
                            // there's no transition from the current state to the
                            // desired state, so ignore the request
                            if (LOGGER.isLoggable(Level.FINER))
                            {
                                LOGGER.finer(String
                                    .format("[%s]: Can't find a valid transition from %s to %s.  Ignoring event %s.",
                                            m_name, stateCurrent, stateDesired, event));
                            }

                            event = null;
                        }
                        else
                        {
                            // fetch the action to execute for the transition
                            TransitionAction<S> actionTransition = transition.getAction();

                            // attempt to execute the action for the transition
                            // (if we have one)
                            if (actionTransition != null)
                            {
                                try
                                {
                                    // perform the action
                                    actionTransition.onTransition(transition.getName(),
                                                                  stateCurrent,
                                                                  transition.getEndingState(),
                                                                  event,
                                                                  this);
                                }
                                catch (RollbackTransitionException e)
                                {
                                    if (LOGGER.isLoggable(Level.FINER))
                                    {
                                        LOGGER.finer(String
                                            .format("[%s]: Transition for event %s from %s to %s has been rolledback due to:\n%s",
                                                    m_name, event, stateCurrent, stateDesired, e));
                                    }

                                    event = null;
                                }
                                catch (RuntimeException e)
                                {
                                    if (m_ignoreRuntimeExceptions)
                                    {
                                        if (LOGGER.isLoggable(Level.FINER))
                                        {
                                            LOGGER.finer(String
                                                .format("[%s]: Transition Action %s for event %s from %s to %s raised runtime exception (continuing with transition and ignoring the exception):\n%s",
                                                        m_name, actionTransition, event, stateCurrent, stateDesired,
                                                        e));
                                        }
                                    }
                                    else
                                    {
                                        m_isAcceptingEvents.set(false);
                                        m_canPerformTransitions.set(false);

                                        if (LOGGER.isLoggable(Level.WARNING))
                                        {
                                            StringWriter writerString = new StringWriter();
                                            PrintWriter  writerPrint  = new PrintWriter(writerString);

                                            e.printStackTrace(writerPrint);
                                            writerPrint.close();

                                            LOGGER.warning(String
                                                .format("[%s]: Stopping the machine as the Transition Action %s for event %s from %s to %s raised runtime exception %s:\n%s",
                                                        m_name, actionTransition, event, stateCurrent, stateDesired, e,
                                                        writerString.toString()));
                                        }

                                        event = null;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // now perform exit and entry actions
                    if (event != null)
                    {
                        // notify the a lifecycle aware event of the completion of transition is caused
                        if (event instanceof LifecycleAwareEvent)
                        {
                            LifecycleAwareEvent<S> lifecycleAwareEvent = (LifecycleAwareEvent<S>) event;

                            lifecycleAwareEvent.onProcessed(this);
                        }

                        // perform the exit action
                        if (!fIsInitialState)
                        {
                            StateExitAction<S> actionExit = m_stateExitActions.get(stateCurrent);

                            if (actionExit != null)
                            {
                                try
                                {
                                    actionExit.onExitState(stateCurrent, event, this);
                                }
                                catch (RuntimeException e)
                                {
                                    if (m_ignoreRuntimeExceptions)
                                    {
                                        if (LOGGER.isLoggable(Level.FINER))
                                        {
                                            LOGGER.finer(String
                                                .format("[%s]: State Exit Action %s for event %s from %s to %s raised runtime exception (continuing with transition and ignoring the exception):\n%s",
                                                        m_name, actionExit, event, stateCurrent, stateDesired, e));
                                        }
                                    }
                                    else
                                    {
                                        m_isAcceptingEvents.set(false);
                                        m_canPerformTransitions.set(false);

                                        if (LOGGER.isLoggable(Level.WARNING))
                                        {
                                            StringWriter writerString = new StringWriter();
                                            PrintWriter  writerPrint  = new PrintWriter(writerString);

                                            e.printStackTrace(writerPrint);
                                            writerPrint.close();

                                            LOGGER.warning(String
                                                .format("[%s]: Stopping the machine as the State Exit Action %s for event %s from %s to %s raised runtime exception %s:\n%s",
                                                        m_name, actionExit, event, stateCurrent, stateDesired, e,
                                                        writerString.toString()));
                                        }

                                        event = null;
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                if (LOGGER.isLoggable(Level.FINER))
                                {
                                    LOGGER.finer(String.format("[%s]: No Exit Action defined for %s", m_name,
                                                               stateCurrent));
                                }
                            }
                        }

                        // we're now in the desired state so set it
                        m_state = stateDesired;

                        // as we've made a transition, count it
                        if (!fIsInitialState)
                        {
                            m_transitionCount.incrementAndGet();
                        }

                        // the instruction to perform after setting the state
                        Instruction instruction = Instruction.NOTHING;

                        // perform the entry action
                        StateEntryAction<S> actionEntry = m_stateEntryActions.get(stateDesired);

                        if (actionEntry != null)
                        {
                            try
                            {
                                // execute the enter state action and determine what to do next
                                instruction = actionEntry.onEnterState(stateCurrent, stateDesired, event, this);
                            }
                            catch (RuntimeException e)
                            {
                                if (m_ignoreRuntimeExceptions)
                                {
                                    if (LOGGER.isLoggable(Level.FINER))
                                    {
                                        LOGGER.finer(String
                                            .format("[%s]: State Entry Action %s for event %s from %s to %s raised runtime exception (continuing and ignoring the exception):\n%s",
                                                    m_name, actionEntry, event, stateCurrent, stateDesired, e));
                                    }
                                }
                                else
                                {
                                    m_isAcceptingEvents.set(false);
                                    m_canPerformTransitions.set(false);

                                    if (LOGGER.isLoggable(Level.WARNING))
                                    {
                                        StringWriter writerString = new StringWriter();
                                        PrintWriter  writerPrint  = new PrintWriter(writerString);

                                        e.printStackTrace(writerPrint);
                                        writerPrint.close();

                                        LOGGER.warning(String
                                            .format("[%s]: Stopping the machine as the State Entry Action %s for event %s from %s to %s raised runtime exception %s:\n%s",
                                                    m_name, actionEntry, event, stateCurrent, stateDesired, e,
                                                    writerString.toString()));
                                    }

                                    event = null;
                                    break;
                                }
                            }

                        }
                        else
                        {
                            if (LOGGER.isLoggable(Level.FINER))
                            {
                                LOGGER.finer(String.format("[%s]: No Entry Action defined for %s", m_name,
                                                           stateDesired));
                            }
                        }

                        if (LOGGER.isLoggable(Level.FINER))
                        {
                            LOGGER.finer(String
                                .format("[%s]: State changed to %s. There are %d remaining events to process", m_name,
                                        stateDesired, m_pendingEventCount.get()));
                        }

                        // now perform the appropriate instruction based on the entry action
                        if (instruction == null || instruction == Instruction.NOTHING)
                        {
                            // nothing to do for the next instruction
                            event = null;
                        }
                        else if (instruction == Instruction.STOP)
                        {
                            // stop the machine immediately (don't wait for scheduled transitions to complete)
                            stop();
                        }
                        else if (instruction instanceof TransitionTo)
                        {
                            // when the instruction is to "transition", we execute the transition
                            // immediately as this prevents the possible race-condition where
                            // asynchronously scheduled events can become "interleaved"
                            // between the completion of a state change and a move to another the desired state
                            TransitionTo<S> eventTransitionTo = (TransitionTo<S>) instruction;

                            event = prepareEvent(eventTransitionTo);
                        }
                        else if (instruction instanceof DelayedTransitionTo)
                        {
                            DelayedTransitionTo<S> eventDelayedTransitionTo = (DelayedTransitionTo<S>) instruction;

                            // schedule the transition event to be processed (and prepared) in the future
                            processLater(eventDelayedTransitionTo,
                                         eventDelayedTransitionTo.getDuration(),
                                         eventDelayedTransitionTo.getTimeUnit());

                            event = null;
                        }
                        else if (instruction instanceof ProcessEvent)
                        {
                            ProcessEvent<S> eventDelegating = (ProcessEvent<S>) instruction;

                            event = prepareEvent(eventDelegating.getEvent());
                        }
                        else if (instruction instanceof ProcessEventLater)
                        {
                            ProcessEventLater<S> eventDelayedInstruction = (ProcessEventLater<S>) instruction;

                            // schedule the event to be processed (and prepared) in the future
                            processLater(eventDelayedInstruction.getEvent(),
                                         eventDelayedInstruction.getDuration(),
                                         eventDelayedInstruction.getTimeUnit());

                            event = null;
                        }
                        else
                        {
                            // we simply ignore unknown types of instruction
                            if (LOGGER.isLoggable(Level.FINER))
                            {
                                LOGGER.finer(String
                                    .format("[%s]: Ignoring Instruction [%s] returned as part of transition to %s as it an unknown type for this Finite State Machine.",
                                            m_name, instruction, stateDesired));
                            }
                        }
                    }
                }
            }

            // when this is the last pending transition and we're not accepting any more,
            // notify waiting threads that we're done
            if (!m_isAcceptingEvents.get() && m_pendingEventCount.get() == 0)
            {
                if (LOGGER.isLoggable(Level.FINER))
                {
                    LOGGER.finer(String.format("[%s]: Completed processing events", m_name));
                }

                notifyAll();
            }
        }

        if (LOGGER.isLoggable(Level.FINER))
        {
            LOGGER.exiting(getClass().getName(), String.format("[%s]: processEvent", getName()));
        }
    }


    /**
     * A {@link CoalescedEvent} is a {@link LifecycleAwareEvent} that coalesces
     * other (wrapped) {@link Event}s with the same discriminator so that only
     * one {@link Event} actually executes.
     * <p>
     * For example:  Given 10 {@link Event}s submitted to a
     * {@link NonBlockingFiniteStateMachine} with the same discriminator, only
     * one of the said {@link Event}s will be processed.  All others will be
     * discarded.  Once the {@link CoalescedEvent} has been processed, a new
     * batch may be created when another {@link CoalescedEvent} of the same
     * discriminator is submitted.
     * <p>
     * The actual {@link Event} processed depends on the mode of coalescing
     * required.  The first {@link CoalescedEvent} submitted to
     * a {@link NonBlockingFiniteStateMachine} for a specific discriminator
     * effectively starts the coalescing of {@link Event}s for the said
     * discriminator.  When the mode is set to {@link Process#FIRST}, then
     * the first {@link Event} (starting the coalescing) will be processed and
     * others will be discarded. When the mode is set of {@link Process#MOST_RECENT}
     * then the most recently submitted {@link Event} will be processed and
     * likewise, all others for the same discriminator will be discarded.
     *
     * @param <S>  the type of the state for the {@link FiniteStateMachine}
     */
    public static class CoalescedEvent<S extends Enum<S>> implements LifecycleAwareEvent<S>
    {
        /**
         * The {@link CoalescedEvent} to process.
         */
        public static enum Process
        {
            /**
             * {link #FIRST} indicates that the first submitted
             * {@link Event} for a specific discriminator will be the one
             * which is processed. All other submitted {@link CoalescedEvent}s
             * of the same discriminator will be discarded.
             */
            FIRST,

            /**
             * {@link #MOST_RECENT} indicates that the most recently
             * submitted {@link Event} for a specified discriminator will be
             * processed. All other previously submitted {@link Event}s of the
             * same discriminator will be discarded.
             */
            MOST_RECENT;
        }


        /**
         * The {@link Event}s to be processed, arranged by discriminator.
         */
        private static ConcurrentHashMap<Discriminator, Event<?>> s_eventsByDiscriminator;

        /**
         * The discriminator/identifier that is used to coalesce {@link Event}s
         * of the same "type".
         */
        private Object m_discriminator;

        /**
         * The {@link Event} to be coalesed.
         */
        private Event<S> m_event;

        /**
         * The mode of coalescing to use for the {@link Event}.
         */
        private Process m_mode;

        /**
         * The {@link Event} that is eventually chosen to process
         * (from all of those submitted and coalesced)
         */
        private Event<S> m_eventChosen;


        /**
         * Constructs a {@link CoalescedEvent} of the specified {@link Event}
         * type using {@link Process#FIRST}.
         *
         * @param event  the {@link Event} to be executed when coalesced
         */
        public CoalescedEvent(Event<S> event)
        {
            this(event, Process.FIRST, event.getClass());
        }


        /**
         * Constructs a {@link CoalescedEvent} of the specified {@link Event}
         * type.
         *
         * @param event  the {@link Event} to be coalesced
         * @param mode   which {@link CoalescedEvent}s to process
         */
        public CoalescedEvent(Event<S> event,
                              Process  mode)
        {
            this(event, mode, event.getClass());
        }


        /**
         * Constructs a {@link CoalescedEvent} with the specified descriminator
         * and {@link Event}.
         *
         * @param event          the {@link Event} to be coalesced
         * @param mode           which {@link CoalescedEvent}s to process
         * @param discriminator  the descriminator used to uniquely coalesce the {@link Event}
         */
        public CoalescedEvent(Event<S> event,
                              Process  mode,
                              Object   discriminator)
        {
            m_discriminator = discriminator == null ? Void.class : discriminator;
            m_event         = event;
            m_mode          = mode;
            m_eventChosen   = null;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public S getDesiredState(S                state,
                                 ExecutionContext context)
        {
            // remove the actual event to be processed for the discriminator
            // (we do this because this event that we're processing may have
            // been replace ie: coalesced by another event)
            m_eventChosen = (Event<S>) s_eventsByDiscriminator.remove(m_discriminator);

            if (m_eventChosen == null)
            {
                // when there's no event to chose, we know that this event was
                // coalesced ie: already executed, hence there's nothing to do now
                return null;
            }
            else
            {
                return m_eventChosen.getDesiredState(state, context);
            }
        }


        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean onAccept(ExecutionContext context)
        {
            // CoalescingEvents may only be accepted by NonBlockingFiniteStateMachines
            if (context instanceof NonBlockingFiniteStateMachine)
            {
                boolean fIsAccepted;

                // ensure that the actual event is accepted
                // (there's no reason to accept unacceptable events)
                if (m_event instanceof LifecycleAwareEvent)
                {
                    fIsAccepted = ((LifecycleAwareEvent<S>) m_event).onAccept(context);
                }
                else
                {
                    fIsAccepted = true;
                }

                // replace the provided discriminator with one that is scoped
                // by the NonBlockingFiniteStateMachine;
                Discriminator discriminator = new Discriminator((NonBlockingFiniteStateMachine) context,
                                                                m_discriminator);

                m_discriminator = discriminator;

                if (fIsAccepted)
                {
                    if (m_mode == Process.FIRST)
                    {
                        fIsAccepted = s_eventsByDiscriminator.putIfAbsent(discriminator, m_event) == null;
                    }
                    else
                    {
                        fIsAccepted = s_eventsByDiscriminator.put(discriminator, m_event) == null;
                    }
                }

                return fIsAccepted;
            }
            else
            {
                throw new UnsupportedOperationException(String
                    .format("CoalescingEvents may only be used with %s instance",
                            NonBlockingFiniteStateMachine.class.getName()));
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessed(ExecutionContext context)
        {
            if (m_eventChosen instanceof LifecycleAwareEvent)
            {
                ((LifecycleAwareEvent<S>) m_eventChosen).onProcessed(context);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessing(ExecutionContext context)
        {
            if (m_eventChosen instanceof LifecycleAwareEvent)
            {
                ((LifecycleAwareEvent<S>) m_eventChosen).onProcessing(context);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return String.format("CoalescedEvent{%s, discriminator=%s, mode=%s}", m_event, m_discriminator, m_mode);
        }


        /**
         * A {@link Discriminator} is an object that is used to uniquely
         * differentiate events to be coalesced, scoped by a
         * {@link NonBlockingFiniteStateMachine}.
         */
        public static class Discriminator
        {
            /**
             * The {@link NonBlockingFiniteStateMachine} to which the
             * discriminator applies.
             */
            private NonBlockingFiniteStateMachine<?> m_machine;

            /**
             * The actual discriminator (not null).
             */
            private Object m_discriminator;


            /**
             * Constructs a {@link Discriminator} for the specified
             * {@link NonBlockingFiniteStateMachine}.
             *
             * @param machine        the {@link NonBlockingFiniteStateMachine}
             * @param discriminator  the discriminator
             */
            public Discriminator(NonBlockingFiniteStateMachine<?> machine,
                                 Object                           discriminator)
            {
                m_machine       = machine;
                m_discriminator = discriminator;
            }


            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode()
            {
                final int prime  = 31;
                int       result = 1;

                result = prime * result + ((m_discriminator == null) ? 0 : m_discriminator.hashCode());
                result = prime * result + ((m_machine == null) ? 0 : m_machine.hashCode());

                return result;
            }


            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object obj)
            {
                if (this == obj)
                {
                    return true;
                }

                if (obj == null)
                {
                    return false;
                }

                if (getClass() != obj.getClass())
                {
                    return false;
                }

                Discriminator other = (Discriminator) obj;

                if (m_discriminator == null)
                {
                    if (other.m_discriminator != null)
                    {
                        return false;
                    }
                }
                else if (!m_discriminator.equals(other.m_discriminator))
                {
                    return false;
                }

                if (m_machine == null)
                {
                    if (other.m_machine != null)
                    {
                        return false;
                    }
                }
                else if (!m_machine.equals(other.m_machine))
                {
                    return false;
                }

                return true;
            }
        }


        /**
         * Initialization of shared state.
         */
        static
        {
            s_eventsByDiscriminator = new ConcurrentHashMap<Discriminator, Event<?>>();
        }
    }


    /**
     * A {@link DelayedTransitionTo} is a specialized {@link Instruction} for
     * {@link NonBlockingFiniteStateMachine}s that enables a
     * {@link StateEntryAction} to request a delayed transition to another state,
     * unlike a {@link TransitionTo} {@link Instruction} which occurs immediately.
     *
     * @see TransitionTo
     */
    public static class DelayedTransitionTo<S extends Enum<S>> implements Instruction, Event<S>
    {
        /**
         * The desired state.
         */
        private S m_desiredState;

        /**
         * The amount of time to wait before the transition should occur.
         */
        private long m_duration;

        /**
         * The {@link TimeUnit} for the delay time.
         */
        private TimeUnit m_timeUnit;


        /**
         * Constructs a {@link DelayedTransitionTo} without a specified time (to be schedule as soon as possible).
         *
         * @param desiredState  the desired state to which to transition
         */
        public DelayedTransitionTo(S desiredState)
        {
            this(desiredState, 0, TimeUnit.MILLISECONDS);
        }


        /**
         * Constructs a {@link DelayedTransitionTo} with the specified time.
         *
         * @param desiredState  the desired state to which to transition
         * @param duration      the amount of time to wait before the desired transition should occur
         * @param timeUnit      the unit of time measure
         */
        public DelayedTransitionTo(S        desiredState,
                                   long     duration,
                                   TimeUnit timeUnit)
        {
            m_desiredState = desiredState;
            m_duration     = duration;
            m_timeUnit     = timeUnit;
        }


        /**
         * {@inheritDoc}
         */
        public S getDesiredState(S                currentState,
                                 ExecutionContext context)
        {
            return m_desiredState;
        }


        /**
         * Obtains the amount of time to wait before the transition to the desired state should occur
         *
         * @return the amount of time in the {@link #getTimeUnit()}
         */
        public long getDuration()
        {
            return m_duration;
        }


        /**
         * Obtains the {@link TimeUnit} for the {@link #getDuration()}
         *
         * @return the {@link TimeUnit}
         */
        public TimeUnit getTimeUnit()
        {
            return m_timeUnit;
        }
    }


    /**
     * A specialized {@link Instruction} for {@link NonBlockingFiniteStateMachine}s
     * that enables a {@link StateEntryAction} to request an {@link Event} to be
     * processed at some point in the future.
     * <p>
     * This is the same as calling {@link NonBlockingFiniteStateMachine#processLater(Event, long, TimeUnit)}
     *
     * @see ProcessEvent
     */
    public static class ProcessEventLater<S extends Enum<S>> implements Instruction
    {
        /**
         * The {@link Event} to process later.
         */
        private Event<S> m_event;

        /**
         * The amount of time to wait before the processing the {@link Event}.
         */
        private long m_duration;

        /**
         * The {@link TimeUnit} for the delay time.
         */
        private TimeUnit m_timeUnit;


        /**
         * Constructs a {@link ProcessEventLater} without a specified time
         * (to be schedule as soon as possible).
         *
         * @param event  the {@link Event} to process later
         */
        public ProcessEventLater(Event<S> event)
        {
            this(event, 0, TimeUnit.MILLISECONDS);
        }


        /**
         * Constructs a {@link ProcessEventLater} with the specified delay time.
         *
         * @param event     the {@link Event} to process later
         * @param duration  the amount of time to wait before processing the {@link Event}
         * @param timeUnit  the unit of time measure
         */
        public ProcessEventLater(Event<S> event,
                                 long     duration,
                                 TimeUnit timeUnit)
        {
            m_event    = event;
            m_duration = duration;
            m_timeUnit = timeUnit;
        }


        /**
         * Obtain the {@link Event} to process later.
         *
         * @return  the {@link Event} to process
         */
        public Event<S> getEvent()
        {
            return m_event;
        }


        /**
         * Obtains the amount of time to wait before the transition to the desired state should occur
         *
         * @return the amount of time in the {@link #getTimeUnit()}
         */
        public long getDuration()
        {
            return m_duration;
        }


        /**
         * Obtains the {@link TimeUnit} for the {@link #getDuration()}
         *
         * @return the {@link TimeUnit}
         */
        public TimeUnit getTimeUnit()
        {
            return m_timeUnit;
        }
    }


    /**
     * A {@link SubsequentEvent} is an {@link Event} that ensures that another
     * (wrapped) {@link Event} to occur if an only if the {@link FiniteStateMachine}
     * is at a certain transition count.  Should an attempt to process the wrapped
     * {@link Event} occur at another transition count, processing of the said
     * event is ignored.
     * <p>
     * {@link SubsequentEvent}s are designed to provide the ability for future
     * scheduled {@link Event}s to be skipped if another {@link Event} has
     * been processed between the time when the {@link SubsequentEvent}
     * was requested to be processed and when it was actually processed. That
     * is, the purpose of this is to allow an {@link Event} to be skipped if
     * other {@link Event}s interleave between the time when the said
     * {@link Event} was actually scheduled and when it was actually meant to
     * be processed.
     *
     * @param <S>  the state type of the {@link FiniteStateMachine}
     */
    public static class SubsequentEvent<S extends Enum<S>> implements LifecycleAwareEvent<S>
    {
        /**
         * The transition count that the {@link FiniteStateMachine}
         * must be at in order for the wrapped {@link Event} to be processed.
         */
        private long m_transitionCount;

        /**
         * The actual {@link Event}
         */
        private Event<S> m_event;


        /**
         * Constructs a {@link SubsequentEvent}.
         *
         * @param event  the actual event to process
         */
        public SubsequentEvent(Event<S> event)
        {
            m_transitionCount = -1;
            m_event           = event;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onAccept(ExecutionContext context)
        {
            // when being accepted use context to determine the transition count
            // at which the event should be processed
            m_transitionCount = context.getTransitionCount();

            // ensure the event can be accepted (if it's a lifecycle aware event)
            // otherwise always accept it
            return m_event instanceof LifecycleAwareEvent ? ((LifecycleAwareEvent<S>) m_event).onAccept(context) : true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessed(ExecutionContext context)
        {
            if (m_event instanceof LifecycleAwareEvent)
            {
                ((LifecycleAwareEvent<S>) m_event).onProcessed(context);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessing(ExecutionContext context)
        {
            if (m_event instanceof LifecycleAwareEvent)
            {
                ((LifecycleAwareEvent<S>) m_event).onProcessing(context);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public S getDesiredState(S                currentState,
                                 ExecutionContext context)
        {
            if (context.getTransitionCount() == m_transitionCount)
            {
                return m_event.getDesiredState(currentState, context);
            }
            else
            {
                if (LOGGER.isLoggable(Level.FINER))
                {
                    LOGGER.finer(String
                        .format("[%s]: Skipping event %s as another event was interleaved between when it was scheduled and when it was processed",
                                context.getName(), this));
                }

                // by returning null we skip the processing of the event
                return null;
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return String.format("SubsequentEvent{%s, @Transition #%d}", m_event, m_transitionCount + 1);
        }
    }
}
