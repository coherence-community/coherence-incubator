/*
 * File: Queue.java
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

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryArrivedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryInsertedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryRemovedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryUpdatedEvent;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.processing.AbstractAsynchronousEventProcessor;
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.events.processing.EventProcessorFactory;
import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.common.processors.InvokeMethodProcessor;
import com.oracle.coherence.patterns.messaging.Subscription.Status;
import com.oracle.coherence.patterns.messaging.management.MessagingMBeanManager;
import com.oracle.coherence.patterns.messaging.management.QueueProxy;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.processor.UpdaterProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A {@link Queue} is a {@link Destination} that manages the state of a
 * one-to-one messaging implementation.  {@link MessageIdentifier}s are sent to the {@link Queue}
 * by the MessageEventManager and distributed to waiting {@link Subscription}s
 * in round robin order.  Each {@link Message} is delivered to a single {@link Subscription} only.  If
 * the {@link Subscription} rolls back the {@link Message} then the {@link Message} is re-delivered to
 * another {@link Subscription}
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class Queue extends Destination implements EventProcessorFactory<EntryEvent>
{
    /**
     * The {@link ProcessorStateManager} manages the state of the queue processor so that
     * the getEntryProcessor method will know whether a processor should be queued up.
     */
    private static ProcessorStateManager stateManager = new ProcessorStateManager();

    /**
     * This is a map<partitionId, lastMsgSeqNum> used to keep track of the last
     * message sequence number received per partition.
     */
    private HashMap<Integer, Long> lastMessageSequenceNumberMap = null;

    /**
     * The set of {@link Message}s that must be re-delivered
     * (before other {@link Message}s) as they have been recovered or rolled back.
     */
    private LinkedList<MessageIdentifier> messagesToRedeliver;

    /**
     * The set of {@link Message}s that must be delivered to {@link Subscription}s.
     */
    private LinkedList<MessageIdentifier> messagesToDeliver;

    /**
     * The queue of requests for {@link Message}s from Subscribers
     * (ie: {@link Subscription}s).
     *
     * When a Subscriber (via it's {@link SubscriptionIdentifier}) requests
     * a {@link Message} from the {@link Queue}, but none are available,
     * the {@link SubscriptionIdentifier} for the said {@link Subscriber} is placed
     * on the end of this ordered-list (ie: queue).
     *
     * When a {@link Message} is published, they are first allocated to
     * Subscribers in the order as they appear in the queue.
     */
    private LinkedList<SubscriptionIdentifier> waitingSubscriptions;

    /**
     * The queue of requests for {@link Message}s from Subscribers that are aleady on
     * the waiting subscription list.  Because @link Message}s are delivered to {@link Subscription}s
     * from a background thread, there is a window where the {@link Subscriber} gets a {@link Message}
     * and requests another one before the persistent waiting subscription list is updated for
     * the {@link Queue}.  As a result, the {@link Queue} must save a duplicate subscription request
     * until processes the delivery results.
     */
    private LinkedList<SubscriptionIdentifier> waitingSubscriptionsDuplicate;

    /**
     * Number of messages delivered to subscriptions since the {@link Queue} was created.
     */
    private long numMessagesDelivered;

    /**
     * Number of messages received since the {@link Queue} was created.
     */
    private long numMessagesReceived;


    /**
     * For {@link ExternalizableLite} and {@link PortableObject}.
     */
    public Queue()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param queueName queue name
     */
    public Queue(String queueName)
    {
        super(queueName);

        this.waitingSubscriptions          = new LinkedList<SubscriptionIdentifier>();
        this.waitingSubscriptionsDuplicate = new LinkedList<SubscriptionIdentifier>();
        this.messagesToDeliver             = new LinkedList<MessageIdentifier>();
        this.messagesToRedeliver           = new LinkedList<MessageIdentifier>();

        this.lastMessageSequenceNumberMap  = new HashMap<Integer, Long>();

        this.numMessagesDelivered          = 0;
        this.numMessagesReceived           = 0;
    }


    /**
     * Return the number of messages received.
     *
     * @return number of messages received.
     */
    public long getNumMessagesReceived()
    {
        return numMessagesReceived;
    }


    /**
     * Return the number of messages to re-deliver.
     *
     * @return number of messages to re-deliver.
     */
    public long getNumMessagesToRedeliver()
    {
        return (long) messagesToRedeliver.size();
    }


    /**
     * Return the number of messages delivered.
     *
     * @return number of messages delivered.
     */
    public long getNumMessagesDelivered()
    {
        return numMessagesDelivered;
    }


    /**
     * Return the number of messages to deliver.
     *
     * @return number of messages to deliver.
     */
    public long getNumMessagesToDeliver()
    {
        return (long) messagesToDeliver.size();
    }


    /**
     * Return the number of subscriptions waiting for messages.
     *
     * @return the number of subscriptions waiting for messages.
     */
    public long getNumWaitingSubscriptions()
    {
        return (long) waitingSubscriptions.size();
    }


    /**
     * Check if there are messages to re-deliver.
     *
     * @return true if there are messages to re-deliver.
     */
    private boolean hasMessagesToRedeliver()
    {
        return this.messagesToRedeliver.size() > 0;
    }


    /**
     * Check if there are messages to deliver.
     *
     * @return true if there are messages to deliver.
     */
    private boolean hasMessagesToDeliver()
    {
        return this.messagesToDeliver.size() > 0;
    }


    /**
     * Returns if there are any pending requests for {@link Message}s from the {@link Queue}.
     *
     * @return true if there waiting subscriptions.
     */
    private boolean hasWaitingSubscriptions()
    {
        return waitingSubscriptions.size() > 0;
    }


    /**
     * Return true if there are messages to deliver or re-deliver.
     *
     * @return true if there are messages to deliver or re-deliver.
     */
    private boolean isReadyToDeliver()
    {
        if (hasWaitingSubscriptions())
        {
            return (hasMessagesToDeliver() || hasMessagesToRedeliver());
        }

        return false;
    }


    /**
     * Get the next message to deliver.
     *
     * @return next message to deliver.
     */
    private MessageIdentifier nextMessageToDeliver()
    {
        return messagesToDeliver.isEmpty() ? null : messagesToDeliver.removeFirst();
    }


    /**
     * Get the next message to re-deliver.
     *
     * @return next message to re-deliver.
     */
    private MessageIdentifier nextMessageToRedeliver()
    {
        return messagesToRedeliver.isEmpty() ? null : messagesToRedeliver.removeFirst();
    }


    /**
     * Get the last sequence number of message received for a specific partition
     *
     * @param partitionId partition id of the message partition
     * @return Long the last sequence number of the message received for a given partition
     * or zero if no messages were received for that partition.
     */
    public Long getLastMessageSequenceNumber(Integer partitionId)
    {
        Long lastMsgSeqNum;

        if (lastMessageSequenceNumberMap.containsKey(partitionId))
        {
            lastMsgSeqNum = this.lastMessageSequenceNumberMap.get(partitionId);
        }
        else
        {
            lastMsgSeqNum = Long.valueOf(0);
        }

        return lastMsgSeqNum;
    }


    /**
     * Returns the {@link SubscriptionIdentifier} of the Subscriber that should
     * be provided with the next delivered {@link Message}.
     *
     * @return next subscription identifier
     */
    private SubscriptionIdentifier nextWaitingSubscription()
    {
        return waitingSubscriptions.isEmpty() ? null : waitingSubscriptions.removeFirst();
    }


    /**
     * Accept the messages in the {@link MessageTracker} and add them to
     * deliver message list.
     *
     * @param messageTracker message tracker
     */
    public void onAcceptMessage(MessageTracker messageTracker)
    {
        Iterator<MessageIdentifier> iter = messageTracker.iterator();

        while (iter.hasNext())
        {
            MessageIdentifier messageIdentifier = iter.next();

            acceptOneMessage(messageIdentifier);
        }
    }


    /**
     * Add a message to the deliver message list.  Ignore the message if it has already been added.
     *
     * @param messageIdentifier message identifier
     */
    public void acceptOneMessage(MessageIdentifier messageIdentifier)
    {
        Integer partitionId = Integer.valueOf(messageIdentifier.getPartitionId());

        if (lastMessageSequenceNumberMap.containsKey(partitionId))
        {
            long lastMsgSeqNum = this.lastMessageSequenceNumberMap.get(partitionId).longValue();

            if (messageIdentifier.getMessageSequenceNumber() <= lastMsgSeqNum)
            {
                return;    // duplicate message, ignore it.
            }
        }

        lastMessageSequenceNumberMap.put(partitionId, new Long(messageIdentifier.getMessageSequenceNumber()));
        messagesToDeliver.add(messageIdentifier);

        numMessagesReceived++;
    }


    /**
     * Requests that a {@link Message} be made visible to a {@link QueueSubscription}.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void requestMessage(SubscriptionIdentifier subscriptionIdentifier)
    {
        // add the subscription to the end of the list of waiting subscriptions
        // unless it
        // is already on the list, in which case add it to the duplicates list.
        if (!waitingSubscriptions.contains(subscriptionIdentifier))
        {
            waitingSubscriptions.add(subscriptionIdentifier);
        }
        else if (!waitingSubscriptionsDuplicate.contains(subscriptionIdentifier))
        {
            waitingSubscriptionsDuplicate.add(subscriptionIdentifier);
        }
    }


    /**
     * Rolls back a {@link MessageTracker} of messages and attempts to re-deliver all of
     * the messages to any waiting subscriptions.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param messageTracker message tracker
     */
    public void rollbackMessages(SubscriptionIdentifier subscriptionIdentifier,
                                 MessageTracker         messageTracker)
    {
        // add the range of messages to roll back to our existing set to
        // re-deliver
        addMessagesToRedeliver(messageTracker);

        // remove the subscriber from the rolled back messages
        ArrayList<MessageKey> messageKeys = messageTracker.getMessageKeys(getIdentifier());

        // Tell the {@link Message} to remove the subscription from its visible
        // list
        CacheFactory.getCache(Message.CACHENAME).invokeAll(messageKeys,
                                                           new UpdaterProcessor("makeInvisibleTo",
                                                                                subscriptionIdentifier));
    }


    /**
     * {@inheritDoc}
     */
    public void subscribe(SubscriptionIdentifier    subscriptionIdentifier,
                          SubscriptionConfiguration subscriptionConfiguration,
                          Subscription              subscription)
    {
        super.subscribe(subscriptionIdentifier,
                        subscriptionConfiguration,
                        subscription == null ? new QueueSubscription(subscriptionIdentifier,
                                                                     Status.ENABLED,
                                                                     (LeasedSubscriptionConfiguration) subscriptionConfiguration,
                                                                     CacheFactory.getSafeTimeMillis()) : subscription);
    }


    /**
     * {@inheritDoc}

     */
    public void unsubscribe(SubscriptionIdentifier subscriptionIdentifier,
                            MessageTracker         messageTracker)
    {
        // remove the subscriber from waiting list of subscribers
        waitingSubscriptions.remove(subscriptionIdentifier);

        // roll back the currently visible messages
        rollbackMessages(subscriptionIdentifier, messageTracker);

        // unsubscribe from the destination
        super.unsubscribe(subscriptionIdentifier, messageTracker);
    }


    /**
     * Add a next message to re-deliver.
     *
     * @param messageTracker message tracker
     */
    private void addMessagesToRedeliver(MessageTracker messageTracker)
    {
        Iterator<MessageIdentifier> iter = messageTracker.iterator();

        while (iter.hasNext())
        {
            messagesToRedeliver.add(iter.next());
        }
    }


    /**
     * Deliver all the messages that can be delivered. NOTE: This is called
     * from a background thread
     *
     * @return delivery results
     */
    private QueueDeliveryResults doDelivery()
    {
        // Delivery results of all messages that were delivered.
        //
        QueueDeliveryResults results = new QueueDeliveryResults();

        // Deliver a message for each waiting subscription from the re-deliver
        // list
        while (hasMessagesToRedeliver() && hasWaitingSubscriptions())
        {
            deliverMessage(results, this.getIdentifier(), nextWaitingSubscription(), nextMessageToRedeliver(), true);
        }

        // Deliver a message for each waiting subscription from the deliver list
        while (hasMessagesToDeliver() && hasWaitingSubscriptions())
        {
            deliverMessage(results, this.getIdentifier(), nextWaitingSubscription(), nextMessageToDeliver(), false);
        }

        return results;
    }


    /**
     * Deliver a single the message. NOTE: This is called from a background thread
     *
     * @param results delivery results
     * @param destinationIdentifier destination identifier
     * @param subscriptionIdentifier subscription identifier
     * @param messageIdentifier message identifier
     * @param forceAccept force the subscription to accept the message.
     *
     */
    private void deliverMessage(QueueDeliveryResults   results,
                                Identifier             destinationIdentifier,
                                SubscriptionIdentifier subscriptionIdentifier,
                                MessageIdentifier      messageIdentifier,
                                boolean                forceAccept)
    {
        MessageKey messageKey = Message.getKey(destinationIdentifier, messageIdentifier);

        try
        {
            // The message event processor will make the message visible to the
            // subscription
            CacheFactory.getCache(Message.CACHENAME).invoke(messageKey,
                                                            new UpdaterProcessor("makeVisibleTo",
                                                                                 subscriptionIdentifier));

            // make the message known to the subscription.
            NamedCache subscriptions = CacheFactory.getCache(Subscription.CACHENAME);
            InvokeMethodProcessor proc = new InvokeMethodProcessor("acceptOneMessage",
                                                                   new Object[] {messageIdentifier,
                                                                                 Boolean.valueOf(forceAccept)});
            Object acceptedObj = subscriptions.invoke(subscriptionIdentifier, proc);

            if (acceptedObj != null)
            {
                // If the message was accepted by the subscription then it is not a duplicate so update the results.
                Boolean accepted = (Boolean) acceptedObj;

                if (accepted.booleanValue())
                {
                    // Save the delivered info so that the queue object can be updated when delivery is done
                    results.addMessage(messageIdentifier);
                    results.addSubscription(subscriptionIdentifier);
                }
                else
                {
                    // The message was rejected as a duplicate.
                    // Add the message so it will be removed from the list of messages to deliver.
                    results.addMessage(messageIdentifier);
                }
            }
        }
        catch (Exception e)
        {
            Logger.log(Logger.WARN,
                       "An exception occurred when the Queue tried to invoke Message.makeVisibleTo for message %s and subscription %s.  The message will be delivered to another subscription\n",
                       messageIdentifier.toString(),
                       subscriptionIdentifier.toString());

            // Add the subscription for removal since it is most likely gone.
            results.addSubscription(subscriptionIdentifier);

            // Add the message so it will be removed from the list of messages to deliver.
            results.addMessage(messageIdentifier);

            // Finally, add the message to the redelivery list so that the subscription will be forced to accept it.
            results.addRedeliverMessage(messageIdentifier);

            // Tell the message to remove the subscription since there was a problem invoking the subscription.
            CacheFactory.getCache(Message.CACHENAME).invoke(messageKey,
                                                            new UpdaterProcessor("makeInvisibleTo",
                                                                                 subscriptionIdentifier));
        }
    }


    /**
     * Remove the entries that were delivered from the queue object.
     *
     * NOTE: This is invoked from the background thread.
     *
     * @param deliveryResults delivery results
     */
    public void processDeliveryResults(QueueDeliveryResults deliveryResults)
    {
        while (deliveryResults.hasSubscriptions())
        {
            SubscriptionIdentifier subscriptionIdentifier = deliveryResults.nextSubscription();

            // Remove the subscription from the waiting subscription list.
            if (waitingSubscriptions.contains(subscriptionIdentifier))
            {
                waitingSubscriptions.remove(subscriptionIdentifier);
            }

            // If we got a another request before these delivery results were processed then
            // add that request to the waiting subscriptions so that it can be processed.
            if (waitingSubscriptionsDuplicate.contains(subscriptionIdentifier))
            {
                waitingSubscriptions.add(subscriptionIdentifier);
                waitingSubscriptionsDuplicate.remove(subscriptionIdentifier);
            }
        }

        // Remove messages that have been delivered.
        while (deliveryResults.hasMessage())
        {
            this.numMessagesDelivered++;

            MessageIdentifier messageIdentifier = deliveryResults.nextMessage();

            if (messagesToDeliver.contains(messageIdentifier))
            {
                messagesToDeliver.remove(messageIdentifier);
            }
            else if (messagesToRedeliver.contains(messageIdentifier))
            {
                messagesToRedeliver.remove(messageIdentifier);
            }
        }

        // Add all of the messages that need to be re-delivered.  This list only contain messages
        // if there was a subscription exception (typically due to an abnormal subscriber termination).
        while (deliveryResults.hasMessagesToRedeliver())
        {
            messagesToRedeliver.add(deliveryResults.nextMessageToRedeliver());
        }

    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);

        messagesToDeliver = new LinkedList<MessageIdentifier>();
        ExternalizableHelper.readCollection(in, messagesToDeliver, Thread.currentThread().getContextClassLoader());

        messagesToRedeliver = new LinkedList<MessageIdentifier>();
        ExternalizableHelper.readCollection(in, messagesToRedeliver, Thread.currentThread().getContextClassLoader());

        waitingSubscriptions = new LinkedList<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in, waitingSubscriptions, Thread.currentThread().getContextClassLoader());

        waitingSubscriptionsDuplicate = new LinkedList<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in,
                                            waitingSubscriptionsDuplicate,
                                            Thread.currentThread().getContextClassLoader());

        lastMessageSequenceNumberMap = new HashMap<Integer, Long>();

        ClassLoader loader = getClass().getClassLoader();

        ExternalizableHelper.readMap(in, lastMessageSequenceNumberMap, loader);

        numMessagesReceived  = ExternalizableHelper.readLong(in);
        numMessagesDelivered = ExternalizableHelper.readLong(in);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeCollection(out, messagesToDeliver);
        ExternalizableHelper.writeCollection(out, messagesToRedeliver);
        ExternalizableHelper.writeCollection(out, waitingSubscriptions);
        ExternalizableHelper.writeCollection(out, waitingSubscriptionsDuplicate);
        ExternalizableHelper.writeMap(out, lastMessageSequenceNumberMap);
        ExternalizableHelper.writeLong(out, numMessagesReceived);
        ExternalizableHelper.writeLong(out, numMessagesDelivered);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        messagesToDeliver = new LinkedList<MessageIdentifier>();
        reader.readCollection(100, messagesToDeliver);

        messagesToRedeliver = new LinkedList<MessageIdentifier>();
        reader.readCollection(101, messagesToRedeliver);

        waitingSubscriptions = new LinkedList<SubscriptionIdentifier>();
        reader.readCollection(102, waitingSubscriptions);

        waitingSubscriptionsDuplicate = new LinkedList<SubscriptionIdentifier>();
        reader.readCollection(103, waitingSubscriptionsDuplicate);

        lastMessageSequenceNumberMap = new HashMap<Integer, Long>();
        reader.readMap(104, lastMessageSequenceNumberMap);

        numMessagesReceived  = reader.readLong(105);
        numMessagesDelivered = reader.readLong(106);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeCollection(100, messagesToDeliver);
        writer.writeCollection(101, messagesToRedeliver);
        writer.writeCollection(102, waitingSubscriptions);
        writer.writeCollection(103, waitingSubscriptionsDuplicate);
        writer.writeMap(104, lastMessageSequenceNumberMap);
        writer.writeLong(105, numMessagesReceived);
        writer.writeLong(106, numMessagesDelivered);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Queue{%s, messagesToDeliver=%s, messagesToRedeliver=%s,  waitingSubscriptions=%s}",
                             super.toString(),
                             messagesToDeliver,
                             messagesToRedeliver,
                             waitingSubscriptions);
    }


    /**
     * Return an event processor to perform asynchronous processing of the {@link Queue} after it has been
     * written to the cache.  The event processor will deliver {@link Message}s to {@link Subscription}s
     *
     *  @param e event
     *  @return EventProcessor processor
     */
    public EventProcessor<EntryEvent> getEventProcessor(EntryEvent e)
    {
        if ((e instanceof BackingMapEntryInsertedEvent) || (e instanceof BackingMapEntryArrivedEvent)
            || (e instanceof BackingMapEntryUpdatedEvent))
        {
            MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

            MBeanManager.registerMBean(this, QueueProxy.class, MBeanManager.buildQueueMBeanName(getIdentifier()));

            // Only return a processor if there are none waiting on the dispatch queue.
            // A processor is really used like an event to trigger background delivery by
            // the event processor, so we only need one trigger.  Without this check, the
            // dispatch event queue backs up eventually causing an out of memory error.
            if (isReadyToDeliver() &&!stateManager.isScheduled(getIdentifier()))
            {
                stateManager.setScheduled(getIdentifier());

                return new QueueEventProcessor();
            }
        }
        else if (e instanceof BackingMapEntryRemovedEvent)
        {
            MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

            MessagingMBeanManager.getInstance().unregisterMBean(this,
                                                                MBeanManager.buildQueueMBeanName(getIdentifier()));
        }

        return null;
    }


    /**
     * A {@link QueueEventProcessor} delivers {@link Message}s stored in the {@link Queue} to
     * any waiting subscriptions.
     */
    static class QueueEventProcessor extends AbstractAsynchronousEventProcessor<EntryEvent>
    {
        /**
         * {@inheritDoc}
         */
        public void processLater(EventDispatcher dispatcher,
                                 EntryEvent      event)
        {
            Identifier destinationIdentifier = (Identifier) event.getEntry().getKey();

            Queue.stateManager.setRunning(destinationIdentifier);

            Queue queue = getQueueFromBackingMap((Identifier) event.getEntry().getKey());

            if (queue == null)
            {
                return;
            }

            // Deliver any messages to waiting subscriptions
            QueueDeliveryResults results = queue.doDelivery();

            if (results.hasMessage())
            {
                // Update the queue object with the delivery results
                NamedCache destinationCache = CacheFactory.getCache(Destination.CACHENAME);

                destinationCache.invoke(queue.getIdentifier(), new UpdaterProcessor("processDeliveryResults", results));
            }

            Queue.stateManager.setIdle(destinationIdentifier);
        }


        /**
         * Get the {@link Queue} from the backing map for performance optimization.
         *
         * @param destinationIdentifier destination identifier
         * @return Queue or null if the Queue has moved or been deleted.
         */
        private Queue getQueueFromBackingMap(Identifier destinationIdentifier)
        {
            Queue                    queue      = null;

            NamedCache               cache      = CacheFactory.getCache(Destination.CACHENAME);
            CacheService             service    = cache.getCacheService();
            BackingMapManager        manager    = service.getBackingMapManager();
            BackingMapManagerContext context    = manager.getContext();
            ObservableMap            backingMap = (ObservableMap) context.getBackingMap(Destination.CACHENAME);

            Object                   binaryKey  = context.getKeyToInternalConverter().convert(destinationIdentifier);

            if (context.isKeyOwned(binaryKey))
            {
                // Get the queue - check if it is null in case the partition
                // moved.
                Object binaryVal = backingMap.get(binaryKey);

                if (binaryVal != null)
                {
                    queue = (Queue) context.getValueFromInternalConverter().convert(binaryVal);
                }
            }

            return queue;
        }
    }
}
