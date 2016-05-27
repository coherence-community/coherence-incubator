/*
 * File: DefaultCommandSubmitter.java
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

package com.oracle.coherence.patterns.command;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.command.ContextConfiguration.ManagementStrategy;
import com.oracle.coherence.patterns.command.internal.CancelCommandProcessor;
import com.oracle.coherence.patterns.command.internal.CommandExecutionRequest;
import com.oracle.coherence.patterns.command.internal.ContextWrapper;
import com.oracle.coherence.patterns.command.internal.SubmissionOutcome;
import com.oracle.coherence.patterns.command.internal.SubmitCommandExecutionRequestProcessor;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.extractor.ReflectionExtractor;

/**
 * The default implementation of a {@link CommandSubmitter}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see CommandSubmitter
 *
 * @author Brian Oliver
 */
public class DefaultCommandSubmitter implements CommandSubmitter
{
    /**
     * The default {@link CommandSubmitter}.
     */
    private final static CommandSubmitter INSTANCE = new DefaultCommandSubmitter();


    /**
     * Standard Constructor
     */
    public DefaultCommandSubmitter()
    {
        // ensure with have appropriate indexes on the CommandExecutionRequests caches
        CacheFactory.getCache(CommandExecutionRequest.getCacheName(ManagementStrategy.DISTRIBUTED))
            .addIndex(new ReflectionExtractor("getContextIdentifier"),
                      false,
                      null);

        CacheFactory.getCache(CommandExecutionRequest.getCacheName(ManagementStrategy.COLOCATED))
            .addIndex(new ReflectionExtractor("getContextIdentifier"),
                      false,
                      null);
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context> Identifier submitCommand(Identifier contextIdentifier,
                                                        Command<C> command,
                                                        boolean    acceptCommandIfContextDoesNotExist)
    {
        // create a CommandExecutionRequest to wrap the provided Command
        // (we do this as we need to track internal state around a Command)
        CommandExecutionRequest commandExecutionRequest = new CommandExecutionRequest(contextIdentifier, command);

        // submit the command to the context
        NamedCache contextsCache = CacheFactory.getCache(ContextWrapper.CACHENAME);
        SubmissionOutcome submissionOutcome = (SubmissionOutcome) contextsCache.invoke(contextIdentifier,
                                                                                       new SubmitCommandExecutionRequestProcessor(commandExecutionRequest,
            acceptCommandIfContextDoesNotExist));

        // handle the outcome of the submission
        if (submissionOutcome instanceof SubmissionOutcome.UnknownContext)
        {
            throw new IllegalArgumentException(String
                .format("Can't submit Command %s to Context %s as the Context does not exist", command,
                        contextIdentifier));

        }
        else
        {
            return ((SubmissionOutcome.Accepted) submissionOutcome);
        }
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context> Identifier submitCommand(Identifier contextIdentifier,
                                                        Command<C> command)
    {
        return submitCommand(contextIdentifier, command, false);
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context> boolean cancelCommand(Identifier commandIdentifier)
    {
        SubmissionOutcome.Accepted submissionOutcome = (SubmissionOutcome.Accepted) commandIdentifier;

        NamedCache commandsCache =
            CacheFactory.getCache(CommandExecutionRequest.getCacheName(submissionOutcome.getManagementStrategy()));

        Boolean result = (Boolean) commandsCache.invoke(submissionOutcome.getCommandExecutionRequestKey(),
                                                        new CancelCommandProcessor());

        return result.booleanValue();
    }


    /**
     * Returns an instance of the {@link DefaultCommandSubmitter}.
     */
    public static CommandSubmitter getInstance()
    {
        return INSTANCE;
    }
}
