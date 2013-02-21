/*
 * File: TaskProcessorMediatorProxyMBean.java
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

package com.oracle.coherence.patterns.processing.internal.task;

/**
 * MBean interface for the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorProxy}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface TaskProcessorMediatorProxyMBean
{
    /**
     * Retrieves the key for the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator}.
     *
     * @return the key as a string
     */
    String getKey();


    /**
     * Retrieves the number of accepted tasks.
     *
     * @return the number of accepted tasks
     */
    int getAcceptedTaskCount();


    /**
     * Retrieves the number of executed tasks.
     *
     * @return the number of executed tasks.
     */
    int getExecutedTaskCount();


    /**
     * Retrieves the number of times tasks have yielded.
     *
     * @return the number of yielded tasks.
     */
    int getYieldTaskCount();


    /**
     * Returns the id(s) of the currently executing tasks.
     *
     * @return returns the id(s) of the currently executing tasks.
     */
    String getIdsOfCurrentlyExecutingTask();


    /**
     * Returns the total execution time (in milliseconds) for all the
     * {@link com.oracle.coherence.patterns.processing.task.Task}s executed by
     * the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     *
     * @return the total time in milliseconds
     */
    public long getTotalTaskExecutionDuration();


    /**
     * Returns the average execution time (in milliseconds) for the
     * {@link com.oracle.coherence.patterns.processing.task.Task}s executed by
     * the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     *
     * @return the average time in milliseconds
     */
    public double getAverageTaskExecutionDuration();


    /**
     * Returns the minimum time (in milliseconds) that a {@link com.oracle.coherence.patterns.processing.task.Task} has
     * taken to execute by the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     *
     * @return the minimum time in milliseconds
     */
    public long getMinimumTaskExecutionDuration();


    /**
     * Returns the maximum time (in milliseconds) that a {@link com.oracle.coherence.patterns.processing.task.Task} has
     * taken to execute by the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     *
     * @return the maximum time in milliseconds
     */
    public long getMaximumTaskExecutionDuration();


    /**
     * Returns the execution time (in milliseconds) for the last
     * {@link com.oracle.coherence.patterns.processing.task.Task} executed by
     * the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     *
     * @return the execution time in milliseconds
     */
    public long getLastTaskExecutionDuration();
}
