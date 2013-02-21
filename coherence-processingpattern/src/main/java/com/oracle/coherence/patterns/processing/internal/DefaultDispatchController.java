/*
 * File: DefaultDispatchController.java
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

package com.oracle.coherence.patterns.processing.internal;

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent;
import com.oracle.coherence.common.events.lifecycle.LifecycleStoppedEvent;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.dependencies.DependencyReference;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.dependencies.DependentResourceReference;
import com.oracle.coherence.patterns.processing.DispatcherFilter;
import com.oracle.coherence.patterns.processing.SubmissionState;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchOutcome;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.oracle.coherence.patterns.processing.exceptions.NoDispatcherForSubmissionException;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorDefinitionManager;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link DefaultDispatchController} is the standard implementation of a
 * {@link DispatchController}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class DefaultDispatchController implements DispatchController
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(DefaultDispatchController.class.getName());

    /**
     * The list of {@link Dispatcher}s this controller will use to dispatch
     * {@link PendingSubmission}s for processing.
     */
    private final TreeMap<Integer, Dispatcher> dispatcherList;

    /**
     * The queue of {@link PendingSubmission} that need to be dispatched.
     */
    private DelayQueue<PendingSubmission> pendingSubmissions;

    /**
     * The thread running the dispatching to dispatchers.
     */
    private Thread dispatcherThread;

    /**
     * The {@link ConfigurableCacheFactory} for this {@link DispatchController}.
     */
    private ConfigurableCacheFactory ccFactory;

    /**
     * The Dispatcher Cache listener.
     */
    MapListener dispatcherCacheListener;

    /**
     * The reference to the NamedCace for dispatchers.
     */
    NamedCache dispatcherCache;

    /**
     * The {@link ObjectProxyFactory} for {@link SubmissionResult}s to use.
     */
    private ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;


    /**
     * Standard Constructor.
     *
     * @param ccFactory the {@link ConfigurableCacheFactory} to use
     * @param submissionResultProxyFactory the {@link ObjectProxyFactory} to use for {@link SubmissionResult}s
     */
    public DefaultDispatchController(ConfigurableCacheFactory             ccFactory,
                                     ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory)
    {
        DefaultSubmission.setDispatchController(this);
        this.ccFactory                    = ccFactory;

        this.submissionResultProxyFactory = submissionResultProxyFactory;

        this.dispatcherList               = new TreeMap<Integer, Dispatcher>();
        this.pendingSubmissions           = new DelayQueue<PendingSubmission>();
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesSatisfied(Environment environment)
    {
        dispatcherCache  = ccFactory.ensureCache(DefaultDispatcherManager.CACHENAME, null);
        dispatcherThread = new Thread(this, "DefaultDispatchController.dispatcherThread");
        dispatcherThread.start();

        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Starting Default Dispatch Controller");
        }

        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStartedEvent<DependentResource>(this));
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesViolated(Environment environment)
    {
        synchronized (dispatcherList)
        {
            if (logger.isLoggable(Level.INFO))
            {
                logger.log(Level.INFO,
                           "Shutting down Default Dispatch Controller - {0} dispatchers active",
                           dispatcherList.size());
            }

            dispatcherThread.interrupt();
            dispatcherCache.removeMapListener(dispatcherCacheListener);

            Iterator<Dispatcher> iter = dispatcherList.values().iterator();

            while (iter.hasNext())
            {
                Dispatcher disp = iter.next();

                if (disp != null)
                {
                    logger.log(Level.INFO, "Calling shutdown on Dispatcher {0}", disp);
                    disp.onShutdown(this);
                }
            }
        }

        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStoppedEvent<DependentResource>(this));
    }


    /**
     * {@inheritDoc}
     */
    public void accept(final PendingSubmission oPendingSubmission)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER,
                       "Accepting submission {0} resultid {1}",
                       new Object[] {oPendingSubmission.getSubmissionKey(), oPendingSubmission.getResultIdentifier()});
        }

        boolean result = pendingSubmissions.offer(oPendingSubmission);

        if (!result)
        {
            if (logger.isLoggable(Level.SEVERE))
            {
                logger.log(Level.SEVERE,
                           "Default Dispatch Controller did not accept {0}",
                           oPendingSubmission.toString());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void acceptTransferredSubmission(DefaultPendingSubmission defaultPendingSubmission)
    {
        // We need to set the Submission to retry if it has been started.
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER,
                       "Accepting TRANSFERRED submission {0} resultid {1}",
                       new Object[] {defaultPendingSubmission.getSubmissionKey(),
                                     defaultPendingSubmission.getResultIdentifier()});
        }

        boolean result = pendingSubmissions.offer(defaultPendingSubmission);

        if (!result)
        {
            if (logger.isLoggable(Level.SEVERE))
            {
                logger.log(Level.SEVERE,
                           "Default Dispatch Controller did not accept {0}",
                           defaultPendingSubmission.toString());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void discard(final PendingSubmission oProcess)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Discarding pending Submission {0}", oProcess);
        }

        pendingSubmissions.remove(oProcess);
    }


    /**
     * {@inheritDoc}
     */
    public void onDispatcherUpdate(final Dispatcher dispatcher)
    {
    }


    /**
     * {@inheritDoc}
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory()
    {
        return ccFactory;
    }


    /**
     * Take messages off the queue and dispatch them.
     */
    public void run()
    {
        // First we need to get a hold of the registered Dispatchers.
        // This code must run here as it's not safe to run on a service thread

        synchronized (dispatcherList)
        {
            final Iterator iter = dispatcherCache.entrySet().iterator();

            while (iter.hasNext())
            {
                Entry            entry      = (Entry) iter.next();
                final Dispatcher dispatcher = (Dispatcher) entry.getValue();

                dispatcherList.put((Integer) entry.getKey(), dispatcher);
                dispatcher.onStartup(this);
            }
        }

        // Register for updates to the dispatcher list
        initializeDispatcherCacheListener(dispatcherCache);

        // Dispatch submissions
        while (true)
        {
            try
            {
                final DefaultPendingSubmission oPendingSubmission =
                    (DefaultPendingSubmission) pendingSubmissions.take();

                dispatchSubmission(oPendingSubmission);
            }
            catch (final InterruptedException oException)
            {
                // When shutdown is called, an InterruptException will occur.
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Dispatcher thread interrupted.");
                }

                break;
            }
            catch (Throwable t)
            {
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE, "Dispatcher thread caught exception {0}", t);
                }
            }

        }
    }


    /**
     * Initializes the {@link MapListener} for the {@link Dispatcher} cache.
     *
     * @param dispatcherCache
     *            the {@link NamedCache} containing {@link Dispatcher}s.
     */
    private void initializeDispatcherCacheListener(final NamedCache dispatcherCache)
    {
        dispatcherCache.addMapListener(dispatcherCacheListener = new MultiplexingMapListener()
        {
            @Override
            protected void onMapEvent(final MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED)
                {
                    final Dispatcher dispatcher = (Dispatcher) mapEvent.getNewValue();

                    if (logger.isLoggable(Level.INFO))
                    {
                        logger.log(Level.INFO, "Starting dispatcher:", dispatcher.getName());
                    }

                    synchronized (dispatcherList)
                    {
                        dispatcherList.put((Integer) mapEvent.getKey(), dispatcher);
                    }

                    dispatcher.onStartup(DefaultDispatchController.this);
                }
                else
                {
                    if (mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                    {
                        final Dispatcher dispatcher = (Dispatcher) mapEvent.getNewValue();

                        if (logger.isLoggable(Level.WARNING))
                        {
                            logger.log(Level.WARNING,
                                       "Dispatchers are immutable and can't be updated",
                                       dispatcher.getName());
                        }
                    }
                    else
                    {
                        if (mapEvent.getId() == MapEvent.ENTRY_DELETED)
                        {
                            synchronized (dispatcherList)
                            {
                                final Dispatcher dispatcher = (Dispatcher) mapEvent.getOldValue();

                                if (dispatcher != null)
                                {
                                    if (logger.isLoggable(Level.INFO))
                                    {
                                        logger.log(Level.INFO, "Shutting down dispatcher {0}", dispatcher.getName());
                                    }

                                    dispatcher.onShutdown(DefaultDispatchController.this);
                                    dispatcherList.remove(mapEvent.getKey());
                                }
                            }
                        }
                    }
                }
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionState getSubmissionState(Object resultId)
    {
        SubmissionResult submissionResult = submissionResultProxyFactory.getProxy(resultId);

        return submissionResult.getSubmissionState();
    }


    /**
     * Dispatches a {@link PendingSubmission}.
     *
     * @param oPendingSubmission
     *            the {@link PendingSubmission} to dispatch
     */
    protected void dispatchSubmission(final DefaultPendingSubmission oPendingSubmission)
    {
        try
        {
            Object payLoad = oPendingSubmission.getPayload();

            if (payLoad != null)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Dispatching submission resultid {1}, submission key {0} payload {2}",
                               new Object[] {oPendingSubmission.getSubmissionKey(),
                                             oPendingSubmission.getResultIdentifier(), payLoad});
                }

                DispatchOutcome oResult = DispatchOutcome.REJECTED;
                final boolean   fFirst  = true;

                synchronized (dispatcherList)
                {
                    for (final Dispatcher dispatcher : dispatcherList.values())
                    {
                        oResult = tryDispatchSubmissionToDispatcher(oPendingSubmission, dispatcher);

                        if (oResult instanceof DispatchOutcome.Accepted)
                        {
                            // Break dispatcher chain since one Dispatcher accepted
                            break;
                        }
                        else
                        {
                            if (oResult == DispatchOutcome.CONTINUE)
                            {
                            }
                            else
                            {
                                if (oResult instanceof DispatchOutcome.RetryLater)
                                {
                                    final DispatchOutcome.RetryLater laterResult = (DispatchOutcome.RetryLater) oResult;
                                    final long                       delay       = laterResult.getDelay();

                                    if (fFirst)
                                    {
                                        oPendingSubmission.setDelay(delay);
                                    }
                                    else
                                    {
                                        if (oPendingSubmission.getDelay(laterResult.getTimeUnit()) > delay)
                                        {
                                            oPendingSubmission.setDelay(delay);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (oResult instanceof DispatchOutcome.RetryLater)
                {
                    if (oResult != DispatchOutcome.CONTINUE)
                    {
                        pendingSubmissions.add(oPendingSubmission);

                        if (logger.isLoggable(Level.FINER))
                        {
                            logger.log(Level.FINER, "Retrying submission {0}", oPendingSubmission.toString());
                        }
                    }
                }
                else
                {
                    if (oResult instanceof DispatchOutcome.Rejected)
                    {
                        // If no dispatcher accepted
                        // Failing means returning exception
                        if (logger.isLoggable(Level.FINER))
                        {
                            logger.log(Level.FINER,
                                       "Submission was REJECTED by every dispatcher {0}",
                                       oPendingSubmission.toString());
                        }

                        SubmissionResult submissionResult =
                            submissionResultProxyFactory.getProxy(oPendingSubmission.getResultIdentifier());

                        if (submissionResult != null)
                        {
                            submissionResult.processingFailed(new NoDispatcherForSubmissionException());
                        }
                        else
                        {
                            if (logger.isLoggable(Level.SEVERE))
                            {
                                logger.log(Level.SEVERE,
                                           "Failed to set the failure result for result with ID:{0}",
                                           oPendingSubmission.getResultIdentifier());
                            }
                        }

                    }
                }
            }
            else
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "The submission with ID {0} had a NULL payload - resultid {1}",
                               new Object[] {oPendingSubmission.getSubmissionKey(),
                                             oPendingSubmission.getResultIdentifier()});
                }
            }
        }
        catch (NoSuchElementException e)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "The submission with ID {0} didn't exist - resultid {1}",
                           new Object[] {oPendingSubmission.getSubmissionKey(),
                                         oPendingSubmission.getResultIdentifier()});
            }

        }
    }


    /**
     * Tries to dispatch a {@link PendingSubmission} to a {@link Dispatcher}.
     *
     * @param oPendingSubmission the {@link PendingSubmission} to dispatch
     * @param dispatcher         the {@link Dispatcher} to try to dispatch it to
     *
     * @return a {@link DispatchOutcome} indicating whether it was ACCEPTED or
     *         REJECTED
     */
    protected DispatchOutcome tryDispatchSubmissionToDispatcher(final DefaultPendingSubmission oPendingSubmission,
                                                                final Dispatcher               dispatcher)
    {
        DispatchOutcome oResult = DispatchOutcome.REJECTED;

        if (oPendingSubmission.getSubmissionConfiguration() != null)
        {
            DispatcherFilter filter = oPendingSubmission.getSubmissionConfiguration().getDispatcherFilter();

            if (filter != null)
            {
                if (filter.filterDispatcher(dispatcher))
                {
                    // DispatcherFilter accepted dispatcher
                    oResult = dispatcher.dispatch(oPendingSubmission);
                }
                else
                {
                }    // Do nothing - dispatcher filtered out
            }
            else

            // No filter exists
            {
                oResult = dispatcher.dispatch(oPendingSubmission);
            }
        }
        else
        {
            // Not even a SubmissionConfiguration exists
            oResult = dispatcher.dispatch(oPendingSubmission);
        }

        return oResult;
    }


    /**
     * {@inheritDoc}
     */
    public Set<DependencyReference> getDependencyReferences()
    {
        Set<DependencyReference> dependencies = new HashSet<DependencyReference>();

        dependencies
            .add(new DependentResourceReference((DependentResource) ((Environment) ccFactory)
                .getResource(TaskProcessorMediator.class)));
        dependencies
            .add(new DependentResourceReference((DependentResource) ((Environment) ccFactory)
                .getResource(Submission.class)));
        dependencies
            .add(new DependentResourceReference((DependentResource) ((Environment) ccFactory)
                .getResource(SubmissionResult.class)));
        dependencies
            .add(new DependentResourceReference((DependentResource) ((Environment) ccFactory)
                .getResource(TaskProcessorDefinitionManager.class)));

        return dependencies;
    }
}
