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
import com.oracle.coherence.patterns.command.Context;
import com.tangosol.net.BackingMapManagerContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
     * The map of {@link Context} {@link ContextIdentifier}s to associated {@link CommandExecutor}s
     * that the {@link CommandExecutorManager} is managing.
     */
    private static ConcurrentHashMap<Identifier, CommandExecutor> commandExecutors;

    /**
     * An {@link ScheduledExecutorService} that we can use to perform asynchronous tasks, like
     * managing {@link CommandExecutor}s.
     */
    private static ScheduledExecutorService executorService;

    /**
     * A {@link ThreadGroup} for the {@link ExecutorService} so that {@link Thread}s
     * are not allocated within Coherence owned {@link Thread}s.
     */
    private static ThreadGroup executorServiceThreadGroup;


    /**
     * Returns the {@link CommandExecutor} for the {@link Context} identified by the provided {@link Identifier}.
     * If a {@link CommandExecutor} does not yet exist, one is created and associated with the specified
     * {@link BackingMapManagerContext}.
     *
     * @param contextIdentifier The {@link Identifier} of the {@link Context} for which we
     *                          are requesting a {@link CommandExecutor}.
     * @param backingMapManagerContext The {@link BackingMapManagerContext} to which the {@link CommandExecutor}
     *                                 is associated
     *
     * @return {@link CommandExecutor}
     */
    public static CommandExecutor ensureCommandExecutor(Identifier               contextIdentifier,
                                                        BackingMapManagerContext backingMapManagerContext)
    {
        CommandExecutor commandExecutor = commandExecutors.get(contextIdentifier);

        if (commandExecutor == null)
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG, "Creating CommandExecutor for %s", contextIdentifier);
            }

            // create and register the new CommandExecutor
            commandExecutor = new CommandExecutor(contextIdentifier, backingMapManagerContext);

            CommandExecutor previouslyRegisteredCommandExecutor = commandExecutors.putIfAbsent(contextIdentifier,
                                                                                               commandExecutor);

            if (previouslyRegisteredCommandExecutor == null)
            {
                if (Logger.isEnabled(Logger.DEBUG))
                {
                    Logger.log(Logger.DEBUG, "Created CommandExecutor for %s", contextIdentifier);
                }
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


    /**
     * Schedules the provided {@link Runnable} to be executed at a specified time in the future.
     *
     * @param runnable The {@link Runnable} to execute
     * @param delay The minimum delay from now until the execution should commence
     * @param timeUnit The {@link TimeUnit} for the specified delay
     */
    public static void schedule(Runnable runnable,
                                long     delay,
                                TimeUnit timeUnit)
    {
        if (delay == 0)
        {
            executorService.execute(runnable);
        }
        else
        {
            executorService.schedule(runnable, delay, timeUnit);
        }
    }


    /**
     * Static initialization.
     */
    static
    {
        // TODO: get the size of the thread pool for our executorService from the system properties (or cache config?)

        commandExecutors           = new ConcurrentHashMap<Identifier, CommandExecutor>();
        executorServiceThreadGroup = new ThreadGroup("CommandExecutorManager");
        executorService =
            ExecutorServiceFactory
                .newScheduledThreadPool(5, ThreadFactories
                    .newThreadFactory(true, "CommandExecutor", executorServiceThreadGroup));
    }
}
