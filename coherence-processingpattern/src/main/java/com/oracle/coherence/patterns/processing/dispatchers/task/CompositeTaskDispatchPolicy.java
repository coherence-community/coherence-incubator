/*
 * File: CompositeTaskDispatchPolicy.java
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
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A composite of two {@link TaskDispatchPolicy}s.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class CompositeTaskDispatchPolicy implements TaskDispatchPolicy, ExternalizableLite, PortableObject
{
    /**
     * The first policy to apply.
     */
    private TaskDispatchPolicy firstPolicy;

    /**
     * The second policy to apply.
     */
    private TaskDispatchPolicy secondPolicy;


    /**
     * Standard Constructor.
     */
    public CompositeTaskDispatchPolicy()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param firstPolicy  the first {@link TaskDispatchPolicy}
     * @param secondPolicy the second {@link TaskDispatchPolicy}
     */
    public CompositeTaskDispatchPolicy(TaskDispatchPolicy firstPolicy,
                                       TaskDispatchPolicy secondPolicy)
    {
        this.firstPolicy  = firstPolicy;
        this.secondPolicy = secondPolicy;
    }


    /**
     * {@inheritDoc}
     */
    public Map<TaskProcessorMediatorKey, TaskProcessorMediator> selectTaskProcessorSet(Task                     task,
                                                                                       SubmissionConfiguration  submissionConfiguration,
                                                                                       Map<TaskProcessorMediatorKey,
                                                                                       TaskProcessorMediator>   taskProcessorMediators,
                                                                                       ConcurrentHashMap<Identifier,
                                                                                       TaskProcessorDefinition> taskProcessorDefinitions)
    {
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> result = firstPolicy.selectTaskProcessorSet(task,
                                                                                                         submissionConfiguration,
                                                                                                         taskProcessorMediators,
                                                                                                         taskProcessorDefinitions);
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> secondresult = secondPolicy.selectTaskProcessorSet(task,
            submissionConfiguration,
            result,
            taskProcessorDefinitions);

        return secondresult;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.firstPolicy  = (TaskDispatchPolicy) ExternalizableHelper.readObject(in);
        this.secondPolicy = (TaskDispatchPolicy) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, firstPolicy);
        ExternalizableHelper.writeObject(out, secondPolicy);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.firstPolicy  = (TaskDispatchPolicy) reader.readObject(0);
        this.secondPolicy = (TaskDispatchPolicy) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, firstPolicy);
        writer.writeObject(1, secondPolicy);
    }
}
