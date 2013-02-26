/*
 * File: Subscription.java
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

import com.oracle.coherence.common.backingmaplisteners.Cause;
import com.oracle.coherence.common.backingmaplisteners.LifecycleAwareCacheEntry;
import com.oracle.coherence.common.ranges.Ranges;
import com.oracle.coherence.patterns.messaging.management.MessagingMBeanManager;
import com.oracle.coherence.patterns.messaging.management.QueueSubscriptionProxy;
import com.oracle.coherence.patterns.messaging.management.SubscriptionProxy;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link Subscription} represents the current state of a
 * subscriber of {@link Message}s for a {@link Destination}.
 * <p>
 * This class is abstract as there are many forms of {@link Subscription}s to
 * {@link Destination}s, each with their own particular semantics and
 * state management requirements.  For example, a {@link Subscription} to
 * a {@link Topic} is very different from a {@link Subscription} to a {@link Queue}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class Subscription implements ExternalizableLite, PortableObject, LifecycleAwareCacheEntry
{
    /**
     * All {@link Subscription}s have a {@link Status} indicating if they are current accepting messages.
     */
    public enum Status
    {
        /**
         * When a {@link Subscription} is {@link #ENABLED} it may accept messages to consume.
         */
        ENABLED,

        /**
         * When a {@link Subscription} is {@link #DISABLED} is won't accept messages to consume.
         */
        DISABLED;
    }


    /**
     * The name of the Coherence Cache that will store {@link Subscription}s.
     */
    public static final String CACHENAME = "coherence.messagingpattern.subscriptions";

    /**
     * The {@link SubscriptionIdentifier} identifying the {@link Subscription}.
     */
    private SubscriptionIdentifier subscriptionIdentifier;

    /**
     * This is a map<partitionId, lastMsgSeqNum> used to keep track of the last
     * message sequence number received per partition.
     */
    private HashMap<Integer, Long> lastMessageSequenceNumberMap = null;

    /**
     * The {@link Map} of {@link Ranges} that are visible to the {@link Subscription}.  Each partition has its own map
     */
    private MessageTracker visibleMessageTracker = null;

    /**
     * Number of messages delivered to subscriptions.
     */
    private long numMessagesAcknowledged;

    /**
     * Number of messages received by subscriber.
     */
    private long numMessagesReceived;

    /**
     * The {@link Status} of the {@link Subscription}.
     */
    private Status status;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public Subscription()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public Subscription(SubscriptionIdentifier subscriptionIdentifier,
                        Status                 status)
    {
        this.subscriptionIdentifier = subscriptionIdentifier;
        this.createVisibleMessageTracker();
        this.numMessagesAcknowledged      = 0;
        this.numMessagesReceived          = 0;
        this.status                       = status;

        this.lastMessageSequenceNumberMap = new HashMap<Integer, Long>();
    }


    /**
     * Get subscription name.
     *
     * @return subscription name
     */
    public String getName()
    {
        return this.subscriptionIdentifier.toString();
    }


    /**
     * Get the number of messages received.
     *
     * @return the number of messages received.
     */
    public long getNumMessagesReceived()
    {
        return numMessagesReceived;
    }


    /**
     * Get the number of messages acknowledged.
     *
     * @return the number of messages acknowledged.
     */
    public long getNumMessagesAcknowledged()
    {
        return numMessagesAcknowledged;
    }


    /**
     * Returns the {@link SubscriptionIdentifier} of the {@link Subscription}.
     *
     * @return SubscriptionIdentifier
     */
    public SubscriptionIdentifier getIdentifier()
    {
        return subscriptionIdentifier;
    }


    /**
     * Determines the {@link Status} of the {@link Subscription}
     *
     * @return The {@link Status}
     */
    public Status getStatus()
    {
        return status;
    }


    /**
     * Sets the {@link Status} of the {@link Subscription} to {@link Status#ENABLED}.
     */
    public void enable()
    {
        this.status = Status.ENABLED;
    }


    /**
     * Sets the {@link Status} of the {@link Subscription} to {@link Status#DISABLED}.
     */
    public void disable()
    {
        this.status = Status.DISABLED;
    }


    /**
     * Check if the subscription has visible messages.
     *
     * @return true if the subscription has visible messages.
     */
    public boolean hasVisibleMessages()
    {
        return visibleMessageTracker.size() != 0;
    }


    /**
     * Get the number of visible messages.
     *
     * @return the number of visible messages.
     */
    public long getNumMessages()
    {
        return visibleMessageTracker.size();
    }


    /**
     * Get the tracker of visible messages.
     *
     * @return the tracker of visible messages.
     */
    public MessageTracker getVisibleMessageTracker()
    {
        return visibleMessageTracker;
    }


    /**
     * Rollback the subscription.  This is implemented by super classes but cannot be abstract since
     * push replication instantiates the Subscription class and never performs rollback.
     */
    public void rollback()
    {
    }


    /**
     * Create the visible message tracker.
     */
    protected void createVisibleMessageTracker()
    {
        this.visibleMessageTracker = new DefaultMessageTracker("VisibleMessageTracker");
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
     * Accept the messages in the {@link MessageTracker} and add them to
     * deliver message list.
     *
     * @param messageTracker message tracker
     *
     * @return Returns <code>true</code> if the {@link Subscription} has accepted the messages, <code>false</code>
     *         otherwise.
     */
    public boolean onAcceptMessage(MessageTracker messageTracker)
    {
        // we should only accept a message if the subscription is enabled
        if (status == Status.ENABLED)
        {
            Iterator<MessageIdentifier> iter = messageTracker.iterator();

            while (iter.hasNext())
            {
                MessageIdentifier messageIdentifier = iter.next();

                acceptOneMessage(messageIdentifier, true);
            }

            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Add a message to the deliver message list.  Ignore the message if it has already been added.
     *
     * @param messageIdentifier message identifier
     * @param forceAccept Always accept the message, don't check if it a duplicate.  This is used for re-delivered messages.
     *
     * @return Returns <code>true</code> if the {@link Subscription} has accepted the message, <code>false</code>
     *         otherwise.
     */
    public Boolean acceptOneMessage(MessageIdentifier messageIdentifier,
                                    Boolean           forceAccept)
    {
        // we should only accept a message if the subscription is enabled
        if (status == Status.ENABLED)
        {
            Integer partitionId = Integer.valueOf(messageIdentifier.getPartitionId());

            if (forceAccept.booleanValue())
            {
                visibleMessageTracker.add(messageIdentifier);
            }
            else
            {
                if (lastMessageSequenceNumberMap.containsKey(partitionId))
                {
                    long lastMsgSeqNum = this.lastMessageSequenceNumberMap.get(partitionId).longValue();

                    if (messageIdentifier.getMessageSequenceNumber() <= lastMsgSeqNum)
                    {
                        return Boolean.FALSE;    // duplicate message, ignore it.
                    }
                }

                lastMessageSequenceNumberMap.put(partitionId, new Long(messageIdentifier.getMessageSequenceNumber()));
                visibleMessageTracker.add(messageIdentifier);
            }

            numMessagesReceived++;

            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Rollback an accepted message so that it can be accepted by this subscription again.  This is needed
     * by queue subscription rollback.  We only keep the last sequence number, not the history so simply set the
     * last sequence number to one less than the input sequence number so that it can be re-accepted.
     *
     * @param messageIdentifier message identifier
     */
    protected void rollbackAcceptedMessage(MessageIdentifier messageIdentifier)
    {
        Integer partitionId = Integer.valueOf(messageIdentifier.getPartitionId());
        long    newSeqNum   = messageIdentifier.getMessageSequenceNumber();

        if (lastMessageSequenceNumberMap.containsKey(partitionId))
        {
            numMessagesReceived--;

            if (newSeqNum == 1)
            {
                lastMessageSequenceNumberMap.remove(partitionId);
            }
            else
            {
                long lastMsgSeqNum = this.lastMessageSequenceNumberMap.get(partitionId).longValue();

                if (newSeqNum >= lastMsgSeqNum)
                {
                    lastMessageSequenceNumberMap.put(partitionId, new Long(newSeqNum - 1));
                }
            }
        }

    }


    /**
     * Returns the next {@link Message} id that should be delivered
     * to the {@link Subscription}.
     *
     * @return MessageIdentifier
     */
    public MessageIdentifier nextMessageToDeliver()
    {
        MessageIdentifier           messageIdentifier;

        Iterator<MessageIdentifier> iter = getVisibleMessageTracker().iterator();

        if (iter.hasNext())
        {
            messageIdentifier = iter.next();
        }
        else
        {
            messageIdentifier = MessageIdentifier.getNullIdentifier();
        }

        return messageIdentifier;
    }


    /**
     * Handles when a {@link MessageTracker} of visible {@link Message}s for this
     * {@link Subscription} have been acknowledged.
     * <p>
     * Once the {@link Message}s have been acknowledged, they are no longer visible
     * to this {@link Subscription}.
     *
     * @param messageTracker A {@link MessageTracker} of {@link Message} ids to acknowledge.
     */
    public void onAcknowledgeMessages(MessageTracker messageTracker)
    {
        numMessagesAcknowledged += messageTracker.size();
        getVisibleMessageTracker().removeAll(messageTracker);
    }


    /**
     * Acknowledge all {@link Message}s that have been delivered.
     *
     * @return MessageTracker containing message identifiers that have been acknowledged.
     */
    public MessageTracker onAcknowledgeAllMessages()
    {
        numMessagesAcknowledged += getVisibleMessageTracker().size();

        MessageTracker outTracker = visibleMessageTracker;

        visibleMessageTracker = new DefaultMessageTracker();

        return outTracker;
    }


    /**
     * {@inheritDoc}
     */
    public void onCacheEntryLifecycleEvent(MapEvent mapEvent,
                                           Cause    cause)
    {
        if (this instanceof LeasedSubscription)
        {
            ((LeasedSubscription) this).onCacheEntryEvent(mapEvent, cause);
        }

        if (mapEvent.getId() == MapEvent.ENTRY_INSERTED || mapEvent.getId() == MapEvent.ENTRY_UPDATED)
        {
            // Register the subscription MBEAN
            if (this instanceof QueueSubscription)
            {
                MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

                MBeanManager.registerMBean(this,
                                           QueueSubscriptionProxy.class,
                                           MBeanManager.buildQueueSubscriptionMBeanName(getIdentifier()));
            }
            else
            {
                MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

                MBeanManager.registerMBean(this,
                                           SubscriptionProxy.class,
                                           MBeanManager.buildTopicSubscriptionMBeanName(getIdentifier()));
            }

        }
        else
        {
            // Unregister the subscription MBEAN
            if (this instanceof QueueSubscription)
            {
                MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

                MBeanManager.unregisterMBean(this, MBeanManager.buildQueueSubscriptionMBeanName(getIdentifier()));
            }
            else
            {
                MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

                MBeanManager.unregisterMBean(this, MBeanManager.buildTopicSubscriptionMBeanName(getIdentifier()));
            }

        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        subscriptionIdentifier       = (SubscriptionIdentifier) ExternalizableHelper.readExternalizableLite(in);
        visibleMessageTracker        = (DefaultMessageTracker) ExternalizableHelper.readObject(in);
        numMessagesReceived          = ExternalizableHelper.readLong(in);
        numMessagesAcknowledged      = ExternalizableHelper.readLong(in);

        lastMessageSequenceNumberMap = new HashMap<Integer, Long>();

        ClassLoader loader = getClass().getClassLoader();

        ExternalizableHelper.readMap(in, lastMessageSequenceNumberMap, loader);

        status = Status.valueOf(ExternalizableHelper.readSafeUTF(in));
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeExternalizableLite(out, subscriptionIdentifier);
        ExternalizableHelper.writeObject(out, visibleMessageTracker);
        ExternalizableHelper.writeLong(out, numMessagesReceived);
        ExternalizableHelper.writeLong(out, numMessagesAcknowledged);
        ExternalizableHelper.writeMap(out, lastMessageSequenceNumberMap);
        ExternalizableHelper.writeSafeUTF(out, status.name());
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        subscriptionIdentifier       = (SubscriptionIdentifier) reader.readObject(0);
        visibleMessageTracker        = (DefaultMessageTracker) reader.readObject(1);
        numMessagesReceived          = reader.readLong(2);
        numMessagesAcknowledged      = reader.readLong(3);

        lastMessageSequenceNumberMap = new HashMap<Integer, Long>();
        reader.readMap(4, lastMessageSequenceNumberMap);
        status = Status.valueOf(reader.readString(5));
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, subscriptionIdentifier);
        writer.writeObject(1, visibleMessageTracker);
        writer.writeLong(2, numMessagesReceived);
        writer.writeLong(3, numMessagesAcknowledged);
        writer.writeMap(4, lastMessageSequenceNumberMap);
        writer.writeString(5, status.name());
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Subscription{subscriptionIdentifier=%s, status=%s}", subscriptionIdentifier, status);

    }
}
