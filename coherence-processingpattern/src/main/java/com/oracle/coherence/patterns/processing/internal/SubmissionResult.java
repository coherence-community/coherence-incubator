/*
 * File: SubmissionResult.java
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
import com.oracle.coherence.patterns.processing.SubmissionState;

/**
 * The {@link SubmissionResult} interface represents the interface for
 * the grid object representing the result of a
 * {@link com.oracle.coherence.patterns.processing.internal.SubmissionResult}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface SubmissionResult
{
    /**
     * Gets the execution time.
     *
     * @return the execution time
     */
    public long getExecutionTime();


    /**
     * Returns the unique ID of the SubmissionResult, also used as key for the
     * grid.
     *
     * @return the unique id
     */
    public Identifier getID();


    /**
     * Gets the latency - i.e. the time it took from submission to execution.
     *
     * @return the latency.
     */
    public long getLatency();


    /**
     * Gets the progress.
     *
     * @return the progress
     */
    public Object getProgress();


    /**
     * Return the underlying result.
     *
     * @return the underlying result
     */
    public Object getResult();


    /**
     * Returns the {@link SubmissionKey}.
     *
     * @return the {@link SubmissionKey}
     */
    public SubmissionKey getSubmissionKey();


    /**
     * Return the {@link SubmissionState} of this Submission.
     *
     * @return the {@link SubmissionState} of this Submission
     */
    public SubmissionState getSubmissionState();


    /**
     * Gets the submission time.
     *
     * @return the submission time.
     */
    public long getSubmissionTime();


    /**
     * Return the {@link Identifier} of the
     * {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     * expecting this result.
     *
     * @return      the {@link Identifier} of the
     *              {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     *              expecting this result.
     */
    public Identifier getSessionIdentifier();


    /**
     * Changes the session identifier for the {@link Submission}.
     *
     * @param newIdentifier the {@link Identifier} for the {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     */
    public void changeSessionIdentifier(Identifier newIdentifier);


    /**
     * Return whether the SubmissionResult is in a final state.
     *
     * @return true if the SubmissionResult is in a final state
     */
    public boolean isFinalState();


    /**
     * Sets the entry to processing has started - i.e. executing.
     *
     * @param processingOwner must match the assigned owner otherwise false is returned
     *
     * @return true if successful
     */
    public boolean processingStarted(Object processingOwner);


    /**
     * Sets execution to failure.
     *
     * @param oFailureResult the failure result
     *
     * @return true if successful
     */
    public boolean processingFailed(Object oFailureResult);


    /**
     * Assigns a Submission to a particular owner and sets state to be assigned.
     *
     * @param owner the owner of the Submission.
     *
     * @return null if successful, the already existing owner if not
     */
    public Object assign(Object owner);


    /**
     * Sets execution to success.
     *
     * @param oResult the successful result
     *
     * @return true if successful
     */
    public boolean processingSucceeded(Object oResult);


    /**
     * Sets the state to suspended.
     *
     * @param oResult the intermediate result
     *
     * @return true if successful
     */
    public boolean suspendExecution(Object oResult);


    /**
     * Sets the Submission to submitted.
     *
     * @param currentTimeMillis the time in milliseconds after the epoch when the Submission was submitted.
     *
     * @return true if successful
     */
    public boolean submitted(long currentTimeMillis);


    /**
     * Sets the Submission to the retry state.
     *
     * @return true if successful
     */
    public boolean retry();


    /**
     * Checks whether the Submission is resuming execution.
     *
     * @return true if resuming, false otherwise
     */
    public boolean isResuming();


    /**
     * Sets the progress.
     *
     * @param progress the progress for the submission.
     */
    public void setProgress(final Object progress);


    /**
     * Set the result of this {@link SubmissionResult}.
     *
     * @param result the result
     */
    public void setResult(final Object result);


    /**
     * Cancels the Submission.
     *
     * @return true if successful
     */
    public boolean cancelSubmission();
}
