/*
 * File: SubmissionOutcome.java
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

package com.oracle.coherence.patterns.processing;

import com.oracle.coherence.common.identifiers.Identifier;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link SubmissionOutcome} acts as a future for the result of the Submission.
 * It allows you to retrieve the status of the Submission as well as execution
 * timing after the Submission is completed.
 *
 * @author Noah Arliss,
 * @author Christer Fahlgren
 */
public interface SubmissionOutcome
{
    /**
     * Wait for the submitted payload to finish being processed and return the
     * result of the said processing.
     *
     * @return the completed result
     *
     * @throws InterruptedException if the processing was interrupted
     * @throws ExecutionException if the processing of the payload
     *         threw an exception
     */
    public Object get() throws InterruptedException, ExecutionException;


    /**
     * Wait the specified amount of time for the submitted payload to be
     * processed and return the result if available.
     *
     * @param lTimeout  the maximum time to wait
     * @param oUnit     the unit of the timeout argument
     *
     * @return the completed result
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws ExecutionException if processing the payload threw an exception
     * @throws InterruptedException if the current thread was interrupted
     *       while waiting
     * @throws TimeoutException if the wait timed out
     */
    public Object get(long     lTimeout,
                      TimeUnit oUnit) throws InterruptedException, ExecutionException, TimeoutException;


    /**
     * Returns the current progress.
     *
     * @return the progress
     */
    public Object getProgress();


    /**
     * Get the {@link SubmissionState} of this outcome.
     *
     * @return the {@link SubmissionState}
     */
    public SubmissionState getSubmissionState();


    /**
     * Returns the time when the
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     * was submitted in milliseconds after the epoch.
     *
     * @return the submission time
     */
    public long getSubmissionTime();


    /**
     * Returns the delta time in milliseconds between submission and execution
     * started - i.e. the wait duration for the
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}.
     *
     * @return the wait duration in milliseconds
     */
    public long getWaitDuration();


    /**
     * Returns the time it took to execute the submission in milliseconds.
     *
     * @return the time to execute in milliseconds
     */
    public long getExecutionDuration();


    /**
     * Return whether the submitted payload has finished being processed.
     *
     * @return true if the submitted payload has finished being processed.
     */
    public boolean isFinalState();


    /**
     * Returns the object that uniquely identifies the
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmissionResult}.
     *
     * @return the identifier of the
     *         {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmissionResult}.
     */
    public Identifier getIdentifier();


    /**
     * Returns {@link SubmissionRetentionPolicy} for this {@link SubmissionOutcome}.
     *
     * @return the {@link SubmissionRetentionPolicy}
     */
    public SubmissionRetentionPolicy getRetentionPolicy();
}
