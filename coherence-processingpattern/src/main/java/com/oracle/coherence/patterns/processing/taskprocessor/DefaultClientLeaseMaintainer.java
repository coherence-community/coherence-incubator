/*
 * File: DefaultClientLeaseMaintainer.java
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

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.common.processors.InvokeMethodProcessor;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.dependencies.DependencyReference;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.dependencies.DependentResourceReference;
import com.oracle.coherence.patterns.processing.dispatchers.task.DefaultTaskDispatcher;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link DefaultClientLeaseMaintainer} maintains a registered {@link Lease} automatically, by extending it well
 * before the expiry.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class DefaultClientLeaseMaintainer implements DependentResource, ClientLeaseMaintainer
{
    /**
     * The default duration of a lease.
     */
    private static final long LEASE_DURATION = 1000 * 125;    // Two minutes and some seconds to allow for TCP timeout

    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(DefaultClientLeaseMaintainer.class.getName());

    /**
     * The {@link ScheduledExecutorService} that we'll use to check the validity of {@link Lease}s.
     */
    private ScheduledExecutorService executorService;

    /**
     * The map of {@link Lease}s, each mapped to a {@link TaskProcessorMediatorKey}.
     */
    private ConcurrentHashMap<TaskProcessorMediatorKey, Lease> leases;

    /**
     * The duration of the delay between {@link Lease} maintenance.
     */
    private long leaseValidityCheckingDelay;

    /**
     * The {@link Environment} to use.
     */
    private Environment environment;

    /**
     * The {@link NamedCache} that stores the {@link TaskProcessorMediator}s.
     */
    private NamedCache taskProcessorMediatorCache;


    /**
     * Standard constructor.
     *
     * @param environment the {@link Environment} to use
     * @param leaseValidityCheckingDelay the period by which lease validity checking is performed
     */
    public DefaultClientLeaseMaintainer(Environment environment,
                                        long        leaseValidityCheckingDelay)
    {
        this.environment                = environment;
        this.leases                     = new ConcurrentHashMap<TaskProcessorMediatorKey, Lease>();
        this.leaseValidityCheckingDelay = leaseValidityCheckingDelay;
        DefaultTaskDispatcher.setClientLeaseMaintainer(this);
    }


    /**
     * Returns the map of {@link Lease}s.
     *
     * @return the map of {@link Lease}s
     */
    public ConcurrentHashMap<TaskProcessorMediatorKey, Lease> getLeases()
    {
        return leases;
    }


    /**
     * {@inheritDoc}
     */
    public void addLease(TaskProcessorMediatorKey taskProcessorKey,
                         Lease                    lease)
    {
        leases.put(taskProcessorKey, lease);
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesSatisfied(Environment environment)
    {
        this.taskProcessorMediatorCache = CacheFactory.getCache(DefaultTaskProcessorMediator.CACHENAME);
        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories
                    .newThreadFactory(true, "ClientLeaseCoordinator", null));
        this.executorService.scheduleAtFixedRate(new LeaseExtender(),
                                                 leaseValidityCheckingDelay,
                                                 leaseValidityCheckingDelay,
                                                 TimeUnit.MILLISECONDS);

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER,
                       "Started ClientLeaseMaintainer - checking lease validity every {0} ms. Extending leases with {1}",
                       new Object[] {leaseValidityCheckingDelay, LEASE_DURATION});
        }

        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStartedEvent<DependentResource>(this));

    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesViolated(Environment environment)
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Shutting down ClientLeaseMaintainer");
        }

        executorService.shutdownNow();

    }


    /**
     * {@inheritDoc}
     */
    public void removeLease(TaskProcessorMediatorKey taskProcessorKey,
                            Lease                    lease)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Removing lease for {0}", taskProcessorKey);
        }

        leases.remove(taskProcessorKey);
    }


    /**
     * {@inheritDoc}
     */
    public Set<DependencyReference> getDependencyReferences()
    {
        Set<DependencyReference> dependencies = new HashSet<DependencyReference>();

        dependencies
            .add(new DependentResourceReference((DependentResource) environment
                .getResource(TaskProcessorMediator.class)));
        dependencies.add(new DependentResourceReference((DependentResource) environment.getResource(Submission.class)));
        dependencies
            .add(new DependentResourceReference((DependentResource) environment.getResource(SubmissionResult.class)));

        return dependencies;
    }


    /**
     * The {@link LeaseExtender} is a {@link Runnable} that iterates over the registered {@link Lease}s and extends
     * them.
     */
    private class LeaseExtender implements Runnable
    {
        /**
         * {@inheritDoc}
         */
        public void run()
        {
            try
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "LeaseExtender running");
                }

                ConcurrentHashMap<TaskProcessorMediatorKey, Lease> leases = getLeases();
                Iterator<TaskProcessorMediatorKey>                 iter   = leases.keySet().iterator();

                while (iter.hasNext())
                {
                    TaskProcessorMediatorKey key = iter.next();

                    if (logger.isLoggable(Level.FINER))
                    {
                        logger.log(Level.FINER, "Extending TaskProcessorLease for {0}", key);
                    }

                    taskProcessorMediatorCache.invoke(key,
                                                      new InvokeMethodProcessor("extendTaskProcessorLease",
                                                                                new Object[] {LEASE_DURATION}));
                }
            }
            catch (Exception e)
            {
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE, "Exception caught during lease extension", e);
                }
            }
        }
    }
}
