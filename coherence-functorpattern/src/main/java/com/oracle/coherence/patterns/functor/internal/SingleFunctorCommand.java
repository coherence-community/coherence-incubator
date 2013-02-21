/*
 * File: SingleFunctorCommand.java
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

package com.oracle.coherence.patterns.functor.internal;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.oracle.coherence.patterns.functor.Functor;
import com.oracle.coherence.patterns.functor.FunctorSubmitter;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SingleFunctorCommand} is a {@link Command} that asynchronously
 * executes a single {@link Functor} and returns the result.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @param <C>
 * @param <T>
 */
@SuppressWarnings("serial")
public class SingleFunctorCommand<C extends Context, T> implements Command<C>, ExternalizableLite, PortableObject
{
    /**
     * The {@link Identifier} of the {@link FunctorSubmitter} that submitted
     * the {@link Functor} for execution.
     */
    private Identifier functorSubmitterIdentifier;

    /**
     * The {@link Identifier} that will be used to locate the result
     * of the {@link Functor} execution in the functor-results cache.
     */
    private Identifier functorResultIdentifier;

    /**
     * The {@link Functor} that will be executed when this {@link Command} is executed.
     */
    private Functor<C, T> functor;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public SingleFunctorCommand()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param functorSubmitterIdentifier
     * @param functorResultIdentifier
     * @param functor
     */
    public SingleFunctorCommand(Identifier    functorSubmitterIdentifier,
                                Identifier    functorResultIdentifier,
                                Functor<C, T> functor)
    {
        this.functorSubmitterIdentifier = functorSubmitterIdentifier;
        this.functorResultIdentifier    = functorResultIdentifier;
        this.functor                    = functor;
    }


    /**
     * {@inheritDoc}
     */
    public void execute(ExecutionEnvironment<C> executionEnvironment)
    {
        SingleFunctorResult<T> result;

        try
        {
            T value = functor.execute(executionEnvironment);

            result = new SingleFunctorResult<T>(functorSubmitterIdentifier, functorResultIdentifier, value, null);

        }
        catch (Exception exception)
        {
            Logger.log(Logger.WARN,
                       "Failed to execute Functor %s with Ticket %s in Context %s as it raised the following exception\n%s\n",
                       functor,
                       executionEnvironment.getTicket(),
                       executionEnvironment.getContext(),
                       exception);

            result = new SingleFunctorResult<T>(functorSubmitterIdentifier, functorResultIdentifier, null, exception);
        }

        // place the result in the results cache (if it's not present)
        NamedCache functorResultsCache = CacheFactory.getCache(FunctorResult.CACHENAME);

        functorResultsCache.invoke(functorResultIdentifier,
                                   new ConditionalPut(new NotFilter(PresentFilter.INSTANCE), result));
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput input) throws IOException
    {
        this.functorSubmitterIdentifier = (Identifier) ExternalizableHelper.readObject(input);
        this.functorResultIdentifier    = (Identifier) ExternalizableHelper.readObject(input);
        this.functor                    = (Functor<C, T>) ExternalizableHelper.readObject(input);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput output) throws IOException
    {
        ExternalizableHelper.writeObject(output, functorSubmitterIdentifier);
        ExternalizableHelper.writeObject(output, functorResultIdentifier);
        ExternalizableHelper.writeObject(output, functor);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.functorSubmitterIdentifier = (Identifier) reader.readObject(0);
        this.functorResultIdentifier    = (Identifier) reader.readObject(1);
        this.functor                    = (Functor<C, T>) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, functorSubmitterIdentifier);
        writer.writeObject(1, functorResultIdentifier);
        writer.writeObject(2, functor);
    }
}
