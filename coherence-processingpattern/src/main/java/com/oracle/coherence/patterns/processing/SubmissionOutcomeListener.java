/*
 * File: SubmissionOutcomeListener.java
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

/**
 * A {@link SubmissionOutcomeListener} is a listener interface that can be
 * registered enabling applications to get a notification for the progress of the
 * Execution of the
 * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface SubmissionOutcomeListener
{
    /**
     * Called when the
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     * has successfully executed.
     *
     * @param oResult the result of execution
     */
    void onDone(Object oResult);


    /**
     *
     * Called when the
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     * failed to successfully execute.
     *
     * @param oResult the error result (usually an exception)
     */
    void onFailed(Object oResult);


    /**
     * Called when the execution of the
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission} is
     * reporting progress.
     *
     * @param oProgress the progress expressed as an object
     */
    void onProgress(Object oProgress);


    /**
     * Called when execution of a
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     * starts.
     */
    void onStarted();


    /**
     * Called when execution of a
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission} is
     * voluntarily suspended.
     */
    void onSuspended();
}
