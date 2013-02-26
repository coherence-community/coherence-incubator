/*
 * File: QueueDeliveryResults.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;

/**
 * A {@link QueueDeliveryResults} contains the {@link MessageIdentifier}s of {@link Message}s
 * delivered along with the list of {@link Subscription}s they were delivered to.
 * {@link Queue#processDeliveryResults} is invoked from the Queue QueueEntryProcessor
 * to process the results after a background delivery of messages completes.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class QueueDeliveryResults implements ExternalizableLite, PortableObject
{
    /**
     * These are messages that were delivered to subscriptions.
     */
    private LinkedList<MessageIdentifier> deliveredMessages;

    /**
     * These are messages that need to added to the re-delivery list.
     */
    private LinkedList<MessageIdentifier> messagesToRedeliver;

    /**
     * The list of subscriptions that had messages delivered to them.
     *
     */
    private LinkedList<SubscriptionIdentifier> deliveredSubscriptions;


    /**
     * For {@link ExternalizableLite} and {@link PortableObject}.
     */
    public QueueDeliveryResults()
    {
        deliveredMessages      = new LinkedList<MessageIdentifier>();
        messagesToRedeliver    = new LinkedList<MessageIdentifier>();
        deliveredSubscriptions = new LinkedList<SubscriptionIdentifier>();
    }


    /**
     * Check if there is a message that was delivered.
     *
     * @return true if there is at least one message in the delivered list.
     */
    public boolean hasMessage()
    {
        return this.deliveredMessages.size() > 0;
    }


    /**
     * Remove the next message that was delivered and return it.
     *
     * @return MessageIdentifier
     */
    public MessageIdentifier nextMessage()
    {
        return deliveredMessages.isEmpty() ? null : deliveredMessages.removeFirst();
    }


    /**
     * Check if there are messages to re-deliver.
     *
     * @return true if there are messages to re-deliver.
     */
    public boolean hasMessagesToRedeliver()
    {
        return this.messagesToRedeliver.size() > 0;
    }


    /**
     * Get the next message to re-deliver.
     *
     * @return next message to re-deliver.
     */
    public MessageIdentifier nextMessageToRedeliver()
    {
        return messagesToRedeliver.isEmpty() ? null : messagesToRedeliver.removeFirst();
    }


    /**
     * Add a message to the delivered list.
     *
     * @param messageIdentifier message identifier
     */
    public void addMessage(MessageIdentifier messageIdentifier)
    {
        deliveredMessages.add(messageIdentifier);
    }


    /**
     * Add a message to the redelivery list.
     *
     * @param messageIdentifier message identifier
     */
    public void addRedeliverMessage(MessageIdentifier messageIdentifier)
    {
        messagesToRedeliver.add(messageIdentifier);
    }


    /**
     * Check if there are any subscriptions that received a message.
     *
     * @return true if a subscription received a message
     */
    public boolean hasSubscriptions()
    {
        return deliveredSubscriptions.size() > 0;
    }


    /**
     * Remove the next subscription that received a message.
     *
     * @return SubscriptionIdentifier
     */
    public SubscriptionIdentifier nextSubscription()
    {
        return deliveredSubscriptions.isEmpty() ? null : deliveredSubscriptions.removeFirst();
    }


    /**
     * Add a subscription to the list of subscriptions that received a message.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void addSubscription(SubscriptionIdentifier subscriptionIdentifier)
    {
        deliveredSubscriptions.add(subscriptionIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        deliveredMessages = new LinkedList<MessageIdentifier>();
        ExternalizableHelper.readCollection(in, deliveredMessages, Thread.currentThread().getContextClassLoader());

        messagesToRedeliver = new LinkedList<MessageIdentifier>();
        ExternalizableHelper.readCollection(in, messagesToRedeliver, Thread.currentThread().getContextClassLoader());

        deliveredSubscriptions = new LinkedList<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in, deliveredSubscriptions, Thread.currentThread().getContextClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeCollection(out, deliveredMessages);
        ExternalizableHelper.writeCollection(out, messagesToRedeliver);
        ExternalizableHelper.writeCollection(out, deliveredSubscriptions);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        deliveredMessages = new LinkedList<MessageIdentifier>();
        reader.readCollection(100, deliveredMessages);

        messagesToRedeliver = new LinkedList<MessageIdentifier>();
        reader.readCollection(101, messagesToRedeliver);

        deliveredSubscriptions = new LinkedList<SubscriptionIdentifier>();
        reader.readCollection(102, deliveredSubscriptions);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeCollection(100, deliveredMessages);
        writer.writeCollection(101, messagesToRedeliver);
        writer.writeCollection(102, deliveredSubscriptions);
    }
}
