/*
 * File: PendingSubmission.java
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

package com.oracle.coherence.patterns.processing.dispatchers;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.internal.SubmissionKey;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A {@link PendingSubmission} represents the payload (with associated
 * {@link  com.oracle.coherence.patterns.processing.SubmissionConfiguration}
 * ) of a {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
 * that has yet to be dispatched for processing using a {@link Dispatcher}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 * @author Christer Fahlgren
 */
public interface PendingSubmission extends Delayed
{
    /**
     * Return the {@link SubmissionKey} associated with the
     * {@link PendingSubmission}.
     *
     * @return the {@link SubmissionKey} associated with the
     *         {@link PendingSubmission}.
     */
    public SubmissionKey getSubmissionKey();


    /**
     * Return the payload of the {@link PendingSubmission}.
     *
     * @return the payload of the {@link PendingSubmission}
     */
    public Object getPayload();


    /**
     * Return the {@link SubmissionConfiguration} associated with the
     * {@link PendingSubmission}.
     *
     * @return the {@link SubmissionConfiguration} associated with the
     *         {@link PendingSubmission}
     */
    public SubmissionConfiguration getSubmissionConfiguration();


    /**
     * Gets the result identifier, the unique key to the SubmissionResult for this Submission.
     *
     * @return the result identifier
     */
    public Identifier getResultIdentifier();


    /**
     * Return the {@link TimeUnit} used to represent the time for the
     * {@link Delayed} interface.
     *
     * @return the {@link TimeUnit} for the {@link PendingSubmission}
     */
    public TimeUnit getTimeUnit();


    /**
     * {@inheritDoc}
     */
    public int hashCode();


    /**
     * {@inheritDoc}
     */
    public boolean equals(Object that);


    /**
     * Returns the creation time (System.currentTimeMillis) of this
     * PendingSubmission.
     *
     * @return the creation time
     */
    public long getCreationTime();
}
