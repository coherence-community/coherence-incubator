/*
 * File: ProcessingSession.java
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

import java.util.Iterator;

/**
 * A {@link ProcessingSession} provides the mechanism by which we may submit
 * and manage a Submission (and any associated configuration) for processing.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Christer Fahlgren
 */
public interface ProcessingSession
{
    /**
     * Submit a payload and associated {@link SubmissionConfiguration} for
     * asynchronous processing.
     *
     * @param oPayload       the payload to be processed
     * @param oConfiguration the {@link SubmissionConfiguration}
     *                       associated with the payload
     *
     * @return A {@link SubmissionOutcome} to track the results of the
     *         asynchronous processing
     * @throws Throwable if the submit fails
     */
    public SubmissionOutcome submit(Object                  oPayload,
                                    SubmissionConfiguration oConfiguration) throws Throwable;


    /**
     * Submit a payload and associated {@link SubmissionConfiguration} for
     * asynchronous processing as well as a {@link SubmissionOutcomeListener}.
     * The SubmissionOutcomeListener can be used to get notifications when
     * the state of the Submission processing changes.
     *
     * @param oPayload           the payload to be processed
     * @param oConfigurationData the {@link SubmissionConfiguration}
     *                           associated with the payload
     * @param oListener          the {@link SubmissionOutcomeListener}
     *
     * @return A {@link SubmissionOutcome} to track the results of the
     *         asynchronous processing
     * @throws Throwable  if the submit fails
     */
    public SubmissionOutcome submit(Object                    oPayload,
                                    SubmissionConfiguration   oConfigurationData,
                                    SubmissionOutcomeListener oListener) throws Throwable;


    /**
     * Submit a payload and associated {@link SubmissionConfiguration} with a
     * specific {@link SubmissionRetentionPolicy} and a specific identifier for asynchronous processing.
     * <p>
     * If the identifier already exists an IllegalStateException will be thrown.
     *
     * @param oPayload        the payload to be processed
     * @param oConfiguration  the {@link SubmissionConfiguration}
     *                        associated with the payload
     * @param identifier      the identifier used to identify this Submission
     * @param retentionPolicy the policy for removing the Submission and its
     *                        corresponding result from the underlying cache
     * @param oListener       the {@link SubmissionOutcomeListener}, pass in
     *                        null if no listener is required
     *
     * @return A {@link SubmissionOutcome} to track the results of the
     *         asynchronous processing
     * @throws Throwable if the submit fails
     */
    public SubmissionOutcome submit(Object                    oPayload,
                                    SubmissionConfiguration   oConfiguration,
                                    Identifier                identifier,
                                    SubmissionRetentionPolicy retentionPolicy,
                                    SubmissionOutcomeListener oListener) throws Throwable;


    /**
     * Acquires control of an already existing Submission. Note that if another
     * active {@link ProcessingSession} is waiting for notifications, it will be
     * disconnected and stop getting notifications. Also note that a "connection"
     * to a Submission is required in order for a discard to be valid.
     *
     * @param identifier      the identifier used to identify this Submission
     * @param retentionPolicy the policy for removing the Submission and its
     *                        corresponding result from the underlying cache
     * @param oListener       the {@link SubmissionOutcomeListener}, null if
     *                        no listener is required
     *
     * @return A {@link SubmissionOutcome} to track the results of the
     *         asynchronous processing
     * @throws Throwable if the submit fails
     */
    public SubmissionOutcome acquireSubmission(Identifier                identifier,
                                               SubmissionRetentionPolicy retentionPolicy,
                                               SubmissionOutcomeListener oListener) throws Throwable;


    /**
     * Removes a Submission and its result.
     *
     * @param identifier the identifier associated with the submission
     *
     * @return true if it was successfully removed, false if it didn't exist
     *
     * @throws Throwable if removing fails
     */
    public boolean discardSubmission(Identifier identifier) throws Throwable;


    /**
     * Releases the Submission from being handled by this {@link ProcessingSession}.
     *
     * @param submissionOutcome the {@link SubmissionOutcome} to release.
     */
    public void releaseSubmission(SubmissionOutcome submissionOutcome);


    /**
     * Get an Iterator to all the {@link Identifier}s for the live Submissions
     * associated with this {@link ProcessingSession}.
     *
     * @return a {@link Iterator} for the "live" {@link Identifier}s for the
     *         Submissions associated with this {@link ProcessingSession}
     */
    public Iterator<Identifier> getIdentifierIterator();


    /**
     * Returns true if a Submission with the passed {@link Identifier} exists.
     *
     * @param submissionIdentifier the {@link Identifier} to check
     *
     * @return true if the {@link Identifier} points to an existing Submission,
     *         false otherwise
     */
    public boolean submissionExists(Identifier submissionIdentifier);


    /**
     * Cancels a Submission and turns its {@link SubmissionState} to
     * {@link SubmissionState#CANCELLED}. The {@link SubmissionOutcome} will
     * immediately be signaled as being in a final state.
     * <p>
     * If a Submission has started processing, its Dispatcher will be signaled,
     * but there is no guarantee that it will stop executing immediately.
     *
     * @param identifier the identifier associated with the submission
     *
     * @return true if it was successfully canceled, false if it wasn't
     */
    public boolean cancelSubmission(Identifier identifier);


    /**
     * Shuts down the current {@link ProcessingSession} cleanly.
     */
    public void shutdown();
}
