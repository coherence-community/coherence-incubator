/*
 * File: TaskProcessorMediator.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.patterns.processing.internal.SubmissionKey;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;

import java.util.List;
import java.util.Map;

/**
 * The {@link TaskProcessorMediator} represents a {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface TaskProcessorMediator
{
    /**
     * This method dequeues a task and returns the dequeued {@link SubmissionKeyPair}. The dequeued
     * task gets transferred to the tasks in progress list.
     *
     * @return the key pair for the task
     */
    SubmissionKeyPair dequeueTask();


    /**
     * This method drains the queue and returns all the tasks. All tasks are
     * transferred to the tasks in progress list.
     *
     * @return the list of Tasks to be queued
     */
    List<SubmissionKeyPair> drainQueueToBeExecuted();


    /**
     * The lease has expired for this {@link TaskProcessorMediator}.
     *
     * @param lease the Lease that has expired.
     */
    void leaseExpired(Lease lease);


    /**
     * Enqueue a particular task for this executor.
     *
     * @param taskId   the {@link SubmissionKey} of the Task to be enqueued.
     * @param resultid the unique identifier for the result.
     */
    void enqueueTask(final SubmissionKey taskId,
                     Identifier          resultid);


    /**
     * Returns the key of the executor this queue belongs to.
     *
     * @return the executor key.
     */
    TaskProcessorMediatorKey getTaskProcessorKey();


    /**
     * Returns the number of tasks in progress.
     *
     * @return the number of tasks in progress
     */
    int numberOfTasksInProgress();


    /**
     * Returns a list of {@link SubmissionKey}s to the tasks in progress.
     *
     * @return the list of {@link SubmissionKey}s to the tasks in progress.
     */
    List<SubmissionKeyPair> getTasksInProgress();


    /**
     * Returns the size of the queue (excluding tasks in progress).
     *
     * @return the size of the queue
     */
    int size();


    /**
     * When a task is done, it is removed from the tasks in progress list.
     *
     * @param oSubmissionKey the key to the task that was completed.
     * @param executionDuration the time it took to execute the task
     * @param yield true if the task is not finished, but has yielded
     */
    void taskDone(final SubmissionKey oSubmissionKey,
                  long                executionDuration,
                  boolean             yield);


    /**
     * Offer the task to the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} return true if accepted, false otherwise.
     *
     * @param submissionKey the key to the task.
     * @param resultIdentifier the key to the {@link com.oracle.coherence.patterns.processing.internal.SubmissionResult}
     * @return true if accepted, false otherwise
     */
    boolean offerTask(SubmissionKey submissionKey,
                      Identifier    resultIdentifier);


    /**
     * A {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} needs to have retrieved a {@link Lease}
     * before being able to claim a task.
     *
     * @param duration the requested duration of the lease
     *
     * @return the {@link Lease}
     */
    public Lease getTaskProcessorLease(long duration);


    /**
     * Called to extend the lease. Note that a {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} must
     * extend the lease before it expires. Expiry of the lease means
     * that the claimed tasks gets revoked.
     *
     * @param duration the duration of the extension.
     *
     * @return true if successful, false otherwise
     */
    public boolean extendTaskProcessorLease(long duration);


    /**
     * Returns the current {@link TaskProcessorStateEnum} of the {@link TaskProcessorMediator}.
     *
     * @return the {@link TaskProcessorStateEnum} for this {@link TaskProcessorMediator}
     */
    public TaskProcessorStateEnum getProcessorState();


    /**
     * Returns an attribute map with attributes associated with the TaskProcessor.
     *
     * @return the attribute map.
     */
    @SuppressWarnings({"rawtypes"})
    public Map getAttributeMap();


    /**
     * Set an attribute for the {@link TaskProcessorMediator}.
     *
     * @param key   the key
     * @param value the value
     */
    public void setAttribute(Object key,
                             Object value);


    /**
     * Called if this entry has arrived to this node due to partitions moving or failover.
     */
    void entryArrived();


    /**
     * Called if this entry has departed from this node due to partitions moving or failover.
     */
    void entryDeparted();
}
