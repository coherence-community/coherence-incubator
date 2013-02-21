/*
 * File: RandomTaskDispatchPolicy.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link TaskDispatchPolicy} that selects a {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} randomly.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class RandomTaskDispatchPolicy implements TaskDispatchPolicy, ExternalizableLite, PortableObject
{
    /**
     * The Random number generator.
     */
    private transient Random generator;


    /**
     * Default Constructor.
     */
    public RandomTaskDispatchPolicy()
    {
        generator = new Random(System.currentTimeMillis());
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
        int randomTaskProcessor = generator.nextInt(taskProcessorMediators.size());
        Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator> selectedEntry     = null;
        int                                                        collectionCounter = 0;
        final Iterator<Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator>> iter =
            taskProcessorMediators.entrySet().iterator();

        while (iter.hasNext())
        {
            Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator> entry = iter.next();

            if (collectionCounter == randomTaskProcessor)
            {
                selectedEntry = entry;
                break;
            }
            else
            {
                iter.next();
            }

            collectionCounter++;
        }

        return Collections.singletonMap(selectedEntry.getKey(), selectedEntry.getValue());
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        generator = new Random(System.currentTimeMillis());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        generator = new Random(System.currentTimeMillis());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
    }
}
