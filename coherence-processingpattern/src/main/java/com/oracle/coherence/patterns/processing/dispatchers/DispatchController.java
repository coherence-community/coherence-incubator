/*
 * File: DispatchController.java
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

import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.patterns.processing.SubmissionState;
import com.oracle.coherence.patterns.processing.internal.DefaultPendingSubmission;
import com.tangosol.net.ConfigurableCacheFactory;

/**
 * A {@link DispatchController} is responsible for asynchronously dispatching
 * {@link PendingSubmission}s to one or more {@link Dispatcher}s for later
 * processing.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public interface DispatchController extends Runnable, DependentResource
{
    /**
     * Requests that the {@link DispatchController} accept the specified
     * {@link PendingSubmission} and attempt to schedule it for dispatching
     * using a registered {@link Dispatcher} for processing.
     *
     * @param defaultPendingSubmission The {@link PendingSubmission} to accept and
     *                                 schedule for dispatching.
     */
    public void accept(PendingSubmission defaultPendingSubmission);


    /**
     * Requests that the {@link DispatchController} accept the specified
     * {@link PendingSubmission} and attempt to schedule it for dispatching
     * using a registered {@link Dispatcher} for processing.
     * The Submission has been transferred from another partition indicating
     * that it has been moved due to partition reassignment or a failover.
     *
     * @param defaultPendingSubmission The {@link PendingSubmission} to accept and
     *            schedule for dispatching.
     */
    public void acceptTransferredSubmission(DefaultPendingSubmission defaultPendingSubmission);


    /**
     * Discards a currently scheduled {@link PendingSubmission} for
     * dispatching.
     *
     * @param oPendingSubmission the {@link PendingSubmission} to discard
     */
    public void discard(PendingSubmission oPendingSubmission);


    /**
     * Hook called when a {@link Dispatcher} has been updated.
     *
     * @param oDispatcher the {@link Dispatcher} that has been updated.
     */
    public void onDispatcherUpdate(Dispatcher oDispatcher);


    /**
     * The dispatch controller is the JVM local object which
     * can keep track of the ConfigurableCacheFactory required to
     * initialize dispatchers.
     *
     * @return the ConfigurableCacheFactory for this JVM.
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory();


    /**
     * Returns the {@link SubmissionState} for the resultId.
     *
     * @param resultId the Id for the {@link com.oracle.coherence.patterns.processing.internal.SubmissionResult}
     *
     * @return the {@link SubmissionState}
     */
    public SubmissionState getSubmissionState(Object resultId);
}
