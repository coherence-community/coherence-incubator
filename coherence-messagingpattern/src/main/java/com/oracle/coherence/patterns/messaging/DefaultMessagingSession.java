/*
 * File: DefaultMessagingSession.java
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
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.identifiers.UUIDBasedIdentifier;
import com.oracle.coherence.patterns.messaging.entryprocessors.CreateDestinationProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.PublishMessageProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.QueueSubscribeProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.TopicSubscribeProcessor;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.filter.LikeFilter;

import java.util.Set;

/**
 * The default implementation of the {@link MessagingSession} interface.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class DefaultMessagingSession implements MessagingSession
{
    /**
     * The publisherId is a UUID that is used by the destination to ensure
     * that all messages for this publisher are put into the same partition.
     */
    private Identifier publisherId;

    /**
     * Request number that, along with publisherId, uniquely identifies a request to publish a {@link Message}.
     */
    private long requestNumber;


    /**
     * Standard Constructor.
     */
    public DefaultMessagingSession()
    {
        publisherId   = UUIDBasedIdentifier.newInstance();
        requestNumber = 1;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier createTopic(String topicName)
    {
        return createTopic(StringBasedIdentifier.newInstance(topicName));
    }


    /**
     * Create either a {@link Topic}.
     *
     * @param destinationIdentifier  destination identifier
     * @return destination identifier
     */
    private Identifier createTopic(Identifier destinationIdentifier)
    {
        StringBasedIdentifier strId = (StringBasedIdentifier) destinationIdentifier;
        Topic                 topic = new Topic(strId.getString());

        return createDestination(destinationIdentifier, topic);
    }


    /**
     * {@inheritDoc}
     */
    public Identifier createQueue(String queueName)
    {
        Queue queue = new Queue(queueName);

        return createDestination(queueName, queue);
    }


    /**
     * Create either a {@link Topic} or {@link Queue}.
     *
     * @param destinationName name of destination
     * @param destination destination
     * @return destination identifier
     */
    private Identifier createDestination(String      destinationName,
                                         Destination destination)
    {
        return createDestination(StringBasedIdentifier.newInstance(destinationName), destination);
    }


    /**
     * Create either a {@link Topic} or {@link Queue} using an existing {@link Destination}.
     *
     * @param destinationIdentifier destination identifier
     * @param destination destination
     * @return destination identifier
     */
    private Identifier createDestination(Identifier  destinationIdentifier,
                                         Destination destination)
    {
        NamedCache destinationsCache = CacheFactory.getCache(Destination.CACHENAME);

        destinationsCache.invoke(destinationIdentifier, new CreateDestinationProcessor(destination));

        return destinationIdentifier;

    }


    /**
     * Publish the message by creating a {@link Message} object and putting it directly into
     * the backing map. See {@link PublishMessageProcessor} for further explanation.
     *
     * This method must be synchronized since we are generating a request id that must be
     * in the order that the {@link PublishMessageProcessor} runs.  This id is used to prevent messages
     * from being published twice in the case of a partition fail over.  This request id is also used
     * by AbstractSubscriber to verify that messages sent by this publisher arrived in the correct order.
     *
     * @param destinationIdentifier destination identifier
     * @param payload message payload
     *
     */
    public synchronized void publishMessage(Identifier destinationIdentifier,
                                            Object     payload)
    {
        // Invoke the processor to put the message in the cache. Note that the
        // publisherId
        // is used as a key but there is no entry in the map for that key. This
        // will ensure
        // that all messages published by this session (i.e. publisher) go to
        // the same partition
        //
        NamedCache               messageCache = CacheFactory.getCache(Message.CACHENAME);
        PublishRequestIdentifier requestId    = new PublishRequestIdentifier(this.publisherId, this.requestNumber++);

        messageCache.invoke(this.publisherId, new PublishMessageProcessor(destinationIdentifier, requestId, payload));

    }


    /**
     * {@inheritDoc}
     */
    public void publishMessage(String destinationName,
                               Object payload)
    {
        publishMessage(StringBasedIdentifier.newInstance(destinationName), payload);
    }


    /**
     * {@inheritDoc}
     */
    public Subscriber subscribe(Identifier                destination,
                                SubscriptionConfiguration subscriptionConfiguration)
    {
        return subscribe(destination, subscriptionConfiguration, null);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public Subscriber subscribe(Identifier                destinationIdentifier,
                                SubscriptionConfiguration subscriptionConfiguration,
                                Subscription              subscription)
    {
        Subscriber subscriber = null;

        // The destinationId may exist before the actual destination object
        // (topic) is put in the cache.
        // Push Replication does this - see DefaultPushReplicationManager.
        //
        if (destinationIdentifier != null)
        {
            NamedCache  destinationsCache = CacheFactory.getCache(Destination.CACHENAME);
            Destination destination       = (Destination) destinationsCache.get(destinationIdentifier);

            if (destination == null)
            {
                this.createTopic(destinationIdentifier);
            }
        }

        // use the class name of the destination to determine the type of
        // subscriber to create
        if (Destination.isQueue(destinationIdentifier))
        {
            SubscriptionIdentifier subscriptionIdentifier = new SubscriptionIdentifier(destinationIdentifier,
                                                                                       UUIDBasedIdentifier
                                                                                           .newInstance());

            subscriber = new QueueSubscriber(this, subscriptionIdentifier);

            // Invoke against the destination cache.
            //
            NamedCache destinationCache = CacheFactory.getCache(Destination.CACHENAME);
            QueueSubscribeProcessor processor =
                new QueueSubscribeProcessor<SubscriptionConfiguration>(subscriptionIdentifier,
                                                                       (SubscriptionConfiguration) subscriptionConfiguration,
                                                                       null);

            destinationCache.invoke(destinationIdentifier, processor);

        }
        else
        {
            SubscriptionIdentifier subscriptionIdentifier;

            if (subscriptionConfiguration instanceof TopicSubscriptionConfiguration
                && ((TopicSubscriptionConfiguration) subscriptionConfiguration).getName() != null)
            {
                subscriptionIdentifier = new SubscriptionIdentifier(destinationIdentifier,
                                                                    ((TopicSubscriptionConfiguration) subscriptionConfiguration)
                                                                        .getName());
            }
            else
            {
                subscriptionIdentifier = new SubscriptionIdentifier(destinationIdentifier,
                                                                    UUIDBasedIdentifier.newInstance());
            }

            subscriber = new TopicSubscriber(this, subscriptionIdentifier);

            // Invoke against the destination cache.
            //
            NamedCache destinationCache = CacheFactory.getCache(Destination.CACHENAME);
            TopicSubscribeProcessor processor =
                new TopicSubscribeProcessor<SubscriptionConfiguration>(subscriptionIdentifier,
                                                                       (SubscriptionConfiguration) subscriptionConfiguration,
                                                                       subscription);

            destinationCache.invoke(destinationIdentifier, processor);
        }

        return subscriber;
    }


    /**
     * {@inheritDoc}
     */
    public Subscriber subscribe(String                    destinationName,
                                SubscriptionConfiguration subscriptionConfiguration)
    {
        return subscribe(StringBasedIdentifier.newInstance(destinationName), subscriptionConfiguration);
    }


    /**
     * {@inheritDoc}
     */
    public Subscriber subscribe(String destinationName)
    {
        return subscribe(StringBasedIdentifier.newInstance(destinationName));
    }


    /**
     * {@inheritDoc}
     */

    public Subscriber subscribe(Identifier destination)
    {
        // return subscribe(destination,
        // TopicSubscriptionConfiguration.newNonDurableConfiguration());
        return subscribe(destination, new DefaultSubscriptionConfiguration());
    }


    /**
     * {@inheritDoc}
     */

    /*
     * public Subscriber subscribe(Identifier destination) { return
     * subscribe(destination, new DefaultSubscriptionConfiguration()); }
     */

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterable<Identifier> getTopicIdentifiers()
    {
        // NOTE: we should use the Command Pattern to provide infrastructure for
        // this...
        Set<Identifier> identifiers =
            (Set<Identifier>) CacheFactory.getCache(Destination.CACHENAME).keySet(new LikeFilter("getClass.getName",
                                                                                                 "%Topic%"));

        return identifiers;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterable<Identifier> getQueueIdentifiers()
    {
        // NOTE: we should use the Command Pattern to provide infrastructure for
        // this...
        Set<Identifier> identifiers =
            (Set<Identifier>) CacheFactory.getCache(Destination.CACHENAME).keySet(new LikeFilter("getClass.getName",
                                                                                                 "%Queue%"));

        return identifiers;
    }


    /**
     * Returns a new instance of the {@link DefaultMessagingSession}.
     *
     * @return messaging session
     */
    public static MessagingSession getInstance()
    {
        DefaultMessagingSession instance = new DefaultMessagingSession();

        return instance;
    }


    /**
     * Get the session identifier.
     *
     * @return session identifier
     */
    public String getSessionId()
    {
        return publisherId.toString();
    }
}
