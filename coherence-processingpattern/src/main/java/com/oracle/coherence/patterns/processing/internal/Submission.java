/*
 * File: Submission.java
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
import com.tangosol.util.UUID;

/**
 * This interface represents the operations possible for a {@link Submission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface Submission
{
    /**
     * Generate a {@link SubmissionKey} for this Submission.
     *
     * @return a {@link SubmissionKey} for this Submission.
     */
    public SubmissionKey generateKey();


    /**
     * Return the actual payload associated with this {@link DefaultSubmission}.
     *
     * @return the actual payload associated with this {@link DefaultSubmission}
     */
    public SubmissionContent getContent();


    /**
     * Returns the identifier that may be used to locate the the result (if
     * one is produced).
     *
     * @return the identifier that may be used to locate the the result (if
     *         one is produced).
     */
    public Identifier getResultIdentifier();


    /**
     * Return the {@link Identifier} for the
     * {@link com.oracle.coherence.patterns.processing.ProcessingSession} that
     * submitted this {@link DefaultSubmission}.
     *
     * @return the {@link Identifier} for the
     *         {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     *         that submitted this {@link DefaultSubmission}
     */
    public Identifier getSessionIdentifier();


    /**
     * Changes the session identifier for the {@link Submission}.
     *
     * @param newIdentifier the {@link Identifier} for the {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     */
    public void changeSessionIdentifier(Identifier newIdentifier);


    /**
     * Returns the {@link UUID} of the {@link DefaultSubmission} as allocated by the
     * {@link com.oracle.coherence.patterns.processing.ProcessingSession}.
     *
     * @return the {@link UUID} of the {@link DefaultSubmission} as allocated by the
     *         {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     *         .
     */
    public UUID getUUID();


    /**
     * Tells this {@link Submission} to redispatch.
     *
     * @param delay potential delay before redispatching
     *
     * @return true if it was possible to redispatch.
     */
    public boolean reDispatch(long delay);
}
