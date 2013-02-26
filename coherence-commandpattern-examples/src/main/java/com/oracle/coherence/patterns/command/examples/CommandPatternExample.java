/*
 * File: CommandPatternExample.java
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

package com.oracle.coherence.patterns.command.examples;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.command.CommandSubmitter;
import com.oracle.coherence.patterns.command.ContextConfiguration.ManagementStrategy;
import com.oracle.coherence.patterns.command.ContextsManager;
import com.oracle.coherence.patterns.command.DefaultCommandSubmitter;
import com.oracle.coherence.patterns.command.DefaultContextConfiguration;
import com.oracle.coherence.patterns.command.DefaultContextsManager;
import com.oracle.coherence.patterns.command.commands.PriorityCommandAdapter;
import com.tangosol.net.CacheFactory;

import java.io.IOException;

/**
 * The {@link CommandPatternExample} will create a {@link GenericContext} for a
 * Java long value after which it updates the value on that context 10000 times.
 * <p>
 * It will also demonstrate the use of priority commands with the logging
 * commands submitted to the system.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CommandPatternExample
{
    /**
     * The Main Entry Point for the {@link CommandPatternExample}.
     *
     * @param args  the arguments for the application
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws InterruptedException, IOException
    {
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                DefaultContextConfiguration contextConfiguration =
                    new DefaultContextConfiguration(ManagementStrategy.DISTRIBUTED);

                String          contextName     = "Context for " + Thread.currentThread().getName();

                ContextsManager contextsManager = DefaultContextsManager.getInstance();
                Identifier contextIdentifier = contextsManager.registerContext(contextName,
                                                                               new GenericContext<Long>(0L),
                                                                               contextConfiguration);

                CommandSubmitter commandSubmitter = DefaultCommandSubmitter.getInstance();

                commandSubmitter.submitCommand(contextIdentifier,
                                               new LoggingCommand(Thread.currentThread().getName() + " Commenced",
                                                                  Logger.DEBUG));

                for (long i = 1; i <= 10000; i++)
                {
                    commandSubmitter.submitCommand(contextIdentifier, new SetValueCommand<Long>(i));

                    // occasionally submit a priority logging command
                    if (i % 1000 == 0)
                    {
                        commandSubmitter.submitCommand(contextIdentifier,
                                                       new PriorityCommandAdapter<GenericContext<Long>>(new LoggingCommand(Thread
                                                           .currentThread().getName() + " Priority Logger @ " + i,
                                                                                                                           Logger
                                                                                                                           .DEBUG)));
                    }
                }

                commandSubmitter.submitCommand(contextIdentifier,
                                               new LoggingCommand(Thread.currentThread().getName() + " Completed",
                                                                  Logger.DEBUG));

                System.out.printf("DONE: %s\n", Thread.currentThread().getName());
            }
        };

        int      THREAD_COUNT = 10;
        Thread[] threads      = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++)
        {
            threads[i] = new Thread(runnable);

            threads[i].start();
        }

        for (int i = 0; i < THREAD_COUNT; i++)
        {
            threads[i].join();
        }

        CacheFactory.shutdown();
    }
}
