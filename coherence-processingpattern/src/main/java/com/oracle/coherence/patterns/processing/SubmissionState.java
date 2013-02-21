/*
 * File: SubmissionState.java
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

import com.oracle.coherence.patterns.processing.internal.Submission;

/**
 * {@link SubmissionState} enumerates the states a {@link Submission} may be in.
 * <ul>
 *      <li>INITIAL is the state for a newly created Submission - not yet
 *          submitted.</li>
 *      <li>SUBMITTED is the state for a Submission that has been submitted, but
 *          not yet executed.</li>
 *      <li>EXECUTING is the state where a Submission is executing.</li>
 *      <li>SUSPENDED is the state where a Submission is voluntarily suspended.</li>
 *      <li>FAILED is a <b>final</b> state where execution was not successful.</li>
 *      <li>DONE is the <b>final</b> state where execution was successful.</li>
 * </ul>
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public enum SubmissionState
{
    /**
     * DONE is the <b>final</b> state where execution was successful.
     */
    DONE,

    /**
     * EXECUTING is the state where a Submission is executing.
     */
    EXECUTING,

    /**
     * FAILED is a <b>final</b> state where execution was not successful.
     */
    FAILED,

    /**
     * INITIAL is the state for a newly created Submission - not yet
     * submitted.
     */
    INITIAL,

    /**
     * SUBMITTED is the state for a Submission that has been submitted, but
     * not yet executed.
     */
    SUBMITTED,

    /**
     * ASSIGNED is the state for a Submission that has been submitted and dispatched
     * and assigned to an owner, but not yet executed.
     */
    ASSIGNED,

    /**
     * SUSPENDED is the state when a voluntary suspension of the execution has
     * happened.
     */
    SUSPENDED,

    /**
     * RETRY is the state when an involuntary transfer of the Submission was done to
     * a Submission from a previous owner.
     */
    RETRY,

    /**
     * CANCELLED is a final state for a Submission when it has been explicitly cancelled.
     */
    CANCELLED;

    /**
     * Determines whether a SubmissionState is considered final.
     *
     * @return true if it is final, false if not
     */
    public boolean isFinalState()
    {
        return (this == SubmissionState.DONE) || (this == SubmissionState.FAILED)
               || (this == SubmissionState.CANCELLED);
    }
}
