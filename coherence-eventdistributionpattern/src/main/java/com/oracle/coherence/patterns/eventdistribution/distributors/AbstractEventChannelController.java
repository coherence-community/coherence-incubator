/*
 * File: AbstractEventChannelController.java
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

package com.oracle.coherence.patterns.eventdistribution.distributors;

import com.oracle.coherence.common.events.Event;

import com.oracle.coherence.common.finitestatemachines.AnnotationDrivenModel;
import com.oracle.coherence.common.finitestatemachines.ExecutionContext;
import com.oracle.coherence.common.finitestatemachines.Instruction;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine;
import com.oracle.coherence.common.finitestatemachines.annotations.OnEnterState;
import com.oracle.coherence.common.finitestatemachines.annotations.Transition;
import com.oracle.coherence.common.finitestatemachines.annotations.Transitions;

import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;

import com.oracle.coherence.common.tuples.Pair;

import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerMBean;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.EventTransformer;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.oracle.coherence.patterns.eventdistribution.transformers.MutatingEventIteratorTransformer;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AbstractEventChannelController} is a based implementation of the {@link EventChannelController} interface.
 * <p>
 * Internally it uses a {@link NonBlockingFiniteStateMachine} to manage the state and transitions
 * required to distribute events over an {@link EventChannel}.
 * <p>
 * If you're planning on implementing your own {@link EventDistributorBuilder} and thus {@link EventChannelController},
 * extending this implementation will save you a bunch of time.
 * <p>
 * Copyright (c) 2011-2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Transitions(
{
    @Transition(
        toState    = "DISTRIBUTING",
        fromStates = {"STARTING", "WAITING", "DELAYING"}
    ) , @Transition(
        toState    = "WAITING",
        fromStates = "DISTRIBUTING"
    ) , @Transition(
        toState    = "DELAYING",
        fromStates = "DISTRIBUTING"
    ) , @Transition(
        toState    = "SUSPENDED",
        fromStates =
        {
            "DISTRIBUTING", "STARTING", "ERROR", "DISABLED", "DRAINING", "WAITING", "DELAYING"
        }
    ) , @Transition(
        toState    = "STARTING",
        fromStates = {"ERROR", "SUSPENDED", "DISABLED"}
    ) , @Transition(
        toState    = "DRAINING",
        fromStates = {"SUSPENDED"}
    ) , @Transition(
        toState    = "DISABLED",
        fromStates =
        {
            "SUSPENDED", "WAITING", "DELAYING", "ERROR"
        }
    ) , @Transition(
        toState    = "ERROR",
        fromStates = {"STARTING", "DISTRIBUTING"}
    ) , @Transition(
        toState    = "STOPPED",
        fromStates =
        {
            "STARTING", "SUSPENDED", "WAITING", "DISABLED", "ERROR", "DELAYING"
        }
    )
})
public abstract class AbstractEventChannelController<T> implements EventChannelController, EventChannelControllerMBean
{
    /**
     * The events that control the internal {@link NonBlockingFiniteStateMachine}.
     */
    public static enum ControllerEvent implements com.oracle.coherence.common.finitestatemachines.Event<State>
    {
        /**
         * The <code>{@link #START}</code> event signals that a connection should be made to the
         * underlying channel.
         */
        START(State.STARTING),

        /**
         * The <code>{@link #STOP}</code> event signals that distribution should stop, never to
         * be recommenced with this instance.
         */
        STOP(State.STOPPED),

        /**
         * The <code>{@link #DISABLE}</code> event signals that distribution should be disabled.
         */
        DISABLE(State.DISABLED),

        /**
         * The <code>{@link #SUSPEND}</code> event signals that distribution should be suspended.
         */
        SUSPEND(State.SUSPENDED),

        /**
         * The <code>{@link #DRAIN}</code> event signals that distribution queues should be drained.
         */
        DRAIN(State.DRAINING),

        /**
         * The <code>{@link #RETRY}</code> event signals that distribution should be retried (after a failure).
         */
        RETRY(State.ERROR),

        /**
         * The <code>{@link #DISTRIBUTE}</code> event signals that distribution should commence.
         */
        DISTRIBUTE(State.DISTRIBUTING);

        /**
         * The desired {@link State} when the {@link ControllerEvent} occurs.
         */
        private State desiredState;


        /**
         * Constructs a {@link ControllerEvent}.
         *
         * @param desiredState  the desired {@link State} of the {@link NonBlockingFiniteStateMachine}
         *                      when the event occurs
         */
        ControllerEvent(State desiredState)
        {
            this.desiredState = desiredState;
        }


        @Override
        public State getDesiredState(State            state,
                                     ExecutionContext context)
        {
            return desiredState;
        }
    }


    /**
     * The {@link State} of the {@link EventChannelController} and internal {@link NonBlockingFiniteStateMachine}.
     */
    public static enum State
    {
        /**
         * <code>{@link #DISABLED}</code> indicates that the {@link EventChannelController} has been taken off-line.
         * Once in this state no distribution will occur for the {@link EventChannelController} until it is
         * manually restarted (typically via JMX).  Further, <strong>no {@link Event}s</strong> will be queued for
         * the distribution by the {@link EventChannelController}.
         * <p>
         * This is a starting state for an {@link EventChannelController}.
         */
        DISABLED,

        /**
         * <code>{@link #SUSPENDED}</code> indicates that the {@link EventChannelController} is not operational.
         * It typically means that the {@link EventChannelController} has either failed too many times (and thus was
         * automatically suspended) or it has been has been manually suspended.  Once in this state no distribution
         * will occur until it is manually restarted (typically via JMX).  Any {@link Event}s that are pending to be
         * distributed, or new {@link Event}s arriving via in the associated {@link EventDistributor}, will be queued
         * for later distribution when the {@link EventChannelController} returns to service.
         * <p>
         * This is a starting state for an {@link EventChannelController}.
         */
        SUSPENDED,

        /**
         * <code>{@link #STARTING}</code> indicates that the {@link EventChannelController} is in the process
         * of starting. Typically this means connecting to the underlying {@link EventChannel}.
         * <p>
         * This is a starting state for an {@link EventChannelController}.
         */
        STARTING,

        /**
         * <code>{@link #DISTRIBUTING}</code> indicates that the {@link EventChannelController} is in the
         * process of distributing {@link Event}s using the {@link EventChannel}.
         */
        DISTRIBUTING,

        /**
         * <code>{@link #WAITING}</code> indicates that the {@link EventChannelController} is in the process
         * of waiting for {@link Event}s to distribute using the {@link EventChannel}.
         */
        WAITING,

        /**
         * <code>{@link #DELAYING}</code> indicates that the {@link EventChannelController} is in the process of
         * delaying distribution between batches.  This occurs when there is more than enough events to distribute
         * in a single batch.  The delay will be {@link Dependencies#getBatchDistributionDelay()} milliseconds.
         */
        DELAYING,

        /**
         * <code>{@link #STOPPED}</code> indicates that the {@link EventChannelController} is stopped and can't be
         * restarted.
         */
        STOPPED,

        /**
         * <code>{@link #ERROR}</code> indicates that a recoverable error has occurred with the
         * {@link EventChannelController} and that an attempt to recover has been scheduled.
         */
        ERROR,

        /**
         * <code>{@link #DRAINING}</code> indicated that the {@link EventChannelController} is in the
         * process of draining internal queues to prevent previously queued {@link DistributableEntryEvent}s from
         * being distributed.
         */
        DRAINING;
    }


    /**
     * The {@link Logger} to use for this class.
     */
    private static Logger logger = Logger.getLogger(AbstractEventChannelController.class.getName());

    /**
     * The {@link ClassLoader} used by the {@link EventChannelController}.
     */
    protected ClassLoader loader;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} for the
     * {@link EventDistributor} in which the {@link EventChannelController} is operating.
     */
    protected EventDistributor.Identifier distributorIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} for the
     * {@link EventChannelController}.
     */
    protected EventChannelController.Identifier controllerIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies} of the {@link EventChannel}.
     */
    protected volatile EventChannelController.Dependencies controllerDependencies;

    /**
     * The {@link EventChannel} that will be used to distribute {@link Event}s for this {@link EventChannelController}.
     */
    protected EventChannel channel;

    /**
     * (optional) The {@link EventIteratorTransformer} that should be used to transform {@link Event}s prior to
     * distribution with an {@link EventChannel}.
     */
    protected EventIteratorTransformer transformer;

    /**
     * A {@link ScheduledExecutorService} that will be used to manage the life-cycle of this {@link EventChannelController}.
     */
    protected ScheduledExecutorService executorService;

    /**
     * The {@link Serializer} to use for (de)serializing {@link Event}s during distribution using this {@link EventChannelController}.
     */
    protected Serializer serializer;

    /**
     * The time (in ms) the last batch of {@link Event}s took to distribute.
     */
    protected long lastDistributionDurationMS;

    /**
     * The minimum time (in ms) a batch of {@link Event}s has taken to distribute.
     */
    protected long minimumDistributionDurationMS;

    /**
     * The maximum time (in ms) a batch of {@link Event}s has taken to distribute.
     */
    protected long maximumDistributionDurationMS;

    /**
     * The total time (in ms) the distribution of all {@link Event}s has taken.
     */
    protected long totalDistributionDurationMS;

    /**
     * The number of consecutive failures that have occurred thus far without success.
     */
    protected int consecutiveDistributionFailures;

    /**
     * The number of batches of {@link Event}s distributed.
     */
    protected int totalBatchesDistributed;

    /**
     * The number of {@link Event}s that have been considered for distribution.
     */
    protected int totalCandidateEvents;

    /**
     * The number {@link Event}s that have been distributed.
     */
    protected int totalEventsDistributed;

    /**
     * The {@link NonBlockingFiniteStateMachine} that controls the behavior of the {@link EventChannelController}.
     */
    protected NonBlockingFiniteStateMachine<State> machine;


    /**
     * Standard Constructor.
     */
    public AbstractEventChannelController(EventDistributor.Identifier         distributorIdentifier,
                                          EventChannelController.Identifier   controllerIdentifier,
                                          EventChannelController.Dependencies dependencies,
                                          ClassLoader                         loader,
                                          ParameterResolver                   parameterResolver,
                                          ParameterizedBuilder<Serializer>    serializerBuilder)
    {
        this.distributorIdentifier  = distributorIdentifier;
        this.controllerIdentifier   = controllerIdentifier;
        this.controllerDependencies = dependencies;
        this.loader                 = loader;

        // realize the channel using the builder
        this.channel = dependencies.getEventChannelBuilder().realize(parameterResolver, null, null);

        // realize the events transformer (if we have one)
        this.transformer = dependencies.getEventsTransformerBuilder() != null
                           ? dependencies.getEventsTransformerBuilder().realize(parameterResolver, null, null) : null;

        // setup an executor service to perform background processing
        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories
                    .newThreadFactory(true, "EventChannelController", new ThreadGroup("EventChannelController")));

        // realize the serializer
        this.serializer = serializerBuilder.realize(parameterResolver, null, null);

        // determine the controller starting state based on the specified starting mode (assume STARTING if unknown)
        State initialState = State.STARTING;

        switch (dependencies.getStartingMode())
        {
        case ENABLED :
            initialState = State.STARTING;
            break;

        case DISABLED :
            initialState = State.DISABLED;
            break;

        case SUSPENDED :
            initialState = State.SUSPENDED;
            break;
        }

        // construct an annotation driven model for the finite state machine
        AnnotationDrivenModel<State> model = new AnnotationDrivenModel<State>(State.class, this);

        // establish the finite state machine for the controller
        this.machine = new NonBlockingFiniteStateMachine<State>(getEventChannelControllerName(),
                                                                model,
                                                                initialState,
                                                                executorService,
                                                                true,
                                                                false);

        // JMX and other management information
        this.lastDistributionDurationMS      = 0;
        this.maximumDistributionDurationMS   = Long.MIN_VALUE;
        this.minimumDistributionDurationMS   = Long.MAX_VALUE;
        this.totalDistributionDurationMS     = 0;
        this.consecutiveDistributionFailures = 0;
        this.totalBatchesDistributed         = 0;
        this.totalCandidateEvents            = 0;
        this.totalEventsDistributed          = 0;
    }


    /**
     * Determines the {@link Serializer} to use for (de)serializing {@link Event}s using this {@link EventChannelController}.
     *
     * @return A {@link Serializer}
     */
    protected Serializer getSerializer()
    {
        return serializer;
    }


    @Override
    public String getEventDistributorName()
    {
        return String.format("%s (%s)",
                             distributorIdentifier.getSymbolicName(),
                             distributorIdentifier.getExternalName());
    }


    @Override
    public String getEventChannelControllerName()
    {
        return String.format("%s (%s)", controllerIdentifier.getSymbolicName(), controllerIdentifier.getExternalName());
    }


    @Override
    public String getEventChannelControllerState()
    {
        return machine.getState().toString();
    }


    @Override
    public long getLastBatchDistributionDuration()
    {
        return lastDistributionDurationMS;
    }


    @Override
    public long getMaximumBatchDistributionDuration()
    {
        return maximumDistributionDurationMS == Long.MIN_VALUE ? 0 : maximumDistributionDurationMS;
    }


    @Override
    public long getMinimumBatchDistributionDuration()
    {
        return minimumDistributionDurationMS == Long.MAX_VALUE ? 0 : minimumDistributionDurationMS;
    }


    @Override
    public long getTotalDistributionDuration()
    {
        return totalDistributionDurationMS;
    }


    @Override
    public int getConsecutiveEventChannelFailures()
    {
        return consecutiveDistributionFailures;
    }


    @Override
    public int getEventBatchesDistributedCount()
    {
        return totalBatchesDistributed;
    }


    @Override
    public int getCandidateEventCount()
    {
        return totalCandidateEvents;
    }


    @Override
    public int getEventsDistributedCount()
    {
        return totalEventsDistributed;
    }


    @Override
    public long getBatchDistributionDelay()
    {
        return controllerDependencies.getBatchDistributionDelay();
    }


    @Override
    public void setBatchDistributionDelay(long delayMS)
    {
        controllerDependencies.setBatchDistributionDelay(delayMS);
    }


    @Override
    public int getBatchSize()
    {
        return controllerDependencies.getBatchSize();
    }


    @Override
    public void setBatchSize(int batchSize)
    {
        controllerDependencies.setBatchSize(batchSize);
    }


    @Override
    public long getRestartDelay()
    {
        return controllerDependencies.getRestartDelay();
    }


    @Override
    public void setRestartDelay(long delayMS)
    {
        controllerDependencies.setRestartDelay(delayMS);
    }


    @Override
    public void start()
    {
        // attempt to start the machine
        if (machine.start())
        {
            // as this was the first time the machine was started there's nothing more to do
            // as the machine will transition to the required initial state
        }
        else
        {
            // when it's not the first time, we should attempt to transition to
            // the starting state so we can start distribution
            machine.process(ControllerEvent.START);
        }
    }


    @Override
    public void stop()
    {
        machine.process(ControllerEvent.STOP);
    }


    @Override
    public void preempt()
    {
        // as there is no limit to the number of times preempt is called we must coalesce the events
        // for the finite state machine to prevent a queue overrun
        machine.process(new NonBlockingFiniteStateMachine.CoalescedEvent<State>(ControllerEvent.DISTRIBUTE));
    }


    @Override
    public void disable()
    {
        machine.process(ControllerEvent.DISABLE);
    }


    @Override
    public void suspend()
    {
        machine.process(ControllerEvent.SUSPEND);
    }


    @Override
    public void drain()
    {
        machine.process(ControllerEvent.DRAIN);
    }


    protected abstract Pair<List<Event>, T> getEventsToDistribute();


    protected abstract void acknowledgeDistributedEvents(List<Event> events,
                                                         T           ack);


    protected abstract void internalDrain();


    protected abstract void internalEnable();


    protected abstract void internalDisable();


    protected abstract void internalStart() throws EventChannelNotReadyException;


    protected abstract void internalStop();


    /**
     * Attempts to connect and start the {@link EventChannel}.
     */
    @OnEnterState("STARTING")
    public Instruction onStarting(State                                                        previousState,
                                  State                                                        newState,
                                  com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                  ExecutionContext                                             context)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Starting EventChannelController {0}", controllerIdentifier);
        }

        try
        {
            if (previousState == State.DISABLED)
            {
                // enable the EventChannelController internals
                internalEnable();
            }

            // start the EventChannelController internals
            internalStart();

            // we've successfully started so reset the distribution failure count
            consecutiveDistributionFailures = 0;

            // schedule distribution to commence immediately
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "Scheduled EventChannelController {0} to commence event distribution.",
                           controllerIdentifier);
            }

            return new Instruction.TransitionTo<State>(State.DISTRIBUTING);
        }
        catch (EventChannelNotReadyException notReadyException)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "The {0} is not ready to start.  Deferring (re)start for {1} ms",
                           new Object[] {controllerIdentifier, controllerDependencies.getRestartDelay()});
            }

            // schedule a restart (retry) after the restart delay
            return new Instruction.TransitionTo<State>(State.ERROR);
        }
        catch (RuntimeException runtimeException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Failed while attempting to start {0}", controllerIdentifier);
            }

            if (logger.isLoggable(Level.INFO))
            {
                logger.log(Level.INFO, "EventChannel Exception was as follows", runtimeException);
            }

            // we've had a failure!
            consecutiveDistributionFailures++;

            // schedule a retry?
            if (controllerDependencies.getTotalConsecutiveFailuresBeforeSuspending() < 0
                || consecutiveDistributionFailures
                   < controllerDependencies.getTotalConsecutiveFailuresBeforeSuspending())
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Scheduled another attempt to start EventChannelController {0}.",
                               controllerIdentifier);
                }

                return new Instruction.TransitionTo<State>(State.ERROR);
            }
            else
            {
                // suspend distribution as we've had too many consecutive failures
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "Suspending {0} as there have been too " + "many (%d) consecutive failures ",
                               controllerIdentifier);
                }

                return new Instruction.TransitionTo<State>(State.SUSPENDED);
            }
        }
    }


    /**
     * Stops the {@link EventChannelController}.  Once stopped, it should not be started again.
     */
    @OnEnterState("STOPPED")
    public Instruction onStopped(State                                                        previousState,
                                 State                                                        newState,
                                 com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                 ExecutionContext                                             context)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Stopping EventChannelController {0}", controllerIdentifier);
        }

        try
        {
            internalStop();
        }
        catch (RuntimeException runtimeException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Failed while attempting to stop {0}", controllerIdentifier);
            }

            if (logger.isLoggable(Level.INFO))
            {
                logger.log(Level.INFO, "EventChannel Exception was as follows", runtimeException);
            }

        }

        return Instruction.STOP;
    }


    @OnEnterState("DISABLED")
    public Instruction onDisabled(State                                                        previousState,
                                  State                                                        newState,
                                  com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                  ExecutionContext                                             context)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Disabling EventChannelController {0}", controllerIdentifier);
        }

        try
        {
            internalDisable();
        }
        catch (RuntimeException runtimeException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "Failed while attempting to disable EventChannelController {0}",
                           controllerIdentifier);
            }
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Disabled EventChannelController {0}", controllerIdentifier);
        }

        return Instruction.NOTHING;
    }


    /**
     * Suspends an {@link EventChannel}.
     */
    @OnEnterState("SUSPENDED")
    public Instruction onSuspended(State                                                        previousState,
                                   State                                                        newState,
                                   com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                   ExecutionContext                                             context)
    {
        // when transitioning from DISABLED state we must call internalEnable()
        if (previousState == State.DISABLED)
        {
            try
            {
                internalEnable();
            }
            catch (RuntimeException e)
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "Failed to enable EventChannelController {0}, moving back to disabled state",
                               controllerIdentifier);
                }

                return new Instruction.TransitionTo<State>(State.DISABLED);
            }
        }

        return Instruction.NOTHING;
    }


    /**
     * Handle when an error occurs on an {@link EventChannel} (by scheduling a restart)
     */
    @OnEnterState("ERROR")
    public Instruction onError(State                                                        previousState,
                               State                                                        newState,
                               com.oracle.coherence.common.finitestatemachines.Event<State> event,
                               ExecutionContext                                             context)
    {
        return new NonBlockingFiniteStateMachine
            .ProcessEventLater<State>(new NonBlockingFiniteStateMachine.SubsequentEvent<State>(ControllerEvent.START),
                                      controllerDependencies.getRestartDelay(),
                                      TimeUnit.MILLISECONDS);
    }


    /**
     * Handle when an {@link EventChannel} must wait for new events to arrive for distribution.
     */
    @OnEnterState("WAITING")
    public Instruction onWaiting(State                                                        previousState,
                                 State                                                        newState,
                                 com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                 ExecutionContext                                             context)
    {
        return new NonBlockingFiniteStateMachine
            .ProcessEventLater<State>(new NonBlockingFiniteStateMachine
                .SubsequentEvent<State>(new NonBlockingFiniteStateMachine
                    .CoalescedEvent<State>(ControllerEvent.DISTRIBUTE)),
                                      controllerDependencies.getEventPollingDelay(),
                                      TimeUnit.MILLISECONDS);
    }


    /**
     * Handle when an {@link EventChannel} must wait between distributing batches.
     */
    @OnEnterState("DELAYING")
    public Instruction onDelaying(State                                                        previousState,
                                  State                                                        newState,
                                  com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                  ExecutionContext                                             context)
    {
        return new NonBlockingFiniteStateMachine
            .ProcessEventLater<State>(new NonBlockingFiniteStateMachine
                .SubsequentEvent<State>(new NonBlockingFiniteStateMachine
                    .CoalescedEvent<State>(ControllerEvent.DISTRIBUTE)),
                                      controllerDependencies.getBatchDistributionDelay(),
                                      TimeUnit.MILLISECONDS);
    }


    /**
     * Drains an {@link EventChannel} of {@link Event}s that may be distributed to it.
     */
    @OnEnterState("DRAINING")
    public Instruction onDraining(State                                                        previousState,
                                  State                                                        newState,
                                  com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                  ExecutionContext                                             context)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Draining EventChannelController {0}", controllerIdentifier);
        }

        try
        {
            internalDrain();
        }
        catch (RuntimeException runtimeException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "Failed to drain the waiting events for EventChannelController {0} due to {1}. Suspending EventChannelController",
                           new Object[] {controllerDependencies.getChannelName(), runtimeException.getCause()});

                logger.log(Level.WARNING, "EventChannel Exception was as follows", runtimeException);
            }
        }

        return new Instruction.TransitionTo<State>(State.SUSPENDED);
    }


    /**
     * Starts the {@link EventChannelController} to distribute {@link Event}s
     */
    @OnEnterState("DISTRIBUTING")
    public Instruction onDistributing(State                                                        previousState,
                                      State                                                        newState,
                                      com.oracle.coherence.common.finitestatemachines.Event<State> event,
                                      ExecutionContext                                             context)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Commenced distribution with EventChannel {0}", controllerIdentifier);
        }

        // the resulting instruction to the Finite State Machine
        Instruction instruction;

        try
        {
            // get the events we need to distribute together with the implementation specific
            // information on how to acknowledge the events once they are distributed
            Pair<List<Event>, T> etp       = getEventsToDistribute();

            List<Event>          eventList = etp.getX();
            T                    ack       = etp.getY();

            // any events to distribute?
            if (eventList.isEmpty())
            {
                // nothing to distribute so move back to the waiting state
                instruction = new Instruction.TransitionTo<State>(State.WAITING);
            }
            else
            {
                int candidateEvents = eventList.size();    // Keep a running track of candidate events

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Commencing distributing {1} event(s) with EventChannel {0}",
                               new Object[] {controllerIdentifier, eventList.size()});
                }

                long startTime = System.currentTimeMillis();

                // transform the current batch of events

                // (ensure each of the EntryEvents have a correct context)
                Iterator<Event> events = new MutatingEventIteratorTransformer(new EventTransformer()
                {
                    @Override
                    public Event transform(Event event)
                    {
                        if (event instanceof DistributableEntryEvent)
                        {
                            DistributableEntryEvent distributableEntryEvent = (DistributableEntryEvent) event;
                            NamedCache namedCache = CacheFactory.getCache(distributableEntryEvent.getCacheName());

                            distributableEntryEvent.getEntry()
                                .setContext(namedCache.getCacheService().getBackingMapManager().getContext());

                            return distributableEntryEvent;
                        }
                        else
                        {
                            return event;
                        }
                    }
                }).transform(eventList.iterator());

                events = transformer == null ? events : transformer.transform(events);

                // send the events
                int eventsDistributed = channel.send(events);

                // calculate JMX statistics
                this.totalBatchesDistributed++;
                this.totalCandidateEvents   = this.totalCandidateEvents + candidateEvents;
                this.totalEventsDistributed = this.totalEventsDistributed + eventsDistributed;

                long distributionDurationMS = System.currentTimeMillis() - startTime;

                lastDistributionDurationMS    = distributionDurationMS;
                maximumDistributionDurationMS = Math.max(maximumDistributionDurationMS, distributionDurationMS);
                minimumDistributionDurationMS = Math.min(minimumDistributionDurationMS, distributionDurationMS);
                totalDistributionDurationMS   += distributionDurationMS;

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Completed distributing {1} Event(s) with EventChannel {0}",
                               new Object[] {controllerIdentifier, eventList.size()});
                }

                // acknowledge the events we've seen
                acknowledgeDistributedEvents(eventList, ack);

                // reset the number of consecutive failures as we've had success!
                consecutiveDistributionFailures = 0;

                instruction                     = new Instruction.TransitionTo<State>(State.DELAYING);
            }
        }
        catch (RuntimeException runtimeException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "EventChannelController {0} failed to distribute events",
                           controllerIdentifier);

                logger.log(Level.INFO, "Exception was as follows:", runtimeException);
            }

            // we've now had a failure!
            consecutiveDistributionFailures++;

            // schedule a retry?
            if (controllerDependencies.getTotalConsecutiveFailuresBeforeSuspending() < 0
                || consecutiveDistributionFailures
                   < controllerDependencies.getTotalConsecutiveFailuresBeforeSuspending())
            {
                instruction = new Instruction.TransitionTo<State>(State.ERROR);
            }
            else
            {
                // suspend distribution as we've had too many consecutive failures
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "Suspending distribution for {0} as there have been too many ({1}) "
                               + "consecutive failures ",
                               new Object[] {controllerIdentifier, consecutiveDistributionFailures});
                }

                instruction = new Instruction.TransitionTo<State>(State.SUSPENDED);
            }
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER,
                       "Completed distribution with {0}.  Next Instruction {1}",
                       new Object[] {controllerIdentifier, instruction});
        }

        return instruction;
    }


    /**
     * The default implementation of the {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies}
     * for a {@link EventChannelController}.
     */
    @SuppressWarnings("serial")
    public static class DefaultDependencies implements Dependencies, PortableObject, ExternalizableLite
    {
        /**
         * By default a {@link EventChannelController} is enabled when it is realized.
         */
        public final static Mode STARTING_MODE_DEFAULT = Mode.ENABLED;

        /**
         * By default a {@link EventChannelController} will wait 1000 milliseconds between attempts to
         * distribute batches of {@link Event}s (when there are batches).
         */
        public final static long BATCH_DISTRIBUTION_DELAY_DEFAULT = 1000;

        /**
         * By default a maximum of 100 {@link Event}s will be in a batch when distributed.
         */
        public final static int BATCH_SIZE_DEFAULT = 100;

        /**
         * By default a {@link EventChannelController} will wait 1000 milliseconds between attempts determine
         * if there are new {@link Event}s to distribute.
         */
        public final static long EVENT_POLLING_DELAY_DEFAULT = 1000;

        /**
         * By default a {@link EventChannelController} will be halted and then retried after a delay of 10000
         * milliseconds when an error occurs.
         */
        public final static long RESTART_DELAY_DEFAULT = 10000;

        /**
         * By default distribution will never be suspended after a failure.  It will simply retry forever
         */
        public final static int TOTAL_CONSECUTIVE_FAILURES_BEFORE_SUSPENDING = -1;

        /**
         * The name of the channel.
         */
        private String channelName;

        /**
         * The name a {@link EventChannelController} will use when registering with external resources.
         * (This is implementation dependent).
         */
        private String externalName;

        /**
         * The {@link ParameterizedBuilder} that may be used to realize a {@link EventChannel} when required by
         * a {@link EventChannelController}.
         */
        private ParameterizedBuilder<EventChannel> eventChannelBuilder;

        /**
         * An {@link ParameterizedBuilder} for the (optional) {@link EventIteratorTransformer}.
         */
        private ParameterizedBuilder<EventIteratorTransformer> transformerBuilder;

        /**
         * The desired starting {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Mode}
         * of the {@link EventChannelController}.
         */
        private Mode startingMode;

        /**
         * The number of milliseconds to wait between attempts to distribute {@link Event}s.
         */
        private long batchDistributionDelayMS;

        /**
         * The maximum number of {@link Event}s that may be "batched" together in a single batch.
         */
        private int batchSize;

        /**
         * The number of milliseconds a {@link EventChannel} waits before retrying after an error has occurred.
         */
        private long restartDelay;

        /**
         * The number of consecutive failures that may occur with an {@link EventChannel} before it is suspended from
         * distributing {@link Event}s.
         */
        private int totalConsecutiveFailuresBeforeSuspending;

        /**
         * The number of milliseconds an {@link EventChannel} will wait before attempting to determine if there
         * are new {@link Event}s to distribute.
         */
        private long eventPollingDelay;


        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public DefaultDependencies()
        {
            // SKIP: deliberately empty
        }


        /**
         * Constructs a {@link DefaultDependencies}.
         */
        public DefaultDependencies(String                                         channelName,
                                   String                                         externalName,
                                   ParameterizedBuilder<EventChannel>             eventChannelBuilder,
                                   ParameterizedBuilder<EventIteratorTransformer> transformerBuilder,
                                   Mode                                           startingMode,
                                   long                                           batchDistributionDelayMS,
                                   int                                            batchSize,
                                   long                                           restartDelay,
                                   int                                            totalConsecutiveFailuresBeforeSuspending,
                                   long                                           eventPollingDelay)
        {
            this.channelName                              = channelName;
            this.externalName                             = externalName;
            this.eventChannelBuilder                      = eventChannelBuilder;
            this.transformerBuilder                       = transformerBuilder;
            this.startingMode                             = startingMode;
            this.batchDistributionDelayMS                 = batchDistributionDelayMS;
            this.batchSize                                = batchSize;
            this.restartDelay                             = restartDelay;
            this.totalConsecutiveFailuresBeforeSuspending = totalConsecutiveFailuresBeforeSuspending;
            this.eventPollingDelay                        = eventPollingDelay;
        }


        @Override
        public String getChannelName()
        {
            return channelName;
        }


        @Override
        public String getExternalName()
        {
            return externalName;
        }


        @Override
        public ParameterizedBuilder<EventChannel> getEventChannelBuilder()
        {
            return eventChannelBuilder;
        }


        @Override
        public ParameterizedBuilder<EventIteratorTransformer> getEventsTransformerBuilder()
        {
            return transformerBuilder;
        }


        @Override
        public Mode getStartingMode()
        {
            return startingMode;
        }


        @Override
        public long getBatchDistributionDelay()
        {
            return batchDistributionDelayMS;
        }


        @Override
        public void setBatchDistributionDelay(long delayMS)
        {
            batchDistributionDelayMS = delayMS;
        }


        @Override
        public int getBatchSize()
        {
            return batchSize;
        }


        @Override
        public void setBatchSize(int batchSize)
        {
            this.batchSize = batchSize;
        }


        @Override
        public long getRestartDelay()
        {
            return restartDelay;
        }


        @Override
        public void setRestartDelay(long delayMS)
        {
            restartDelay = delayMS;
        }


        @Override
        public long getEventPollingDelay()
        {
            return eventPollingDelay;
        }


        @Override
        public int getTotalConsecutiveFailuresBeforeSuspending()
        {
            return totalConsecutiveFailuresBeforeSuspending;
        }


        @Override
        public void readExternal(DataInput in) throws IOException
        {
            this.channelName         = ExternalizableHelper.readSafeUTF(in);
            this.externalName        = ExternalizableHelper.readSafeUTF(in);
            this.eventChannelBuilder = (ParameterizedBuilder<EventChannel>) ExternalizableHelper.readObject(in);
            this.transformerBuilder =
                (ParameterizedBuilder<EventIteratorTransformer>) ExternalizableHelper.readObject(in);
            this.startingMode                             = Mode.valueOf(ExternalizableHelper.readSafeUTF(in));
            this.batchDistributionDelayMS                 = ExternalizableHelper.readLong(in);
            this.batchSize                                = ExternalizableHelper.readInt(in);
            this.restartDelay                             = ExternalizableHelper.readLong(in);
            this.totalConsecutiveFailuresBeforeSuspending = ExternalizableHelper.readInt(in);
            this.eventPollingDelay                        = ExternalizableHelper.readLong(in);
        }


        @Override
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeSafeUTF(out, channelName);
            ExternalizableHelper.writeSafeUTF(out, externalName);
            ExternalizableHelper.writeObject(out, eventChannelBuilder);
            ExternalizableHelper.writeObject(out, transformerBuilder);
            ExternalizableHelper.writeSafeUTF(out, startingMode.name());
            ExternalizableHelper.writeLong(out, batchDistributionDelayMS);
            ExternalizableHelper.writeInt(out, batchSize);
            ExternalizableHelper.writeLong(out, restartDelay);
            ExternalizableHelper.writeInt(out, totalConsecutiveFailuresBeforeSuspending);
            ExternalizableHelper.writeLong(out, eventPollingDelay);
        }


        @Override
        public void readExternal(PofReader reader) throws IOException
        {
            this.channelName                              = reader.readString(1);
            this.externalName                             = reader.readString(2);
            this.eventChannelBuilder                      = (ParameterizedBuilder<EventChannel>) reader.readObject(3);
            this.transformerBuilder = (ParameterizedBuilder<EventIteratorTransformer>) reader.readObject(4);
            this.startingMode                             = Mode.valueOf(reader.readString(5));
            this.batchDistributionDelayMS                 = reader.readLong(6);
            this.batchSize                                = reader.readInt(7);
            this.restartDelay                             = reader.readLong(8);
            this.totalConsecutiveFailuresBeforeSuspending = reader.readInt(9);
            this.eventPollingDelay                        = reader.readLong(10);
        }


        @Override
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeString(1, channelName);
            writer.writeString(2, externalName);
            writer.writeObject(3, eventChannelBuilder);
            writer.writeObject(4, transformerBuilder);
            writer.writeString(5, startingMode.name());
            writer.writeLong(6, batchDistributionDelayMS);
            writer.writeInt(7, batchSize);
            writer.writeLong(8, restartDelay);
            writer.writeInt(9, totalConsecutiveFailuresBeforeSuspending);
            writer.writeLong(10, eventPollingDelay);
        }


        @Override
        public String toString()
        {
            return String.format("AbstractEventChannelController.Dependencies{channelName=%s, externalName=%s, "
                + "eventChannelBuilder=%s, transformerBuilder=%s, startingMode=%s, batchDistributionDelayMS=%d, "
                + "batchSize=%d, restartDelay=%d, totalConsecutiveFailuresBeforeSuspended=%d, eventPollingDelay=%d}",
                                 channelName,
                                 externalName,
                                 eventChannelBuilder,
                                 transformerBuilder,
                                 startingMode,
                                 batchDistributionDelayMS,
                                 batchSize,
                                 restartDelay,
                                 totalConsecutiveFailuresBeforeSuspending,
                                 eventPollingDelay);
        }
    }
}
