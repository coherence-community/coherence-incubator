/*
 * File: AttributeMatchTaskDispatchPolicy.java
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link AttributeMatchTaskDispatchPolicy} is a {@link TaskDispatchPolicy}
 * that selects the first {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}
 * from a list that has the same task type.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class AttributeMatchTaskDispatchPolicy implements TaskDispatchPolicy, ExternalizableLite, PortableObject
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(AttributeMatchTaskDispatchPolicy.class.getName());


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Map<TaskProcessorMediatorKey, TaskProcessorMediator> selectTaskProcessorSet(Task                     task,
                                                                                       SubmissionConfiguration  submissionConfiguration,
                                                                                       Map<TaskProcessorMediatorKey,
                                                                                       TaskProcessorMediator>   taskProcessorMediators,
                                                                                       ConcurrentHashMap<Identifier,
                                                                                       TaskProcessorDefinition> taskProcessorDefinitions)
    {
        HashMap<TaskProcessorMediatorKey, TaskProcessorMediator> result = new HashMap<TaskProcessorMediatorKey,
                                                                                      TaskProcessorMediator>();
        final Iterator<Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator>> iter =
            taskProcessorMediators.entrySet().iterator();

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Selecting TaskProcessor for {0}", task);
        }

        while (iter.hasNext())
        {
            Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator> entry    = iter.next();
            TaskProcessorMediator                                      mediator = entry.getValue();

            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "Matching attributes for TaskProcessor {0}, task {1}",
                           new Object[] {mediator, task});
            }

            // Lets match all attributes in the Configuration Map with the Map from the TaskProcessorMediator
            Boolean             matches = true;    // No attributes in the conf map = yes we match

            Map<String, String> confMap = submissionConfiguration.getConfigurationDataMap();
            Map<String, String> tpmMap  = mediator.getAttributeMap();

            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Configuration Map:{0}", new Object[] {confMap});
                logger.log(Level.FINER, "Attribute Map:{0}", new Object[] {tpmMap});
            }

            Iterator<Entry<String, String>> confMapiter = confMap.entrySet().iterator();

            while (confMapiter.hasNext())
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Matching attributes for TaskProcessor {0}, task {1}",
                               new Object[] {mediator, task});
                }

                Entry<String, String> confMapEntry = confMapiter.next();
                Object                attrkey      = confMapEntry.getKey();
                Object                confValue    = confMapEntry.getValue();

                if (confValue != null)
                {
                    Object tpmValue = tpmMap.get(attrkey);

                    if (logger.isLoggable(Level.FINER))
                    {
                        logger.log(Level.FINER,
                                   "Comparing submission attribute {0}, value: {1}, TaskProcessor attribute value:{2}",
                                   new Object[] {attrkey, confValue, tpmValue});
                    }

                    if (tpmValue != null)
                    {
                        if (confValue.equals(tpmValue))
                        {
                            matches = true;
                        }
                        else
                        {
                            matches = false;
                        }
                    }
                    else
                    {
                        matches = false;
                    }
                }
            }

            if (matches)
            {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
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
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
    }
}
