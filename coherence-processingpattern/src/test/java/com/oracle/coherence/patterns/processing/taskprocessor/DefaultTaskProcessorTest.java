/*
 * File: DefaultTaskProcessorTest.java
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

package com.oracle.coherence.patterns.processing.taskprocessor;

import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.internal.DefaultProcessingSessionTest;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionContent;
import com.oracle.coherence.patterns.processing.internal.SubmissionKey;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.tangosol.net.NamedCache;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;
import org.junit.Test;
import org.mockito.Matchers;

import static org.mockito.Mockito.*;

/**
 * A {@link DefaultProcessingSessionTest}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */

public class DefaultTaskProcessorTest
{
    /**
     * Ensure simple task processor execution.
     */
    @Test
    public void testStartup() throws InterruptedException
    {
        DefaultTaskProcessor  taskprocessor         = new DefaultTaskProcessor("Test processor", 1);
        TaskProcessorMediator taskProcessorMediator = mock(TaskProcessorMediator.class);
        TaskProcessorMediatorKey taskProcessorMediatorKey =
            new TaskProcessorMediatorKey(StringBasedIdentifier.newInstance("TPM"),
                                         1,
                                         new UID());
        ObjectProxyFactory<Submission>            submissionProxyFactory            = mock(ObjectProxyFactory.class);
        ObjectProxyFactory<SubmissionResult>      submissionResultProxyFactory      = mock(ObjectProxyFactory.class);
        ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorProxyFactory = mock(ObjectProxyFactory.class);
        ClientLeaseMaintainer                     clientLeaseMaintainer             = mock(ClientLeaseMaintainer.class);
        NamedCache                                taskProcessorCache                = mock(NamedCache.class);
        Submission                                submission                        = mock(Submission.class);
        SubmissionContent                         sp                                = mock(SubmissionContent.class);

        TestTask                                  task                              = new TestTask();
        SubmissionResult                          sr                                = mock(SubmissionResult.class);

        stub(sp.getPayload()).toReturn(task);
        stub(taskProcessorMediatorProxyFactory.getNamedCache()).toReturn(taskProcessorCache);
        stub(submissionProxyFactory.getProxy(Matchers.any())).toReturn(submission);
        stub(submissionResultProxyFactory.getProxy(Matchers.any())).toReturn(sr);
        stub(submission.getContent()).toReturn(sp);
        stub(sr.processingStarted(Matchers.any())).toReturn(true);

        SubmissionKeyPair taskToExecute = new SubmissionKeyPair(new SubmissionKey(null,
                                                                                  new UUID()),
                                                                StringBasedIdentifier.newInstance("test"));

        taskprocessor.onStartup(taskProcessorMediator,
                                taskProcessorMediatorKey,
                                submissionProxyFactory,
                                submissionResultProxyFactory,
                                taskProcessorMediatorProxyFactory,
                                clientLeaseMaintainer);
        taskprocessor.executeTask(taskToExecute);
        taskprocessor.onShutdown();
        Thread.currentThread().sleep(1000);
        verify(sr, never()).processingFailed(Matchers.any());
        verify(sr, never()).processingSucceeded(Matchers.any());

    }
}
