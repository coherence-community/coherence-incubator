/*
 * File: AbstractSubscriber.java
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

package com.oracle.coherence.patterns.messaging;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.messaging.entryprocessors.UnsubscribeProcessor;
import com.oracle.coherence.patterns.messaging.exceptions.SubscriberInterruptedException;
import com.oracle.coherence.patterns.messaging.exceptions.SubscriptionLostException;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.processor.UpdaterProcessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The base implementation of a {@link Subscriber} for
 * {@link LeasedSubscription}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S> subscription
 *
 * @author Brian Oliver
 */
abstract class AbstractSubscriber<S extends LeasedSubscription> implements Subscriber
{
    /**
     * The internal {@link State} of the {@link Subscriber}.
     */
    protected enum State
    {
        /**
         * <code>Starting</code> indicates that the {@link Subscriber} has started
         * but is yet to receive it's {@link Subscription} state from the cluster.
         */
        Starting,

        /**
         * <code>Active</code> indicates that the {@link Subscriber} is ready for use.
         */
        Active,

        /**
         * <code>Interrupted</code> indicates that the {@link Subscriber} was interrupted while
         * attempting to perform some action.  This is a terminating state.
         */
        Interrupted,

        /**
         * <code>Removed</code> indicates that the {@link Subscription} for the {@link Subscriber}
         * has been removed.  This is a terminating state.
         */
        Removed,

        /**
         * <code>Shutdown</code> indicates that the {@link Subscriber} has been shutdown.
         * This is a terminating state.
         */
        Shutdown
    }


    /**
     * The {@link MessagingSession} that owns this {@link Subscriber}.
     */
    private MessagingSession messagingSession;

    /**
     * The {@link Identifier} allocated to the {@link Subscriber}.
     */
    private SubscriptionIdentifier subscriptionIdentifier;

    /**
     * This {@link Queue} is used by the {@link Subscriber} to wait for updates
     * on it's {@link Subscription} to arrive.  This is especially especially useful
     * to we known when {@link Message}s have arrived.
     */
    private LinkedBlockingQueue<S> subscriptionUpdates;

    /**
     * Should messages received during a getMessage be automatically
     * acknowledged (and thus committed as read).
     */
    private boolean autocommit;

    /**
     * True if a subscription was offered at least once.
     */
    private boolean subscriptionOffered;

    /**
     * The {@link MapListener} that will be used to receive call back events
     * (and new {@link Subscription} state) from the cluster.
     */
    private MapListener mapListener;

    /**
     * A {@link Thread} that is responsible for maintaining the
     * {@link Lease} associated with this {@link Subscription}.
     */
    private LeaseMaintainerThread leaseMaintainerThread;

    /**
     * A map that keeps track of the previous sequence number received by this subscriber by a given publisher.
     * This is used to verify that sequence numbers are in order for a given publisher.
     */
    private ConcurrentHashMap<Identifier, Long> prevMessageIdentifierMap = new ConcurrentHashMap<Identifier, Long>();

    /**
     * State of the subscriber.
     */
    protected State state;


