/*
 * File: Topic.java
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
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.events.processing.EventProcessorFactory;
import com.oracle.coherence.patterns.messaging.Subscription.Status;
import com.oracle.coherence.patterns.messaging.entryprocessors.AcknowledgeMessageProcessor;
import com.oracle.coherence.patterns.messaging.management.MessagingMBeanManager;
import com.oracle.coherence.patterns.messaging.management.TopicProxy;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.net.CacheFactory;

import java.util.ArrayList;

/**
 * A {@link Topic} is a {@link Destination} that manages the state of a
 * one-to-many messaging implementation.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Topic extends Destination implements EventProcessorFactory<EntryEvent>
{
    /**
     * For {@link ExternalizableLite}.
     */
    public Topic()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param topicName topic name
     */
    public Topic(String topicName)
    {
        super(topicName);
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
                        subscription == null ? new TopicSubscription(subscriptionIdentifier,
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
        ArrayList<MessageKey> messageKeys = messageTracker.getMessageKeys(getIdentifier());

        CacheFactory.getCache(Message.CACHENAME).invokeAll(messageKeys,
                                                           new AcknowledgeMessageProcessor(subscriptionIdentifier));

        super.unsubscribe(subscriptionIdentifier, messageTracker);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Topic{%s}", super.toString());
    }


    /**
     * Register or unregister the MBean depending on the event.  There is no need to run an
     * event processor for Topic.
     *
     * @param e event
     * @return event processor
     */
    public EventProcessor<EntryEvent> getEventProcessor(EntryEvent e)
    {
        if ((e instanceof BackingMapEntryInsertedEvent) || (e instanceof BackingMapEntryArrivedEvent)
            || (e instanceof BackingMapEntryUpdatedEvent))
        {
            MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

            MBeanManager.registerMBean(this, TopicProxy.class, MBeanManager.buildTopicMBeanName(getIdentifier()));
        }
        else if (e instanceof BackingMapEntryRemovedEvent)
        {
            MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

            MessagingMBeanManager.getInstance().unregisterMBean(this,
                                                                MBeanManager.buildTopicMBeanName(getIdentifier()));
        }

        return null;
    }
}
