/*
 * File: RoundRobinTaskDispatchPolicyTest.java
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
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;

/**
 * Test for RoundRobinTaskDispatchPolicy.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *  @author Christer Fahlgren
 */
public class RoundRobinTaskDispatchPolicyTest
{
    Task                                                               task;
    SubmissionConfiguration                                            submissionConfiguration;
    ConcurrentHashMap<TaskProcessorMediatorKey, TaskProcessorMediator> taskProcessorMediators;
    ConcurrentHashMap<Identifier, TaskProcessorDefinition>             taskProcessorDefinitions;
    Iterator                                                           iter;
    Set                                                                entrySet;
    Map.Entry                                                          entry;
    TaskProcessorMediator                                              taskProcessorMediator;
    TaskProcessorMediatorKey                                           key;
    Identifier                                                         identifier;
    TaskProcessorDefinition                                            definition;


    /**
     * Setup the test.
     */
    @Before
    public void initialize()
    {
        task                     = EasyMock.createMock(Task.class);
        submissionConfiguration  = EasyMock.createMock(SubmissionConfiguration.class);
        taskProcessorMediators   = org.easymock.classextension.EasyMock.createMock(ConcurrentHashMap.class);
        taskProcessorDefinitions = org.easymock.classextension.EasyMock.createMock(ConcurrentHashMap.class);
        entrySet                 = EasyMock.createMock(Set.class);
        iter                     = EasyMock.createMock(Iterator.class);
        entry                    = EasyMock.createMock(Map.Entry.class);
        taskProcessorMediator    = EasyMock.createMock(TaskProcessorMediator.class);
        key                      = org.easymock.classextension.EasyMock.createMock(TaskProcessorMediatorKey.class);
        identifier               = EasyMock.createMock(Identifier.class);
        definition               = EasyMock.createMock(TaskProcessorDefinition.class);

    }


    /**
     * Replay all interactions.
     */
    public void replayAll()
    {
        EasyMock.replay(task);
        EasyMock.replay(submissionConfiguration);
        org.easymock.classextension.EasyMock.replay(taskProcessorMediators);
        org.easymock.classextension.EasyMock.replay(taskProcessorDefinitions);
        EasyMock.replay(entrySet);
        EasyMock.replay(iter);
        EasyMock.replay(entry);
        org.easymock.classextension.EasyMock.replay(key);
        EasyMock.replay(identifier);
        EasyMock.replay(definition);
        EasyMock.replay(taskProcessorMediator);

    }


    /**
     * Verify Everything.
     */
    public void verifyAll()
    {
        EasyMock.verify(task);
        EasyMock.verify(submissionConfiguration);
        org.easymock.classextension.EasyMock.verify(taskProcessorMediators);
        org.easymock.classextension.EasyMock.verify(taskProcessorDefinitions);
        EasyMock.verify(entrySet);
        EasyMock.verify(iter);
        EasyMock.verify(entry);
        org.easymock.classextension.EasyMock.verify(key);
        EasyMock.verify(identifier);
        EasyMock.verify(definition);
        EasyMock.verify(taskProcessorMediator);

    }


    /**
     * Ensure that we can select a {@link TaskProcessor}.
     */
    @Test
    public void testSelectTaskProcessor()
    {
        EasyMock.expect(taskProcessorMediators.entrySet()).andReturn(entrySet);
        EasyMock.expect(entrySet.iterator()).andReturn(iter);
        EasyMock.expect(taskProcessorMediators.size()).andReturn(1);
        EasyMock.expect(taskProcessorMediators.isEmpty()).andReturn(false);
        EasyMock.expect(entry.getKey()).andReturn(key);
        EasyMock.expect(entry.getValue()).andReturn(taskProcessorMediator);
        EasyMock.expect(iter.hasNext()).andReturn(true);
        EasyMock.expect(iter.hasNext()).andReturn(false);
        EasyMock.expect(iter.next()).andReturn(entry);

        replayAll();

        RoundRobinTaskDispatchPolicy pol = new RoundRobinTaskDispatchPolicy();
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> result = pol.selectTaskProcessorSet(task,
                                                                                                 submissionConfiguration,
                                                                                                 taskProcessorMediators,
                                                                                                 taskProcessorDefinitions);
        TaskProcessorMediatorKey resultKey = result.keySet().iterator().next();

        junit.framework.Assert.assertEquals(resultKey, key);
        verifyAll();

    }


    /**
     * Ensure that a {@link TaskProcessor} can't be matched.
     */
    @Test
    public void testSelectTaskProcessorNoMatch()
    {
        EasyMock.expect(taskProcessorMediators.isEmpty()).andReturn(true).anyTimes();

        replayAll();

        RoundRobinTaskDispatchPolicy pol = new RoundRobinTaskDispatchPolicy();
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> result = pol.selectTaskProcessorSet(task,
                                                                                                 submissionConfiguration,
                                                                                                 taskProcessorMediators,
                                                                                                 taskProcessorDefinitions);
        TaskProcessorMediatorKey resultKey = null;

        assertTrue(result.isEmpty());
        verifyAll();
    }


    /**
     * Ensure that we can select the next {@link TaskProcessor}.
     */
    @Test
    public void testSelectNextTaskProcessor()
    {
        EasyMock.expect(taskProcessorMediators.entrySet()).andReturn(entrySet).times(2);
        EasyMock.expect(entrySet.iterator()).andReturn(iter).times(2);
        EasyMock.expect(taskProcessorMediators.size()).andReturn(2).times(2);
        EasyMock.expect(taskProcessorMediators.isEmpty()).andReturn(false).anyTimes();

        EasyMock.expect(entry.getKey()).andReturn(key);
        EasyMock.expect(entry.getValue()).andReturn(taskProcessorMediator).times(2);
        EasyMock.expect(iter.hasNext()).andReturn(true);
        EasyMock.expect(iter.hasNext()).andReturn(true);
        EasyMock.expect(iter.hasNext()).andReturn(false);
        EasyMock.expect(iter.hasNext()).andReturn(true);
        EasyMock.expect(iter.hasNext()).andReturn(true);
        EasyMock.expect(iter.hasNext()).andReturn(false);
        EasyMock.expect(iter.next()).andReturn(entry).times(4);

        TaskProcessorMediatorKey key2 = org.easymock.classextension.EasyMock.createMock(TaskProcessorMediatorKey.class);

        EasyMock.expect(entry.getKey()).andReturn(key2);

        replayAll();
        org.easymock.classextension.EasyMock.replay(key2);

        RoundRobinTaskDispatchPolicy pol = new RoundRobinTaskDispatchPolicy();
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> result = pol.selectTaskProcessorSet(task,
                                                                                                 submissionConfiguration,
                                                                                                 taskProcessorMediators,
                                                                                                 taskProcessorDefinitions);
        TaskProcessorMediatorKey resultKey = result.keySet().iterator().next();

        result = pol.selectTaskProcessorSet(task, submissionConfiguration, taskProcessorMediators,
                                            taskProcessorDefinitions);
        resultKey = result.keySet().iterator().next();

        junit.framework.Assert.assertEquals(resultKey, key2);
        verifyAll();
        org.easymock.classextension.EasyMock.verify(key2);

    }
}