    /**
     * Protected Constructor.
     *
     * @param messagingSession The {@link MessagingSession} that owns this {@link Subscriber}.
     * @param subscriptionIdentifier subscription identifier
     */
    protected AbstractSubscriber(MessagingSession       messagingSession,
                                 SubscriptionIdentifier subscriptionIdentifier)
    {
        this.messagingSession       = messagingSession;
        this.subscriptionIdentifier = subscriptionIdentifier;
        this.subscriptionUpdates    = new LinkedBlockingQueue<S>();
        this.autocommit             = true;
        this.subscriptionOffered    = false;
        this.state                  = State.Starting;

        // register a MapListener for keeps our local copy of the subscription
        // up-to-date
        this.mapListener = new MapListener()
        {
            @SuppressWarnings("unchecked")
            public void entryInserted(MapEvent mapEvent)
            {
                if (Logger.isEnabled(Logger.QUIET))
                {
                    Logger.log(Logger.QUIET, "%s received insert event %s", this, mapEvent);
                }

                // added the newly arrived subscription state to the queue for
                // internal processing
                subscriptionOffered = true;
                subscriptionUpdates.offer((S) mapEvent.getNewValue());
            }

            @SuppressWarnings("unchecked")
            public void entryUpdated(MapEvent mapEvent)
            {
                if (Logger.isEnabled(Logger.QUIET))
                {
                    Logger.log(Logger.QUIET, "%s received update event %s", this, mapEvent);
                }

                S oldSubscription = (S) mapEvent.getOldValue();
                S newSubscription = (S) mapEvent.getNewValue();

                // only queue the updated subscription if the event was a result
                // of;
                // 1. the visible range has increased in size (ie: a message was
                // delivered)
                // 2. a lease was unsuspended
                if (oldSubscription.getNumMessages() < newSubscription.getNumMessages()
                    || (((LeasedSubscription) oldSubscription).getLease().isSuspended()
                        &&!((LeasedSubscription) newSubscription).getLease().isSuspended()))
                {
                    // added the updated arrived subscription state to the queue
                    // for internal processing
                    subscriptionOffered = true;
                    subscriptionUpdates.offer((S) mapEvent.getNewValue());
                }
                else if ((getState() == State.Starting) && (!subscriptionOffered))
                {
                    // The application just subscribed using an existing subscription (e.g. durable subscription).
                    // Add the subscription to the queue so the subscriber can move to the Active state.
                    subscriptionOffered = true;
                    subscriptionUpdates.offer((S) mapEvent.getNewValue());
                }

            }

            @SuppressWarnings("unchecked")
            public void entryDeleted(MapEvent mapEvent)
            {
                if (Logger.isEnabled(Logger.QUIET))
                {
                    Logger.log(Logger.QUIET, "%s received delete event %s", this, mapEvent);
                }

                // shutdown the subscriber
                shutdown(State.Removed);

                // added the removed subscription state to the queue for
                // internal processing
                subscriptionUpdates.offer((S) mapEvent.getOldValue());
            }
        };
        CacheFactory.getCache(Subscription.CACHENAME).addMapListener(mapListener, subscriptionIdentifier, false);

        leaseMaintainerThread = new LeaseMaintainerThread(this);
        leaseMaintainerThread.setDaemon(true);
        leaseMaintainerThread.setName(String.format("LeaseMaintainerThread[%s]",
                                                    subscriptionIdentifier.getSubscriberIdentifier()));
        leaseMaintainerThread.start();
    }


    /**
     * This verifies that a message from a given publisher was received in order by this subscriber.
     *
     * @param message message
     * @return true if verification succeeds
     */
    boolean verifyMessageSequence(Message message)
    {
        long       newSequenceNumber   = message.getRequestIdentifier().getMessageSequenceNumber();
        Identifier publisherIdentifier = message.getRequestIdentifier().getPublisherIdentifier();
        Long       prevSequenceNumber  = prevMessageIdentifierMap.get(publisherIdentifier);

        if (prevSequenceNumber != null)
        {
            prevMessageIdentifierMap.put(publisherIdentifier, newSequenceNumber);

            // Verify that the new message requestId is greater or equal than the previous message.
            // It will only be equal in the case of failover where the same message may get delivered twice.
            if (prevSequenceNumber.longValue() > newSequenceNumber)
            {
                // An out-of-order message can happend in the case of a rollback.
                Logger.log(Logger.WARN,
                           "Message sequence number out of order probably due to a rollback.  Prev = "
                           + prevSequenceNumber + " New = " + newSequenceNumber);
                ;
            }
        }
        else
        {
            prevMessageIdentifierMap.put(publisherIdentifier, Long.valueOf(newSequenceNumber));
        }

        return true;
    }


    /**
     * Clear the sequence number map for this subscriber.  This is done in the case of a rollback.
     */
    void resetMessageSequenceVerification()
    {
        prevMessageIdentifierMap = new ConcurrentHashMap<Identifier, Long>();
    }


    /**
     * {@inheritDoc}
     */
    public MessagingSession getMessagingSession()
    {
        return messagingSession;
    }


