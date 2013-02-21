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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.common.tuples.Pair;
import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.ScopedParameterProvider;
import com.oracle.coherence.environment.Environment;
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
 * If you're planning on implementing your own {@link EventDistributorBuilder} and thus {@link EventChannelController},
 * extending this implementation will save you a bunch of time.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractEventChannelController<T> implements EventChannelController, EventChannelControllerMBean
{
    /**
     * The {@link State} of the {@link EventChannelController}.
     */
    public static enum State
    {
        /**
         * <code>DISABLING</code> indicates that the {@link EventChannelController} is in the process of
         * being taken off-line.
         */
        DISABLING,

        /**
         * <code>DISABLED</code> indicates that the {@link EventChannelController} has been taken off-line.  Once in this
         * state no distribution will occur for the {@link EventChannelController} until it is manually restarted
         * (typically via JMX).  Further, <strong>no {@link Event}s</strong> will be queued for the distribution by the
         * {@link EventChannelController}.
         * <p>
         * This is a starting state for a {@link EventChannelController}.
         */
        DISABLED,

        /**
         * <code>Suspended</code> indicates that the {@link EventChannelController} is not operational.  It typically means
         * that the {@link EventChannelController} has either failed too many times (and thus was automatically suspended)
         * or it has been has been manually suspended.  Once in this state no distribution will occur until it is
         * manually restarted (typically via JMX).  Any {@link Event}s that are pending to be distributed, or new
         * {@link Event}s arriving via in the associated {@link EventDistributor}, will be queued for later distribution
         * when the {@link EventChannelController} returns to service.
         * <p>
         * This is a starting state for a {@link EventChannelController}.
         */
        SUSPENDED,

        /**
         * <code>PAUSED</code> indicates that the {@link EventChannelController} has not been started but it has been
         * scheduled to start at some time in the future.
         * <p>
         * This is a starting state for a {@link EventChannelController}.
         */
        PAUSED,

        /**
         * <code>STARTING</code> indicates that the {@link EventChannelController} is in the process of starting.
         */
        STARTING,

        /**
         * <code>SCHEDULED</code> indicates that the {@link EventChannelController} is scheduled to commence distribution
         * of {@link Event}s.
         */
        SCHEDULED,

        /**
         * <code>DISTRIBUTING</code> indicates that the {@link EventChannelController} is currently in the process of
         * distributing {@link Event}s using the {@link EventChannel}.
         */
        DISTRIBUTING,

        /**
         * <code>WAITING</code> indicates that the {@link EventChannelController} is currently in the process of waiting
         * for {@link Event}s to distribute using the {@link EventChannel}.
         */
        WAITING,

        /**
         * <code>STOPPING</code> indicates that the {@link EventChannelController} is in the process of stopping
         * the distribution of {@link Event}s, most likely because it is shutting down.
         */
        STOPPING,

        /**
         * <code>Stopped</code> indicates that the {@link EventChannelController} is stopped and can't be restarted.
         */
        STOPPED;
    }


    /**
     * The {@link Logger} to use for this class.
     */
    private static Logger logger = Logger.getLogger(AbstractEventChannelController.class.getName());

    /**
     * The {@link Environment} in which the {@link EventChannelController} is operating.
     */
    protected Environment environment;

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
    protected EventChannelController.Dependencies controllerDependencies;

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

    ;

    /**
     * The current {@link State} of the {@link EventChannelController}.
     */
    private State state;


    /**
     * Standard Constructor.
     */
    public AbstractEventChannelController(EventDistributor.Identifier         distributorIdentifier,
                                          EventChannelController.Identifier   controllerIdentifier,
                                          EventChannelController.Dependencies dependencies,
                                          Environment                         environment,
                                          ParameterProvider                   parameterProvider,
                                          ParameterizedBuilder<Serializer>    serializerBuilder)
    {
        this.distributorIdentifier  = distributorIdentifier;
        this.controllerIdentifier   = controllerIdentifier;
        this.controllerDependencies = dependencies;
        this.environment            = environment;

        // create a parameter provider for realizing the necessary builders
        MutableParameterProvider mutableParameterProvider = new ScopedParameterProvider(parameterProvider);

        // add the standard coherence parameters to the parameter provider
        mutableParameterProvider.addParameter(new Parameter("class-loader", environment.getClassLoader()));

        // realize the channel using the builder
        this.channel = dependencies.getEventChannelBuilder().realize(mutableParameterProvider);

        // realize the events transformer (if we have one)
        this.transformer = dependencies.getEventsTransformerBuilder() != null
                           ? dependencies.getEventsTransformerBuilder().realize(mutableParameterProvider) : null;

        // setup an executor service to perform background processing
        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories
                    .newThreadFactory(true, "EventChannelController", new ThreadGroup("EventChannelController")));

        // realize the serializer
        this.serializer = serializerBuilder.realize(mutableParameterProvider);

        // determine the controller starting state based on the specified starting mode
        switch (dependencies.getStartingMode())
        {
        case ENABLED :
            this.state = State.PAUSED;
            break;

        case DISABLED :
            this.state = State.DISABLED;
            break;

        case SUSPENDED :
            this.state = State.SUSPENDED;
            break;
        }

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


    /**
     * Sets the {@link State} of the {@link EventChannelController}.
     *
     * @param state
     */
    protected synchronized void setState(State state)
    {
        this.state = state;
    }


    /**
     * Returns the current {@link State} of the {@link EventChannelController}.
     */
    protected synchronized State getState()
    {
        return state;
    }


    /**
     * {@inheritDoc}
     */
    public String getEventDistributorName()
    {
        return String.format("%s (%s)",
                             distributorIdentifier.getSymbolicName(),
                             distributorIdentifier.getExternalName());
    }


    /**
     * {@inheritDoc}
     */
    public String getEventChannelControllerName()
    {
        return String.format("%s (%s)", controllerIdentifier.getSymbolicName(), controllerIdentifier.getExternalName());
    }


    /**
     * {@inheritDoc}
     */
    public String getEventChannelControllerState()
    {
        return state.toString();
    }


    /**
     * {@inheritDoc}
     */
    public long getLastBatchDistributionDuration()
    {
        return lastDistributionDurationMS;
    }


    /**
     * {@inheritDoc}
     */
    public long getMaximumBatchDistributionDuration()
    {
        return maximumDistributionDurationMS == Long.MIN_VALUE ? 0 : maximumDistributionDurationMS;
    }


    /**
     * {@inheritDoc}
     */
    public long getMinimumBatchDistributionDuration()
    {
        return minimumDistributionDurationMS == Long.MAX_VALUE ? 0 : minimumDistributionDurationMS;
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalDistributionDuration()
    {
        return totalDistributionDurationMS;
    }


    /**
     * {@inheritDoc}
     */
    public int getConsecutiveEventChannelFailures()
    {
        return consecutiveDistributionFailures;
    }


    /**
     * {@inheritDoc}
     */

    public int getEventBatchesDistributedCount()
    {
        return totalBatchesDistributed;
    }


    /**
     * {@inheritDoc}
     */

    public int getCandidateEventCount()
    {
        return totalCandidateEvents;
    }


    /**
     * {@inheritDoc}
     */

    public int getEventsDistributedCount()
    {
        return totalEventsDistributed;
    }


    /**
     * {@inheritDoc}
     */
    public void start()
    {
        final State state = getState();

        if (state == State.PAUSED || state == State.DISABLED || state == State.SUSPENDED)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Scheduled EventChannelController {0} to start.", controllerIdentifier);
            }

            // schedule to start as soon as possible
            setState(State.STARTING);
            schedule(new Runnable()
            {
                public void run()
                {
                    onStart(state);
                }
            }, 0, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        State currentState = getState();

        if (currentState != State.STOPPING && currentState != State.STOPPED)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Scheduled EventChannelController {0} to stop.", controllerIdentifier);
            }

            // schedule to stop as soon as possible
            setState(State.STOPPING);
            schedule(new Runnable()
            {
                public void run()
                {
                    onStop();
                }
            }, 0, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * Preempting an {@link EventChannelController} forces it to immediately schedule distribution of {@link Event}s
     * if it is in a {@link State#WAITING} state.  If the {@link EventChannelController} is not in a
     * {@link State#WAITING}, the request is ignored and nothing happens.
     */
    public void preempt()
    {
        State currentState = getState();

        if (currentState == State.WAITING)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "Premptively scheduled EventChannelController {0} to commence event distribution.",
                           controllerIdentifier);
            }

            setState(State.SCHEDULED);
            schedule(new Runnable()
            {
                public void run()
                {
                    onDistribute();
                }
            }, 0, TimeUnit.MILLISECONDS);
        }
    }


    protected abstract void internalDisable();


    /**
     * {@inheritDoc}
     */
    @Override
    public void disable()
    {
        synchronized (this)
        {
            State currentState = getState();

            if (currentState == State.SCHEDULED || currentState == State.WAITING || currentState == State.PAUSED
                || currentState == State.DISTRIBUTING)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Scheduled EventChannelController {0} to become disabled.",
                               controllerIdentifier);
                }

                setState(State.DISABLING);

                schedule(new Runnable()
                {
                    public void run()
                    {
                        onDisable();
                    }
                }, 0, TimeUnit.MILLISECONDS);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void suspend()
    {
        synchronized (this)
        {
            State currentState = getState();

            if (currentState == State.DISTRIBUTING || currentState == State.SCHEDULED || currentState == State.PAUSED
                || currentState == State.DISABLED || currentState == State.WAITING)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Suspended EventChannelController {0}.", controllerIdentifier);
                }

                setState(State.SUSPENDED);

                if (currentState == State.DISABLED)
                {
                    // re-enable the EventChannelController internals
                    internalEnable();
                }
            }
        }
    }


    protected abstract void internalDrain();


    /**
     * {@inheritDoc}
     */
    public void drain()
    {
        synchronized (this)
        {
            if (getState() == State.SUSPENDED)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINE,
                               "Commenced Draining events for EventChannelController {0}",
                               controllerIdentifier);
                }

                internalDrain();

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINE,
                               "Completed Draining events for EventChannelController {0}",
                               controllerIdentifier);
                }

            }
            else
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "Can't drain events for {0} as it is not Suspended. "
                               + "Please suspend first before attempting to drain.",
                               controllerIdentifier);
                }

            }
        }
    }


    /**
     * Schedules a {@link Runnable} to be executed by the {@link EventChannelController}.
     * This is typically used to schedule starting and stopping of the said {@link EventChannelController}.
     *
     * @param runnable The runnable to schedule.
     * @param delay    The amount of time to wait before running the runnable.
     * @param timeUnit The time unit for the delay.
     */
    protected void schedule(Runnable runnable,
                            long     delay,
                            TimeUnit timeUnit)
    {
        if (getState() != State.STOPPED)
        {
            executorService.schedule(runnable, delay, timeUnit);
        }
    }


    protected abstract void internalEnable();


    protected abstract void internalStart() throws EventChannelNotReadyException;


    /**
     * Starts the {@link EventChannelController} by attempting to start the {@link EventChannel}
     * and place the said {@link EventChannelController} into {@link State#SCHEDULED} mode, after which
     * it schedules to start event distribution.
     */
    private void onStart(final State previousState)
    {
        if (getState() == State.STARTING)
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

                setState(State.SCHEDULED);
                schedule(new Runnable()
                {
                    public void run()
                    {
                        onDistribute();
                    }
                }, 0, TimeUnit.MILLISECONDS);
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
                setState(State.PAUSED);
                schedule(new Runnable()
                {
                    public void run()
                    {
                        setState(State.STARTING);
                        onStart(previousState);
                    }
                }, controllerDependencies.getRestartDelay(), TimeUnit.MILLISECONDS);
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

                    // when an error occurs, we go back to the paused state, and
                    // schedule a restart (retry) after the restart delay
                    setState(State.PAUSED);
                    schedule(new Runnable()
                    {
                        public void run()
                        {
                            setState(State.STARTING);
                            onStart(previousState);
                        }
                    }, controllerDependencies.getRestartDelay(), TimeUnit.MILLISECONDS);

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

                    setState(State.SUSPENDED);
                }
            }
        }
    }


    protected abstract void internalStop();


    /**
     * Stops the {@link EventChannelController}.  Once stopped, it should not be started again.
     */
    private void onStop()
    {
        if (getState() == State.STOPPING)
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
            finally
            {
                setState(State.STOPPED);
            }
        }
    }


    private void onDisable()
    {
        if (getState() == State.DISABLING)
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

                if (logger.isLoggable(Level.INFO))
                {
                    logger.log(Level.INFO, "EventChannelController Exception was as follows", runtimeException);
                }

            }
            finally
            {
                setState(State.DISABLED);

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Disabled EventChannelController {0}", controllerIdentifier);
                }
            }
        }
    }


    protected abstract Pair<List<Event>, T> getEventsToDistribute();


    protected abstract void acknowledgeDistributedEvents(List<Event> events,
                                                         T           ack);


    /**
     * Starts the {@link EventChannelController} to distribute {@link Event}s
     */
    private void onDistribute()
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Commenced distribution with EventChannel {0}", controllerIdentifier);
        }

        // we can only move to the DISTRIBUTING state if we've been SCHEDULED or are WAITING
        synchronized (this)
        {
            State currentState = getState();

            if (currentState == State.SCHEDULED || currentState == State.WAITING)
            {
                setState(State.DISTRIBUTING);
            }
        }

        // only attempt to distribute if we're in the DISTRIBUTING state
        while (getState() == State.DISTRIBUTING)
        {
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
                    // nothing to distribute so move back to the waiting state (if we're still in the DISTRIBUTING)
                    synchronized (this)
                    {
                        if (getState() == State.DISTRIBUTING)
                        {
                            setState(State.WAITING);
                            schedule(new Runnable()
                            {
                                public void run()
                                {
                                    onDistribute();
                                }
                            }, controllerDependencies.getBatchDistributionDelay(), TimeUnit.MILLISECONDS);
                        }
                    }
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

                    // now schedule for the next batch
                    // (only if we're still in the DISTRIBUTING state)
                    synchronized (this)
                    {
                        if (getState() == State.DISTRIBUTING && controllerDependencies.getBatchDistributionDelay() > 0)
                        {
                            setState(State.SCHEDULED);

                            // schedule the next time we'll attempt to distribute a batch
                            schedule(new Runnable()
                            {
                                public void run()
                                {
                                    onDistribute();
                                }
                            }, controllerDependencies.getBatchDistributionDelay(), TimeUnit.MILLISECONDS);
                        }
                    }
                }

            }
            catch (RuntimeException runtimeException)
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.SEVERE,
                               "EventChannelController {0} failed to distribute events",
                               controllerIdentifier);
                }

                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.INFO, "Exception was as follows:", runtimeException);
                }

                // we've now had a failure!
                consecutiveDistributionFailures++;

                // schedule a retry?
                if (controllerDependencies.getTotalConsecutiveFailuresBeforeSuspending() < 0
                    || consecutiveDistributionFailures
                       < controllerDependencies.getTotalConsecutiveFailuresBeforeSuspending())
                {
                    // we immediately go back to the PAUSED state and schedule an attempt to restart
                    setState(State.PAUSED);
                    schedule(new Runnable()
                    {
                        public void run()
                        {
                            setState(State.STARTING);
                            onStart(State.DISTRIBUTING);
                        }
                    }, controllerDependencies.getRestartDelay(), TimeUnit.MILLISECONDS);
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

                    setState(State.SUSPENDED);
                }
            }
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Completed distribution with {0}", controllerIdentifier);
        }
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
         * By default a {@link EventChannelController} will wait 1000 milliseconds between attempts to distribute batches
         * of {@link Event}s.
         */
        public final static long BATCH_DISTRIBUTION_DELAY_DEFAULT = 1000;

        /**
         * By default a maximum of 100 {@link Event}s will be in a batch when distributed.
         */
        public final static int BATCH_SIZE_DEFAULT = 100;

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
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public DefaultDependencies()
        {
            // SKIP: deliberately empty
        }


        /**
         * Standard Constructor.
         */
        public DefaultDependencies(String                                         channelName,
                                   String                                         externalName,
                                   ParameterizedBuilder<EventChannel>             eventChannelBuilder,
                                   ParameterizedBuilder<EventIteratorTransformer> transformerBuilder,
                                   Mode                                           startingMode,
                                   long                                           batchDistributionDelayMS,
                                   int                                            batchSize,
                                   long                                           restartDelay,
                                   int                                            totalConsecutiveFailuresBeforeSuspending)
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
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String getChannelName()
        {
            return channelName;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String getExternalName()
        {
            return externalName;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ParameterizedBuilder<EventChannel> getEventChannelBuilder()
        {
            return eventChannelBuilder;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ParameterizedBuilder<EventIteratorTransformer> getEventsTransformerBuilder()
        {
            return transformerBuilder;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Mode getStartingMode()
        {
            return startingMode;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public long getBatchDistributionDelay()
        {
            return batchDistributionDelayMS;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int getBatchSize()
        {
            return batchSize;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public long getRestartDelay()
        {
            return restartDelay;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int getTotalConsecutiveFailuresBeforeSuspending()
        {
            return totalConsecutiveFailuresBeforeSuspending;
        }


        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
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
        }


        /**
         * {@inheritDoc}
         */
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
        }


        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
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
        }


        /**
         * {@inheritDoc}
         */
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
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return String.format("AbstractEventChannelController.Dependencies{channelName=%s, externalName=%s, "
                + "eventChannelBuilder=%s, transformerBuilder=%s, startingMode=%s, batchDistributionDelayMS=%d, "
                + "batchSize=%d, restartDelay=%d, totalConsecutiveFailuresBeforeSuspended=%d}",
                                 channelName,
                                 externalName,
                                 eventChannelBuilder,
                                 transformerBuilder,
                                 startingMode,
                                 batchDistributionDelayMS,
                                 batchSize,
                                 restartDelay,
                                 totalConsecutiveFailuresBeforeSuspending);
        }
    }
}
