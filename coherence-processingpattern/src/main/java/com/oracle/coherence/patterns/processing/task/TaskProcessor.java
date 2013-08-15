/*
 * File: TaskProcessor.java
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

package com.oracle.coherence.patterns.processing.task;

import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.tangosol.io.pof.PortableObject;

import java.io.Serializable;

/**
 * An {@link TaskProcessor} executes {@link Task} objects. A {@link TaskProcessor} has a
 * task type which is a string that is used to match tasks to {@link TaskProcessor}s.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface TaskProcessor extends PortableObject, Serializable
{
    /**
     * Returns a unique key for this {@link TaskProcessor}.
     *
     * @return the unique key
     */
    public TaskProcessorMediatorKey getTaskProcessorKey();


    /**
     * Returns a name for the {@link TaskProcessor}.
     *
     * @return the name
     */
    public String getName();


    /**
     * Called when the {@link TaskProcessor} is unregistered.
     */
    public void onShutdown();


    /**
     * Called when the {@link TaskProcessor} is registered.
     *
     * @param taskProcessorMediator          the {@link TaskProcessorMediator} this {@link TaskProcessor}
     *                                       will interact with
     * @param key                            the {@link TaskProcessorMediatorKey} for the {@link TaskProcessorMediator}
     *                                       we will interact with.
     * @param submissionProxyFactory         the {@link ObjectProxyFactory} for {@link Submission}s to use.
     * @param submissionResultProxyFactory   the {@link ObjectProxyFactory} for {@link SubmissionResult}s to use.
     * @param taskProcessorMediatorProxyFactory the {@link ObjectProxyFactory} for {@link TaskProcessorMediator}s to use.
     * @param clientLeaseMaintainer          the {@link ClientLeaseMaintainer} to use
     */
    public void onStartup(TaskProcessorMediator                     taskProcessorMediator,
                          TaskProcessorMediatorKey                  key,
                          ObjectProxyFactory<Submission>            submissionProxyFactory,
                          ObjectProxyFactory<SubmissionResult>      submissionResultProxyFactory,
                          ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorProxyFactory,
                          ClientLeaseMaintainer                     clientLeaseMaintainer);
}
