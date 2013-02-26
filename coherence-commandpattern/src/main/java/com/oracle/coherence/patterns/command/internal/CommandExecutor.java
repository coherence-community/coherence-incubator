/*
 * File: CommandExecutor.java
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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.common.sequencegenerators.ClusteredSequenceGenerator;
import com.oracle.coherence.common.sequencegenerators.SequenceGenerator;
import com.oracle.coherence.common.ticketing.Ticket;
import com.oracle.coherence.common.ticketing.TicketAggregator;
import com.oracle.coherence.common.ticketing.TicketBook;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.CommandSubmitter;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ContextConfiguration;
import com.oracle.coherence.patterns.command.ContextConfiguration.ManagementStrategy;
import com.oracle.coherence.patterns.command.ContextsManager;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.oracle.coherence.patterns.command.PriorityCommand;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.management.Registry;
import com.tangosol.run.component.ExecutionContext;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.processor.UpdaterProcessor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * An {@link CommandExecutor} is responsible for coordinating and
 * executing {@link Command}s (represented as {@link CommandExecutionRequest}s)
 * for a single a {@link Context}.
 * <p>
 * Included in this responsibility is the recovery and potential
 * re-execution of {@link CommandExecutionRequest}s
 * (in-the-originally-submitted-order).  Such recovery may be due
 * to Coherence load-balancing or recovering cache partitions.
 * <p>
 * Internally {@link CommandExecutor}s operate as a simple
 * finite-state-machine (see {@link State} for details on states).
 * <p>
 * {@link CommandExecutor}s state transitions are typically controlled
 * via 'events' delivered a {@link ContextBackingMapListener} or through the
 * {@link SubmitCommandExecutionRequestProcessor}.
 * <p>
 * All {@link CommandExecutor}s are monitorable through JMX.  They are
 * automatically registered and de-registered when they created and stopped
 * (respectively).
 * <p>
 * Use a {@link ContextsManager} register and manage {@link Context}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see CommandExecutionRequest
 * @see CommandSubmitter
 * @see CommandExecutorMBean
 * @see SubmitCommandExecutionRequestProcessor
 *
 * @author Brian Oliver
 * @author Simon Bisson
 */
public class CommandExecutor implements CommandExecutorMBean
{
    /**
     * The set of possible {@link State}s for a {@link CommandExecutor}.
     */
    public static enum State
    {
        /**
         * The <code>New</code> state indicates that a {@link CommandExecutor}
         * has been created, and may accept {@link CommandExecutionRequest}s, but
         * will not execute them until the associated {@link Context} has
         * been provided.
         */
        New,

        /**
         * The <code>Starting</code> state indicates that a {@link CommandExecutor}
         * is in the process of initializing itself to commence executing
         * {@link CommandExecutionRequest}s.
         */
        Starting,

        /**
         * <code>Waiting</code> indicates that a {@link CommandExecutor} has
         * been successfully started but is currently waiting for
         * {@link CommandExecutionRequest}s to arrive.
         */
        Waiting,

        /**
         * <code>Scheduled</code> indicates that a {@link CommandExecutor} has
         * been been scheduled to execute {@link CommandExecutionRequest}s,
         * but has yet to commence execution.
         */
        Scheduled,

        /**
         * <code>Executing</code> indicates that a {@link CommandExecutor}
         * is in the process of executing {@link CommandExecutionRequest}s.
         */
        Executing,

        /**
         * <code>Stopped</code> indicates that a {@link CommandExecutor} is
         * now stopped and will no longer execute or accept requests
         * to execute {@link CommandExecutionRequest}s.
         */
        Stopped
    }


    /**
     * The amount of time we should delay performing some task
     * when ownership of the {@link Context} (and {@link CommandExecutor}) is in doubt
     * due to a partition being (potentially) moved.
     */
    private static long OWNERSHIP_DOUBT_DELAY_SECONDS = 2;

    /**
     * The {@link Identifier} of the {@link Context} for which this {@link CommandExecutor} is
     * coordinating {@link CommandExecutionRequest} execution.
     *
     * {@link Context} {@link Identifier}s are unique across a cluster.
     */
    private Identifier contextIdentifier;

    /**
     * The {@link ContextConfiguration} for the {@link Context}
     * being managed by this {@link CommandExecutor}.
     */
    private ContextConfiguration contextConfiguration;

    /**
     * The {@link BackingMapManagerContext} to which this {@link CommandExecutor}
     * is associated.  This is used to help locate the underlying {@link PartitionedService}
     * on which the {@link CommandExecutor} depends.
     */
    private BackingMapManagerContext backingMapManagerContext;

    /**
     * The current {@link State} of the {@link CommandExecutor}.
     */
    private volatile State state;

    /**
     * A {@link SequenceGenerator} that may be used to generate ticket issuer ids
     */
    private SequenceGenerator sequenceGenerator;

    /**
     * The current ticket issuer id that this {@link CommandExecutor}
     * is using for issuing {@link Ticket}s.  Over time this value may change
     * as it's not dependent on an individual instance of a {@link CommandExecutor}.
     */
    private long ticketIssuerId;

    /**
     * The ordered set of {@link TicketBook}s representing the {@link Ticket}s
     * of {@link CommandExecutionRequest}s that need to be (re)executed. ie:
     * {@link Ticket}s for {@link CommandExecutionRequest}s that have been
     * recovered.
     */
    private LinkedList<TicketBook> recoveredTicketBooks;

