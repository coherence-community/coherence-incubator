/*
 * File: TaskDispatchPolicy.java
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

package com.oracle.coherence.patterns.processing.dispatchers.task;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.task.Task;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link TaskDispatchPolicy} is consulted by the {@link DefaultTaskDispatcher} to select
 * which {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} will execute a particular Task.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface TaskDispatchPolicy extends Serializable
{
    /**
     * Method called by the {@link DefaultTaskDispatcher} to select an {@link TaskProcessorMediator}.
     *
     * @param task                      is the {@link Task} to be executed.
     * @param submissionConfiguration   is the {@link SubmissionConfiguration} supplied with the {@link Task}
     * @param taskProcessorMediators    is the current list of registered {@link TaskProcessorMediator}s.
     * @param taskProcessorDefinitions  is the current list of registered {@link TaskProcessorDefinition}s
     *
     * @return the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} that shall execute the task
     */
    public Map<TaskProcessorMediatorKey, TaskProcessorMediator> selectTaskProcessorSet(Task                     task,
                                                                                       SubmissionConfiguration  submissionConfiguration,
                                                                                       Map<TaskProcessorMediatorKey,
                                                                                       TaskProcessorMediator>   taskProcessorMediators,
                                                                                       ConcurrentHashMap<Identifier,
                                                                                       TaskProcessorDefinition> taskProcessorDefinitions);
}
