/*
 * File: LeaseExpiryCoordinator.java
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

package com.oracle.coherence.common.leasing;

import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.common.tuples.Pair;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link LeaseExpiryCoordinator} is responsible for ensuring that a collection
 * of registered {@link Lease}s are valid (ie: not expired).
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class LeaseExpiryCoordinator
{
    /**
     * The {@link Logger} for the class.
     */
    private static final Logger logger = Logger.getLogger(LeaseExpiryCoordinator.class.getName());

    /**
     * A singleton instance of a {@link LeaseExpiryCoordinator}.
     */
    public static final LeaseExpiryCoordinator INSTANCE = new LeaseExpiryCoordinator(1000);

    /**
     * The {@link ScheduledExecutorService} that we'll use to check the validity
     * of {@link Lease}s.
     */
    private ScheduledExecutorService executorService;

    /**
     * The collection of {@link Lease}s being coordinated, indexed by
     * their "leaseOwner" {@link Object} together with a {@link LeaseListener}
     * that will be notified when a said {@link Lease} expires.
     */
    private ConcurrentHashMap<Object, Pair<Lease, LeaseListener>> leases;


    /**
     * Standard Constructor.
     *
     * @param leaseValidityCheckingDelay The number of milliseconds to wait before the validity
     *                                   of the coordinated {@link Lease}s are checked.
     */
    public LeaseExpiryCoordinator(long leaseValidityCheckingDelay)
    {
        this.executorService =
            ExecutorServiceFactory
                .newSingleThreadScheduledExecutor(ThreadFactories.newThreadFactory(true, "LeaseCoordinator", null));
        this.leases = new ConcurrentHashMap<Object, Pair<Lease, LeaseListener>>();

        this.executorService.scheduleAtFixedRate(new LeaseValidator(),
                                                 leaseValidityCheckingDelay,
                                                 leaseValidityCheckingDelay,
                                                 TimeUnit.MILLISECONDS);
    }


    /**
     * Registers the specified {@link Lease}, owned by a specified "owner" to be coordinated
     * (ie: monitored) by the {@link LeaseExpiryCoordinator}.
     * <p>
     * When the said {@link Lease} expires, the {@link LeaseExpiryCoordinator} will notify the
     * specified {@link LeaseListener}.
     *
     * @param leaseOwner          The owner of the {@link Lease}
     * @param lease               The {@link Lease} to register
     * @param leaseExpiryListener The {@link LeaseListener} listening for the {@link Lease} to expire
     */
    public void registerLease(Object        leaseOwner,
                              Lease         lease,
                              LeaseListener leaseExpiryListener)
    {
        leases.put(leaseOwner, new Pair<Lease, LeaseListener>(lease, leaseExpiryListener));
    }


    /**
     * Deregisters the previously registered {@link Lease} with the specified lease owner.
     *
     * @param leaseOwner The owner of the lease
     */
    public void deregisterLease(Object leaseOwner)
    {
        leases.remove(leaseOwner);
    }


    /**
     * A {@link LeaseValidator} is responsible for ensuring all of the {@link Lease}s
     * coordinated by a {@link LeaseExpiryCoordinator} are valid.
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
                if (logger.isLoggable(Level.FINEST))
                {
                    logger.finest(String.format("Validating %d Lease(s)", leases.size()));
                }

                long               currentTime        = System.currentTimeMillis();
                LinkedList<Object> expiredLeaseOwners = null;

                // find all of the expired leases as of now
                for (Object leaseOwner : leases.keySet())
                {
                    Pair<Lease, LeaseListener> pair            = leases.get(leaseOwner);
                    Lease                      lease           = pair.getX();
                    LeaseListener              leaseListener   = pair.getY();

                    boolean                    deregisterLease = false;

                    if (lease.isCanceled())
                    {
                        if (logger.isLoggable(Level.FINEST))
                        {
                            logger.finest(String.format("%s for %s has been canceled", lease, leaseOwner));
                        }

                        if (leaseListener != null)
                        {
                            leaseListener.onLeaseCanceled(leaseOwner, lease);
                        }

                        deregisterLease = true;

                    }
                    else if (lease.isSuspended())
                    {
                        if (logger.isLoggable(Level.FINEST))
                        {
                            logger.finest(String.format("%s for %s has been suspended", lease, leaseOwner));
                        }

                        if (leaseListener != null)
                        {
                            leaseListener.onLeaseSuspended(leaseOwner, lease);
                        }

                        deregisterLease = true;

                    }
                    else if (!lease.isValidAt(currentTime))
                    {
                        if (logger.isLoggable(Level.FINEST))
                        {
                            logger.finest(String.format("%s for %s has expired", lease, leaseOwner));
                        }

                        if (leaseListener != null)
                        {
                            leaseListener.onLeaseExpiry(leaseOwner, lease);
                        }

                        deregisterLease = true;
                    }

                    if (deregisterLease)
                    {
                        if (expiredLeaseOwners == null)
                        {
                            expiredLeaseOwners = new LinkedList<Object>();
                        }

                        expiredLeaseOwners.add(leaseOwner);
                    }
                }

                // remove all of the expired leases
                if (expiredLeaseOwners != null)
                {
                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.finest(String.format("Deregistering %d Lease(s)", expiredLeaseOwners.size()));
                    }

                    for (Object leaseOwner : expiredLeaseOwners)
                    {
                        deregisterLease(leaseOwner);
                    }
                }
            }
            catch (Exception exception)
            {
                // Don't throw an exception since it will stop the LeaseValidator thread.
                logger.log(Level.SEVERE, String.format("LeaseValidator Failed due to\n%s\n", exception));
            }
        }
    }
}