    /**
     * The in-order collection of {@link Ticket}s for {@link CommandExecutionRequest}s
     * that the {@link CommandExecutor} has accepted and are currently pending execution.
     */
    private TicketBook ticketBook;

    /**
     * The in-order collection of {@link Ticket}s for {@link CommandExecutionRequest}s
     * that are have high-priority for execution by the {@link CommandExecutor}.
     */
    private TicketBook priorityTicketBook;

    /**
     * The version of the {@link Context} (ie: {@link ContextWrapper}) this
     * {@link CommandExecutor} is managing.
     */
    private long contextVersion;

    /**
     * The cluster-wide uniquely generated JMX bean name for
     * the {@link CommandExecutor}.
     */
    private String mBeanName;

    /**
     * The local number of {@link CommandExecutionRequest}s that
     * have been submitted for execution by the {@link CommandExecutor}.
     */
    private long localCommandsSubmitted;

    /**
     * The local number of {@link CommandExecutionRequest}s that
     * have been executed by the {@link CommandExecutor}.
     */
    private long localCommandsExecuted;

    /**
     * The time (in milliseconds) the most recently locally executed
     * {@link CommandExecutionRequest} took to execute.
     */
    private long localLastCommandExecutionDuration;

    /**
     * The total local time taken (in milliseconds) for the {@link CommandExecutionRequest}s
     * executed thus far.
     */
    private long localCommandExecutionDuration;

    /**
     * The minimum time (in milliseconds) that a {@link CommandExecutionRequest}
     * has taken to execute (locally).
     */
    private long localMinimumCommandExecutionDuration;

    /**
     * The maximum time (in milliseconds) that a {@link CommandExecutionRequest}
     * has taken to execute (locally).
     */
    private long localMaximumCommandExecutionDuration;

    /**
     * The total time it takes to execute a command including the time to
     * retriever the command.
     */
    private long localCommandExecutionServiceDuration;

    /**
     * The total of {@link CommandExecutionRequest}s that have been executed.
     */
    private long totalCommandsExecuted;

    /**
     * The total time taken (in milliseconds) for the {@link CommandExecutionRequest}s
     * executed thus far.
     */
    private long totalCommandExecutionDuration;

    /**
     * The total time (in milliseconds) for the {@link CommandExecutionRequest}s
     * to commence execution.
     */
    private long totalCommandExecutionWaitingDuration;

    /**
     * The maximum number of {@link CommandExecutionRequest}s to execute in a single batch.
     */
    private int maximumBatchSize;


    /**
     * Internal Constructor.
     *
     * <b>NOTE:</b> To construct a {@link CommandExecutor} for your application,
     * you should use a {@link ContextsManager}.
     *
     * @param contextIdentifier
     * @param backingMapManagerContext
     */
    public CommandExecutor(Identifier               contextIdentifier,
                           BackingMapManagerContext backingMapManagerContext)
    {
        this.contextIdentifier        = contextIdentifier;
        this.contextConfiguration     = null;
        this.backingMapManagerContext = backingMapManagerContext;
        this.state                    = State.New;
        this.sequenceGenerator        = new ClusteredSequenceGenerator("ticketIssuerIds", 0);

        // we need two Ticket issuer ids.  The first is used to allocate Tickets to
        // CommandExecutionRequests that arrive *before* our Context (and ContextConfiguration)
        // has been established (or even created!).  These CommandExecutionRequests will
        // always be COLOCATED (as we would not yet have a ManagementStrategy from the
        // ContextConfiguration).  The second issuer id is used for all other
        // CommandExecutionRequests.  They will be located using the ManagementStrategy
        // provided by the ContextConfiguration.
        this.ticketIssuerId = sequenceGenerator.next(2).getFrom();
        this.ticketBook     = new TicketBook(ticketIssuerId);

        // tickets for priority commands have a negative issuer id
        this.priorityTicketBook   = new TicketBook(Long.MIN_VALUE + ticketIssuerId);

        this.recoveredTicketBooks = new LinkedList<TicketBook>();
        this.contextVersion       = -1;

        // JMX attributes
        this.mBeanName                            = null;
        this.localCommandsSubmitted               = 0;
        this.localCommandsExecuted                = 0;
        this.localLastCommandExecutionDuration    = 0;
        this.localCommandExecutionDuration        = 0;
        this.localCommandExecutionServiceDuration = 0;
        this.localMaximumCommandExecutionDuration = Long.MIN_VALUE;
        this.localMinimumCommandExecutionDuration = Long.MAX_VALUE;
        this.totalCommandsExecuted                = 0;
        this.totalCommandExecutionDuration        = 0;
        this.totalCommandExecutionWaitingDuration = 0;

        this.maximumBatchSize                     = 50;
    }


    /**
     * Returns the {@link Identifier} of the {@link Context} for which
     * this {@link CommandExecutor} is coordinating {@link CommandExecutionRequest} execution.
     *
     * {@link Context} {@link Identifier}s are unique across a cluster.
     */
    public Identifier getContextIdentifier()
    {
        return contextIdentifier;
    }


