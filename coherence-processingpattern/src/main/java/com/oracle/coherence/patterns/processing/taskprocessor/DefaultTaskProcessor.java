/*
 * File: DefaultTaskProcessor.java
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

import com.oracle.coherence.common.threading.ExecutorListener;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ObservableExecutor;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.task.AbstractTaskProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link DefaultTaskProcessor} executes {@link com.oracle.coherence.patterns.processing.task.ResumableTask}s on the
 * node.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */

@SuppressWarnings("serial")
public class DefaultTaskProcessor extends AbstractTaskProcessor implements ExternalizableLite,
                                                                           PortableObject,
                                                                           ExecutorListener
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(DefaultTaskProcessor.class.getName());

    /**
     * The number of threads to use when executing.
     */
    private int numberOfThreads;

    /**
     * The {@link ExecutorService} used to execute the tasks.
     */
    private transient ExecutorService executorService;

    /**
     * The currently executing {@link Runnable}s.
     */
    private ConcurrentLinkedQueue<Runnable> currentlyExecuting;


    /**
     * The default Constructor.
     */
    public DefaultTaskProcessor()
    {
        super();
        currentlyExecuting = new ConcurrentLinkedQueue<Runnable>();
    }


    /**
     * Constructor taking parameters.
     *
     * @param name the display name of the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}
     * @param numberOfThreads the number of threads that this
     *        {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} uses to execute jobs on a particular
     *        node
     */
    public DefaultTaskProcessor(final String name,
                                final int    numberOfThreads)
    {
        super(null, name);
        currentlyExecuting   = new ConcurrentLinkedQueue<Runnable>();
        this.numberOfThreads = numberOfThreads;
    }


    /**
     * {@inheritDoc}
     */
    public void onStartup(TaskProcessorMediator                     taskProcessorMediator,
                          TaskProcessorMediatorKey                  taskProcessorMediatorKey,
                          ObjectProxyFactory<Submission>            submissionProxyFactory,
                          ObjectProxyFactory<SubmissionResult>      submissionResultProxyFactory,
                          ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorProxyFactory,
                          ClientLeaseMaintainer                     clientLeaseMaintainer)
    {
        executorService =
            ExecutorServiceFactory
                .newFixedThreadPool(numberOfThreads,
                                    ThreadFactories
                                        .newThreadFactory(true, "DefaultTaskProcessor-" + taskProcessorMediatorKey,
                                                          null));

        if (executorService instanceof ObservableExecutor)
        {
            ((ObservableExecutor) executorService).setCallback(this);
        }

        super.onStartup(taskProcessorMediator,
                        taskProcessorMediatorKey,
                        submissionProxyFactory,
                        submissionResultProxyFactory,
                        taskProcessorMediatorProxyFactory,
                        clientLeaseMaintainer);

        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO,
                       "Starting TaskProcessor {0} with {1} thread(s)",
                       new Object[] {getDisplayName(), numberOfThreads});
        }
    }


    /**
     * {@inheritDoc}
     */
    public void onShutdown()
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Shutting down TaskProcessor {0}", this);
        }

        super.onShutdown();

        // Notify all running TaskRunners that we should shutdown
        for (Runnable r : currentlyExecuting)
        {
            if (r instanceof TaskRunner)
            {
                ((TaskRunner) r).interruptExecution();
            }
        }

        if (executorService != null)
        {
            executorService.shutdownNow();
        }
    }


    /**
     * Execute a particular task.
     *
     * @param taskToExecute the {@link SubmissionKeyPair} to execute
     */
    public void executeTask(final SubmissionKeyPair taskToExecute)
    {
        if (taskToExecute != null)
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "TaskProcessor {0} is executing {1}", new Object[] {this, taskToExecute});
            }

            Submission submission = getSubmissionProxyFactory().getProxy(taskToExecute.getKey());
            TaskRunner task = new TaskRunner(taskToExecute,
                                             getTaskProcessorMediator(),
                                             submission,
                                             taskToExecute.getResultId(),
                                             getSubmissionResultProxyFactory(),
                                             getTaskProcessorKey());

            currentlyExecuting.add(task);
            executorService.execute(task);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "DefaultTaskProcessor [numberOfThreads=" + numberOfThreads + ", "
               + (getDisplayName() != null ? "getDisplayName()=" + getDisplayName() + ", " : "")
               + (getTaskProcessorKey() != null ? "getTaskProcessorKey()=" + getTaskProcessorKey() : "") + "]";
    }


    /**
     * {@inheritDoc}
     */
    public void afterExecute(Runnable  runnable,
                             Throwable t)
    {
        currentlyExecuting.remove(runnable);
    }


    /**
     * {@inheritDoc}
     */
    public void beforeExecute(Runnable runnable)
    {
        // Task is added to currentlyExecuting when submitted to the {@link ExecutorService}
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final DataInput in) throws IOException
    {
        super.readExternal(in);
        numberOfThreads = ExternalizableHelper.readInt(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeInt(out, numberOfThreads);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final PofReader reader) throws IOException
    {
        super.readExternal(reader);
        numberOfThreads = reader.readInt(10);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeInt(10, numberOfThreads);
    }
}
