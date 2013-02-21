/*
 * File: ResumableTask.java
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

/**
 *
 * The {@link ResumableTask} interface is the main interface for a
 * general purpose task. A task has a type which is used to match it against an
 * executor. The task is passed a {@link TaskExecutionEnvironment} with which
 * it can interact.
 * <p>
 * When a task executes, it can return any object as a result. A special
 * result is the {@link Yield} object which can be used to pause the execution
 * and store an intermediate state for a later resumption of execution.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface ResumableTask extends Task
{
    /**
     * run is called by the {@link TaskProcessor} to execute the task. {@link Yield} may be
     * returned to pause execution.
     *
     * @param oEnvironment   is the {@link TaskExecutionEnvironment} used to interact
     *                       with the environment
     *
     * @return the result of the task
     */
    public Object run(TaskExecutionEnvironment oEnvironment);
}