    /**
     * Sets the {@link State} of the {@link CommandExecutor}.
     *
     * @param state
     */
    public void setState(State state)
    {
        if (this.state == State.Stopped && state != State.Stopped)
        {
            Logger.log(Logger.ERROR,
                       "Attempting to set the CommandExecutor %s state to %s when it's already Stopped",
                       contextIdentifier,
                       this.state);
        }
        else
        {
            this.state = state;
        }
    }


    /**
     * Returns the current {@link State} of the {@link CommandExecutor}.
     */
    public State getState()
    {
        return state;
    }


    /**
     * Returns the cluster-wide uniquely generated JMX bean name for
     * the {@link CommandExecutor}.
     */
    public String getMBeanName()
    {
        return mBeanName;
    }


    /**
     * Sets the cluster-wide unique JMX bean name for
     * the {@link CommandExecutor}.
     *
     * @param mBeanName
     */
    void setMBeanName(String mBeanName)
    {
        this.mBeanName = mBeanName;
    }


    /**
     * {@inheritDoc}
     */
    public String getContextIdentity()
    {
        return contextIdentifier.toString();
    }


    /**
     * {@inheritDoc}
     */
    public long getContextVersion()
    {
        return contextVersion;
    }


    /**
     * {@inheritDoc}
     */
    public String getStatus()
    {
        return state.name();
    }


    /**
     * {@inheritDoc}
     */
    public long getTicketIssuerId()
    {
        return ticketIssuerId;
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalCommandsPendingExecution()
    {
        long totalPendingCommands = ticketBook.size();

        if (recoveredTicketBooks != null &&!recoveredTicketBooks.isEmpty())
        {
            for (TicketBook ticketBook : recoveredTicketBooks)
            {
                totalPendingCommands += ticketBook.size();
            }
        }

        return totalPendingCommands;
    }


    /**
     * {@inheritDoc}
     */
    public long getLocalCommandsSubmitted()
    {
        return localCommandsSubmitted;
    }


    /**
     * {@inheritDoc}
     */
    public long getLocalCommandsExecuted()
    {
        return localCommandsExecuted;
    }


    /**
     * {@inheritDoc}
     */
    public double getLocalAverageCommandExecutionDuration()
    {
        if (getLocalCommandsExecuted() == 0)
        {
            return 0.0;
        }
        else
        {
            return ((double) localCommandExecutionDuration) / ((double) getLocalCommandsExecuted());
        }
    }


    /**
     * {@inheritDoc}
     */
    public double getLocalLastCommandExecutionDuration()
    {
        return localLastCommandExecutionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public double getLocalCommandExecutionServiceDuration()
    {
        return localCommandExecutionServiceDuration;
    }


    /**
     * {@inheritDoc}
     */
    public double getLocalMinimumCommandExecutionDuration()
    {
        if (localMinimumCommandExecutionDuration == Long.MAX_VALUE)
        {
            return 0;
        }
        else
        {
            return localMinimumCommandExecutionDuration;
        }
    }


    /**
     * {@inheritDoc}
     */
    public double getLocalMaximumCommandExecutionDuration()
    {
        if (localMaximumCommandExecutionDuration == Long.MIN_VALUE)
        {
            return 0;
        }
        else
        {
            return localMaximumCommandExecutionDuration;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalCommandsExecuted()
    {
        return totalCommandsExecuted;
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalCommandExecutionDuration()
    {
        return totalCommandExecutionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalCommandExecutionWaitingDuration()
    {
        return totalCommandExecutionWaitingDuration;
    }


    /**
     * Returns if the {@link Context} is still owned by the Cluster {@link Member} (JVM)
     * in which the {@link Context} is running.
     * <p>
     * This method is used to help us discover if a partition is in the process of moving.
     */
    private boolean isContextOwned()
    {
        PartitionedService partitionedService = (PartitionedService) backingMapManagerContext.getCacheService();
        Member             keyOwner           = partitionedService.getKeyOwner(contextIdentifier);

        return CacheFactory.getCluster().getLocalMember().equals(keyOwner);
    }


    /**
     * Starts up, initializes, recovers and schedules the {@link CommandExecutor}
     * to start processing {@link CommandExecutionRequest}s.
     */
    @SuppressWarnings("unchecked")
    public void start()
    {
        if (getState() == State.New)
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG, "Starting CommandExecutor for %s", contextIdentifier);
            }

            // claim ownership of the Context
            NamedCache contextsCache = CacheFactory.getCache(ContextWrapper.CACHENAME);
            ContextWrapper contextWrapper = (ContextWrapper) contextsCache.invoke(contextIdentifier,
                                                                                  new ClaimContextProcessor(CacheFactory
                                                                                      .getCluster().getLocalMember()
                                                                                      .getUid()));

            if (contextWrapper == null)
            {
                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG,
                               "CommandExecutor for %s can not be started.  The Context has been moved (or deleted) since the CommandExecutor was established and scheduled to start.  Now stopped.",
                               contextIdentifier);
                }

                stop();

            }
            else
            {
                synchronized (this)
                {
                    // move onto the next ticket issuer id
                    // NOTE: we do this as any Tickets that have been issued prior to now
                    // will have been managed without ContextConfiguration).
                    //
                    // (this allows us to accept CommandExecutionRequests for this Context
                    // before it has been created/started)
                    this.ticketIssuerId++;
                    this.ticketBook                           = new TicketBook(ticketIssuerId);
                    this.priorityTicketBook                   = new TicketBook(Long.MIN_VALUE + ticketIssuerId);

                    this.contextConfiguration                 = contextWrapper.getContextConfiguration();
                    this.contextVersion                       = contextWrapper.getContextVersion();
                    this.totalCommandsExecuted                = contextWrapper.getTotalCommandsExecuted();
                    this.totalCommandExecutionDuration        = contextWrapper.getTotalCommandExecutionDuration();
                    this.totalCommandExecutionWaitingDuration =
                        contextWrapper.getTotalCommandExecutionWaitingDuration();
                }

                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG,
                               "CommandExecutor for %s has been configured as %s",
                               contextIdentifier,
                               contextConfiguration);
                }

                // recover any previously accepted CommandExecutionRequests (but have not been/completed execution)
                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG,
                               "Recovering unexecuted commands for CommandExecutor %s ",
                               contextIdentifier);
                }

