/*
 * File: TaskRunner.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.util.ObjectChangeCallback;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.SubmissionState;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionContent;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.task.ResumableTask;
import com.oracle.coherence.patterns.processing.task.TaskExecutionEnvironment;
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.oracle.coherence.patterns.processing.task.Yield;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link TaskRunner} is responsible for claiming ownership of, running and returning the result of an executed
 * {@link com.oracle.coherence.patterns.processing.task.Task}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class TaskRunner implements Runnable, TaskExecutionEnvironment, ObjectChangeCallback<SubmissionResult>
{
    /**
     * The {@link Logger} to use.
     */
    private static final Logger logger = Logger.getLogger(TaskRunner.class.getName());

    /**
     * The {@link TaskProcessorMediator} that this {@link TaskRunner} is executing on behalf of.
     */
    private final TaskProcessorMediator taskProcessorMediator;

    /**
     * The {@link SubmissionKeyPair} identifying the {@link Submission}.
     */
    private final SubmissionKeyPair pendingTaskKeyPair;

    /**
     * A boolean indicating whether we are resuming a task.
     */
    private boolean m_bIsResuming;

    /**
     * The {@link Submission} to run.
     */
    private final Submission submission;

    /**
     * The {@link SubmissionResult} to report the result to.
     */
    private SubmissionResult submissionResult;

    /**
     * The {@link ObjectProxyFactory} for a {@link SubmissionResult}.
     */
    private final ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;

    /**
     * The {@link Identifier} pointing to the {@link SubmissionResult}.
     */
    private Identifier submissionResultIdentifier;

    /**
     * The thread of execution.
     */
    private volatile Thread executionThread;

    /**
     * Whether the current job has been cancelled during execution.
     *  of Submissions offered to this {@link TaskExecutor}.
     */
    private transient volatile boolean isCancelled;

    /**
     * The {@link TaskProcessorMediatorKey} identifying the {@link TaskProcessor} processing this task.
     */
    private TaskProcessorMediatorKey taskProcessorMediatorKey;


    /**
     * Constructor for the {@link TaskRunner}.
     *
     * @param pendingTaskKeyPair     the {@link SubmissionKeyPair} identifying the {@link Submission} to be executed
     * @param taskProcessorMediator the {@link TaskProcessorMediator} which we are running on behalf of
     * @param submission         the {@link Submission} to run
     * @param submissionResultIdentifier the {@link Identifier} identifying the {@link SubmissionResult}
     * @param submissionResultProxyFactory   the {@link ObjectProxyFactory} for {@link SubmissionResult}s to use
     * @param taskProcessorMediatorKey  the {@link TaskProcessorMediatorKey} identifying the {@link TaskProcessor}
     */
    public TaskRunner(final SubmissionKeyPair                    pendingTaskKeyPair,
                      final TaskProcessorMediator                taskProcessorMediator,
                      final Submission                           submission,
                      final Identifier                           submissionResultIdentifier,
                      final ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory,
                      TaskProcessorMediatorKey                   taskProcessorMediatorKey)
    {
        this.pendingTaskKeyPair           = pendingTaskKeyPair;
        this.taskProcessorMediator        = taskProcessorMediator;
        this.submissionResultProxyFactory = submissionResultProxyFactory;
        this.submissionResultIdentifier   = submissionResultIdentifier;
        this.submission                   = submission;
        this.submissionResult             = null;
        this.m_bIsResuming                = false;
        this.isCancelled                  = false;
        this.taskProcessorMediatorKey     = taskProcessorMediatorKey;
    }


    /**
     * {@inheritDoc}
     */
    public Object loadCheckpoint()
    {
        return submissionResult.getResult();
    }


    /**
     * {@inheritDoc}
     */
    public void reportProgress(final Object oProgress)
    {
        submissionResult.setProgress(oProgress);
    }


    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(final Object intermediateState)
    {
        submissionResult.setResult(intermediateState);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isResuming()
    {
        return m_bIsResuming;
    }


    /**
     * {@inheritDoc}
     */
    public void run()
    {
        submissionResult = submissionResultProxyFactory.getProxy(submissionResultIdentifier);

        try
        {
            // Make note of current thread in order to be able to interrupt
            this.executionThread = Thread.currentThread();
            submissionResultProxyFactory.registerChangeCallback(submissionResult, this);

            ResumableTask     taskToExecute     = null;

            SubmissionContent submissionContent = submission.getContent();

            if (submissionContent.getPayload() instanceof ResumableTask)
            {
                taskToExecute = (ResumableTask) submissionContent.getPayload();
            }

            if (taskToExecute == null)
            {
                throw new UnsupportedOperationException(String.format("Can't execute %s as it's not a Task",
                                                                      pendingTaskKeyPair));
            }
            else
            {
                try
                {
                    // determine if the current state is resuming, then we set the isResuming flag
                    if (submissionResult.isResuming())
                    {
                        m_bIsResuming = true;
                    }

                    doExecuteTask(submissionResult,
                                  submission,
                                  taskToExecute,
                                  submissionContent.getSubmissionConfiguration());
                }
                catch (final Exception exception)
                {
                    if (logger.isLoggable(Level.WARNING))
                    {
                        logger.log(Level.WARNING,
                                   "TaskRunner - Failed to process {0} due to:\n{1}",
                                   new Object[] {pendingTaskKeyPair, exception});
                    }

                    setProcessingFailed(submissionResult, exception);
                }

            }

        }
        finally
        {
            submissionResultProxyFactory.unregisterChangeCallback(submissionResult, this);

        }
    }


    /**
     * Check the result of the run.
     *
     * @param submissionResult  the {@link SubmissionResult} to report to.
     * @param submission        the {@link Submission} that was run.
     * @param oRequestData      the {@link SubmissionConfiguration} that was in effect.
     * @param oResult           the result object
     * @param executionDuration the duration of the execution in milliseconds
     */
    private void checkResult(SubmissionResult              submissionResult,
                             Submission                    submission,
                             final SubmissionConfiguration oRequestData,
                             final Object                  oResult,
                             long                          executionDuration)
    {
        if (!isCancelled)
        {
            if (oResult instanceof Yield)
            {
                Yield yieldresult = (Yield) oResult;

                yield(submissionResult,
                      submission,
                      yieldresult.getIntermediateState(),
                      yieldresult.getDelay(),
                      executionDuration);
            }
            else
            {
                if (setProcessingSucceeded(submissionResult, oResult, executionDuration))
                {
                    if (logger.isLoggable(Level.FINER))
                    {
                        logger.log(Level.FINER,
                                   "Executed {0} to produce {1}",
                                   new Object[] {pendingTaskKeyPair, oResult});
                    }
                }
                else
                {
                    if (logger.isLoggable(Level.FINER))
                    {
                        logger.log(Level.FINER,
                                   "Failed to set processing to executed for task {0}",
                                   pendingTaskKeyPair);
                    }

                }
            }
        }
    }


    /**
     * Execute the task.
     *
     * @param submissionResult the {@link SubmissionResult} to report to
     * @param submission       the {@link Submission} that we are operating on
     * @param task             the {@link ResumableTask} to run
     * @param configuration    the {@link SubmissionConfiguration} that was in effect
     *
     * @return true if successful, false otherwise
     */
    private boolean doExecuteTask(SubmissionResult              submissionResult,
                                  Submission                    submission,
                                  final ResumableTask           task,
                                  final SubmissionConfiguration configuration)
    {
        if (setProcessingStarted(submissionResult))
        {
            long         startTime = System.currentTimeMillis();
            final Object oResult   = task.run(this);

            if (Thread.interrupted())    // Note: calling this clears the flag
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "The Task {0} was cancelled during execution",
                               new Object[] {pendingTaskKeyPair});
                }
            }

            long executionDuration = System.currentTimeMillis() - startTime;

            checkResult(submissionResult, submission, configuration, oResult, executionDuration);

            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Sets the execution result to failed.
     *
     * @param submissionResult the {@link SubmissionResult} to set.
     * @param oException       the result of the execution.
     *
     * @return whether it was possible to set the SubmissionResult to failed.
     */
    private boolean setProcessingFailed(SubmissionResult submissionResult,
                                        final Exception  oException)
    {
        taskProcessorMediator.taskDone(pendingTaskKeyPair.getKey(), 0, false);

        return submissionResult.processingFailed(oException);
    }


    /**
     * Sets the Submission state to started.
     *
     * @param submissionResult the {@link SubmissionResult} to set.
     * @return true if it was possible to set the {@link SubmissionResult} to started.
     */
    private boolean setProcessingStarted(SubmissionResult submissionResult)
    {
        return submissionResult.processingStarted(taskProcessorMediatorKey);
    }


    /**
     * Store the successful result and sets the SubmissionState to DONE.
     *
     * @param submissionResult  the {@link SubmissionResult} to set.
     * @param oResult           the successful result
     * @param executionDuration the duration of the execution in milliseconds
     *
     * @return true if it was possible to set the {@link SubmissionResult} to DONE.
     */
    private boolean setProcessingSucceeded(SubmissionResult submissionResult,
                                           final Object     oResult,
                                           long             executionDuration)
    {
        taskProcessorMediator.taskDone(pendingTaskKeyPair.getKey(), executionDuration, false);

        Boolean result = submissionResult.processingSucceeded(oResult);

        return result;
    }


    /**
     * Yield and store away the intermediate state.
     *
     * @param submissionResult  the {@link SubmissionResult} to set to SUSPENDED
     * @param submission        the {@link Submission} that shall be reDispatched
     * @param intermediateState is the intermediate state to store away when yielding
     * @param delay             the minimum delay until we restart execution
     * @param executionDuration the duration of the execution in milliseconds
     *
     * @return true if yield is successful
     */
    private boolean yield(SubmissionResult submissionResult,
                          Submission       submission,
                          final Object     intermediateState,
                          long             delay,
                          long             executionDuration)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Yielding {0}", pendingTaskKeyPair);
        }

        taskProcessorMediator.taskDone(pendingTaskKeyPair.getKey(), executionDuration, true);

        boolean result = submissionResult.suspendExecution(intermediateState);

        submission.reDispatch(delay);

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void objectChanged(SubmissionResult object)
    {
        if (object != null)
        {
            if (object.getSubmissionState() == SubmissionState.CANCELLED)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Received change of state to CANCELLED - interrupting execution of {0}",
                               new Object[]
                    {
                    });
                }

                interruptExecution();
            }
        }

    }


    /**
     * Interrupts the execution.
     */
    public void interruptExecution()
    {
        isCancelled = true;

        if (executionThread != null)
        {
            executionThread.interrupt();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void objectCreated(SubmissionResult object)
    {
        // Do nothing
    }


    /**
     * {@inheritDoc}
     */
    public void objectDeleted(Object key)
    {
        // Do nothing
    }
}
