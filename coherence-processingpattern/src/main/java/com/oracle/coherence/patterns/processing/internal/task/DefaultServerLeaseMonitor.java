/*
 * File: DefaultServerLeaseMonitor.java
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

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.lifecycle.LifecycleEvent;
import com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent;
import com.oracle.coherence.common.events.lifecycle.LifecycleStoppedEvent;
import com.oracle.coherence.common.events.lifecycle.NamedCacheStorageReleasedEvent;
import com.oracle.coherence.common.events.processing.AbstractAsynchronousEventProcessor;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.common.processors.InvokeMethodProcessor;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.LifecycleEventFilter;
import com.oracle.coherence.environment.extensible.dependencies.DependencyReference;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.dependencies.DependentResourceReference;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link ServerLeaseMonitor} is responsible for monitoring Leases for expiration. When a lease expires, the
 * corresponding {@link TaskProcessorMediator} will be notified.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class DefaultServerLeaseMonitor extends AbstractAsynchronousEventProcessor<LifecycleEvent<?>>
    implements ServerLeaseMonitor,
               DependentResource
{
    /**
     * The {@link Logger} to use.
     */
    private static final Logger logger = Logger.getLogger(DefaultServerLeaseMonitor.class.getName());

    /**
     * The {@link ScheduledExecutorService} that we'll use to check the validity of {@link Lease}s.
     */
    private ScheduledExecutorService executorService;

    /**
     * The collection of {@link Lease}s being coordinated, indexed by their "leaseOwner" {@link Object} together with a
     * {@link com.oracle.coherence.common.leasing.LeaseListener} that will be notified when a said {@link Lease} expires.
     */
    private ConcurrentHashMap<TaskProcessorMediatorKey, Lease> leases;

    /**
     * The {@link NamedCache} that contains {@link TaskProcessorMediator}s.
     */
    private NamedCache taskProcessorMediatorCache;

    /**
     * The duration of the interval between checking Leases.
     */
    private long leaseValidityCheckingDelay;

    /**
     * The {@link Environment} to use.
     */
    private Environment environment;


    /**
     * Standard Constructor.
     *
     * @param environment                the {@link Environment} to use
     * @param leaseValidityCheckingDelay the number of milliseconds to wait before the validity of the coordinated
     *                                   {@link Lease}s are checked.
     */
    public DefaultServerLeaseMonitor(Environment environment,
                                     long        leaseValidityCheckingDelay)
    {
        DefaultTaskProcessorMediator.setLeaseMonitor(this);
        this.environment = environment;
        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories.newThreadFactory(true, "ServerLeaseMonitor", null));
        this.leases                     = new ConcurrentHashMap<TaskProcessorMediatorKey, Lease>();
        this.leaseValidityCheckingDelay = leaseValidityCheckingDelay;
    }


    /**
     * {@inheritDoc}
     */
    public void registerLease(TaskProcessorMediatorKey leaseOwner,
                              Lease                    lease)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINEST,
                       "Registering Lease for TaskProcessor {0}. Lease is {1}.",
                       new Object[] {leaseOwner, lease});
        }

        leases.put(leaseOwner, lease);
    }


    /**
     * {@inheritDoc}
     */
    public void deregisterLease(TaskProcessorMediatorKey leaseOwner)
    {
        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "De-Registering Lease for TaskProcessor {0}.", new Object[] {leaseOwner});
        }

        leases.remove(leaseOwner);
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesSatisfied(Environment environment)
    {
        taskProcessorMediatorCache = CacheFactory.getCache(DefaultTaskProcessorMediator.CACHENAME);
        this.executorService.scheduleAtFixedRate(new LeaseValidator(),
                                                 leaseValidityCheckingDelay,
                                                 leaseValidityCheckingDelay,
                                                 TimeUnit.MILLISECONDS);

        // Start listening for lifecycle events since we want to know when we are shutting down
        environment.getResource(EventDispatcher.class).registerEventProcessor(LifecycleEventFilter.INSTANCE, this);

        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStartedEvent<DependentResource>(this));
    }


    /**
     * {@inheritDoc}
     */
    public Set<DependencyReference> getDependencyReferences()
    {
        return Collections
            .singleton((DependencyReference) new DependentResourceReference(environment
                .getResource(DispatchController.class)));
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesViolated(Environment environment)
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Shutting down ServerLeaseMonitor.");
        }

        executorService.shutdown();
        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStoppedEvent<DependentResource>(this));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void processLater(EventDispatcher   eventDispatcher,
                             LifecycleEvent<?> event)
    {
        if (event instanceof NamedCacheStorageReleasedEvent)
        {
            if (DefaultTaskProcessorMediator.CACHENAME.equals(((NamedCacheStorageReleasedEvent) event).getCacheName()))
            {
                onDependenciesViolated(eventDispatcher.getEnvironment());
            }
        }
    }


    /**
     * A {@link LeaseValidator} is responsible for ensuring all of the {@link Lease}s coordinated by a
     * {@link ServerLeaseMonitor} are valid.
     */
    private class LeaseValidator implements Runnable
    {
        /**
         * {@inheritDoc}
         */
        public void run()
        {
            try
            {
                long                                 currentTime        = System.currentTimeMillis();
                LinkedList<TaskProcessorMediatorKey> expiredLeaseOwners = null;

                // find all of the expired leases as of now
                for (TaskProcessorMediatorKey leaseOwner : leases.keySet())
                {
                    Lease   lease           = leases.get(leaseOwner);
                    boolean deregisterLease = false;

                    if (!lease.isValidAt(currentTime))
                    {
                        if (logger.isLoggable(Level.WARNING))
                        {
                            logger.log(Level.WARNING,
                                       "Lease for TaskProcessor {0} has expired. Lease is {1}. Removing TaskProcessor.",
                                       new Object[] {leaseOwner, lease});
                        }

                        taskProcessorMediatorCache.invoke(leaseOwner,
                                                          new InvokeMethodProcessor("leaseExpired",
                                                                                    new Object[] {lease}));
                        taskProcessorMediatorCache.remove(leaseOwner);
                        deregisterLease = true;
                    }

                    if (deregisterLease)
                    {
                        if (expiredLeaseOwners == null)
                        {
                            expiredLeaseOwners = new LinkedList<TaskProcessorMediatorKey>();
                        }

                        expiredLeaseOwners.add(leaseOwner);
                    }
                }

                // remove all of the expired leases
                if (expiredLeaseOwners != null)
                {
                    for (TaskProcessorMediatorKey leaseOwner : expiredLeaseOwners)
                    {
                        deregisterLease(leaseOwner);
                    }
                }
            }
            catch (Exception exception)
            {
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE, "LeaseValidator failed due to {0}", exception);
                }
            }
        }
    }
}