                // create a filter that will locate all of the CommandExecutionRequests for this Context
                // that are in the cache.  We need to (re)execute these to recover the Context in this Member
                Filter filter = new EqualsFilter("getContextIdentifier", getContextIdentifier());

                // aggregate all of the DISTRIBUTED commands to recover
                TreeSet<TicketBook> recoveredTicketBookSet = new TreeSet<TicketBook>();

                recoveredTicketBookSet
                    .addAll((LinkedList<TicketBook>) CacheFactory
                        .getCache(CommandExecutionRequest.getCacheName(ManagementStrategy.DISTRIBUTED))
                        .aggregate(filter, new TicketAggregator("getTicket")));

                // aggregate all of the COLOCATED commands to recover
                recoveredTicketBookSet
                    .addAll((LinkedList<TicketBook>) CacheFactory
                        .getCache(CommandExecutionRequest.getCacheName(ManagementStrategy.COLOCATED))
                        .aggregate(new KeyAssociatedFilter(filter,
                                                           getContextIdentifier()), new TicketAggregator("getTicket")));

                // combine the commands to recover
                synchronized (this)
                {
                    recoveredTicketBooks.addAll(recoveredTicketBookSet);

                    if (recoveredTicketBooks.size() > 0)
                    {
                        if (Logger.isEnabled(Logger.DEBUG))
                        {
                            Logger.log(Logger.DEBUG,
                                       "Recovered %d Tickets for CommandExecutor %s. Books are %s",
                                       getTotalCommandsPendingExecution() - ticketBook.size(),
                                       contextIdentifier,
                                       recoveredTicketBooks);
                        }

                    }
                    else
                    {
                        if (Logger.isEnabled(Logger.DEBUG))
                        {
                            Logger.log(Logger.DEBUG,
                                       "No commands to recover for CommandExecutor %s",
                                       contextIdentifier);
                        }
                    }
                }

                // register MBean here (if required)
                Registry registry = CacheFactory.ensureCluster().getManagement();

                if (registry != null)
                {
                    if (Logger.isEnabled(Logger.DEBUG))
                    {
                        Logger.log(Logger.DEBUG,
                                   "Registering JMX management extensions for CommandExecutor %s",
                                   contextIdentifier);
                    }

                    setMBeanName(registry.ensureGlobalName(String.format("type=CommandExecutor,id=%s",
                                                                         contextIdentifier)));
                    registry.register(getMBeanName(), this);
                }

