/*
 * File: CoherenceEventChannelController.java
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

package com.oracle.coherence.patterns.eventdistribution.distributors.coherence;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.processors.InvokeMethodProcessor;
import com.oracle.coherence.common.tuples.Pair;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.distributors.AbstractEventChannelController;
import com.oracle.coherence.patterns.messaging.DefaultMessageTracker;
import com.oracle.coherence.patterns.messaging.Message;
import com.oracle.coherence.patterns.messaging.MessageIdentifier;
import com.oracle.coherence.patterns.messaging.MessageKey;
import com.oracle.coherence.patterns.messaging.MessageTracker;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.oracle.coherence.patterns.messaging.entryprocessors.AcknowledgeMessageProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.AcknowledgeSubscriptionMessagesProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.DrainSubscriptionMessagesProcessor;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.processor.ExtractorProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link CoherenceEventChannelController} is a JMX monitorable implementation of a {@link EventChannelController}. It
 * is responsible for asynchronously providing {@link EventChannel} with {@link Event}s to distribute in the
 * back ground.  There is typically one instance of this class per registered {@link EventChannel}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceEventChannelController extends AbstractEventChannelController<MessageTracker>
    implements CoherenceEventChannelControllerMBean
{
    /**
     * The {@link Logger} to use for this class.
     */
    private static Logger logger = Logger.getLogger(CoherenceEventChannelController.class.getName());

    /**
     * The {@link SubscriptionIdentifier} used to determine the subscription state for messages in the messaging layer.
     */
    private SubscriptionIdentifier subscriptionIdentifier;

    /**
     * A map that keeps track of the previous sequence number received by this subscriber by a given {@link EventChannel}.
     * This is used to verify that sequence numbers are in order.
     */
    private ConcurrentHashMap<com.oracle.coherence.common.identifiers.Identifier, Long> prevMessageIdentifierMap =
        new ConcurrentHashMap<com.oracle.coherence.common.identifiers.Identifier, Long>();


    /**
     * Standard Constructor.
     */
    public CoherenceEventChannelController(EventDistributor.Identifier         distributorIdentifier,
                                           EventChannelController.Identifier   controllerIdentifier,
                                           EventChannelController.Dependencies controllerDependencies,
                                           Environment                         environment,
                                           ParameterProvider                   parameterProvider,
                                           ParameterizedBuilder<Serializer>    serializerBuilder)
    {
        super(distributorIdentifier,
              controllerIdentifier,
              controllerDependencies,
              environment,
              parameterProvider,
              serializerBuilder);

        // the subscriber is to the topic identified by the cacheName, with the subscriber being the external name.
        this.subscriptionIdentifier =
            new SubscriptionIdentifier(StringBasedIdentifier.newInstance(distributorIdentifier.getExternalName()),
                                       controllerIdentifier.getExternalName());
    }


    /**
     * This verifies that a message from a given controller was received in order by this subscriber.
     *
     * @param message
     */
    private boolean verifyMessageSequence(Message message)
    {
        long newSequenceNumber = message.getRequestIdentifier().getMessageSequenceNumber();
        com.oracle.coherence.common.identifiers.Identifier controllerIdentifier =
            message.getRequestIdentifier().getPublisherIdentifier();
        Long prevSequenceNumber = prevMessageIdentifierMap.get(controllerIdentifier);

        if (prevSequenceNumber != null)
        {
            prevMessageIdentifierMap.put(controllerIdentifier, newSequenceNumber);

            // Verify that the new message requestId is greater or equal than the previous message.
            // It will only be equal in the case of failover where the same message may get delivered twice.
            if (prevSequenceNumber.longValue() > newSequenceNumber)
            {
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE,
                               "Message sequence number out of order.  Prev = {0} New = {1}",
                               new Object[] {prevSequenceNumber, newSequenceNumber});
                }

                return false;
            }
        }
        else
        {
            prevMessageIdentifierMap.put(controllerIdentifier, Long.valueOf(newSequenceNumber));
        }

        return true;
    }


    /**
     * Determines the {@link SubscriptionIdentifier} of the {@link CoherenceEventChannelSubscription}
     * that this {@link EventChannelController} will be using to distribute {@link Event}s.
     *
     * @return A {@link SubscriptionIdentifier}.
     */
    public SubscriptionIdentifier getSubscriptionIdentifier()
    {
        return subscriptionIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalEnable()
    {
        NamedCache subscriptionsCache = CacheFactory.getCache(Subscription.CACHENAME);

        subscriptionsCache.invoke(subscriptionIdentifier, new InvokeMethodProcessor("enable"));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalDisable()
    {
        NamedCache subscriptionsCache = CacheFactory.getCache(Subscription.CACHENAME);

        subscriptionsCache.invoke(subscriptionIdentifier, new InvokeMethodProcessor("disable"));

        internalDrain();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalDrain()
    {
        // NOTE: This could be dangerous as if this member dies some messages may be left orphaned
        // (we should fix this by having the messaging layer do it for us)

        // determine the MessageTracker of messages to delete
        NamedCache subscriptionsCache = CacheFactory.getCache(Subscription.CACHENAME);
        MessageTracker messageTracker = (MessageTracker) subscriptionsCache.invoke(subscriptionIdentifier,
                                                                                   new DrainSubscriptionMessagesProcessor());

        if ((messageTracker != null) &&!messageTracker.isEmpty())
        {
            // remove the messages that fall in the range
            NamedCache messagesCache = CacheFactory.getCache(Message.CACHENAME);

            messagesCache.invokeAll(messageTracker.getMessageKeys(subscriptionIdentifier.getDestinationIdentifier()),
                                    new AcknowledgeMessageProcessor(subscriptionIdentifier));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalStart() throws EventChannelNotReadyException
    {
        channel.connect(distributorIdentifier, controllerIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalStop()
    {
        channel.disconnect();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Pair<List<Event>, MessageTracker> getEventsToDistribute()
    {
        MessageTracker   messageTracker = new DefaultMessageTracker();
        ArrayList<Event> eventList      = new ArrayList<Event>(controllerDependencies.getBatchSize());

        // get the range of message ids we could possibly distribute
        NamedCache subscriptions = CacheFactory.getCache(Subscription.CACHENAME);
        MessageTracker candidateMessageTracker = (MessageTracker) subscriptions.invoke(subscriptionIdentifier,
                                                                                       new ExtractorProcessor("getVisibleMessageTracker"));

        // any messages to distribute?
        if (candidateMessageTracker != null &&!candidateMessageTracker.isEmpty())
        {
            // create the set of keys for messages to retrieve
            ArrayList<MessageKey>       messageKeys = new ArrayList<MessageKey>(controllerDependencies.getBatchSize());

            Iterator<MessageIdentifier> candidateMessageIds = candidateMessageTracker.iterator();

            while (candidateMessageIds.hasNext() && messageKeys.size() < controllerDependencies.getBatchSize())
            {
                MessageIdentifier messageId = candidateMessageIds.next();

                messageKeys.add(Message.getKey(subscriptionIdentifier.getDestinationIdentifier(), messageId));
                messageTracker.add(messageId);
            }

            // get all of the messages
            NamedCache               messagesCache = CacheFactory.getCache(Message.CACHENAME);
            Map<MessageKey, Message> messages      = (Map<MessageKey, Message>) messagesCache.getAll(messageKeys);

            // fill the event list with the messages containing events
            for (MessageKey messageKey : messageKeys)
            {
                Message message = messages.get(messageKey);

                if (message != null &&!message.isAcknowledgedBy(subscriptionIdentifier))
                {
                    verifyMessageSequence(message);

                    if (message.getPayload() instanceof Event)
                    {
                        eventList.add((Event) message.getPayload());
                    }
                }

            }

        }

        return new Pair<List<Event>, MessageTracker>(eventList, messageTracker);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void acknowledgeDistributedEvents(List<Event>    events,
                                                MessageTracker messageTracker)
    {
        // create the set of keys for messages to acknowledge
        ArrayList<MessageKey>       messageKeys = new ArrayList<MessageKey>(events.size());
        Iterator<MessageIdentifier> messageIds  = messageTracker.iterator();

        while (messageIds.hasNext())
        {
            MessageIdentifier messageId = messageIds.next();

            messageKeys.add(Message.getKey(subscriptionIdentifier.getDestinationIdentifier(), messageId));
        }

        // remove this subscriber from each of the messages in this batch
        // (to help clean up the messages when they are completely consumed)
        NamedCache messagesCache = CacheFactory.getCache(Message.CACHENAME);

        messagesCache.invokeAll(messageKeys, new AcknowledgeMessageProcessor(subscriptionIdentifier));

        // remove the range we've just distributed from the subscriber
        NamedCache subscriptions = CacheFactory.getCache(Subscription.CACHENAME);

        subscriptions.invoke(subscriptionIdentifier, new AcknowledgeSubscriptionMessagesProcessor(messageTracker));
    }
}
