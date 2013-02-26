/*
 * File: DefaultProcessingSession.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.identifiers.UUIDBasedIdentifier;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.ProcessingSession;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.SubmissionOutcome;
import com.oracle.coherence.patterns.processing.SubmissionOutcomeListener;
import com.oracle.coherence.patterns.processing.SubmissionRetentionPolicy;
import com.oracle.coherence.patterns.processing.SubmissionState;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.UUID;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of the {@link ProcessingSession} interface.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Christer Fahlgren
 *
 */
public class DefaultProcessingSession implements ProcessingSession
{
    /**
     * The {@link Logger} to use.
     */
    private static final Logger logger = Logger.getLogger(DefaultProcessingSession.class.getName());

    /**
     * The Submission Cache.
     */
    private final NamedCache submissionCache;

    /**
     * A map of {@link DefaultSubmissionResult}s for the payloads that were submitted by this
     * {@link DefaultProcessingSession} but are yet to be processed.
     */
    private final ConcurrentHashMap<Object, DefaultSubmissionOutcome> submissionOutcomeMap;

    /**
     * The SubmissionResults Cache.
     */
    private final NamedCache submissionResultsCache;

    /**
     * The {@link Identifier} for this {@link DefaultProcessingSession} instance.
     * <p>
     * This is used to register listeners for the results from a {@link DefaultSubmission}s.
     */
    private final Identifier sessionId;

    /**
     * The {@link ObjectProxyFactory} for {@link SubmissionResult}s used by this {@link ProcessingSession}.
     */
    private ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;

    /**
     * The {@link ObjectProxyFactory} for {@link Submission}s used by this {@link ProcessingSession}.
     */
    private ObjectProxyFactory<Submission> submissionProxyFactory;

    /**
     * Synchronization object for a clean shutdown of the {@link ProcessingSession}.
     */
    private Object shutdownSync;

    /**
     * The {@link MapListener} for the {@link NamedCache} containing {@link SubmissionResult}s.
     */
    MapListener submissionResultsListener;

    /**
     * An {@link ExecutorService} used to run the asynchronous removal of cache objects.
     */
    private final ExecutorService executorService;


    /**
     * Standard Constructor.
     *
     * @param sessionIdentifier is the {@link Identifier} for this Session.
     */
    @SuppressWarnings("unchecked")
    public DefaultProcessingSession(Identifier sessionIdentifier)
    {
        ConfigurableCacheFactory ccFactory   = CacheFactory.getConfigurableCacheFactory();
        Environment              environment = (Environment) ccFactory;

        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories
                    .newThreadFactory(true, "DefaultProcessingSession", null));
        this.shutdownSync         = new Object();
        this.sessionId            = sessionIdentifier;
        this.submissionOutcomeMap = new ConcurrentHashMap<Object, DefaultSubmissionOutcome>();
        this.submissionResultProxyFactory =
            (ObjectProxyFactory<SubmissionResult>) environment.getResource(SubmissionResult.class);

        if (submissionResultProxyFactory == null)
        {
            throw new RuntimeException("ProcessingPattern not correctly initialized. Make sure the cache configuration defines the use of the Processing Pattern.");
        }

