/*
 * File: TopicSubscription.java
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
import com.oracle.coherence.patterns.messaging.entryprocessors.SubscriptionRollbackProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * A {@link TopicSubscription} represents the <b>state</b> of {@link Subscription} to
 * a {@link Topic}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class TopicSubscription extends LeasedSubscription
{
    /**
     * The {@link Map} of {@link MessageTracker}s that are visible to the {@link Subscription}.  Each partition has its own map.
     * A message is in the delivered list when it has been given to the subscriber but the subscriber has not committed it.
     * In autocommit mode the delivered message range will be empty.
     *
     */
    private MessageTracker deliveredMessageTracker = new DefaultMessageTracker();


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public TopicSubscription()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param leasedSubscriptionConfiguration subscription configuration
     * @param creationTime subscription creation time
     */
    public TopicSubscription(SubscriptionIdentifier          subscriptionIdentifier,
                             Status                          status,
                             LeasedSubscriptionConfiguration leasedSubscriptionConfiguration,
                             long                            creationTime)
    {
        super(subscriptionIdentifier, status, leasedSubscriptionConfiguration, creationTime);
        this.deliveredMessageTracker = new DefaultMessageTracker();
    }


    /**
     * Standard Constructor.
     *
     * @param destinationIdentifier destination identifier
     * @param subscriberName subscriber name
     * @param leasedSubscriptionConfiguration subscription configuration
     * @param creationTime subscription
     */
    public TopicSubscription(Identifier                      destinationIdentifier,
                             String                          subscriberName,
                             Status                          status,
                             LeasedSubscriptionConfiguration leasedSubscriptionConfiguration,
                             long                            creationTime)
    {
        this(new SubscriptionIdentifier(destinationIdentifier,
                                        subscriberName), status, leasedSubscriptionConfiguration, creationTime);

        this.deliveredMessageTracker = new DefaultMessageTracker();

    }


    /**
     * Get the MessageTracker for all delivered messages.
     *
     * @return MessageTracker
     */
    public MessageTracker getDeliveredMessageTracker()
    {
        return this.deliveredMessageTracker;
    }


    /**
     * Returns if the {@link TopicSubscription} is durable.
     *
     * @return true if subscription is durable
     */
    public boolean isDurable()
    {
        return getConfiguration() != null && getConfiguration() instanceof TopicSubscriptionConfiguration
               && ((TopicSubscriptionConfiguration) getConfiguration()).isDurable();
    }


    /**
     * {@inheritDoc}
     */
    public void onLeaseSuspended(Object leaseOwner,
                                 Lease  lease)
    {
        if (Logger.isEnabled(Logger.DEBUG))
        {
            Logger.log(Logger.DEBUG, "The lease for %s has been suspended. Rolling back delivered messages\n", this);
        }

        CacheFactory.getCache(Subscription.CACHENAME).invoke(getIdentifier(), new SubscriptionRollbackProcessor());
    }


    /**
     * {@inheritDoc}
     */
    public void onLeaseExpiry(Object leaseOwner,
                              Lease  lease)
    {
        if (isDurable())
        {
            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG,
                           "The lease for durable %s has expired. Rolling back delivered messages\n",
                           this);
            }

            CacheFactory.getCache(Subscription.CACHENAME).invoke(getIdentifier(), new SubscriptionRollbackProcessor());
        }
        else
        {
            super.onLeaseExpiry(leaseOwner, lease);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void onAcknowledgeMessages(MessageTracker messageTracker)
    {
        super.onAcknowledgeMessages(messageTracker);
        this.deliveredMessageTracker.removeAll(messageTracker);
    }


    /**
     * {@inheritDoc}
     */
    public MessageTracker onAcknowledgeAllMessages()
    {
        MessageTracker outTracker = super.onAcknowledgeAllMessages();

        deliveredMessageTracker.clear();

        return outTracker;
    }


    /**
     * Handles when a {@link Message} is delivered to a {@link TopicSubscription}.
     * Remove the messages from the visible list and put them on the delivered list.
     *
     * @param messageTracker message tracker
     */
    public void onMessagesDelivered(MessageTracker messageTracker)
    {
        getVisibleMessageTracker().removeAll(messageTracker);
        this.deliveredMessageTracker.addAll(messageTracker);
    }


    /**
     * Returns the next {@link MessageIdentifier} that should be delivered
     * to the {@link Subscription}.
     *
     * @return message identifier
     */
    public MessageIdentifier nextMessageToDeliver()
    {
        MessageIdentifier messageIdentifier = super.nextMessageToDeliver();

        return messageIdentifier;
    }


    /**
     * Roll back all messages in the {@link MessageTracker}.
     *
     * @param messagesToRollback messages to rollback
     */
    public void rollback(MessageTracker messagesToRollback)
    {
        this.deliveredMessageTracker.removeAll(messagesToRollback);
        this.getVisibleMessageTracker().addAll(messagesToRollback);
    }


    /**
     * Roll back all messages that have been delivered but not acknowledged.
     */
    public void rollback()
    {
        this.getVisibleMessageTracker().addAll(deliveredMessageTracker);
        this.deliveredMessageTracker.clear();
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        deliveredMessageTracker = (DefaultMessageTracker) ExternalizableHelper.readObject(in);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, deliveredMessageTracker);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        deliveredMessageTracker = (DefaultMessageTracker) reader.readObject(300);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(300, deliveredMessageTracker);

    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("TopicSubscription{%s, deliveredMessages=%s}",
                             super.toString(),
                             deliveredMessageTracker.toString());

    }
}
