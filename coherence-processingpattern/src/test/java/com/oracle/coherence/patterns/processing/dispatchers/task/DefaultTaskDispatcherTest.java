/*
 * File: DefaultTaskDispatcherTest.java
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
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionKey;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.task.ResumableTask;
import com.oracle.coherence.patterns.processing.task.Task;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * A {@link DefaultTaskDispatcherTest} tests the {@link DefaultTaskDispatcher}. </p>
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class DefaultTaskDispatcherTest
{
    /**
     * Tests a submission that is null.
     */
    @Test
    public void testDispatchingOfNull()
    {
        PendingSubmission     submission = mock(PendingSubmission.class);
        DefaultTaskDispatcher disp       = new DefaultTaskDispatcher();

        assertFalse(disp.dispatchTask(submission, null));
    }


    /**
     * Tests a submission but the TaskDispatchPolicy doesn't respond with anything useful.
     */
    @Test
    public void testDispatchingTask()
    {
        PendingSubmission     submission         = mock(PendingSubmission.class);
        TaskDispatchPolicy    taskdispatchpolicy = mock(TaskDispatchPolicy.class);
        Environment           environment        = mock(Environment.class);
        DefaultTaskDispatcher disp = new DefaultTaskDispatcher(environment,
                                                               "DefaultTaskDispatcher",
                                                               taskdispatchpolicy);
        ResumableTask                                          task                     = mock(ResumableTask.class);
        SubmissionConfiguration submissionConfiguration = mock(SubmissionConfiguration.class);
        ConcurrentHashMap<Identifier, TaskProcessorDefinition> taskProcessorDefinitions = mock(ConcurrentHashMap.class);
        Map<TaskProcessorMediatorKey, TaskProcessorMediator>   taskProcessorMediators   = mock(Map.class);
        Map<TaskProcessorMediatorKey, TaskProcessorMediator>   resultMap                = mock(Map.class);

        stub(taskdispatchpolicy.selectTaskProcessorSet(task, submissionConfiguration, taskProcessorMediators,
                                                       taskProcessorDefinitions)).toReturn(resultMap);
        assertFalse(disp.dispatchTask(submission, task));
    }


    /**
     * Tests a submission where the TaskMediator accepts the Task.
     */
    @Test
    public void testDispatchingTaskAcceptingTaskMediator()
    {
        PendingSubmission                         submission         = mock(PendingSubmission.class);
        TaskDispatchPolicy                        taskdispatchpolicy = mock(TaskDispatchPolicy.class);
        Environment                               environment        = mock(Environment.class);
        ObjectProxyFactory<TaskProcessorMediator> taskproxy          = mock(ObjectProxyFactory.class);
        ObjectProxyFactory<SubmissionResult>      submissionproxy    = mock(ObjectProxyFactory.class);
        ;
        ObjectProxyFactory<Submission>            subresproxy        = mock(ObjectProxyFactory.class);
        DefaultTaskDispatcher disp = new DefaultTaskDispatcher(environment,
                                                               "DefaultTaskDispatcher",
                                                               taskdispatchpolicy,
                                                               taskproxy,
                                                               submissionproxy,
                                                               subresproxy);
        ResumableTask                                        task                       = mock(ResumableTask.class);
        SubmissionConfiguration submissionConfiguration = mock(SubmissionConfiguration.class);
        ConcurrentHashMap<Identifier, TaskProcessorDefinition> taskProcessorDefinitions = mock(ConcurrentHashMap.class);
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> taskProcessorMediators     = mock(Map.class);
        HashMap<TaskProcessorMediatorKey, TaskProcessorMediator> resultMap = new HashMap<TaskProcessorMediatorKey,
                                                                                         TaskProcessorMediator>();
        TaskProcessorMediatorKey key      = mock(TaskProcessorMediatorKey.class);
        TaskProcessorMediator    mediator = mock(TaskProcessorMediator.class);

        resultMap.put(key, mediator);
        stub(taskdispatchpolicy.selectTaskProcessorSet(Matchers.any(Task.class),
                                                       Matchers.any(SubmissionConfiguration.class), Matchers.anyMap(),
                                                       Matchers.any(ConcurrentHashMap.class))).toReturn(resultMap);

        SubmissionResult subres = mock(SubmissionResult.class);

        stub(submissionproxy.getProxy(Matchers.any())).toReturn(subres);
        stub(taskproxy.getProxy(Matchers.any())).toReturn(mediator);
        stub(subres.assign(Matchers.any())).toReturn(true);

        // Testing the case where the mediator accepts the task
        stub(mediator.offerTask(Matchers.any(SubmissionKey.class), Matchers.any(Identifier.class))).toReturn(true);
        assertTrue(disp.dispatchTask(submission, task));
    }


    /**
     * Tests a submission where the TaskMediator does not accept the Task.
     */
    @Test
    public void testDispatchingTaskNotAcceptingTaskMediator()
    {
        PendingSubmission                         submission         = mock(PendingSubmission.class);
        TaskDispatchPolicy                        taskdispatchpolicy = mock(TaskDispatchPolicy.class);
        Environment                               environment        = mock(Environment.class);
        ObjectProxyFactory<TaskProcessorMediator> taskproxy          = mock(ObjectProxyFactory.class);
        ObjectProxyFactory<SubmissionResult>      submissionproxy    = mock(ObjectProxyFactory.class);
        ;
        ObjectProxyFactory<Submission>            subresproxy        = mock(ObjectProxyFactory.class);
        DefaultTaskDispatcher disp = new DefaultTaskDispatcher(environment,
                                                               "DefaultTaskDispatcher",
                                                               taskdispatchpolicy,
                                                               taskproxy,
                                                               submissionproxy,
                                                               subresproxy);
        ResumableTask                                        task                       = mock(ResumableTask.class);
        SubmissionConfiguration submissionConfiguration = mock(SubmissionConfiguration.class);
        ConcurrentHashMap<Identifier, TaskProcessorDefinition> taskProcessorDefinitions = mock(ConcurrentHashMap.class);
        Map<TaskProcessorMediatorKey, TaskProcessorMediator> taskProcessorMediators     = mock(Map.class);
        HashMap<TaskProcessorMediatorKey, TaskProcessorMediator> resultMap = new HashMap<TaskProcessorMediatorKey,
                                                                                         TaskProcessorMediator>();
        TaskProcessorMediatorKey key      = mock(TaskProcessorMediatorKey.class);
        TaskProcessorMediator    mediator = mock(TaskProcessorMediator.class);

        resultMap.put(key, mediator);
        stub(taskdispatchpolicy.selectTaskProcessorSet(Matchers.any(Task.class),
                                                       Matchers.any(SubmissionConfiguration.class), Matchers.anyMap(),
                                                       Matchers.any(ConcurrentHashMap.class))).toReturn(resultMap);

        SubmissionResult subres = mock(SubmissionResult.class);

        stub(submissionproxy.getProxy(Matchers.any())).toReturn(subres);
        stub(taskproxy.getProxy(Matchers.any())).toReturn(mediator);
        stub(subres.assign(Matchers.any())).toReturn(null);

        // Testing the case where the mediator does not accept the task
        stub(mediator.offerTask(Matchers.any(SubmissionKey.class), Matchers.any(Identifier.class))).toReturn(false);
        assertFalse(disp.dispatchTask(submission, task));
    }
}
