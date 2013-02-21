/*
 * File: DefaultSubmissionOutcome.java
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

package com.oracle.coherence.patterns.processing.internal;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.SubmissionOutcome;
import com.oracle.coherence.patterns.processing.SubmissionOutcomeListener;
import com.oracle.coherence.patterns.processing.SubmissionRetentionPolicy;
import com.oracle.coherence.patterns.processing.SubmissionState;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of the {@link SubmissionOutcome} interface.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Christer Fahlgren
 */
public class DefaultSubmissionOutcome implements SubmissionOutcome
{
    /**
     * The time it took to execute the submission.
     */
    private long executionDuration;

    /**
     * The time it took until it started executing from submission.
     */
    private long latency;

    /**
     * The time the submission was submitted for execution.
     */
    private long submissionTime;

    /**
     * The result produced by processing the associated {@link DefaultSubmission}.
     */
    private Object result;

    /**
     * The Identifier of the {@link DefaultSubmissionResult} object.
     */
    private final Identifier resultIdentifier;

    /**
     * The listener for this {@link SubmissionOutcome}.
     */
    private SubmissionOutcomeListener submissionOutcomeListener;

    /**
     * The {@link SubmissionState} for the Submission.
     */
    private SubmissionState submissionState;

    /**
     * The {@link SubmissionResult} that this {@link SubmissionOutcome} represents.
     */
    private SubmissionResult submissionResult;

    /**
     * The {@link SubmissionRetentionPolicy} indicates how to deal with the cache objects when reaching a final state.
     */
    SubmissionRetentionPolicy retentionPolicy;

    /**
     * Proxy factory for SubmissionResults.
     */
    private ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;


    /**
     * Standard constructor taking the id of the {@link DefaultSubmissionResult} as a
     * parameter.
     *
     * @param resultIdentifier  the id of the {@link DefaultSubmissionResult}
     * @param retentionPolicy   the {@link SubmissionRetentionPolicy} for this {@link SubmissionOutcome}
     */
    public DefaultSubmissionOutcome(final Identifier                     resultIdentifier,
                                    SubmissionRetentionPolicy            retentionPolicy,
                                    ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory)
    {
        this.resultIdentifier             = resultIdentifier;
        this.retentionPolicy              = retentionPolicy;
        this.submissionResultProxyFactory = submissionResultProxyFactory;
    }


    /**
     * Constructor that also registers a {@link SubmissionOutcomeListener}.
     *
     * @param resultIdentifier  the identifier of the {@link DefaultSubmissionResult}
     * @param listener          the {@link SubmissionOutcomeListener} to
     *                          callback to
     * @param submissionResult  the {@link SubmissionResult} that this {@link SubmissionOutcome} represents
     * @param retentionPolicy   the {@link SubmissionRetentionPolicy} for this {@link SubmissionOutcome}
     */
    public DefaultSubmissionOutcome(Identifier                           resultIdentifier,
                                    SubmissionOutcomeListener            listener,
                                    SubmissionResult                     submissionResult,
                                    SubmissionRetentionPolicy            retentionPolicy,
                                    ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory)
    {
        this.resultIdentifier             = resultIdentifier;
        this.submissionOutcomeListener    = listener;
        this.submissionResult             = submissionResult;
        this.retentionPolicy              = retentionPolicy;
        this.submissionResultProxyFactory = submissionResultProxyFactory;
    }


    /**
     * This method is called by a {@link com.tangosol.util.MapListener} in the
     * {@link DefaultProcessingSession} implementation when a
     * {@link DefaultSubmissionResult} has been created. It is used to notify a local
     * waiting thread of the arrival of a {@link DefaultSubmissionResult}.
     *
     * @param result            the result of the execution
     * @param state             the {@link SubmissionState}
     * @param submissionTime    the time the submission was submitted
     * @param latency           the time it took until execution was started
     * @param executionDuration the time it took to execute the submission
     */
    public void acceptProcessResult(final Object          result,
                                    final SubmissionState state,
                                    final long            submissionTime,
                                    final long            latency,
                                    final long            executionDuration)
    {
        synchronized (this)
        {
            this.result            = result;
            this.submissionTime    = submissionTime;
            this.latency           = latency;
            this.executionDuration = executionDuration;
            this.submissionState   = state;
            notify();

            if (submissionOutcomeListener != null)
            {
                if (submissionState == SubmissionState.FAILED)
                {
                    submissionOutcomeListener.onFailed(this.result);
                }
                else
                {
                    submissionOutcomeListener.onDone(this.result);
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object get() throws InterruptedException, ExecutionException
    {
        synchronized (this)
        {
            if (!isFinalState())
            {
                wait();
            }

            // if the result has failed, re-throw the exception
            if (result instanceof Exception)
            {
                throw new ExecutionException("Execution Failed", (Exception) result);
            }
            else
            {
                return result;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object get(final long     timeout,
                      final TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
    {
        synchronized (this)
        {
            // If we aren't in a final state, then wait
            if (!isFinalState())
            {
                timeUnit.timedWait(this, timeout);
            }

            // Check if we still are not finished, then we actually timed out
            if (!isFinalState())
            {
                throw new TimeoutException();
            }
            else
            {
                // if the result has failed, re-throw the exception
                if (result instanceof Exception)
                {
                    throw new ExecutionException("Execution Failed", (Exception) result);
                }
                else
                {
                    return result;
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getExecutionDuration()
    {
        return executionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public Object getProgress()
    {
        return submissionResult.getProgress();
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionState getSubmissionState()
    {
        return submissionState;
    }


    /**
     * {@inheritDoc}
     */
    public long getSubmissionTime()
    {
        return submissionTime;
    }


    /**
     * {@inheritDoc}
     */
    public long getWaitDuration()
    {
        return latency;
    }


    /**
     * This method is used to check the SubmissionResult to see if it wasn't previously put in to a final state.
     * If so, we will transfer the data to the local outcome.
     */
    public void checkSubmissionResult()
    {
        this.submissionState = submissionResult.getSubmissionState();

        if ((submissionState != null) && (submissionState.isFinalState()))
        {
            SubmissionResult local = submissionResultProxyFactory.getLocalCopyOfRemoteObject(resultIdentifier);

            this.result            = local.getResult();
            this.submissionTime    = local.getSubmissionTime();
            this.latency           = local.getLatency();
            this.executionDuration = local.getExecutionTime();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFinalState()
    {
        return ((submissionState != null) && (submissionState.isFinalState()));
    }


    /**
     * Called when there is a progress notification.
     *
     * @param object Object representing the progress
     */
    public void onProgress(final Object object)
    {
        if (submissionOutcomeListener != null)
        {
            submissionOutcomeListener.onProgress(object);
        }
    }


    /**
     * Called when execution has started.
     */
    public void onStarted()
    {
        if (submissionOutcomeListener != null)
        {
            submissionOutcomeListener.onStarted();
        }
    }


    /**
     * Called when execution was suspended.
     */
    public void onSuspended()
    {
        if (submissionOutcomeListener != null)
        {
            submissionOutcomeListener.onSuspended();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getIdentifier()
    {
        return resultIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionRetentionPolicy getRetentionPolicy()
    {
        return retentionPolicy;
    }
}
