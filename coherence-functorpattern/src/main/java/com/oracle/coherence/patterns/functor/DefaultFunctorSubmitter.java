/*
 * File: DefaultFunctorSubmitter.java
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

package com.oracle.coherence.patterns.functor;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.identifiers.UUIDBasedIdentifier;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.CommandSubmitter;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.DefaultCommandSubmitter;
import com.oracle.coherence.patterns.functor.internal.FunctorFuture;
import com.oracle.coherence.patterns.functor.internal.FunctorResult;
import com.oracle.coherence.patterns.functor.internal.SingleFunctorCommand;
import com.oracle.coherence.patterns.functor.internal.SingleFunctorFuture;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * The default implementation of a {@link FunctorSubmitter}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @see FunctorSubmitter
 */
public class DefaultFunctorSubmitter implements CommandSubmitter, FunctorSubmitter
{
    /**
     * The default {@link FunctorSubmitter}.
     */
    private final static FunctorSubmitter INSTANCE = new DefaultFunctorSubmitter();

    /**
     * The unique {@link Identifier} for the {@link FunctorSubmitter}.
     * This is used to register listeners for the results of {@link Functor}
     * executions.
     */
    private Identifier identifier;

    /**
     * A map of {@link FunctorFuture}s for the {@link Functor}s
     * that were submitted by this {@link FunctorSubmitter} but are
     * yet to complete execution.
     */
    private ConcurrentHashMap<Identifier, FunctorFuture> functorFutures;

    /**
     * The {@link CommandSubmitter} that will be used to submit {@link Functor}s
     * for execution.
     */
    private CommandSubmitter commandSubmitter;


    /**
     * Standard Constructor (without an explicitly
     * defined {@link Identifier} and using a {@link DefaultCommandSubmitter}).
     */
    public DefaultFunctorSubmitter()
    {
        this(UUIDBasedIdentifier.newInstance());
    }


    /**
     * Standard Constructor (using a {@link DefaultCommandSubmitter}).
     */
    public DefaultFunctorSubmitter(Identifier identifier)
    {
        this(identifier, DefaultCommandSubmitter.getInstance());
    }


    /**
     * Standard Constructor (with an explicitly provided {@link Identifier}).
     *
     * @param identifier
     * @param commandSubmitter
     */
    public DefaultFunctorSubmitter(Identifier       identifier,
                                   CommandSubmitter commandSubmitter)
    {
        this.identifier       = identifier;
        this.functorFutures   = new ConcurrentHashMap<Identifier, FunctorFuture>();
        this.commandSubmitter = commandSubmitter;

        // register a map listener on the functor results cache to handle the delivery
        // of Results back to this FunctorSubmitter from the Coherence member
        // that executed the task.  We use the results cache as a mechanism to hand back
        // results to the FunctorSubmitter from the CommandExecutors
        NamedCache functorResultsCache = CacheFactory.getCache(FunctorResult.CACHENAME);

        functorResultsCache.addMapListener(new MultiplexingMapListener()
        {
            protected void onMapEvent(MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED || mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                {
                    // the FunctorResult is delivered to us as the mapEvent new value
                    FunctorResult functorResult = (FunctorResult) mapEvent.getNewValue();

                    // find the FunctorFuture for the FunctorResult just delivered (using the functorResultIdentifier)
                    FunctorFuture functorFuture = functorFutures.get(mapEvent.getKey());

                    // have the FunctorFuture accept the provided result
                    if (functorFuture != null)
                    {
                        functorFuture.acceptFunctorResult(functorResult);
                    }

                    // remove the FunctorResult and FunctorFuture iff the result is complete
                    if (functorResult.isComplete())
                    {
                        mapEvent.getMap().remove(mapEvent.getKey());
                        functorFutures.remove(mapEvent.getKey());
                    }
                }
            }
        }, new MapEventFilter(new EqualsFilter("getFunctorSubmitterIdentifier", identifier)), false);
    }


    /**
     * Returns the {@link Identifier} of the {@link FunctorSubmitter}.
     */
    public Identifier getIdentifier()
    {
        return identifier;
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context> Identifier submitCommand(Identifier contextIdentifier,
                                                        Command<C> command)
    {
        return commandSubmitter.submitCommand(contextIdentifier, command);
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context> Identifier submitCommand(Identifier contextIdentifier,
                                                        Command<C> command,
                                                        boolean    allowSubmissionWhenContextDoesNotExist)
    {
        return commandSubmitter.submitCommand(contextIdentifier, command, allowSubmissionWhenContextDoesNotExist);
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context, T> Future<T> submitFunctor(Identifier    contextIdentifier,
                                                          Functor<C, T> functor)
    {
        return submitFunctor(contextIdentifier, functor, false);
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context, T> Future<T> submitFunctor(Identifier    contextIdentifier,
                                                          Functor<C, T> functor,
                                                          boolean       allowSubmissionWhenContextDoesNotExist)
    {
        // create an identifier for the functor result
        Identifier functorResultIdentifier = UUIDBasedIdentifier.newInstance();

        // create a future representing the result of the functor to be executed.
        SingleFunctorFuture<T> future = new SingleFunctorFuture<T>(identifier, functorResultIdentifier, this);

        // register the pending future for this functor execution
        functorFutures.put(functorResultIdentifier, future);

        // submit the functor (as a command) for execution
        Identifier commandIdentifier = submitCommand(contextIdentifier,
                                                     new SingleFunctorCommand<C, T>(identifier,
                                                                                    functorResultIdentifier,
                                                                                    functor),
                                                     allowSubmissionWhenContextDoesNotExist);

        future.setFunctorIdentifier(commandIdentifier);

        return future;
    }


    /**
     * {@inheritDoc}
     */
    public <C extends Context> boolean cancelCommand(Identifier commandIdentifier)
    {
        return commandSubmitter.cancelCommand(commandIdentifier);
    }


    /**
     * Returns an instance of the {@link DefaultFunctorSubmitter}.
     */
    public static FunctorSubmitter getInstance()
    {
        return INSTANCE;
    }
}
