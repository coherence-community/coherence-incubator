/*
 * File: LocalExecutorDispatcher.java
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

package com.oracle.coherence.patterns.processing.dispatchers.local;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.common.util.ObjectChangeCallback;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.SubmissionState;
import com.oracle.coherence.patterns.processing.dispatchers.AbstractDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchOutcome;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionContent;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A {@link LocalExecutorDispatcher} is a
 * {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher} that
 * will dispatch standard Java {@link Runnable}s and {@link Callable}s to be
 * executed locally (in the JVM that is hosting {@link LocalExecutorDispatcher})
 * by an {@link ExecutorService}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class LocalExecutorDispatcher extends AbstractDispatcher implements ExternalizableLite,
                                                                           PortableObject,
                                                                           LocalExecutorDispatcherMBean
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(LocalExecutorDispatcher.class.getName());

    /**
     * The {@link ExecutorService} that will manage and perform the execution of
     * {@link PendingSubmission}s dispatched
     * by this {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}.
     */
    private transient ExecutorService executorService;

    /**
     * The {@link ObjectProxyFactory} for {@link Submission}s that is to be used.
     */
    private transient ObjectProxyFactory<Submission> submissionProxyFactory;

    /**
     * The {@link ObjectProxyFactory} for {@link SubmissionResult}s that is to be used.
     */
    private transient ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;

    /**
     * The number of Submissions offered to this {@link LocalExecutorDispatcher}.
     */
    private transient int noOfferedSubmissions;

    /**
     * The number of Submissions accepted by this {@link LocalExecutorDispatcher}.
     */
    private transient int noAcceptedSubmissions;

    /**
     * The {@link Environment} to use.
     */
    private transient Environment environment;

    /**
     * The number of threads in the thread pool.
     */
    private int numberOfThreads;


    /**
     * Standard Constructor.
     * <p>
     * NOTE: Also required to support {@link com.tangosol.io.ExternalizableLite} and
     * {@link com.tangosol.io.pof.PortableObject}.
     */
    public LocalExecutorDispatcher()
    {
    }


    /**
     * Constructor that includes the {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher} name.
     *
     * @param environment     the {@link Environment} for this {@link LocalExecutorDispatcher}.
     * @param sName           the name of this {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}
     * @param numberOfThreads the number of threads in the thread pool for this {@link LocalExecutorDispatcher}
     */
    @SuppressWarnings("unchecked")
    public LocalExecutorDispatcher(Environment  environment,
                                   final String sName,
                                   int          numberOfThreads)
    {
        super(sName);
        this.environment = environment;
        this.submissionResultProxyFactory =
            (ObjectProxyFactory<SubmissionResult>) environment.getResource(SubmissionResult.class);
        this.submissionProxyFactory = (ObjectProxyFactory<Submission>) environment.getResource(Submission.class);
        this.numberOfThreads        = numberOfThreads;
    }


    /**
     * {@inheritDoc}
     */
    public DispatchOutcome dispatch(final PendingSubmission oPendingProcess)
    {
        noOfferedSubmissions++;

        if (oPendingProcess.getPayload() instanceof Runnable || oPendingProcess.getPayload() instanceof Callable)
        {
            Submission remoteSubmission = submissionProxyFactory.getProxy(oPendingProcess.getSubmissionKey());

            // submit the pending process to the local executor to execute
            executorService.execute(new ProcessRunner(submissionResultProxyFactory,
                                                      new SubmissionKeyPair(oPendingProcess.getSubmissionKey(),
                                                                            oPendingProcess.getResultIdentifier()),
                                                      oPendingProcess.getResultIdentifier(),
                                                      remoteSubmission));
            noAcceptedSubmissions++;

            return DispatchOutcome.ACCEPTED;
        }
        else
        {
            // we can't handle this type of pending process
            return DispatchOutcome.REJECTED;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown(final DispatchController dispatchController)
    {
        // unregisterMBean();
        if (executorService != null)
        {
            executorService.shutdownNow();
        }

        super.onShutdown(dispatchController);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onStartup(final DispatchController dispatchController)
    {
        this.environment = (Environment) dispatchController.getConfigurableCacheFactory();
        submissionResultProxyFactory =
            (ObjectProxyFactory<SubmissionResult>) environment.getResource(SubmissionResult.class);
        submissionProxyFactory = (ObjectProxyFactory<Submission>) environment.getResource(Submission.class);
        super.onStartup(dispatchController);
        registerMBean();
        this.executorService =
            ExecutorServiceFactory.newFixedThreadPool(numberOfThreads,
                                                      ThreadFactories.newThreadFactory(true, "LocalExecutorDispatcher",
                                                                                       null));

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Starting with a Thread pool size of {0}", numberOfThreads);
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getDispatcherName()
    {
        return getName();
    }


    /**
     * {@inheritDoc}
     */
    public int getSubmissionsAccepted()
    {
        return noAcceptedSubmissions;
    }


    /**
     * {@inheritDoc}
     */
    public int getSubmissionsOffered()
    {
        return noOfferedSubmissions;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final DataInput in) throws IOException
    {
        super.readExternal(in);
        numberOfThreads = in.readInt();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final DataOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(numberOfThreads);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final PofReader oReader) throws IOException
    {
        super.readExternal(oReader);
        numberOfThreads = oReader.readInt(10);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final PofWriter oWriter) throws IOException
    {
        super.writeExternal(oWriter);
        oWriter.writeInt(10, numberOfThreads);
    }


    /**
     * A {@link ProcessRunner} is responsible for claiming ownership of, running and returning the result of an executed
     * {@link PendingSubmission}.
     */
    public static class ProcessRunner implements Runnable, ObjectChangeCallback<SubmissionResult>
    {
        /**
         * The {@link Logger} to use.
         */
        private static Logger logger = Logger.getLogger(ProcessRunner.class.getName());

        /**
         * The {@link Identifier} to report the result to.
         */
        private Identifier submissionResultIdentifier;

        /**
         * The {@link Submission} to execute.
         */
        private Submission submission;

        /**
         * The {@link SubmissionKeyPair} identifying the {@link Submission} and {@link SubmissionResult}.
         */
        private SubmissionKeyPair submissionKeyPair;

        /**
         * The thread of execution.
         */
        private Thread executionThread;

        /**
         * The {@link ObjectProxyFactory} for {@link SubmissionResult}s to use.
         */
        private ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;

        /**
         * Whether the current job has been cancelled during execution.
         *  of Submissions offered to this {@link LocalExecutorDispatcher}.
         */
        private transient volatile boolean isCancelled;


        /**
         * Constructor taking the {@link Submission} and the {@link SubmissionResult}.
         *
         * @param submissionResultProxyFactory the {@link ObjectProxyFactory} for the {@link SubmissionResult}s
         * @param submissionKeyPair the {@link SubmissionKeyPair} for the {@link Submission} and
         *        {@link SubmissionResult}
         * @param submissionResultIdentifier the {@link Identifier} pointing to the {@link SubmissionResult}
         * @param submission the {@link Submission} to run
         */
        public ProcessRunner(final ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory,
                             final SubmissionKeyPair                    submissionKeyPair,
                             final Identifier                           submissionResultIdentifier,
                             final Submission                           submission)
        {
            this.submissionResultProxyFactory = submissionResultProxyFactory;
            this.submissionResultIdentifier   = submissionResultIdentifier;
            this.submission                   = submission;
            this.submissionKeyPair            = submissionKeyPair;
            isCancelled                       = false;
        }


        /**
         * {@inheritDoc}
         */
        public void run()
        {
            SubmissionResult submissionResult = submissionResultProxyFactory.getProxy(submissionResultIdentifier);

            try
            {
                // Make note of current thread in order to be able to interrupt
                this.executionThread = Thread.currentThread();

                submissionResultProxyFactory.registerChangeCallback(submissionResult, this);

                Integer MemberId = new Integer(CacheFactory.getCluster().getLocalMember().getId());

                if (submissionResult.assign(MemberId) == null)
                {
                    if (submissionResult.processingStarted(MemberId))
                    {
                        // attempt to execute the payload.
                        // this ProcessRunner only runs Runnable or Callables
                        //
                        try
                        {
                            Object            oResult           = null;
                            SubmissionContent submissionContent = submission.getContent();

                            if (submissionContent.getPayload() instanceof Runnable)
                            {
                                ((Runnable) submissionContent.getPayload()).run();
                                oResult = null;
                            }
                            else
                            {
                                if (submissionContent.getPayload() instanceof Callable)
                                {
                                    oResult = ((Callable) submissionContent.getPayload()).call();

                                }
                                else
                                {
                                    throw new UnsupportedOperationException(String
                                        .format("Can't execute %s as it's neither Runnable or Callable",
                                                submissionKeyPair));
                                }
                            }

                            if (Thread.interrupted())    // Note: calling this clears the flag
                            {
                                if (logger.isLoggable(Level.WARNING))
                                {
                                    logger.log(Level.WARNING,
                                               "The Submission {0} was intentionally interrupted (by calling cancelSubmission or shutting down) during execution",
                                               new Object[] {submissionContent.getPayload()});
                                }
                            }

                            if (!isCancelled)
                            {
                                if (submissionResult.processingSucceeded(oResult))
                                {
                                    if (logger.isLoggable(Level.FINER))
                                    {
                                        logger.log(Level.FINER,
                                                   "Executed {0} to produce {1}",
                                                   new Object[] {submissionKeyPair, oResult});
                                    }
                                }
                            }
                            else
                            {
                                if (logger.isLoggable(Level.FINER))
                                {
                                    logger.log(Level.FINER,
                                               "Submission {0} was CANCELLED",
                                               new Object[] {submissionKeyPair});
                                }
                            }
                        }
                        catch (final Exception oException)
                        {
                            if (logger.isLoggable(Level.WARNING))
                            {
                                logger.log(Level.WARNING,
                                           "Failed to process {0} due to:\n{1}",
                                           new Object[] {submissionKeyPair, oException});
                            }

                            submissionResult.processingFailed(oException);
                        }
                    }
                }
            }
            finally
            {
                submissionResultProxyFactory.unregisterChangeCallback(submissionResult, this);
            }
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
                                   new Object[] {submissionKeyPair});
                    }

                    isCancelled = true;
                    executionThread.interrupt();
                }
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
}