        this.submissionResultsCache = ccFactory.ensureCache(DefaultSubmissionResult.CACHENAME, null);
        this.submissionProxyFactory = (ObjectProxyFactory<Submission>) environment.getResource(Submission.class);
        this.submissionCache        = ccFactory.ensureCache(DefaultSubmission.CACHENAME, null);
        setSubmissionResultsListener();
    }


    /**
     * Standard Constructor.
     *
     * @param sessionIdentifier is the {@link Identifier} for this Session.
     */
    public DefaultProcessingSession(ConfigurableCacheFactory             ccFactory,
                                    ObjectProxyFactory<Submission>       submissionProxyFactory,
                                    ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory,
                                    Identifier                           sessionIdentifier)
    {
        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories
                    .newThreadFactory(true, "DefaultProcessingSession", null));
        this.shutdownSync                 = new Object();
        this.sessionId                    = sessionIdentifier;
        this.submissionOutcomeMap         = new ConcurrentHashMap<Object, DefaultSubmissionOutcome>();
        this.submissionResultProxyFactory = submissionResultProxyFactory;

        if (submissionResultProxyFactory == null)
        {
            throw new RuntimeException("ProcessingPattern not correctly initialized. Make sure the cache configuration defines the use of the Processing Pattern.");
        }

        this.submissionResultsCache = ccFactory.ensureCache(DefaultSubmissionResult.CACHENAME, null);
        this.submissionProxyFactory = submissionProxyFactory;
        this.submissionCache        = ccFactory.ensureCache(DefaultSubmission.CACHENAME, null);
        setSubmissionResultsListener();
    }


    /**
     * Initializes the MapListener for the SubmissionResults cache.
     */
    private void setSubmissionResultsListener()
    {
        this.submissionResultsListener = new MultiplexingMapListener()
        {
            @Override
            protected void onMapEvent(final MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED || mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                {
                    synchronized (shutdownSync)
                    {
                        final SubmissionResult oResult = (SubmissionResult) mapEvent.getNewValue();

                        handleResultChange(oResult, mapEvent.getKey());
                    }
                }
            }
        };
        this.submissionResultsCache.addMapListener(submissionResultsListener,
                                                   new MapEventFilter(MapEventFilter.E_INSERTED
                                                                      | MapEventFilter.E_UPDATED,
                                                                      new EqualsFilter("getSessionIdentifier",
                                                                                       sessionId)),
                                                   false);
    }


    /**
     * Handles the new result status.
     *
     * @param submissionResult the new {@link SubmissionResult}
     * @param key the key to the {@link SubmissionResult} that changed
     */
    protected void handleResultChange(SubmissionResult submissionResult,
                                      Object           key)
    {
        // the SubmissionResult is delivered to us as the
        // mapEvent new value
        // find the submissionOutcome for the SubmissionResult
        // just delivered (using the key for the SubmissionResult)
        final DefaultSubmissionOutcome submissionOutcome = submissionOutcomeMap.get(key);

        if (submissionResult.isFinalState())
        {
            if (submissionOutcome != null)
            {
                submissionOutcome.acceptProcessResult(submissionResult.getResult(),
                                                      submissionResult.getSubmissionState(),
                                                      submissionResult.getSubmissionTime(),
                                                      submissionResult.getLatency(),
                                                      submissionResult.getExecutionTime());
            }
        }
        else
        {
            // If not final it could either be suspended
            // or a
            // progress update
            if (submissionOutcome != null)
            {
                if (submissionResult.getSubmissionState() == SubmissionState.SUSPENDED)
                {
                    submissionOutcome.onSuspended();
                }
                else
                {
                    if (submissionResult.getSubmissionState() == SubmissionState.EXECUTING)
                    {
                        // We are still executing, thus it
                        // must be a progress result
                        if (submissionResult.getProgress() == null)
                        {
                            submissionOutcome.onStarted();
                        }
                        else
                        {
                            submissionOutcome.onProgress(submissionResult.getProgress());
                        }
                    }
                }
            }
            else
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               " Result update:" + submissionResult.getID() + ":" + key
                               + " submissionoutcome is null BUT NOT IN FINAL STATE");
                }
            }
        }

        // remove the processResult and result
        // iff the result is complete
        if (submissionOutcome != null)
        {
            if (submissionOutcome.isFinalState())
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Submission is, done REMOVING submission {0} and submissionResult {1}",
                               new Object[] {submissionResult.getSubmissionKey(), key});
                }

                if (submissionOutcome.getRetentionPolicy() == SubmissionRetentionPolicy.RemoveOnFinalState)
                {
                    removeCacheObjectsAsynch(submissionResult.getSubmissionKey(), submissionResult.getID());
                }

                submissionOutcomeMap.remove(key);
            }
        }
        else
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           " Result update:" + submissionResult.getID() + ":" + key + " submissionoutcome is null");
            }
        }
    }


    /**
     * Removes the Cache objects asynchronously in a separate thread.
     *
     * @param submissionKey the key to the {@link Submission}
     * @param resultId the key to the {@link SubmissionResult}
     */
    private void removeCacheObjectsAsynch(final SubmissionKey submissionKey,
                                          final Identifier    resultId)
    {
        executorService.execute(new Runnable()
        {
            public void run()
            {
                synchronized (shutdownSync)
                {
                    try
                    {
                        submissionProxyFactory.destroyRemoteObject(submissionKey);
                        submissionResultProxyFactory.destroyRemoteObject(resultId);
                    }
                    catch (Throwable e)
                    {
                        logger.log(Level.SEVERE,
                                   "Failed to remove cache objects with result key {0} and submission key {1} due to {2}",
                                   new Object[] {resultId, submissionKey, e});
                    }
                }
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionOutcome submit(final Object                  oPayload,
                                    final SubmissionConfiguration oRequestData) throws Throwable
    {
        synchronized (shutdownSync)
        {
            final DefaultSubmission submission = new DefaultSubmission(new UUID(),
                                                                       oPayload,
                                                                       oRequestData,
                                                                       UUIDBasedIdentifier.newInstance(),
                                                                       sessionId);
            final DefaultSubmissionOutcome submissionOutcome =
                new DefaultSubmissionOutcome(submission.getResultIdentifier(),
                                             SubmissionRetentionPolicy.RemoveOnFinalState,
                                             submissionResultProxyFactory);

            submissionOutcomeMap.put(submission.getResultIdentifier(), submissionOutcome);
            storeInCache(submission);

            return submissionOutcome;
        }
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionOutcome submit(final Object                    oPayload,
                                    final SubmissionConfiguration   oRequestData,
                                    final SubmissionOutcomeListener oListener) throws Throwable
    {
        synchronized (shutdownSync)
        {
            final DefaultSubmission submission = new DefaultSubmission(new UUID(),
                                                                       oPayload,
                                                                       oRequestData,
                                                                       UUIDBasedIdentifier.newInstance(),
                                                                       sessionId);
            SubmissionResult submissionResult = submissionResultProxyFactory.getProxy(submission.getResultIdentifier());
            final DefaultSubmissionOutcome submissionOutcome =
                new DefaultSubmissionOutcome(submission.getResultIdentifier(),
                                             oListener,
                                             submissionResult,
                                             SubmissionRetentionPolicy.RemoveOnFinalState,
                                             submissionResultProxyFactory);

            submissionOutcomeMap.put(submission.getResultIdentifier(), submissionOutcome);
            storeInCache(submission);

            return submissionOutcome;
        }
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionOutcome submit(Object                    oPayload,
                                    SubmissionConfiguration   oConfiguration,
                                    Identifier                identifier,
                                    SubmissionRetentionPolicy retentionPolicy,
                                    SubmissionOutcomeListener oListener) throws Throwable
    {
        synchronized (shutdownSync)
        {
            final DefaultSubmission submission = new DefaultSubmission(new UUID(),
                                                                       oPayload,
                                                                       oConfiguration,
                                                                       identifier,
                                                                       sessionId);
            SubmissionResult submissionResult = submissionResultProxyFactory.getProxy(submission.getResultIdentifier());
            final DefaultSubmissionOutcome submissionOutcome =
                new DefaultSubmissionOutcome(submission.getResultIdentifier(),
                                             oListener,
                                             submissionResult,
                                             retentionPolicy,
                                             submissionResultProxyFactory);

            if (!submissionOutcomeMap.containsKey(submission.getResultIdentifier()))
            {
                // Even if the submissionOutcomeMap didn't contain the identifier we can still have
                // a submission already stored through a previous session.
                submissionOutcomeMap.put(submission.getResultIdentifier(), submissionOutcome);

                try
                {
                    storeInCache(submission);
                }
                catch (IllegalStateException e)
                {
                    // If the store raised an exception, we should remove the identifier from the map
                    submissionOutcomeMap.remove(submission.getResultIdentifier());

                    throw e;
                }
            }
            else
            {
                // In this case we have already submitted a Submission through this session
                throw new IllegalStateException("Identifier:" + submission.getResultIdentifier() + " already exists.");
            }

            return submissionOutcome;
        }
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionOutcome acquireSubmission(Identifier                identifier,
                                               SubmissionRetentionPolicy retentionPolicy,
                                               SubmissionOutcomeListener oListener)
    {
        synchronized (shutdownSync)
        {
            SubmissionResult submissionResult = submissionResultProxyFactory.getProxy(identifier);
            SubmissionKey    submissionKey    = submissionResult.getSubmissionKey();
            Submission       submission       = submissionProxyFactory.getProxy(submissionKey);

            submission.changeSessionIdentifier(sessionId);
            submissionResult.changeSessionIdentifier(sessionId);

            final DefaultSubmissionOutcome oResult = new DefaultSubmissionOutcome(identifier,
                                                                                  oListener,
                                                                                  submissionResult,
                                                                                  retentionPolicy,
                                                                                  submissionResultProxyFactory);

            submissionOutcomeMap.put(identifier, oResult);

            // Since we now have created a new SubmissionOutcome, we need to transfer the submission state to the new outcome
            oResult.checkSubmissionResult();

            return oResult;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean discardSubmission(Identifier identifier) throws Throwable
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, " discardSubmission for " + identifier);
        }

        SubmissionResult submissionResult = submissionResultProxyFactory.getLocalCopyOfRemoteObject(identifier);

        if (submissionResult.getSessionIdentifier().equals(sessionId))
        {
            submissionResultProxyFactory.destroyRemoteObject(identifier);
            submissionProxyFactory.destroyRemoteObject(submissionResult.getSubmissionKey());

            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, " discardSubmission for " + identifier + " successful");
            }

            submissionOutcomeMap.remove(identifier);

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           " discardSubmission - failed to remove - submissionResult owned by  "
                           + submissionResult.getSessionIdentifier());
            }

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean cancelSubmission(Identifier identifier)
    {
        SubmissionResult submissionResult = submissionResultProxyFactory.getProxy(identifier);

        return submissionResult.cancelSubmission();
    }


    /**
     * {@inheritDoc}
     */
    public void releaseSubmission(SubmissionOutcome submissionOutcome)
    {
        submissionOutcomeMap.remove(submissionOutcome.getIdentifier());
    }


    /**
     * Stores the Submission in the NamedCache and creates the SubmissionResult in the SubmissionResults cache.
     *
     * @param submission the Submission to store
     *
     * @throws Throwable if store fails
     */
    private void storeInCache(final DefaultSubmission submission) throws Throwable
    {
        submissionResultProxyFactory.createRemoteObjectIfNotExists(submission.getResultIdentifier(),
                                                                   DefaultSubmissionResult.class,
                                                                   new Object[] {submission.getResultIdentifier(),
                                                                                 submission.generateKey(),
                                                                                 submission.getSessionIdentifier(),
                                                                                 SubmissionState.SUBMITTED.name()});

        Filter filter = new NotFilter(PresentFilter.INSTANCE);

        submissionCache.invoke(submission.generateKey(), new ConditionalPut(filter, submission));
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterator<Identifier> getIdentifierIterator()
    {
        Filter                filter         = new EqualsFilter("getSessionIdentifier", sessionId);
        ArrayList<Identifier> identifierList = new ArrayList<Identifier>();

        for (Iterator<Identifier> iter = submissionResultsCache.entrySet(filter).iterator(); iter.hasNext(); )
        {
            Map.Entry  entry = (Map.Entry) iter.next();
            Identifier key   = (Identifier) entry.getKey();

            identifierList.add(key);
        }

        return identifierList.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public boolean submissionExists(Identifier submissionIdentifier)
    {
        return submissionResultsCache.containsKey(submissionIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void shutdown()
    {
        synchronized (shutdownSync)
        {
            submissionResultsCache.removeMapListener(submissionResultsListener);
        }

        executorService.shutdown();
    }
}