    /**
     * Returns the {@link State} of the {@link Subscriber}.
     *
     * @return return the state
     */
    protected synchronized State getState()
    {
        return state;
    }


    /**
     * Returns the current {@link State} of the {@link Subscriber}.
     *
     * @param state subscriber state
     */
    protected synchronized void setState(State state)
    {
        // ensure to don't try to "reset" from a terminating state
        if (this.state != State.Removed && this.state != State.Interrupted && this.state != State.Shutdown)
        {
            this.state = state;
        }
    }


    /**
     * Returns the next state update of the {@link Subscription} for this {@link Subscriber}.
     * If an updated {@link Subscription} state is not available
     * (ie: has not been sent to the {@link Subscriber}), this method waits until one
     * arrives (via the {@link MapListener}).
     *
     * @return return the subscription object
     */
    protected S getNextSubscriptionUpdate()
    {
        try
        {
            // get the next available subscription state
            // (wait's if it has not arrived)
            S subscription = subscriptionUpdates.take();

            // set the state to active
            setState(State.Active);

            return subscription;
        }
        catch (InterruptedException interruptedException)
        {
            if (Logger.isEnabled(Logger.WARN))
            {
                Logger.log(Logger.WARN,
                           "Subscriber was interrupted while waiting for its subscription state to arrive.\n%s",
                           interruptedException);
            }

            // attempt to clean up the subscriber (local resources)
            shutdown(State.Interrupted);

            // throw a suitable runtime exception
            throw new SubscriberInterruptedException(getSubscriptionIdentifier(),
                                                     "Subscriber was interrupted while waiting for subscription state to arrive",
                                                     interruptedException);
        }
    }


    /**
     * Ensures that we have a {@link Subscription} from which we may retrieve {@link Message}s.
     */
    void ensureSubscription()
    {
        State state = getState();

        if (state == State.Starting)
        {
            // wait for the subscription state to arrive
            getNextSubscriptionUpdate();
        }
        else if (state == State.Removed)
        {
            // throw a suitable runtime exception
            throw new SubscriptionLostException(getSubscriptionIdentifier(),
                                                "Subscriber subscription was lost (with expired or the underlying destination has been removed)");
        }
        else if (state == State.Shutdown)
        {
            // throw a suitable runtime exception
            throw new SubscriptionLostException(getSubscriptionIdentifier(),
                                                "Attempting to use a Subscription that has been previously released or shutdown");
        }
        else if (state == State.Interrupted)
        {
            // throw a suitable runtime exception
            throw new SubscriptionLostException(getSubscriptionIdentifier(),
                                                "Attempting to use a Subscription that was previously interrupted and shutdown");
        }
    }


