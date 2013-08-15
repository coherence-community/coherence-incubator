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
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

import com.oracle.coherence.common.liveobjects.LiveObject;
import com.oracle.coherence.common.liveobjects.OnArrived;
import com.oracle.coherence.common.liveobjects.OnDeparting;
import com.oracle.coherence.common.liveobjects.OnInserted;
import com.oracle.coherence.common.liveobjects.OnRemoved;
import com.oracle.coherence.common.liveobjects.OnUpdated;

import com.oracle.coherence.patterns.messaging.Subscription.Status;
import com.oracle.coherence.patterns.messaging.entryprocessors.AcknowledgeMessageProcessor;
import com.oracle.coherence.patterns.messaging.management.MessagingMBeanManager;
import com.oracle.coherence.patterns.messaging.management.TopicProxy;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.BinaryEntry;

import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link Topic} is a {@link Destination} that manages the state of a
 * one-to-many messaging implementation.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@LiveObject
@SuppressWarnings("serial")
public class Topic extends Destination
{
    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(Topic.class.getName());


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
     * When an Entry is inserted, updated, or arrives via a partition transfer need to ensure that the MBean
     * for this {@link Topic} is properly visible.
     *
     * @param entry The {@link BinaryEntry} provided by the LiveObjectInterceptor
     */
    @OnInserted
    @OnUpdated
    @OnArrived
    public void onChanged(BinaryEntry entry)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Topic:onChanged identifier:{0}", getIdentifier());
        }

        MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

        MBeanManager.registerMBean(this, TopicProxy.class, MBeanManager.buildTopicMBeanName(getIdentifier()));
    }


    /**
     * When a {@link Topic} is removed from the cache or is removed via a departing partition
     * need to unregister its MBean.
     *
     * @param entry The {@link BinaryEntry} provided by the LiveObjectInterceptor
     */
    @OnRemoved
    @OnDeparting
    public void onRemoved(BinaryEntry entry)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Topic:onRemoved identifier:{0}", getIdentifier());
        }

        MessagingMBeanManager MBeanManager = MessagingMBeanManager.getInstance();

        MBeanManager.unregisterMBean(this, MBeanManager.buildTopicMBeanName(getIdentifier()));
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Topic{%s}", super.toString());
    }
}
