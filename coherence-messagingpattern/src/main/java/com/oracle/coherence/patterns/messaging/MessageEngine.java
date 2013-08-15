/*
 * File: MessageFSM.java
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

import com.oracle.coherence.common.finitestatemachines.AnnotationDrivenModel;
import com.oracle.coherence.common.finitestatemachines.Event;
import com.oracle.coherence.common.finitestatemachines.Instruction;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine.CoalescedEvent;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.patterns.messaging.entryprocessors.ExposeMessageToQueueProcessor;
import com.oracle.coherence.patterns.messaging.entryprocessors.RegisterSubscriptionsWithMessageProcessor;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;

import com.tangosol.util.extractor.ChainedExtractor;

import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.UpdaterProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The finite state machine for coalescing of message event processing.
 *
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *
 * @author David Rowlands
 */
public class MessageEngine extends AbstractEngine
{
    /**
     * A continuous query cache used to optimize access to the {@link Topic} subscription lists.
     */
    private volatile static ContinuousQueryCache cqcTopicCache = null;


    /**
     * Constructor for message finite state machine.
     * @param destinationIdentifier  identifier for destination.
     */
    protected MessageEngine(Identifier destinationIdentifier)
    {
        super(destinationIdentifier);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        m_fsm = new NonBlockingFiniteStateMachine<State>("Message",
                                                       new AnnotationDrivenModel<State>(State.class,
                                                                                        new Model()),
                                                       State.IDLE,
                                                       executor,
                                                       false);

    }


    /**
     * Method description
     *
     * @param msgpublisher
     */
    public void processRunEvent(MessagePublisher msgpublisher)
    {
        Event<State> messageEvent = new MessageEvent<State>(new StateEvent(State.RUNNING),
                                                            NonBlockingFiniteStateMachine.CoalescedEvent.Process
                                                                .MOST_RECENT,
                                                            msgpublisher,
                                                            msgpublisher);

        m_fsm.process(messageEvent);
    }


    Instruction onRunning(Event<State> event)
    {
        MessageTracker   tracker   = MessagesToExpose.getInstance().getTrackerSnapShot(getDestinationIdentifier());

        MessagePublisher publisher = ((MessageEvent<State>) event).getMessagePublisher();

        if ((tracker != null) && (!tracker.isEmpty()))
        {
            if (publisher.isQueue())
            {
                exposeQueueMessageBatch(getDestinationIdentifier(), tracker);
            }
            else if (publisher.isTopic())
            {
                exposeTopicMessageBatch(getDestinationIdentifier(), tracker);
            }
        }

        return new Instruction.TransitionTo<State>(State.IDLE);
    }


    /**
     * Expose the message batch to a queue or topic.
     *
     * @param destinationIdentifier destination identifier
     * @param tracker messages to expose
     */
    private void exposeQueueMessageBatch(Identifier     destinationIdentifier,
                                         MessageTracker tracker)
    {
        NamedCache destinations = CacheFactory.getCache(Destination.CACHENAME);

        destinations.invoke(destinationIdentifier, new ExposeMessageToQueueProcessor(tracker));

        ArrayList<MessageKey> messageKeys = tracker.getMessageKeys(destinationIdentifier);

        CacheFactory.getCache(Message.CACHENAME).invokeAll(messageKeys,
                                                           new UpdaterProcessor("setVisible", Boolean.TRUE));
    }


    /**
     * Expose all of the messages identifiers in the delivery tracker to all of the topic subscriptions.  Then call
     * the "makeVisibleTo" method for each message.
     *
     * @param destinationIdentifier destination identifier
     * @param tracker message tracker
     */
    @SuppressWarnings("unchecked")
    private void exposeTopicMessageBatch(Identifier     destinationIdentifier,
                                         MessageTracker tracker)
    {
        NamedCache messageCache = CacheFactory.getCache(Message.CACHENAME);

        // Get the subscriptions to be notified about the messages.
        NamedCache                  cqcTopicCache = getDestinationCache();
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


    /**
     * Get the continuous query cache for the  {@link Topic}.
     *
     * @return continuous query cache topic cache
     */
    private ContinuousQueryCache getDestinationCache()
    {
        if (cqcTopicCache == null)
        {
            // Create a continuous query cache for the topic so that the subscription list can be retrieved quickly
            NamedCache cache  = CacheFactory.getCache(Destination.CACHENAME);
            Filter     filter = new EqualsFilter(new ChainedExtractor("getClass.getName"), Topic.class.getName());

            cqcTopicCache = new ContinuousQueryCache(cache, filter);
        }

        return cqcTopicCache;
    }


    /**
     * Class description
     *
     * @param <S>
     *
     * @version        Enter version here..., 13/07/03
     * @author         Enter your name here...
     */
    public static class MessageEvent<S extends Enum<S>> extends NonBlockingFiniteStateMachine.CoalescedEvent<S>
    {
        private MessagePublisher publisher;


        /**
         * Constructs ...
         *
         * @param event          the {@link Event} to be coalesced
         * @param mode           which {@link CoalescedEvent}s to process
         * @param discriminator  the descriminator used to uniquely coalesce the {@link Event}
         * @param publisher      the message publisher which is stored in the event is used in the event processing
          */
        public MessageEvent(Event<S>         event,
                            Process          mode,
                            Object           discriminator,
                            MessagePublisher publisher)
        {
            super(event, mode, discriminator);
            this.publisher = publisher;
        }


        /**
         * Method description
         *
         * @return MessagePublisher
         */
        public MessagePublisher getMessagePublisher()
        {
            return publisher;
        }
    }
}
