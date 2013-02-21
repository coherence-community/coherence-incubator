/*
 * File: ExecutorQueueTest.java
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

package com.oracle.coherence.patterns.processing;

import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.patterns.processing.internal.SubmissionKey;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.ServerLeaseMonitor;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * ExecutorQueueTest tests the ExecutorQueue.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ExecutorQueueTest
{
    /**
     * Initialize the test.
     */
    @Before
    public void initialize()
    {
        ServerLeaseMonitor leaseMonitor = EasyMock.createMock(ServerLeaseMonitor.class);

        leaseMonitor.registerLease(EasyMock.isA(TaskProcessorMediatorKey.class), EasyMock.isA(Lease.class));
        DefaultTaskProcessorMediator.setLeaseMonitor(leaseMonitor);

    }


    /**
     * Tests that a newly created queue is empty.
     */
    @Test
    public void testExecutorQueue()
    {
        TaskProcessorMediator queue = new DefaultTaskProcessorMediator();

        assertTrue(queue.size() == 0);
    }


    /**
     * Testing enqueueing.
     */
    @Test
    public void testEnqueue()
    {
        SubmissionKey         key   = new SubmissionKey(null, new UUID());
        TaskProcessorMediator queue = new DefaultTaskProcessorMediator();

        queue.enqueueTask(key, null);

        Lease lease = queue.getTaskProcessorLease(10000);

        assertTrue(queue.size() == 1);
        assertTrue(queue.dequeueTask().getKey().equals(key));
        assertTrue(queue.size() == 0);

    }


    /**
     * Tests enqueueing multiple tasks.
     */
    @Test
    public void testEnqueueMultiple()
    {
        SubmissionKey         key   = new SubmissionKey(null, new UUID());
        TaskProcessorMediator queue = new DefaultTaskProcessorMediator();
        Lease                 lease = queue.getTaskProcessorLease(60000);

        queue.enqueueTask(key, null);

        SubmissionKey secondkey = new SubmissionKey(null, new UUID());

        queue.enqueueTask(secondkey, null);
        assertTrue(queue.size() == 2);
        assertTrue(queue.dequeueTask().getKey().equals(key));
        assertTrue(queue.dequeueTask().getKey().equals(secondkey));
        assertTrue(queue.size() == 0);

    }


    /**
     * Tests draining a queue.
     */
    @Test
    public void testDrainMultiple()
    {
        TaskProcessorMediator queue =
            new DefaultTaskProcessorMediator(new TaskProcessorMediatorKey(StringBasedIdentifier.newInstance("Test"),
                                                                          0,
                                                                          new UID()));
        Lease         lease = queue.getTaskProcessorLease(10000);
        SubmissionKey key   = new SubmissionKey(null, new UUID());

        queue.enqueueTask(key, null);

        SubmissionKey secondkey = new SubmissionKey(null, new UUID());

        queue.enqueueTask(secondkey, null);
        assertTrue(queue.size() == 2);

        List<SubmissionKeyPair> drain = queue.drainQueueToBeExecuted();

        assertTrue(drain.get(0).getKey().equals(key));
        drain.remove(0);
        assertTrue(drain.get(0).getKey().equals(secondkey));
        drain.remove(0);
        assertTrue(queue.size() == 0);
    }


    /**
     * Testing enqueue, drain and taskdone.
     */
    @Test
    public void testTaskDone()
    {
        ServerLeaseMonitor leaseMonitor = EasyMock.createMock(ServerLeaseMonitor.class);

        leaseMonitor.registerLease(EasyMock.isA(TaskProcessorMediatorKey.class), EasyMock.isA(Lease.class));
        DefaultTaskProcessorMediator.setLeaseMonitor(leaseMonitor);

        SubmissionKey key = new SubmissionKey(null, new UUID());
        TaskProcessorMediator queue =
            new DefaultTaskProcessorMediator(new TaskProcessorMediatorKey(StringBasedIdentifier.newInstance("Test"),
                                                                          0,
                                                                          new UID()));
        Lease lease = queue.getTaskProcessorLease(1000);

        EasyMock.replay(leaseMonitor);

        queue.enqueueTask(key, null);
        assertTrue(queue.size() == 1);

        SubmissionKeyPair taskInProgress = queue.dequeueTask();

        assertTrue(queue.size() == 0);
        assertTrue(queue.numberOfTasksInProgress() == 1);
        queue.taskDone(taskInProgress.getKey(), 1000, false);
        assertTrue(queue.numberOfTasksInProgress() == 0);
    }


    /**
     * Testing a named queue.
     */
    @Test
    public void testNamedQueue()
    {
        UID    uid       = new UID();
        String uidstring = uid.toString();
        TaskProcessorMediator queue =
            new DefaultTaskProcessorMediator(new TaskProcessorMediatorKey(StringBasedIdentifier
                .newInstance("testqueue"),
                                                                          1,
                                                                          uid));
        TaskProcessorMediatorKey ident = queue.getTaskProcessorKey();

        assertTrue(ident.toString().equals("TPSK:{Identifier{testqueue},1," + uidstring + "}"));
    }


    /**
     * Testing getting all tasks in progress.
     */
    @Test
    public void testGetTasksInProgress()
    {
        ServerLeaseMonitor leaseMonitor = EasyMock.createMock(ServerLeaseMonitor.class);

        leaseMonitor.registerLease(EasyMock.isA(TaskProcessorMediatorKey.class), EasyMock.isA(Lease.class));
        DefaultTaskProcessorMediator.setLeaseMonitor(leaseMonitor);

        EasyMock.replay(leaseMonitor);

        SubmissionKey key = new SubmissionKey(null, new UUID());
        TaskProcessorMediator queue =
            new DefaultTaskProcessorMediator(new TaskProcessorMediatorKey(StringBasedIdentifier
                .newInstance("testqueue"),
                                                                          1,
                                                                          new UID()));
        Lease lease = queue.getTaskProcessorLease(1000);

        queue.enqueueTask(key, null);
        queue.dequeueTask();

        List<SubmissionKeyPair> keylist   = queue.getTasksInProgress();

        SubmissionKeyPair       firstTask = keylist.get(0);

        assertTrue(key.equals(firstTask.getKey()));
    }
}