    /**
     * {@inheritDoc}
     */
    public SubscriptionIdentifier getSubscriptionIdentifier()
    {
        return subscriptionIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getDestinationIdentifier()
    {
        return subscriptionIdentifier.getDestinationIdentifier();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isActive()
    {
        return getState() == State.Starting || getState() == State.Active;
    }


    /**
     * Ensures that the {@link Subscription} for the {@link Subscriber}.
     * is still valid and that the {@link Subscriber} instance may be used
     * to receive {@link Message}s
     */
    void ensureActive()
    {
        if (!isActive())
        {
            throw new SubscriptionLostException(getSubscriptionIdentifier(), "Attempted to use an inactive Subscriber");
        }
    }


    /**
     * Shutdowns the local resources for the {@link Subscriber}.
     *
     * @param finalState final state
     */
    protected synchronized void shutdown(State finalState)
    {
        if (isActive())
        {
            // clean up the map listener
            CacheFactory.getCache(Subscription.CACHENAME).removeMapListener(mapListener, subscriptionIdentifier);
            this.mapListener = null;

            // we're now shutdown
            setState(finalState);

            // shutdown the lease maintainer - it's no longer needed
            if (leaseMaintainerThread != null && leaseMaintainerThread.isAlive())
            {
                leaseMaintainerThread.terminate();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void unsubscribe()
    {
        if (isActive())
        {
            // shutdown local resources for the subscription
            shutdown(State.Shutdown);

            // Unsubscribe from the destination.  Wait until the subscription is deleted to avoid the bug in INC-795
            CacheFactory.getCache(Destination.CACHENAME).invoke(getDestinationIdentifier(),
                                                                new UnsubscribeProcessor(getSubscriptionIdentifier()));
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAutoCommitting()
    {
        return autocommit;
    }


    /**
     * {@inheritDoc}
     */
    public void setAutoCommit(boolean autoCommit)
    {
        if (autoCommit &&!this.autocommit)
        {
            this.rollback();
        }

        this.autocommit = autoCommit;
    }


    /**
     * A {@link Thread} that will attempt to continuously maintain
     * the {@link Lease} for the specified {@link QueueSubscription}.
     */
    static class LeaseMaintainerThread extends Thread
    {
        /**
         * The duration to wait before attempting to refresh the lease for
         * the {@link Subscriber}.
         */
        private static final long LEASE_REFRESH_TIME_MS = 1000 * 2;    // two seconds

        /**
         * The {@link Subscriber} for which we are maintaining it's {@link Lease}.
         */
        private Subscriber subscriber;

        /**
         * Should the {@link LeaseMaintainerThread} be terminated at the next opportunity?
         */
        private boolean isTerminated;


        /**
         * Standard Constructor.
         *
         * @param subscriber destination subscriber
         */
        public LeaseMaintainerThread(Subscriber subscriber)
        {
            this.subscriber   = subscriber;
            this.isTerminated = false;
        }


        /**
         * Terminate this {@link LeaseMaintainerThread} at the next opportunity.
         */
        public synchronized void terminate()
        {
            this.isTerminated = true;
        }


        /**
         * Returns if the {@link LeaseMaintainerThread} should be terminated at the next opportunity.
         *
         * @return true if subscription terminated.
         */
        public synchronized boolean isTerminated()
        {
            return isTerminated;
        }


        /**
         * {@inheritDoc}
         */
        public void run()
        {
            if (Logger.isEnabled(Logger.QUIET))
            {
                Logger.log(Logger.QUIET,
                           "LeaseMaintainerThread for %s commenced",
                           subscriber.getSubscriptionIdentifier());
            }

            while (!isTerminated())
            {
                try
                {
                    if (Logger.isEnabled(Logger.QUIET))
                    {
                        Logger.log(Logger.QUIET,
                                   "LeaseMaintainerThread for %s sleeping for %d ms",
                                   subscriber.getSubscriptionIdentifier(),
                                   LEASE_REFRESH_TIME_MS);
                    }

                    Thread.sleep(LEASE_REFRESH_TIME_MS);

                    if (isTerminated())
                    {
                        break;
                    }

                    // Extend the lease.  Passing 0 will result in using default value in the LeasedSubscriptionConfiguration
                    CacheFactory.getCache(Subscription.CACHENAME).invoke(subscriber.getSubscriptionIdentifier(),
                                                                         new UpdaterProcessor("extendLease", (long) 0));

                    // update the lease (using an entry processor)
                    if (Logger.isEnabled(Logger.QUIET))
                    {
                        Logger.log(Logger.QUIET,
                                   "LeaseMaintainerThread for %s extending current lease",
                                   subscriber.getSubscriptionIdentifier());
                    }

                    if (Logger.isEnabled(Logger.QUIET))
                    {
                        Logger.log(Logger.QUIET,
                                   "LeaseMaintainerThread for %s has extended the lease",
                                   subscriber.getSubscriptionIdentifier());
                    }
                }
                catch (InterruptedException interruptedException)
                {
                    if (Logger.isEnabled(Logger.QUIET))
                    {
                        Logger.log(Logger.QUIET,
                                   "LeaseMaintainerThread for %s interrupted",
                                   subscriber.getSubscriptionIdentifier());
                    }
                }
            }

            if (Logger.isEnabled(Logger.QUIET))
            {
                Logger.log(Logger.QUIET,
                           "LeaseMaintainerThread for %s completed",
                           subscriber.getSubscriptionIdentifier());
            }
        }
    }
}
