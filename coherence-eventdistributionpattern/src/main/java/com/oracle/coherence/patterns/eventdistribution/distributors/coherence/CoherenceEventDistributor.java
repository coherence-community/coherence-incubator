/*
 * File: CoherenceEventDistributor.java
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
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.messaging.DefaultMessagingSession;
import com.oracle.coherence.patterns.messaging.DefaultSubscriptionConfiguration;
import com.oracle.coherence.patterns.messaging.Destination;
import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionConfiguration;
import com.oracle.coherence.patterns.messaging.entryprocessors.TopicSubscribeProcessor;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link CoherenceEventDistributor} is a {@link EventDistributor} implementation that uses Coherence to manage the
 * distribution of {@link Event}s to configured {@link EventChannel}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceEventDistributor implements EventDistributor
{
    /**
     * The {@link Logger} for the class.
     */
    private static final Logger logger = Logger.getLogger(CoherenceEventDistributorBuilder.class.getName());

    /**
     * The {@link Identifier} for the {@link EventDistributor}.
     */
    private EventDistributor.Identifier identifier;

    /**
     * The {@link MessagingSession} that we'll use to distribute {@link Event}s.
     */
    private MessagingSession messagingSession;

    /**
     * A {@link ParameterizedBuilder} that will realize the {@link Serializer} to (de)serialize {@link Event}s
     * during distribution.
     */
    private ParameterizedBuilder<Serializer> serializerBuilder;

    /**
     * The {@link Serializer} to use for (de)serializing {@link Event}s.
     */
    private Serializer serializer;


    /**
     * Standard Constructor.
     *
     * @param symbolicName              The symbolic name of the {@link EventDistributor}.
     * @param suggestedExternalName     The suggested external name of the {@link EventDistributor}.
     * @param serializerBuilder         The {@link ParameterizedBuilder} for the {@link Serializer}.
     */
    public CoherenceEventDistributor(String                           symbolicName,
                                     String                           suggestedExternalName,
                                     ParameterizedBuilder<Serializer> serializerBuilder)
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO,
                       String.format("Using Coherence-based Event Distributor [%s] (%s).", symbolicName,
                                     suggestedExternalName));
        }

        Base.azzert(symbolicName != null);
        Base.azzert(suggestedExternalName != null);
        Base.azzert(serializerBuilder != null);

        // determine an appropriate identifier for the EventDistributor
        this.identifier        = new EventDistributor.Identifier(symbolicName, suggestedExternalName);

        this.serializerBuilder = serializerBuilder;

        // NOTE: when Messaging uses the Environment, we should get the MessagingSession from the Environment
        // (or it should be injected)
        this.messagingSession = DefaultMessagingSession.getInstance();

        // create the topic to which events will be sent for distribution
        messagingSession.createTopic(getIdentifier().getExternalName());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventDistributor.Identifier getIdentifier()
    {
        return identifier;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventChannelController.Identifier establishEventChannelController(Dependencies      dependencies,
                                                                             ParameterProvider parameterProvider)
    {
        // establish the serializer (if we haven't done so already)
        if (serializer == null)
        {
            serializer = serializerBuilder.realize(parameterProvider);
        }

        // determine an EventChannelController.Identifier based on the channel and external name
        EventChannelController.Identifier controllerIdentifier =
            new EventChannelController.Identifier(dependencies.getChannelName(),
                                                  dependencies.getExternalName());

        // create a customized subscription that we'll use to trigger distribution when the subscription changes
        // ie: when a message arrives for the subscription, changes to the subscription will appropriately trigger
        // distribution of the message (Event) with an associated EventChannel.
        Subscription subscription = new CoherenceEventChannelSubscription(getIdentifier(),
                                                                          controllerIdentifier,
                                                                          dependencies,
                                                                          parameterProvider,
                                                                          serializerBuilder);

        // NOTE: The following code takes advantage of the fact that we can replace the subscription implementation
        NamedCache destinationCache = CacheFactory.getCache(Destination.CACHENAME);

        destinationCache.invoke(subscription.getIdentifier().getDestinationIdentifier(),
                                new TopicSubscribeProcessor<SubscriptionConfiguration>(subscription.getIdentifier(),
                                                                                       new DefaultSubscriptionConfiguration(),
                                                                                       subscription));

        return controllerIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void distribute(Event event)
    {
        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "Distributing {0}.", event);
        }

        messagingSession.publishMessage(StringBasedIdentifier.newInstance(getIdentifier().getExternalName()), event);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void distribute(List<Event> events)
    {
        for (Event event : events)
        {
            distribute(event);
        }
    }
}
