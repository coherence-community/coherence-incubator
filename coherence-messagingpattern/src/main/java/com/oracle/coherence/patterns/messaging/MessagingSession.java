/*
 * File: MessagingSession.java
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

/**
 * A {@link MessagingSession} provides mechanisms for a). publishing
 * {@link Message}s to {@link Destination}s (typically {@link Topic}s
 * and {@link Queue}s) and b). creating and managing {@link Subscriber}s
 * that may receive {@link Message}s for the said {@link Destination}s.
 * <p>
 * While a {@link MessagingSession} instance is generally designed to be used by
 * a single-thread, multiple-threads may use a single {@link MessagingSession}
 * instance to <strong>publish</strong> a {@link Message} to any {@link Destination}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface MessagingSession
{
    /**
     * Creates a {@link Topic} {@link Destination} to which messages may be published.
     * Returns a unique {@link Identifier} for the created {@link Topic}.  If the topic
     * already exists, the existing {@link Topic} {@link Identifier} is returned.
     *
     * @param topicName topic name
     * @return destination identifier
     */
    public Identifier createTopic(String topicName);


    /**
     * Creates a {@link Queue} {@link Destination} to which messages may be published.
     * Returns a unique {@link Identifier} for the created {@link Queue}.  If the queue
     * already exists, the existing {@link Queue} {@link Identifier} is returned.
     *
     * @param queueName queue name
     * @return destination identifier
     */
    public Identifier createQueue(String queueName);


    /**
     * Creates a {@link Subscription} to the specified {@link Destination} configured using the
     * provided {@link SubscriptionConfiguration} and returns a {@link Subscriber} capable of
     * consuming {@link Message}s from the said {@link Destination}.
     *
     * @param destination destination
     * @param subscriptionConfiguration subscription configuration
     * @param subscription subscription
     * @return subscriber
     */
    public Subscriber subscribe(Identifier                destination,
                                SubscriptionConfiguration subscriptionConfiguration,
                                Subscription              subscription);


    /**
     * Creates a {@link Subscription} to the specified {@link Destination} configured using the
     * provided {@link SubscriptionConfiguration} and returns a {@link Subscriber} capable of
     * consuming {@link Message}s from the said {@link Destination}.
     *
     * @param destination destination
     * @param subscriptionConfiguration subscription configuration
     * @return subscriber
     */
    public Subscriber subscribe(Identifier                destination,
                                SubscriptionConfiguration subscriptionConfiguration);


    /**
     * Creates a {@link Subscription} to the named {@link Destination} configured using the
     * provided {@link SubscriptionConfiguration} and returns a {@link Subscriber} capable of
     * consuming {@link Message}s from the said {@link Destination}.
     *
     * @param destinationName name of destination
     * @param subscriptionConfiguration subscription configuration
     * @return subscriber
     */
    public Subscriber subscribe(String                    destinationName,
                                SubscriptionConfiguration subscriptionConfiguration);


    /**
     * Creates and returns a {@link Subscriber} for the specified {@link Destination}
     * using a {@link DefaultSubscriptionConfiguration}.
     *
     * @param destination destination
     * @return subscriber
     */
    public Subscriber subscribe(Identifier destination);


    /**
     * Creates and returns a {@link Subscriber} for the named {@link Destination}
     * using a {@link DefaultSubscriptionConfiguration}.
     *
     * @param destinationName name of destination
     * @return subscriber
     */
    public Subscriber subscribe(String destinationName);


    /**
     * Publishes some payload at a message to a {@link Destination} with the specified
     * {@link Identifier}.
     *
     * @param destinationIdentifier destination identifier
     * @param payload message payload
     */
    public void publishMessage(Identifier destinationIdentifier,
                               Object     payload);


    /**
     * Publishes some payload at a message to a {@link Destination} with the specified
     * name.
     *
     * @param destinationName name of destination
     * @param payload message payload
     */
    public void publishMessage(String destinationName,
                               Object payload);


    /**
     * Returns an {@link Iterable} for the current {@link Identifier}s of {@link Topic}s.
     *
     * @return topic iterator
     */
    public Iterable<Identifier> getTopicIdentifiers();


    /**
     * Returns an {@link Iterable} for the current {@link Identifier}s of {@link Queue}s.
     *
     * @return queue iterator
     */
    public Iterable<Identifier> getQueueIdentifiers();
}