                // wait for commands to arrive or schedule execution?
                if (recoveredTicketBooks.isEmpty() && ticketBook.isEmpty())
                {
                    if (Logger.isEnabled(Logger.DEBUG))
                    {
                        Logger.log(Logger.DEBUG,
                                   "No commands to execute for CommandExecutor %s.  (waiting for commands to be submitted)",
                                   contextIdentifier);
                    }

                    setState(State.Waiting);
                }
                else
                {
                    if (Logger.isEnabled(Logger.DEBUG))
                    {
                        Logger.log(Logger.DEBUG,
                                   "Scheduling CommandExecutor %s to execute commands",
                                   contextIdentifier);
                    }

                    // schedule execution immediately
                    setState(State.Scheduled);
                    CommandExecutorManager.schedule(new Runnable()
                    {
                        public void run()
                        {
                            execute();
                        }
                    }, 0, TimeUnit.SECONDS);
                }

                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG, "Started CommandExecutor for %s", contextIdentifier);
                }
            }

        }
        else
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG,
                           "Attempted to start CommandExecutor for %s, but it's already Started (current state %s)",
                           contextIdentifier,
                           getState());
            }
        }
    }


    /**
     * Immediately stops the {@link CommandExecutor} from executing
     * any further {@link CommandExecutionRequest}s.
     */
    public void stop()
    {
        if (Logger.isEnabled(Logger.DEBUG))
        {
            Logger.log(Logger.DEBUG, "Stopping CommandExecutor for %s", contextIdentifier);
        }

        // stop immediately
        setState(State.Stopped);

        // this CommandExecutor must not be available any further to other threads
        CommandExecutorManager.removeCommandExecutor(this.getContextIdentifier());

        // unregister JMX mbean for the CommandExecutor
        Registry registry = CacheFactory.ensureCluster().getManagement();

        if (registry != null)
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG,
                           "Unregistering JMX management extensions for CommandExecutor %s",
                           contextIdentifier);
            }

            registry.unregister(getMBeanName());
        }

        if (Logger.isEnabled(Logger.DEBUG))
        {
            Logger.log(Logger.DEBUG, "Stopped CommandExecutor for %s", contextIdentifier);
        }
    }


    /**
     * Accepts the specified {@link CommandExecutionRequest} for execution by this {@link CommandExecutor}.
     * <p>
     * As part of this process, the {@link CommandExecutionRequest} is issued with a {@link Ticket}, has
     * the instant in time the {@link CommandExecutionRequest} was accepted (queued), places the
     * {@link CommandExecutionRequest} into the {@link CommandExecutionRequest}s cache and queues
     * the {@link CommandExecutionRequest} for execution.
     * <p>
     * If the Member in which this {@link CommandExecutor} is running no longer owns the {@link Context}
     * to which the {@link CommandExecutionRequest} has been submitted (or ownership is in doubt), a {@link Ticket#NONE}
     * is returned.
     *
     * @param commandExecutionRequest The {@link CommandExecutionRequest} to be accepted
     */
    @SuppressWarnings("unchecked")
    public synchronized SubmissionOutcome acceptCommandExecutionRequest(final CommandExecutionRequest commandExecutionRequest)
    {
        if (getState() == State.Stopped)
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG,
                           "CommandExecutor for %s can't accept %s as it has been Stopped",
                           contextIdentifier,
                           commandExecutionRequest);
            }

            return new SubmissionOutcome.UnknownContext();

        }
        else if (isContextOwned())
        {
            // generate a ticket for the CommandExecutionRequest
            // (this will be used to keep it in order, just in case we need to recover it later)
            Ticket ticket = (commandExecutionRequest.getCommand() instanceof PriorityCommand
                             ? priorityTicketBook : ticketBook).extend();

            commandExecutionRequest.setTicket(ticket);

            // we set the instant the CommandExecutionRequest was queued
            // so we can track the time it has waited (in the queue)
            // before it was executed (relative to the estimated cluster time)
            commandExecutionRequest.setInstantQueued(CacheFactory.getSafeTimeMillis());

            // determine how we are going to manage and store commands
            ManagementStrategy managementStrategy = (ticket.getIssuerId() % 2 == 0 || ticket.getIssuerId() < 0)
                                                    ? ManagementStrategy.COLOCATED
                                                    : contextConfiguration.getManagementStrategy();

            // store the CommandExecutionRequest in the cache so we can execute it later
            // (we dynamically create a key here based on the ManagementStrategy)
            CommandExecutionRequest.Key key = new CommandExecutionRequest.Key(contextIdentifier,
                                                                              ticket,
                                                                              managementStrategy);

            // determine the name of the cache in which to place the command (based on the strategy)
            String cacheName = CommandExecutionRequest.getCacheName(managementStrategy);

            // depending on the strategy we change how we "put" the entry into the cache
            if (managementStrategy == ManagementStrategy.COLOCATED)
            {
                // for COLOCATED strategies, we put the command directly into the backing map on this JVM
                DistributedCacheService distributedCacheService =
                    (DistributedCacheService) CacheFactory.getService("DistributedCacheForCommandPattern");
                DefaultConfigurableCacheFactory.Manager backingMapManager =
                    (DefaultConfigurableCacheFactory.Manager) distributedCacheService.getBackingMapManager();

                backingMapManager.getBackingMap(cacheName)
                    .put(backingMapManager.getContext().getKeyToInternalConverter().convert(key),
                         backingMapManager.getContext().getValueToInternalConverter().convert(commandExecutionRequest));

            }
            else
            {
                // for DISTRIBUTED strategies, we use the distributed cache
                NamedCache commandsCache = CacheFactory.getCache(cacheName);

                commandsCache.put(key, commandExecutionRequest);
            }

            // we've now accepted a command
            localCommandsSubmitted++;

            // schedule the CommandExecutor to process the CommandExecutionRequests
            if (getState() == State.Waiting)
            {
                // schedule execution immediately
                setState(State.Scheduled);
                CommandExecutorManager.schedule(new Runnable()
                {
                    public void run()
                    {
                        execute();
                    }
                }, 0, TimeUnit.SECONDS);
            }

            return new SubmissionOutcome.Accepted(key, managementStrategy);

        }
        else
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG,
                           "CommandExecutor for %s can't accept %s as Context ownership is currently in doubt",
                           contextIdentifier,
                           commandExecutionRequest);
            }

            // TODO: perhaps we need to return something here?  but this should never happen.
            // as the Context must have moved by Coherence.
            return new SubmissionOutcome.UnknownContext();
        }
    }


    /**
     * The method is used to asynchronously execute queued {@link CommandExecutionRequest}s.
     */
    @SuppressWarnings("unchecked")
    public void execute()
    {
        try
        {
            // only execute if we're scheduled to execute
            if (getState() == State.Scheduled)
            {
                // execute if and only if we're the key owner
                if (isContextOwned())
                {
                    // determine a TicketBook from which we'll use Tickets to locate CommandExecutionRequests to execute.
                    TicketBook       executionTicketBook = null;
                    Iterator<Ticket> ticketsToExecute    = null;

                    synchronized (this)
                    {
                        // attempt to use a recovered ticket book first
                        while (!recoveredTicketBooks.isEmpty() && executionTicketBook == null)
                        {
                            executionTicketBook = recoveredTicketBooks.getFirst();

                            if (executionTicketBook.isEmpty())
                            {
                                recoveredTicketBooks.removeFirst();
                                executionTicketBook = null;
                            }
                        }

                        // attempt to use the priority ticket book next
                        // (if we don't have a recovered ticket book or
                        // the tickets to recover aren't priority ones)
                        if (!priorityTicketBook.isEmpty())
                        {
                            if (executionTicketBook == null)
                            {
                                executionTicketBook = priorityTicketBook;
                            }
                            else
                            {
                                // we must execute priority commands before
                                // recovered non-priority commands
                                if (executionTicketBook.getIssuerId() >= 0)
                                {
                                    executionTicketBook = priorityTicketBook;
                                }
                            }
                        }

                        // lastly we use the regular ticket book
                        if (executionTicketBook == null)
                        {
                            executionTicketBook = ticketBook;
                        }

                        // execute if and only if we have tickets!
                        if (executionTicketBook.isEmpty())
                        {
                            // no Tickets to execute, so we have to wait until some arrive
                            setState(State.Waiting);

                        }
                        else
                        {
                            // we've got tickets to execute, so let's go into the Execution state
                            setState(State.Executing);

                            // get the batch of tickets we're going to execute
                            ticketsToExecute = executionTicketBook.first(maximumBatchSize);
                        }
                    }

                    // start the execution if and only if we are still in the execution state
                    if (getState() == State.Executing)
                    {
                        // get the ContextWrapper for the Context for which we're executing CommandExecutionRequests
                        NamedCache     contextWrapperCache = CacheFactory.getCache(ContextWrapper.CACHENAME);
                        ContextWrapper contextWrapper      =
                            (ContextWrapper) contextWrapperCache.get(contextIdentifier);

                        // remember the last number of commands we've executed
                        totalCommandsExecuted = contextWrapper.getTotalCommandsExecuted();

                        // ensure this CommandExecutor still owns the Context (by checking the version number)
                        if (contextWrapper.getContextVersion() == contextVersion)
                        {
                            // create an environment in which to execute CommandExecutionRequests
                            Environment environment = new Environment(contextWrapper);

                            // execute all of the tickets in our batch while this Member owns the Context and we remain in the execution state.
                            while (isContextOwned() && getState() == State.Executing && ticketsToExecute.hasNext())
                            {
                                // get the ticket of the next CommandExecutionRequest
                                Ticket ticket = ticketsToExecute.next();

                                // determine the cache that contains the CommandExecutionRequest
                                // NOTE: All odd tickets are COLOCATED as these are issued to CommandExecutionRequests
                                // before Contexts are provide.  All even tickets are managed according to the
                                // ContextConfiguration of the associated Context
                                ManagementStrategy managementStrategy =
                                    (ticket.getIssuerId() % 2 == 0 || ticket.getIssuerId() < 0)
                                    ? ManagementStrategy.COLOCATED : contextConfiguration.getManagementStrategy();

                                // determine the key of the next CommandExecutionRequest to execute
                                CommandExecutionRequest.Key commandExecutionRequestKey =
                                    new CommandExecutionRequest.Key(contextIdentifier,
                                                                    ticket,
                                                                    managementStrategy);

                                long startLocalServiceExecuteTime = CacheFactory.getSafeTimeMillis();

                                // get the next CommandExecutionRequest to execute
                                NamedCache commandExecutionRequestsCache =
                                    CacheFactory.getCache(CommandExecutionRequest.getCacheName(managementStrategy));
                                CommandExecutionRequest commandExecutionRequest =
                                    (CommandExecutionRequest) commandExecutionRequestsCache
                                        .invoke(commandExecutionRequestKey,
                                                new StartCommandProcessor());

                                // ensure that the CommandExecutionRequest exists
                                if (commandExecutionRequest == null)
                                {
                                    // SKIP: This is not so severe anymore as we'll only lose commands if they have been executed previously or deliberately removed

                                }
                                else if (commandExecutionRequest.isCanceled())
                                {
                                    if (Logger.isEnabled(Logger.QUIET))
                                    {
                                        Logger.log(Logger.QUIET,
                                                   "Skipping %s for CommandExecutor %s as it was canceled",
                                                   commandExecutionRequest,
                                                   contextIdentifier);
                                    }

                                }
                                else
                                {
                                    // configure the environment for the next command to execute
                                    // (we are recovering when the command execution request ticket is from a previous instance of a command executor)
                                    environment.configure(ticket,
                                                          managementStrategy,
                                                          ticket.getIssuerId() < getTicketIssuerId() - 1
                                                          && ticket.getIssuerId() + Long.MIN_VALUE
                                                             != getTicketIssuerId() - 1,
                                                          commandExecutionRequest.getCheckpoint());

                                    // try to execute the CommandExecutionRequest
                                    long startCommandExecutionTime = CacheFactory.getSafeTimeMillis();

                                    try
                                    {
                                        if (Logger.isEnabled(Logger.QUIET))
                                        {
                                            Logger.log(Logger.QUIET,
                                                       "Executing %s for CommandExecutor %s",
                                                       commandExecutionRequest,
                                                       contextIdentifier);
                                        }

                                        // execute the command
                                        commandExecutionRequest.getCommand().execute(environment);

                                    }
                                    catch (RuntimeException runtimeException)
                                    {
                                        Logger.log(Logger.ERROR,
                                                   "Failed to execute %s with CommandExecutor %s",
                                                   commandExecutionRequestKey,
                                                   contextIdentifier);
                                        CacheFactory.log(runtimeException);

                                        // FUTURE: set the state of the CommandExecutionRequest as being executed (but failed)

                                    }
                                    catch (Throwable throwable)
                                    {
                                        Logger.log(Logger.ERROR,
                                                   "Failed to execute %s with CommandExecutor %s",
                                                   commandExecutionRequestKey,
                                                   contextIdentifier);
                                        CacheFactory.log(throwable);

                                        // FUTURE: set the state of the CommandExecutionRequest as being executed (but failed)
                                    }
                                    finally
                                    {
                                        // update local JMX statistics
                                        long endCommandExecutionTime = CacheFactory.getSafeTimeMillis();

                                        localCommandsExecuted++;
                                        localLastCommandExecutionDuration = Math.max(0, endCommandExecutionTime
                                                                                     - startCommandExecutionTime);
                                        localCommandExecutionServiceDuration += endCommandExecutionTime
                                                                                - startLocalServiceExecuteTime;

                                        long localLastCommandExecutionWaitingDuration = Math.max(0,
                                                                                                 startCommandExecutionTime
                                                                                                 - commandExecutionRequest
                                                                                                     .getInstantQueued());

                                        localCommandExecutionDuration += localLastCommandExecutionDuration;
                                        localMaximumCommandExecutionDuration =
                                            Math.max(localMaximumCommandExecutionDuration,
                                                     localLastCommandExecutionDuration);
                                        localMinimumCommandExecutionDuration =
                                            Math.min(localMinimumCommandExecutionDuration,
                                                     localLastCommandExecutionDuration);

                                        totalCommandsExecuted++;
                                        totalCommandExecutionDuration        += localLastCommandExecutionDuration;
                                        totalCommandExecutionWaitingDuration +=
                                            localLastCommandExecutionWaitingDuration;

                                        // update the ContextWrapper with the Ticket we've just executed and the Context (if we changed it)
                                        // (if this is successful we know we've executed the Command successfully)
                                        if ((Boolean) contextWrapperCache
                                            .invoke(contextIdentifier, new UpdateContextProcessor(contextVersion,
                                                                                                  environment.isContextUpdated()
                                                                                                  ? environment.getContext()
                                                                                                  : null,
                                                                                                  ticket,
                                                                                                  localCommandExecutionDuration,
                                                                                                  localLastCommandExecutionWaitingDuration)))
                                        {
                                            // remove executed CommandExecutionRequest
                                            commandExecutionRequestsCache.remove(commandExecutionRequestKey);

                                        }
                                        else
                                        {
                                            if (Logger.isEnabled(Logger.DEBUG))
                                            {
                                                Logger.log(Logger.DEBUG,
                                                           "Failed to update ContextWrapper for %s as the CommandExecutor no longer owns the Context.  Detected that the context version number has changed during UpdateContextProcessor.",
                                                           contextIdentifier);
                                            }

                                            stop();
                                        }
                                    }    // try
                                }        // if - CommandExecutionRequest exists

                                // remove the ticket from the current ticket book
                                synchronized (this)
                                {
                                    executionTicketBook.consume(1);

                                    if (executionTicketBook.isEmpty())
                                    {
                                        if (Logger.isEnabled(Logger.QUIET))
                                        {
                                            Logger.log(Logger.QUIET,
                                                       "Completed %s for ContextWrapper for %s",
                                                       executionTicketBook,
                                                       contextIdentifier);
                                        }
                                    }
                                }
                            }    // while

                            // schedule the next execution
                            synchronized (this)
                            {
                                if (isContextOwned())
                                {
                                    if (getState() == State.Executing)
                                    {
                                        // do we need to reschedule execution or just wait as there's nothing to do?
                                        if (ticketBook.isEmpty() && recoveredTicketBooks.isEmpty()
                                            && priorityTicketBook.isEmpty())
                                        {
                                            setState(State.Waiting);

                                        }
                                        else
                                        {
                                            setState(State.Scheduled);
                                            CommandExecutorManager.schedule(new Runnable()
                                            {
                                                public void run()
                                                {
                                                    execute();
                                                }
                                            }, 0, TimeUnit.SECONDS);
                                        }
                                    }
                                    else
                                    {
                                        if (Logger.isEnabled(Logger.DEBUG))
                                        {
                                            Logger.log(Logger.DEBUG,
                                                       "CommandExecutor for %s has been Stopped during execution.",
                                                       contextIdentifier,
                                                       getState());
                                        }
                                    }

                                }
                                else
                                {
                                    if (Logger.isEnabled(Logger.DEBUG))
                                    {
                                        Logger.log(Logger.DEBUG,
                                                   "Ownership for CommandExecutor %s is currently in doubt (after execution).  Delaying execution for %d seconds",
                                                   contextIdentifier,
                                                   OWNERSHIP_DOUBT_DELAY_SECONDS);
                                    }

                                    setState(State.Scheduled);
                                    CommandExecutorManager.schedule(new Runnable()
                                    {
                                        public void run()
                                        {
                                            execute();
                                        }
                                    }, OWNERSHIP_DOUBT_DELAY_SECONDS, TimeUnit.SECONDS);

                                }
                            }

                        }
                        else
                        {
                            if (Logger.isEnabled(Logger.DEBUG))
                            {
                                Logger.log(Logger.DEBUG,
                                           "CommandExecutor for %s no longer owns the Context.  Detected that the context version number has changed.",
                                           contextIdentifier);
                            }

                            stop();
                        }

                    }
                    else
                    {
                        if (Logger.isEnabled(Logger.DEBUG))
                        {
                            Logger.log(Logger.DEBUG,
                                       "CommandExecutor for %s was scheduled to execute, but no longer needs to do so.  (current state is %s, %d pending commands)",
                                       contextIdentifier,
                                       getState(),
                                       getTotalCommandsPendingExecution());
                        }
                    }

                }
                else
                {
                    if (Logger.isEnabled(Logger.DEBUG))
                    {
                        Logger.log(Logger.DEBUG,
                                   "Ownership for CommandExecutor %s is currently in doubt.  Delaying execution for %d seconds",
                                   contextIdentifier,
                                   OWNERSHIP_DOUBT_DELAY_SECONDS);
                    }

                    synchronized (this)
                    {
                        setState(State.Scheduled);
                        CommandExecutorManager.schedule(new Runnable()
                        {
                            public void run()
                            {
                                execute();
                            }
                        }, OWNERSHIP_DOUBT_DELAY_SECONDS, TimeUnit.SECONDS);
                    }
                }

            }
            else
            {
                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG,
                               "CommandExecutor for %s was scheduled to execute, but it is no longer executable (current state is %s)",
                               contextIdentifier,
                               getState());
                }
            }

        }
        catch (RuntimeException runtimeException)
        {
            if (Logger.isEnabled(Logger.ERROR))
            {
                Logger.log(Logger.ERROR, "CommandExecutor for %s failed horribly...", contextIdentifier);
            }

            runtimeException.printStackTrace();

            setState(State.Waiting);
        }
        catch (Throwable throwable)
        {
            if (Logger.isEnabled(Logger.ERROR))
            {
                Logger.log(Logger.ERROR, "CommandExecutor for %s failed horribly...", contextIdentifier);
            }

            throwable.printStackTrace();

            setState(State.Waiting);
        }
    }


    /**
     * An internal implementation of an {@link ExecutionEnvironment}.
     */
    @SuppressWarnings("unchecked")
    static class Environment implements ExecutionEnvironment
    {
        private Identifier           contextIdentifier;
        private Context              context;
        private ContextConfiguration contextConfiguration;
        private ManagementStrategy   managementStrategy;
        private boolean              isContextUpdated;
        private boolean              isRecovering;
        private Ticket               ticket;
        private Object               checkpoint;


        /**
         * Constructs an {@link ExecutionEnvironment}
         *
         * @param contextWrapper
         */
        public Environment(ContextWrapper contextWrapper)
        {
            this.contextIdentifier    = contextWrapper.getContentIdentifier();
            this.context              = contextWrapper.getContext();
            this.contextConfiguration = contextWrapper.getContextConfiguration();

            this.managementStrategy   = ManagementStrategy.COLOCATED;
            this.isContextUpdated     = false;
            this.isRecovering         = false;
            this.ticket               = null;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Identifier getContextIdentifier()
        {
            return contextIdentifier;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Context getContext()
        {
            return context;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void setContext(Context context)
        {
            if (context == null)
            {
                throw new IllegalStateException(String
                    .format("Attempted to set the Context known as %s to null for Command with %s", contextIdentifier,
                            ticket));
            }
            else
            {
                this.context     = context;
                isContextUpdated = true;
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ContextConfiguration getContextConfiguration()
        {
            return contextConfiguration;
        }


        /**
         * Obtains the {@link ManagementStrategy} for the {@link ExecutionContext}.
         *
         * @return the {@link ManagementStrategy}
         */
        public ManagementStrategy getManagementStrategy()
        {
            return managementStrategy;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isRecovering()
        {
            return isRecovering;
        }


        /**
         * Configure the {@link ExecutionContext}.
         *
         * @param ticket
         * @param managementStrategy
         * @param isRecovering
         * @param checkpoint
         */
        public void configure(Ticket             ticket,
                              ManagementStrategy managementStrategy,
                              boolean            isRecovering,
                              Object             checkpoint)
        {
            this.isContextUpdated   = false;
            this.ticket             = ticket;
            this.managementStrategy = managementStrategy;
            this.isRecovering       = isRecovering;
            this.checkpoint         = checkpoint;
        }


        /**
         * Method description
         *
         * @return
         */
        public boolean isContextUpdated()
        {
            return isContextUpdated;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Ticket getTicket()
        {
            return ticket;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasCheckpoint()
        {
            return checkpoint != null;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Object loadCheckpoint()
        {
            return checkpoint;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void saveCheckpoint(Object checkpoint)
        {
            this.checkpoint = checkpoint;

            // immediately store the checkpoint in the command execution request
            CommandExecutionRequest.Key commandExecutionRequestKey = new CommandExecutionRequest.Key(contextIdentifier,
                                                                                                     ticket,
                                                                                                     managementStrategy);

            CacheFactory.getCache(CommandExecutionRequest.getCacheName(managementStrategy))
                .invoke(commandExecutionRequestKey,
                        new UpdaterProcessor("setCheckpoint",
                                             checkpoint));
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void removeCheckpoint()
        {
            saveCheckpoint(null);
        }
    }
}
