/*
 * File: CommandExecutorManager.java
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
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.common.util.Primes;
import com.oracle.coherence.patterns.command.Context;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.PartitionedService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An {@link CommandExecutorManager} is responsible for managing and scheduling
 * the current {@link CommandExecutor}s for a Coherence Cluster Member.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class CommandExecutorManager
{
    /**
     * The number of {@link ScheduledExecutorService}s we'll create for {@link CommandExecutor}s.
     *
     * <p>THIS MUST BE A PRIME NUMBER</p>
     */
    private static int EXECUTOR_SERVICES = 3;

    /**
     * The map of {@link Context} {@link Identifier}s to associated {@link CommandExecutor}s
     * that the {@link CommandExecutorManager} is managing.
     */
    private static ConcurrentHashMap<Identifier, CommandExecutor> commandExecutors;

    /**
     * The {@link ScheduledExecutorService}s that we can use to perform asynchronous tasks, like
     * managing {@link CommandExecutor}s.
     */
    private static ScheduledExecutorService[] executorServices;

    /**
     * A {@link ThreadGroup} for the {@link ExecutorService} so that {@link Thread}s
     * are not allocated within Coherence owned {@link Thread}s.
     */
    private static ThreadGroup executorServiceThreadGroup;


    /**
     * Static initialization.
     */
    static
    {
        // determine the number of executor service (threads) to allocate
        try
        {
            EXECUTOR_SERVICES =
                Primes.closestPrimeTo(Integer.parseInt(System.getProperty("coherence.commandpattern.suggestedthreads",
                                                                          "3")));
        }
        catch (NumberFormatException e)
        {
            EXECUTOR_SERVICES = 3;
        }

        commandExecutors           = new ConcurrentHashMap<Identifier, CommandExecutor>();
        executorServiceThreadGroup = new ThreadGroup("CommandExecutorManager");

        // establish the ScheduledExecutorServices we'll allocate to CommandExecutors
        executorServices = new ScheduledExecutorService[EXECUTOR_SERVICES];

        for (int i = 0; i < EXECUTOR_SERVICES; i++)
        {
            executorServices[i] =
                ExecutorServiceFactory.newSingleThreadScheduledExecutor(ThreadFactories.newThreadFactory(true,
                                                                                                         "CommandExecutor",
                                                                                                         executorServiceThreadGroup));

        }
    }


    /**
     * Returns the {@link CommandExecutor} for the {@link Context} identified by the provided {@link Identifier}.
     * If a {@link CommandExecutor} does not yet exist, one is created and associated with the specified
     * {@link BackingMapManagerContext}.
     *
     * @param contextIdentifier   The {@link Identifier} of the {@link Context} for which we
     *                            are requesting a {@link CommandExecutor}.
     * @param partitionedService  the {@link PartitionedService} on which the {@link CommandExecutor}
     *                            is operating
     *
     * @return {@link CommandExecutor}
     */
    public static CommandExecutor ensureCommandExecutor(Identifier         contextIdentifier,
                                                        PartitionedService partitionedService)
    {
        CommandExecutor commandExecutor = commandExecutors.get(contextIdentifier);

        if (commandExecutor == null)
        {
            // determine which executor service the CommandExecutor should use
            int executorServiceId = contextIdentifier.hashCode() % EXECUTOR_SERVICES;

            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG,
                           "Creating CommandExecutor for %s (using ExecutorService %d)",
                           contextIdentifier,
                           executorServiceId);
            }

            // create and register the new CommandExecutor
            commandExecutor = new CommandExecutor(contextIdentifier,
                                                  partitionedService,
                                                  executorServices[executorServiceId]);

            CommandExecutor previouslyRegisteredCommandExecutor = commandExecutors.putIfAbsent(contextIdentifier,
                                                                                               commandExecutor);

            if (previouslyRegisteredCommandExecutor == null)
            {
                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG, "Created CommandExecutor for %s", contextIdentifier);
                }

                // ensure the command executor is started
                commandExecutor.start();
            }
            else
            {
                // use the previously created CommandExecutor
                commandExecutor = previouslyRegisteredCommandExecutor;

                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG, "Using previously created CommandExecutor for %s", contextIdentifier);
                }
            }
        }

        return commandExecutor;
    }


    /**
     * Returns the {@link CommandExecutor} for the {@link Context} identified by the provided {@link Identifier}.
     *
     * @param contextIdentifier The {@link Identifier} of the {@link Context} for which we are requesting a {@link CommandExecutor}.
     *
     * @return <code>null</code> if no such {@link CommandExecutor} exists with the specified {@link Identifier}.
     */
    public static CommandExecutor getCommandExecutor(Identifier contextIdentifier)
    {
        CommandExecutor commandExecutor = commandExecutors.get(contextIdentifier);

        return commandExecutor;
    }


    /**
     * Removes the {@link CommandExecutor} for the {@link Context} with the specified {@link Identifier}.
     *
     * @param contextIdentifier
     */
    public static CommandExecutor removeCommandExecutor(Identifier contextIdentifier)
    {
        if (Logger.isEnabled(Logger.DEBUG))
        {
            Logger.log(Logger.DEBUG, "Removing CommandExecutor for %s", contextIdentifier);
        }

        return commandExecutors.remove(contextIdentifier);
    }
}
