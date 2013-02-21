/*
 * File: Message.java
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
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.events.processing.EventProcessorFactory;
import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link Message} represents a developer provided
 * payload to be acknowledged by one or more {@link Subscription}s.
 * <p>
 * To publish a {@link Message} use an implementation of
 * {@link MessagingSession#publishMessage(Identifier, Object)}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Message implements EventProcessorFactory<EntryEvent>, ExternalizableLite, PortableObject
{
    /**
     * The name of the Coherence Cache that will store {@link Message}s.
     */
    public static final String CACHENAME = "coherence.messagingpattern.messages";

    /**
     * The key that identifies a {@link Message} in a coherence cache..
     */
    private MessageKey key;

    /**
     * The set of {@link SubscriptionIdentifier}s of {@link Subscription}s
     * to which the {@link Message} is visible.  This is the set of
     * {@link Subscription}s that must acknowledged the {@link Message} before
     * the {@link Message} is removed from the messaging infrastructure.
     */
    private Set<SubscriptionIdentifier> visibleTo;

    /**
     * The set of {@link SubscriptionIdentifier}s of {@link Subscription}s
     * that have received delivery of the {@link Message}.  When a {@link Subscription}
     * reads a {@link Message} they place their {@link SubscriptionIdentifier} in
     * this set.
     */
    private Set<SubscriptionIdentifier> deliveredTo;

    /**
     * The set of {@link SubscriptionIdentifier}s of {@link Subscription}s
     * that have acknowledged the {@link Message}.  When this set is the same as
     * the {@link #visibleTo} set, the {@link Message} may be safely removed
     * from the messaging infrastructure.
     *
     * A {@link Subscription} may not receive a {@link Message} again once it
     * belongs to this set.
     */
    private Set<SubscriptionIdentifier> acknowledgedBy;

    /**
     * The developer provided payload of the {@link Message}.
     */
    private Object payload;

    /**
     * True if message is visible to topic or queue.
     */
    private boolean visible;

    /**
     * The publisher generated {@link Identifier} associated with the request to publish the message.
     * This identifier is used to prevent a message from being published twice in the case of partition transfer.
     */
    private PublishRequestIdentifier requestIdentifier;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public Message()
    {
    }


    /**
     * Standard Constructor.
     *
     * To construct a {@link Message}, you should use an implementation of
     * {@link MessagingSession#publishMessage(Identifier, Object)} .
     *
     * @param destinationIdentifier The {@link Identifier} of the {@link Destination} to which the {@link Message} was sent.
     * @param requestIdentifier The identifier of the publish message request.
     * @param messageIdentifier The unique and in-order sequence number of the {@link Message}.
     * @param payload The payload for the {@link Message}
     */
    public Message(Identifier               destinationIdentifier,
                   PublishRequestIdentifier requestIdentifier,
                   MessageIdentifier        messageIdentifier,
                   Object                   payload)
    {
        this.key               = new MessageKey(destinationIdentifier, messageIdentifier);
        this.visibleTo         = new TreeSet<SubscriptionIdentifier>();
        this.deliveredTo       = new TreeSet<SubscriptionIdentifier>();
        this.acknowledgedBy    = new TreeSet<SubscriptionIdentifier>();
        this.payload           = payload;
        this.requestIdentifier = requestIdentifier;
        this.visible           = false;
    }


    /**
     * Get the request identifier for the request issued to publish this message.
     *
     * @return Identifier
     */
    public PublishRequestIdentifier getRequestIdentifier()
    {
        return requestIdentifier;
    }


    /**
     * Check if {@link Message} has been made visible to the {@link Topic} or {@link Queue}.
     *
     * @return true if visible
     */
    public boolean isVisible()
    {
        return visible;
    }


    /**
     * Set flag indicating {@link Message} has been made visible to the {@link Topic} or {@link Queue}.
     *
     * @param visible true if message visible
     */
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }


    /**
     * Builds a cluster-wide unique key for the {@link Message}
     * with in it's {@link Destination}.
     *
     * @param destinationIdentifier destination identifier
     * @param partitionId partition id
     * @param messageSequenceNumber messages sequence number
     * @return MessageKey
     */
    public static MessageKey getKey(Identifier destinationIdentifier,
                                    int        partitionId,
                                    long       messageSequenceNumber)
    {
        return new MessageKey(destinationIdentifier, new MessageIdentifier(partitionId, messageSequenceNumber));
    }


    /**
     * Builds a cluster-wide unique key for the {@link Message}
     * with in it's {@link Destination}.
     *
     * @param destinationIdentifier destination identifier
     * @param messageIdentifier message identifier
     * @return MessageKey
     */
    public static MessageKey getKey(Identifier        destinationIdentifier,
                                    MessageIdentifier messageIdentifier)
    {
        return new MessageKey(destinationIdentifier, messageIdentifier);
    }


    /**
     * Returns the cluster-wide unique key for the {@link Message}.
     *
     * @return MessageKey
     */
    public MessageKey getKey()
    {
        return key;
    }


    /**
     * Get the {@link Identifier} of the {@link Destination} to which the {@link Message}
     * was published.
     *
     * @return Identifier
     */
    public Identifier getDestinationIdentifier()
    {
        return key.getDestinationIdentifier();
    }


    /**
     * Returns the unique {@link MessageIdentifier} that
     * will be used to order the delivery and consumption
     * of the said {@link Message}.
     *
     * @return MessageIdentifier
     */
    public MessageIdentifier getMessageIdentifier()
    {
        return key.getMessageIdentifier();
    }


    /**
     * Returns the payload of the {@link Message}.
     *
     * @return payload
     */
    public Object getPayload()
    {
        return payload;
    }


    /**
     * Makes the {@link Message} visible to the {@link Subscription} identified
     * by the {@link SubscriptionIdentifier}.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void makeVisibleTo(SubscriptionIdentifier subscriptionIdentifier)
    {
        visibleTo.add(subscriptionIdentifier);
        setVisible(true);
    }


    /**
     * Makes the {@link Message} visible to the {@link Subscription} identified
     * by the {@link SubscriptionIdentifier}s.
     *
     * @param subscriptionIdentifierSet subscription identifiers
     */
    public void makeVisibleTo(SubscriptionIdentifierSet subscriptionIdentifierSet)
    {
        visibleTo.addAll(subscriptionIdentifierSet.getSubscriptionIdentifiers());
        setVisible(true);
    }


    /**
     * Removes the visibility of the {@link Message} to the specified {@link Subscription}
     * by the {@link SubscriptionIdentifier}.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void makeInvisibleTo(SubscriptionIdentifier subscriptionIdentifier)
    {
        visibleTo.remove(subscriptionIdentifier);
    }


    /**
     * Returns if the {@link Message} is visible to the specified {@link SubscriptionIdentifier}.
     *
     * @param subscriptionIdentifier subscription identifier
     * @return true if message is visible
     */
    public boolean isVisibleTo(SubscriptionIdentifier subscriptionIdentifier)
    {
        return visibleTo.contains(subscriptionIdentifier);
    }


    /**
     * Adds the {@link SubscriptionIdentifier} of a {@link Subscription}
     * that has acknowledged the {@link Message}.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void acknowledgedBy(SubscriptionIdentifier subscriptionIdentifier)
    {
        // If a subscription is acknowledging the message then that message
        // was also delivered to the subscription so update the delivered list.
        this.deliveredTo.add(subscriptionIdentifier);
        this.acknowledgedBy.add(subscriptionIdentifier);
    }


    /**
     * Returns if the {@link Message} has been complete acknowledged by it's {@link Subscription}s.
     *
     * @return true if message acknowledged
     */
    public boolean isAcknowledged()
    {
        return visibleTo.size() == acknowledgedBy.size();
    }


    /**
     * Returns if the {@link Message} has already been acknowledged by the specified {@link SubscriptionIdentifier}.
     *
     * @param subscriptionIdentifier subscription identifier
     * @return true if message acknowledged
     */
    public boolean isAcknowledgedBy(SubscriptionIdentifier subscriptionIdentifier)
    {
        return acknowledgedBy.contains(subscriptionIdentifier);
    }


    /**
     * Adds the {@link SubscriptionIdentifier} of a {@link Subscription}
     * that has received (been delivered) the {@link Message}.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void deliveredTo(SubscriptionIdentifier subscriptionIdentifier)
    {
        deliveredTo.add(subscriptionIdentifier);
    }


    /**
     * Removes all traces of a {@link SubscriptionIdentifier} from the {@link Message}, including
     * if it was visible, delivered and/or acknowledged.
     *
     * This method is called when a {@link Subscription} is removed from the messaging infrastructure
     * or the said {@link Subscription} is closed.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void removeSubscriptionIdentifer(SubscriptionIdentifier subscriptionIdentifier)
    {
        visibleTo.remove(subscriptionIdentifier);
        deliveredTo.remove(subscriptionIdentifier);
        acknowledgedBy.remove(subscriptionIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        key       = (MessageKey) ExternalizableHelper.readObject(in);
        visible   = ((Boolean) ExternalizableHelper.readObject(in)).booleanValue();
        visibleTo = new HashSet<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in, visibleTo, Thread.currentThread().getContextClassLoader());
        deliveredTo = new HashSet<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in, deliveredTo, Thread.currentThread().getContextClassLoader());
        acknowledgedBy = new HashSet<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in, acknowledgedBy, Thread.currentThread().getContextClassLoader());
        payload           = ExternalizableHelper.readObject(in);
        requestIdentifier = (PublishRequestIdentifier) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, key);
        ExternalizableHelper.writeObject(out, Boolean.valueOf(visible));
        ExternalizableHelper.writeCollection(out, visibleTo);
        ExternalizableHelper.writeCollection(out, deliveredTo);
        ExternalizableHelper.writeCollection(out, acknowledgedBy);
        ExternalizableHelper.writeObject(out, payload);
        ExternalizableHelper.writeObject(out, requestIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        key       = (MessageKey) reader.readObject(0);
        visible   = ((Boolean) reader.readObject(1)).booleanValue();
        visibleTo = new HashSet<SubscriptionIdentifier>();
        reader.readCollection(2, visibleTo);
        deliveredTo = new HashSet<SubscriptionIdentifier>();
        reader.readCollection(3, deliveredTo);
        acknowledgedBy = new HashSet<SubscriptionIdentifier>();
        reader.readCollection(4, acknowledgedBy);
        payload           = reader.readObject(5);
        requestIdentifier = (PublishRequestIdentifier) reader.readObject(6);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, key);
        writer.writeObject(1, Boolean.valueOf(visible));
        writer.writeCollection(2, visibleTo);
        writer.writeCollection(3, deliveredTo);
        writer.writeCollection(4, acknowledgedBy);
        writer.writeObject(5, payload);
        writer.writeObject(6, requestIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Message{key=%s,visibleTo=%s, deliveredTo=%s, acknowledgedBy=%s, payload=%s}",
                             key.toString(),
                             visibleTo,
                             deliveredTo,
                             acknowledgedBy,
                             payload);
    }


    /**
     * Return an event processor to perform asynchronous processing of the {@link Message} after it has been
     * written to the cache.  If there is nothing to do, then null is returned.  The event processor will be scheduled
     * to run immediately
     *
     * @param e event
     * @return event processor
     */
    public EventProcessor<EntryEvent> getEventProcessor(EntryEvent e)
    {
        return MessageEventManager.getInstance().getEventProcessor(e, this);
    }
}
