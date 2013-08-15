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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;

import com.oracle.coherence.patterns.command.Context;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.ResourceRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link CommandExecutorManager} is responsible for managing and scheduling
 * the current {@link CommandExecutor}s for a Coherence Cluster Member.
 * <p>
 * Copyright (c) 2008-2012. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class CommandExecutorManager
{
    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(CommandExecutorManager.class.getName());

    /**
     * The map of {@link Context} {@link ContextIdentifier}s to associated {@link CommandExecutor}s
     * that the {@link CommandExecutorManager} is managing.
     */
    private ConcurrentHashMap<Identifier, CommandExecutor> m_mapCommandExecutors;

    /**
     * An {@link ScheduledExecutorService} that we can use to perform asynchronous tasks, like
     * managing {@link CommandExecutor}s.
     */
    private ScheduledExecutorService m_executorService;

    /**
     * A {@link ThreadGroup} for the {@link ExecutorService} so that {@link Thread}s
     * are not allocated within Coherence owned {@link Thread}s.
     */
    private ThreadGroup m_executorServiceThreadGroup;


    /**
     * Constructs a {@link CommandExecutorManager}.
     */
    private CommandExecutorManager()
    {
        m_mapCommandExecutors        = new ConcurrentHashMap<Identifier, CommandExecutor>();
        m_executorServiceThreadGroup = new ThreadGroup("CommandExecutorManager");
        m_executorService = ExecutorServiceFactory.newScheduledThreadPool(5, ThreadFactories.newThreadFactory(true,
            "CommandExecutor", m_executorServiceThreadGroup));
    }


    /**
     * Ensures that the {@link CommandExecutorManager} has been created and
     * exists in the {@link ResourceRegistry}.
     *
     * @return the registered {@link CommandExecutorManager}
     */
    public static CommandExecutorManager ensure()
    {
        ResourceRegistry registry =
            ((ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory()).getResourceRegistry();

        CommandExecutorManager manager = registry.getResource(CommandExecutorManager.class);

        if (manager == null)
        {
            synchronized (registry)
            {
                manager = registry.getResource(CommandExecutorManager.class);

                if (manager == null)
                {
                    manager = new CommandExecutorManager();
                    registry.registerResource(CommandExecutorManager.class, manager);
                }
            }
        }

        return manager;
    }


    /**
     * Returns the {@link CommandExecutor} for the {@link Context} identified by the provided {@link Identifier}.
     * If a {@link CommandExecutor} does not yet exist, one is created and associated with the specified
     * {@link BackingMapManagerContext}.
     *
     * @param contextIdentifier The {@link Identifier} of the {@link Context} for which we
     *                          are requesting a {@link CommandExecutor}.
     * @param svcPartitioned    the {@link PartitionedService} that owns the {@link CommandExecutor}
     *
     * @return {@link CommandExecutor}
     */
    public CommandExecutor ensureCommandExecutor(Identifier contextIdentifier,
                                                 PartitionedService svcPartitioned)
    {
        CommandExecutor commandExecutor = m_mapCommandExecutors.get(contextIdentifier);

        if (commandExecutor == null)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, String.format("Creating CommandExecutor for %s", contextIdentifier));
            }

            // create and register the new CommandExecutor
            commandExecutor = new CommandExecutor(contextIdentifier, svcPartitioned, m_executorService);

            CommandExecutor previouslyRegisteredCommandExecutor = m_mapCommandExecutors.putIfAbsent(contextIdentifier,
                                                                                                    commandExecutor);

            if (previouslyRegisteredCommandExecutor == null)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, String.format("Created CommandExecutor for %s", contextIdentifier));
                }
            }
            else
            {
                // use the previously created CommandExecutor
                commandExecutor = previouslyRegisteredCommandExecutor;

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               String.format("Using previously created CommandExecutor for %s", contextIdentifier));
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
    public CommandExecutor getCommandExecutor(Identifier contextIdentifier)
    {
        CommandExecutor commandExecutor = m_mapCommandExecutors.get(contextIdentifier);

        return commandExecutor;
    }


    /**
     * Removes the {@link CommandExecutor} for the {@link Context} with the specified {@link Identifier}.
     *
     * @param contextIdentifier
     */
    public CommandExecutor removeCommandExecutor(Identifier contextIdentifier)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, String.format("Removing CommandExecutor for %s", contextIdentifier));
        }

        return m_mapCommandExecutors.remove(contextIdentifier);
    }


    /**
     * Schedules the provided {@link Runnable} to be executed at a specified time in the future.
     *
     * @param runnable The {@link Runnable} to execute
     * @param delay The minimum delay from now until the execution should commence
     * @param timeUnit The {@link TimeUnit} for the specified delay
     */
    public void schedule(Runnable runnable,
                         long delay,
                         TimeUnit timeUnit)
    {
        if (delay == 0)
        {
            m_executorService.execute(runnable);
        }
        else
        {
            m_executorService.schedule(runnable, delay, timeUnit);
        }
    }
}
