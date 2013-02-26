/*
 * File: SingleFunctorFuture.java
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
import com.oracle.coherence.patterns.functor.Functor;
import com.oracle.coherence.patterns.functor.FunctorSubmitter;
import com.tangosol.net.CacheFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The implementation of the {@link FunctorFuture} and {@link Future} that will represent
 * the result returned from an asynchronously executed {@link Functor}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @param <T>
 */
public class SingleFunctorFuture<T> implements FunctorFuture, Future<T>
{
    /**
     * The {@link Identifier} of the {@link FunctorSubmitter} that
     * produced this {@link FunctorFuture}.
     */
    private Identifier functorSubmitterIdentifier;

    /**
     * The {@link Identifier} that will be used to identify the result
     * of the {@link Functor} execution.
     */
    private Identifier functorResultIdentifier;

    /**
     * The {@link FunctorResult} from executing the {@link Functor}.
     */
    private SingleFunctorResult<T> functorResult;

    /**
     * The {@link FunctorSubmitter} that submitted the {@link Functor} for this future.
     */
    private FunctorSubmitter functorSubmitter;

    /**
     * The {@link Identifier} associated with the submitted {@link Functor} for this future.
     */
    private Identifier functorIdentifier;

    /**
     * Whether this FutureResult has been canceled or not.
     */
    private boolean isCanceled;


    /**
     * Standard Constructor.
     *
     * @param functorSubmitterIdentifier
     * @param functorResultIdentifier
     * @param functorSubmitter
     */
    public SingleFunctorFuture(Identifier       functorSubmitterIdentifier,
                               Identifier       functorResultIdentifier,
                               FunctorSubmitter functorSubmitter)
    {
        this.functorSubmitterIdentifier = functorSubmitterIdentifier;
        this.functorResultIdentifier    = functorResultIdentifier;
        this.functorSubmitter           = functorSubmitter;
        this.functorResult              = null;
        this.isCanceled                 = false;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getFunctorSubmitterIdentifier()
    {
        return functorSubmitterIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getFunctorResultIdentifier()
    {
        return functorResultIdentifier;
    }


    /**
     * Return the {@link Identifier} associated with the submitted {@link Functor} for this future.
     * @return the {@link Identifier} associated with the submitted {@link Functor} for this future
     */
    public Identifier getFunctorIdentifier()
    {
        return functorIdentifier;
    }


    /**
     * Set the {@link Identifier} associated with the submitted {@link Functor} for this future.
     * @param identifier the {@link Identifier} associated with the submitted {@link Functor} for this future
     */
    public void setFunctorIdentifier(Identifier identifier)
    {
        this.functorIdentifier = identifier;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void acceptFunctorResult(FunctorResult functorResult)
    {
        synchronized (this)
        {
            this.functorResult = (SingleFunctorResult<T>) functorResult;
            notify();
        }
    }


    /**
     * Attempts to cancel execution of the submitted task. This attempt will fail if the task has already completed,
     * already been canceled, or could not be canceled for some other reason. If successful, and this task has not
     * started when cancel is called, this task will never run. Attempts to interrupt an already running Functor is
     * not currently supported and will have no effect.
     *
     * @param mayInterruptIfRunning  if true, the cancel operation should attempt to stop the running Functor. This
     *                               functionality is not currently supported and will have no effect.
     *
     * @return false if the task could not be canceled, typically because it has already completed normally;
     *         true otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (isCanceled)
        {
            return true;
        }
        else
        {
            synchronized (this)
            {
                if (functorResult != null && (functorResult.getException() != null || functorResult.getValue() != null))
                {
                    return false;
                }
            }

            isCanceled = functorSubmitter.cancelCommand(functorIdentifier);

            return isCanceled;
        }
    }


    /**
     * {@inheritDoc}
     */
    public T get() throws InterruptedException, ExecutionException
    {
        if (isCanceled)
        {
            throw new CancellationException();
        }

        synchronized (this)
        {
            if (!isDone())
            {
                wait();
            }
        }

        // if the result has failed, re-throw the exception
        if (functorResult.getException() != null)
        {
            throw new ExecutionException("Execution Failed", functorResult.getException());
        }
        else
        {
            return functorResult.getValue();
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T get(long     time,
                 TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
    {
        if (isCanceled)
        {
            throw new CancellationException();
        }

        synchronized (this)
        {
            if (!isDone())
            {
                timeUnit.timedWait(this, time);

                // FUTURE: We could switch to a polling model here it the "time" is really large (> 1 sec?)
            }

            if (functorResult == null)
            {
                // just in case the map event may not have arrived as yet, see if the result is in the cache for us
                functorResult =
                    (SingleFunctorResult<T>) CacheFactory.getCache(SingleFunctorResult.CACHENAME)
                        .get(functorResultIdentifier);
            }

            if (functorResult == null)
            {
                throw new TimeoutException();
            }
            else
            {
                // if the result has failed, re-throw the exception
                if (functorResult.getException() != null)
                {
                    throw new ExecutionException("Execution Failed", functorResult.getException());
                }
                else
                {
                    return functorResult.getValue();
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isCancelled()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDone()
    {
        return functorResult != null;
    }
}
