/*
 * File: TaskTypeSelectionPolicyTest.java
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
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.tangosol.util.UID;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test for {@link AttributeMatchTaskDispatchPolicy}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class TaskTypeSelectionPolicyTest
{
    Task                                                   task;
    SubmissionConfiguration                                submissionConfiguration;
    Map<TaskProcessorMediatorKey, TaskProcessorMediator>   taskProcessorMediators;
    ConcurrentHashMap<Identifier, TaskProcessorDefinition> taskProcessorDefinitions;
    TaskProcessorMediator                                  taskProcessorMediator;
    TaskProcessorMediator                                  taskProcessorMediator2;
    Identifier                                             identifier;
    Identifier                                             identifier2;
    TaskProcessorDefinition                                definition;
    TaskProcessorDefinition                                definition2;


    /**
     * Setup the test.
     */
    @Before
    public void initialize()
    {
        task                     = EasyMock.createMock(Task.class);
        submissionConfiguration  = EasyMock.createMock(SubmissionConfiguration.class);
        taskProcessorMediators   = new HashMap();
        taskProcessorDefinitions = new ConcurrentHashMap();
        taskProcessorMediator    = EasyMock.createMock(TaskProcessorMediator.class);
        taskProcessorMediator2   = EasyMock.createMock(TaskProcessorMediator.class);
        identifier               = EasyMock.createMock(Identifier.class);
        identifier2              = EasyMock.createMock(Identifier.class);
        definition               = EasyMock.createMock(TaskProcessorDefinition.class);
        definition2              = EasyMock.createMock(TaskProcessorDefinition.class);

    }


    /**
     * Replay all interactions.
     */
    public void replayAll()
    {
        EasyMock.replay(task);
        EasyMock.replay(submissionConfiguration);
        EasyMock.replay(identifier);
        EasyMock.replay(identifier2);
        EasyMock.replay(definition);
        EasyMock.replay(definition2);
        EasyMock.replay(taskProcessorMediator);
        EasyMock.replay(taskProcessorMediator2);

    }


    /**
     * Verify all interactions.
     */
    public void verifyAll()
    {
        EasyMock.verify(task);
        EasyMock.verify(submissionConfiguration);
        EasyMock.verify(identifier);
        EasyMock.verify(identifier2);
        EasyMock.verify(definition);
        EasyMock.verify(definition2);
        EasyMock.verify(taskProcessorMediator);
        EasyMock.verify(taskProcessorMediator2);

    }


    /**
     * Ensure we can select a {@link TaskProcessor}.
     */
    @Test
    public void testSelectTaskProcessor()
    {
        UID uidone = new UID();
        UID uidtwo = new UID();

        EasyMock.expect(definition.getIdentifier()).andReturn(identifier).anyTimes();

        EasyMock.expect(definition2.getIdentifier()).andReturn(identifier2).anyTimes();

        EasyMock.expect(taskProcessorMediator.getTaskProcessorKey()).andReturn(new TaskProcessorMediatorKey(identifier,
                                                                                                            1,
                                                                                                            uidone))
                                                                                                            .anyTimes();
        EasyMock.expect(taskProcessorMediator2.getTaskProcessorKey())
            .andReturn(new TaskProcessorMediatorKey(identifier2,
                                                    2,
                                                    uidtwo)).anyTimes();

        Map<String, String> confMap = new HashMap<String, String>();

        confMap.put("ipaddress", "127.0.0.1");
        EasyMock.expect(submissionConfiguration.getConfigurationDataMap()).andReturn(confMap).anyTimes();

        Map<String, String> attrMap = new HashMap<String, String>();

        attrMap.put("ipaddress", "127.0.0.1");
        EasyMock.expect(taskProcessorMediator.getAttributeMap()).andReturn(attrMap);

        Map<String, String> attrMap2 = new HashMap<String, String>();

        attrMap2.put("ipaddress", "127.0.0.2");

        EasyMock.expect(taskProcessorMediator2.getAttributeMap()).andReturn(attrMap2);

        replayAll();
        taskProcessorMediators.put(new TaskProcessorMediatorKey(definition.getIdentifier(),
                                                                1,
                                                                uidone), taskProcessorMediator);
        taskProcessorMediators.put(new TaskProcessorMediatorKey(definition2.getIdentifier(),
                                                                2,
                                                                uidtwo), taskProcessorMediator2);
        taskProcessorDefinitions.put(definition.getIdentifier(), definition);
        taskProcessorDefinitions.put(definition2.getIdentifier(), definition2);

        AttributeMatchTaskDispatchPolicy pol = new AttributeMatchTaskDispatchPolicy();
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> result = pol.selectTaskProcessorSet(task,
                                                                                                 submissionConfiguration,
                                                                                                 taskProcessorMediators,
                                                                                                 taskProcessorDefinitions);
        TaskProcessorMediatorKey resultKey = result.keySet().iterator().next();

        junit.framework.Assert.assertEquals(resultKey,
                                            new TaskProcessorMediatorKey(definition.getIdentifier(), 1, uidone));
        verifyAll();
    }


    /**
     * Ensure that we don't match a {@link TaskProcessor}.
     */
    @Test
    public void testSelectTaskProcessorNoMatch()
    {
        Logger logger = Logger.getLogger("");

        logger.setLevel(Level.ALL);

        Handler[] h = logger.getHandlers();

        if (h.length > 0)
        {
            h[0].setLevel(Level.FINEST);
        }

        UID uidone = new UID();
        UID uidtwo = new UID();

        EasyMock.expect(definition2.getIdentifier()).andReturn(identifier2).anyTimes();

        EasyMock.expect(taskProcessorMediator.getTaskProcessorKey()).andReturn(new TaskProcessorMediatorKey(identifier,
                                                                                                            1,
                                                                                                            uidone))
                                                                                                            .anyTimes();
        EasyMock.expect(taskProcessorMediator2.getTaskProcessorKey())
            .andReturn(new TaskProcessorMediatorKey(identifier2,
                                                    2,
                                                    uidtwo)).anyTimes();
        EasyMock.expect(definition.getIdentifier()).andReturn(identifier).anyTimes();

        Map<String, String> confMap = new HashMap<String, String>();

        confMap.put("ipaddress", "127.0.0.1");
        EasyMock.expect(submissionConfiguration.getConfigurationDataMap()).andReturn(confMap).anyTimes();

        Map<String, String> attrMap = new HashMap<String, String>();

        attrMap.put("ipaddress", "127.0.0.2");
        EasyMock.expect(taskProcessorMediator.getAttributeMap()).andReturn(attrMap);

        Map<String, String> attrMap2 = new HashMap<String, String>();

        attrMap2.put("ipaddress", "127.0.0.2");

        EasyMock.expect(taskProcessorMediator2.getAttributeMap()).andReturn(attrMap2);
        replayAll();

        taskProcessorMediators.clear();
        taskProcessorMediators.put(new TaskProcessorMediatorKey(definition.getIdentifier(),
                                                                1,
                                                                uidone), taskProcessorMediator);
        taskProcessorMediators.put(new TaskProcessorMediatorKey(definition2.getIdentifier(),
                                                                2,
                                                                uidtwo), taskProcessorMediator2);
        taskProcessorDefinitions.put(definition.getIdentifier(), definition);

        AttributeMatchTaskDispatchPolicy pol = new AttributeMatchTaskDispatchPolicy();
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> result = pol.selectTaskProcessorSet(task,
                                                                                                 submissionConfiguration,
                                                                                                 taskProcessorMediators,
                                                                                                 taskProcessorDefinitions);

        TaskProcessorMediatorKey resultKey = null;

        if (result.keySet().iterator().hasNext())
        {
            resultKey = result.keySet().iterator().next();
        }

        junit.framework.Assert.assertNull(resultKey);
        verifyAll();
    }
}
