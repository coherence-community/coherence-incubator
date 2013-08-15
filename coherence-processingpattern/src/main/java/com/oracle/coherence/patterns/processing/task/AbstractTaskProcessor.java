/*
 * File: AbstractTaskProcessor.java
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

import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract Executor class helping the implementation of an {@link TaskProcessor}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public abstract class AbstractTaskProcessor implements TaskProcessor, ExternalizableLite, PortableObject
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(AbstractTaskProcessor.class.getName());

    /**
     * The default duration of the lease we request.
     */
    private static int LEASE_DURATION = 60 * 1000;

    /**
     * The {@link TaskProcessorMediatorKey} used as a key for the
     * {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator} cache.
     */
    private TaskProcessorMediatorKey taskProcessorKey;

    /**
     * The display name of the {@link TaskProcessor}.
     */
    private String displayName;

    /**
     * A factory to create proxy objects for {@link TaskProcessorMediator}
     * objects living in a distributed cache.
     */
    private transient ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorProxyFactory;

    /**
     * The {@link MapListener} for the {@link TaskProcessorMediator} cache.
     */
    private transient MapListener taskProcessorMediatorListener;

    /**
     * The {@link ObjectProxyFactory} to use for {@link Submission}s.
     */
    private transient ObjectProxyFactory<Submission> submissionProxyFactory;

    /**
     * The {@link ObjectProxyFactory} to use for {@link SubmissionResult}s.
     */
    private transient ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;

    /**
     * The {@link TaskProcessorMediator} object that we interact with.
     */
    private transient TaskProcessorMediator taskProcessorMediator;

    /**
     * A {@link ClientLeaseMaintainer} heartbeats (extends) the
     * {@link com.oracle.coherence.common.leasing.Lease} that
     * this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}
     * is dependent on.
     */
    private transient ClientLeaseMaintainer clientLeaseMaintainer;

    /**
     * Tracks whether we have received the onStartup call.
     * If we haven't we should not call executeTask yet.
     */
    private transient volatile boolean isStarted;


    /**
     * Default constructor.
     */
    public AbstractTaskProcessor()
    {
    }


    /**
     * Standard constructor.
     *
     * @param taskProcessorKey the {@link TaskProcessorMediatorKey}
     * @param displayName the readable name for the executor
     */
    public AbstractTaskProcessor(final TaskProcessorMediatorKey taskProcessorKey,
                                 final String                   displayName)
    {
        this.displayName      = displayName;
        this.taskProcessorKey = taskProcessorKey;
        this.isStarted        = false;
    }


    /**
     * Execute a particular task.
     *
     * @param taskToExecute the {@link SubmissionKeyPair} to execute
     */
    public abstract void executeTask(final SubmissionKeyPair taskToExecute);


    /**
     * Getter for displayName.
     *
     * @return the displayName
     */
    public String getDisplayName()
    {
        return displayName;
    }


    /**
     * Getter for the {@link ObjectProxyFactory} for {@link TaskProcessorMediator}s.
     *
     * @return the {@link ObjectProxyFactory} for {@link TaskProcessorMediator}s
     */
    public ObjectProxyFactory<TaskProcessorMediator> getTaskProcessorMediatorProxyFactory()
    {
        return taskProcessorMediatorProxyFactory;
    }


    /**
     * Getter for the {@link ObjectProxyFactory} for {@link Submission}s.
     *
     * @return the {@link ObjectProxyFactory} for {@link Submission}s
     */
    public ObjectProxyFactory<Submission> getSubmissionProxyFactory()
    {
        return submissionProxyFactory;
    }


    /**
     * Getter for the {@link ObjectProxyFactory} for {@link SubmissionResult}s.
     *
     * @return the {@link ObjectProxyFactory} for {@link SubmissionResult}s
     */
    public ObjectProxyFactory<SubmissionResult> getSubmissionResultProxyFactory()
    {
        return submissionResultProxyFactory;
    }


    /**
     * Getter for the taskProcessorMediator.
     *
     * @return the {@link TaskProcessorMediator}
     */
    public TaskProcessorMediator getTaskProcessorMediator()
    {
        return taskProcessorMediator;
    }


    /**
     * Sets the {@link TaskProcessorMediatorKey}.
     *
     * @param taskProcessorKey the new key
     */
    public void setTaskProcessorKey(final TaskProcessorMediatorKey taskProcessorKey)
    {
        this.taskProcessorKey = taskProcessorKey;
    }


    /**
     * Gets the {@link TaskProcessorMediatorKey}.
     *
     * @return the {@link TaskProcessorMediatorKey}
     */
    public TaskProcessorMediatorKey getTaskProcessorKey()
    {
        return taskProcessorKey;
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return displayName;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final String result = "TaskProcessor name:" + displayName;

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void onStartup(TaskProcessorMediator                     taskProcessorMediator,
                          TaskProcessorMediatorKey                  key,
                          ObjectProxyFactory<Submission>            submissionProxyFactory,
                          ObjectProxyFactory<SubmissionResult>      submissionResultProxyFactory,
                          ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorProxyFactory,
                          ClientLeaseMaintainer                     clientLeaseMaintainer)
    {
        this.taskProcessorMediator = taskProcessorMediator;
        this.setTaskProcessorKey(key);
        this.taskProcessorMediatorProxyFactory = taskProcessorMediatorProxyFactory;
        this.submissionProxyFactory            = submissionProxyFactory;
        this.submissionResultProxyFactory      = submissionResultProxyFactory;
        this.clientLeaseMaintainer             = clientLeaseMaintainer;

        try
        {
            clientLeaseMaintainer.addLease(getTaskProcessorKey(),
                                           taskProcessorMediator.getTaskProcessorLease(LEASE_DURATION));
        }
        catch (Throwable e)
        {
            logger.log(Level.SEVERE, "TaskProcessor unable to add lease", e);

            throw Base.ensureRuntimeException(e);
        }

        initializeTaskProcessorMediatorListener();
        isStarted = true;
        drainQueue();
    }


    /**
     * Initializes the {@link TaskProcessorMediator} listener.
     */
    private void initializeTaskProcessorMediatorListener()
    {
        taskProcessorMediatorProxyFactory.getNamedCache().addMapListener(taskProcessorMediatorListener =
            new MultiplexingMapListener()
        {
            @Override
            protected void onMapEvent(final MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED)
                {
                    if (isStarted)
                    {
                        dequeueTaskLocally();
                    }
                }
                else
                {
                    if (mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                    {
                        if (isStarted)
                        {
                            dequeueTaskLocally();
                        }
                    }
                }
            }
        }, getTaskProcessorKey(), true);
    }


    /**
     * Dequeues a task and submits it for execution.
     */
    private void dequeueTaskLocally()
    {
        if (isStarted)
        {
            final SubmissionKeyPair taskInProgress = taskProcessorMediator.dequeueTask();

            executeTask(taskInProgress);
        }
    }


    /**
     * Drains the queue and submits all Submissions for execution.
     */
    private void drainQueue()
    {
        if (isStarted)
        {
            final List<SubmissionKeyPair> toBeExecuted = taskProcessorMediator.drainQueueToBeExecuted();

            if (toBeExecuted != null)
            {
                for (final SubmissionKeyPair taskInProgress : toBeExecuted)
                {
                    executeTask(taskInProgress);
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void onShutdown()
    {
        isStarted = false;
    }


    /**
     * {@inheritDoc}
     *
     */
    public void readExternal(final DataInput in) throws IOException
    {
        taskProcessorKey = (TaskProcessorMediatorKey) ExternalizableHelper.readObject(in);
        displayName      = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     *
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, taskProcessorKey);
        ExternalizableHelper.writeSafeUTF(out, displayName);
    }


    /**
     * {@inheritDoc}
     *
     */
    public void readExternal(final PofReader reader) throws IOException
    {
        taskProcessorKey = (TaskProcessorMediatorKey) reader.readObject(0);
        displayName      = reader.readString(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final PofWriter writer) throws IOException
    {
        writer.writeObject(0, taskProcessorKey);
        writer.writeString(1, displayName);
    }
}
