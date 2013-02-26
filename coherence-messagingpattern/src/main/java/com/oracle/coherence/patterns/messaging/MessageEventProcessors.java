/*
 * File: MessageEventProcessors.java
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
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.processing.AbstractAsynchronousEventProcessor;
import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.messaging.entryprocessors.ExposeMessageToQueueProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.RegisterSubscriptionsWithMessageProcessor;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.processor.UpdaterProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The {@link MessageEventProcessors} class contains the event processors that perform actions asynchronously
 * after a message has been inserted into the message cache. The event processors expose a batch of messages
 * to either a queue or topic.  If the destination is a queue, then a batch of message identifiers is exposed
 * to the queue, which in turn exposes the messages to subscriptions.
 * If the  destination is a topic, the the batch is exposed to each subscription in the topic.  Messages that need
 * to be exposed are managed by the {@link MessagesToExpose} class, which batches messages per destination.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see {@link MessageEventManager}
 *
 * @author Paul Mackin
 */
class MessageEventProcessors
{
    /**
     * A {@link AbstractMessageEventProcessor}�is the base class for message event processors.  This class
     * wait until partition transfer is done.
     * �
     */
    public abstract static class AbstractMessageEventProcessor extends AbstractAsynchronousEventProcessor<EntryEvent>
    {
        /**
         * {@link Identifier} of destination that owns message batch.
         */
        private Identifier destinationIdentifier;


        /**
         * Constructor.
         *
         * @param destinationIdentifier destination identifier
         */
        AbstractMessageEventProcessor(Identifier destinationIdentifier)
        {
            this.destinationIdentifier = destinationIdentifier;
        }


        /**
         * {@inheritDoc}
         */
        public void processLater(EventDispatcher dispatcher,
                                 EntryEvent      event)
        {
            ProcessorStateManager stateManager = MessageEventManager.getInstance().getStateManager();

            // Get the tracker containing message identifiers of messages to be delivered.
            stateManager.setRunning(destinationIdentifier);

            MessageTracker tracker = MessagesToExpose.getInstance().getTrackerSnapShot(destinationIdentifier);

            if ((tracker == null) || (tracker.isEmpty()))
            {
                stateManager.setIdle(destinationIdentifier);

                return;
            }

            exposeMessageBatch(destinationIdentifier, tracker);
            stateManager.setIdle(destinationIdentifier);

        }


        /**
         * Abstract class implemented by subclasses to process the delivery tracker batch of {@link MessageIdentifier}s.
         *
         * @param destinationIdentifier destination identifier
         * @param tracker message tracker
         */
        abstract void exposeMessageBatch(Identifier     destinationIdentifier,
                                         MessageTracker tracker);
    }


    /**
     * A {@link ExposeToQueueProcessor}�exposes a batch of messages to a queue.
     * �
     */
    public static class ExposeToQueueProcessor extends AbstractMessageEventProcessor
    {
        /**
         * Constructor.
         *
         * @param destinationIdentifier destination identifier
         */
        ExposeToQueueProcessor(Identifier destinationIdentifier)
        {
            super(destinationIdentifier);
        }


        /**
         * Expose the message batch to a queue or topic.
         *
         * @param destinationIdentifier destination identifier
         * @param tracker messages to expose
         */
        @Override
        void exposeMessageBatch(Identifier     destinationIdentifier,
                                MessageTracker tracker)
        {
            NamedCache destinations = CacheFactory.getCache(Destination.CACHENAME);

            destinations.invoke(destinationIdentifier, new ExposeMessageToQueueProcessor(tracker));

            ArrayList<MessageKey> messageKeys = tracker.getMessageKeys(destinationIdentifier);

            CacheFactory.getCache(Message.CACHENAME).invokeAll(messageKeys,
                                                               new UpdaterProcessor("setVisible", Boolean.TRUE));

        }
    }


    /**
     * A {@link ExposeToTopicProcessor}�exposes a batch of messages to a all of the subscriptions in a topic.
     * �
     */
    public static class ExposeToTopicProcessor extends AbstractMessageEventProcessor
    {
        /**
         *  Constructor.
         *
         * @param destinationIdentifier destination identifier
         */
        ExposeToTopicProcessor(Identifier destinationIdentifier)
        {
            super(destinationIdentifier);
        }


        /**
         * Expose all of the messages identifiers in the delivery tracker to all of the topic subscriptions.  Then call
         * the "makeVisibleTo" method for each message.
         *
         * @param destinationIdentifier destination identifier
         * @param tracker message tracker
         */
        void exposeMessageBatch(Identifier     destinationIdentifier,
                                MessageTracker tracker)
        {
            NamedCache messageCache = CacheFactory.getCache(Message.CACHENAME);

            // Get the subscriptions to be notified about the messages.
            NamedCache                  cqcTopicCache = MessageEventManager.getInstance().getDestinationCache();
            Destination                 destination   = (Destination) cqcTopicCache.get(destinationIdentifier);
            Set<SubscriptionIdentifier> subscriptions = destination.getSubscriptionIdentifiers();

            // If there are no subscriptions then delete the message we just put
            // in the cache.
            if (subscriptions.isEmpty())
            {
                Iterator<MessageIdentifier> iter = tracker.iterator();

                while (iter.hasNext())
                {
                    MessageIdentifier messageIdentifier = iter.next();
                    MessageKey        messageKey        = Message.getKey(destinationIdentifier, messageIdentifier);

                    messageCache.remove(messageKey);
                }

                return;
            }

            // Tell all of the subscriptions about the messages
            NamedCache subscriptionCache = CacheFactory.getCache(Subscription.CACHENAME);
            Map<SubscriptionIdentifier, Boolean> mapSubscriptions =
                (Map<SubscriptionIdentifier, Boolean>) subscriptionCache.invokeAll(subscriptions,
                                                                                   new UpdaterProcessor("onAcceptMessage", tracker));

            // build a set of the subscriptions that accepted the messages
            HashSet<SubscriptionIdentifier> activeSubscriptions = new HashSet<SubscriptionIdentifier>();

            for (SubscriptionIdentifier subscriptionIdentifier : mapSubscriptions.keySet())
            {
                if (mapSubscriptions.get(subscriptionIdentifier))
                {
                    activeSubscriptions.add(subscriptionIdentifier);
                }
            }

            // Call the makeVisibleTo method for each message to let the message know about the
            // subscriptions that are consuming the message.
            // INC-1033. There is a race condition where the subscription could ack the message before
            // the message is visible.  This is why a custom EntryProcessor is used here (it handles that case)
            // The old code used and UpdaterProcessor which is not good enough.
            ArrayList<MessageKey> messageKeys = tracker.getMessageKeys(destinationIdentifier);

            messageCache.invokeAll(messageKeys,
                                   new RegisterSubscriptionsWithMessageProcessor(new SubscriptionIdentifierSet(activeSubscriptions)));
        }
    }
}
