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

import com.oracle.coherence.common.events.Event;

import com.oracle.coherence.common.identifiers.StringBasedIdentifier;

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

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.ValueChangeEventFilter;

import com.tangosol.util.transformer.ExtractorEventTransformer;
import com.tangosol.util.transformer.SemiLiteEventTransformer;

import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

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
     * A {@link NamedCache} representing the starting modes of
     * {@link EventChannel}s according to their {@link EventChannelController}.
     */
    private volatile ContinuousQueryCache eventChannelStartingModes;

    /**
     * The number of active (enabled or suspended) {@link EventChannel}s.
     */
    private AtomicInteger activeEventChannelCount;


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

        // initially we haven't tracked the starting modes of event channels
        this.eventChannelStartingModes = null;

        // initially we haven't tracked any active event channels
        this.activeEventChannelCount = new AtomicInteger(-1);
    }


    @Override
    public EventDistributor.Identifier getIdentifier()
    {
        return identifier;
    }


    @Override
    public EventChannelController.Identifier establishEventChannelController(Dependencies      dependencies,
                                                                             ParameterResolver resolver,
                                                                             ClassLoader       loader)
    {
        // establish the NamedCache to track the event channel starting modes
        establishEventChannelStartingMode();

        // establish the serializer (if we haven't done so already)
        if (serializer == null)
        {
            serializer = serializerBuilder.realize(resolver, loader, null);
        }

        // determine an EventChannelController.Identifier based on the channel and external name
        EventChannelController.Identifier controllerIdentifier =
            new EventChannelController.Identifier(dependencies.getChannelName(),
                                                  dependencies.getExternalName());

        Parameter testName  = resolver.resolve("cache-name");
        Value     val       = (Value) testName.evaluate(resolver);
        String    cacheName = (String) val.get();

        // create a customized subscription that we'll use to trigger distribution when the subscription changes
        // ie: when a message arrives for the subscription, changes to the subscription will appropriately trigger
        // distribution of the message (Event) with an associated EventChannel.
        Subscription subscription = new CoherenceEventChannelSubscription(getIdentifier(),
                                                                          controllerIdentifier,
                                                                          dependencies,
                                                                          cacheName,
                                                                          resolver);

        // NOTE: The following code takes advantage of the fact that we can replace the subscription implementation
        NamedCache destinationCache = CacheFactory.getCache(Destination.CACHENAME);

        destinationCache.invoke(subscription.getIdentifier().getDestinationIdentifier(),
                                new TopicSubscribeProcessor<SubscriptionConfiguration>(subscription.getIdentifier(),
                                                                                       new DefaultSubscriptionConfiguration(),
                                                                                       subscription));

        return controllerIdentifier;
    }


    /**
     * Acquire the {@link NamedCache} that is a local view of the {@link EventChannel}
     * starting mode states.
     *
     * @return a {@link NamedCache}
     */
    protected NamedCache establishEventChannelStartingMode()
    {
        if (eventChannelStartingModes == null)
        {
            synchronized (this)
            {
                if (eventChannelStartingModes == null)
                {
                    eventChannelStartingModes =
                        new CustomContinuousQueryCache(CacheFactory.getCache(Subscription.CACHENAME),
                                                       new EqualsFilter(new KeyExtractor("getDestinationIdentifier"),
                                                                        StringBasedIdentifier
                                                                            .newInstance(identifier.getExternalName())),
                                                       false,
                                                       new MapListener()
                    {
                        @Override
                        public void entryInserted(MapEvent mapEvent)
                        {
                            EventChannelController.Mode mode = (EventChannelController.Mode) mapEvent.getNewValue();

                            if (mode != EventChannelController.Mode.DISABLED)
                            {
                                int count = activeEventChannelCount.getAndIncrement();

                                if (count == -1)
                                {
                                    activeEventChannelCount.incrementAndGet();
                                }
                            }
                        }

                        @Override
                        public void entryUpdated(MapEvent mapEvent)
                        {
                            EventChannelController.Mode oldMode = (EventChannelController.Mode) mapEvent.getOldValue();
                            EventChannelController.Mode newMode = (EventChannelController.Mode) mapEvent.getNewValue();

                            if (oldMode == EventChannelController.Mode.DISABLED
                                && newMode != EventChannelController.Mode.DISABLED)
                            {
                                activeEventChannelCount.incrementAndGet();
                            }

                            if (oldMode != EventChannelController.Mode.DISABLED
                                && newMode == EventChannelController.Mode.DISABLED)
                            {
                                activeEventChannelCount.decrementAndGet();
                            }
                        }

                        @Override
                        public void entryDeleted(MapEvent mapEvent)
                        {
                            EventChannelController.Mode mode = (EventChannelController.Mode) mapEvent.getOldValue();

                            if (mode != EventChannelController.Mode.DISABLED)
                            {
                                activeEventChannelCount.decrementAndGet();
                            }
                        }
                    });
                }
            }
        }

        return eventChannelStartingModes;
    }


    @Override
    public void distribute(Event event)
    {
        if (activeEventChannelCount.get() == 0)
        {
            // SKIP: don't send events if there are no actively tracked event channels
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "Skipping {0} (all event channels disabled).", event);
            }
        }
        else
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "Distributing {0}.", event);
            }

            messagingSession.publishMessage(StringBasedIdentifier.newInstance(getIdentifier().getExternalName()),
                                            event);
        }
    }


    @Override
    public void distribute(List<Event> events)
    {
        for (Event event : events)
        {
            distribute(event);
        }
    }


    /**
     * A custom {@link ContinuousQueryCache} to specialize the
     * {@link MapEventTransformerFilter}.
     */
    public static class CustomContinuousQueryCache extends ContinuousQueryCache
    {
        /**
         * The {@link ValueExtractor} to extract the starting mode.
         */
        private static ValueExtractor transformer = new ReflectionExtractor("getStartingMode");


        /**
         * Constructs a CustomContinuousQueryCache.
         *
         * @param cache         the {@link NamedCache}
         * @param filter        the {@link Filter} for the view
         * @param fCacheValues  are values cached?
         * @param listener      the {@link MapListener}
         */
        public CustomContinuousQueryCache(NamedCache  cache,
                                          Filter      filter,
                                          boolean     fCacheValues,
                                          MapListener listener)
        {
            super(cache, filter, fCacheValues, listener, transformer);
        }


        @Override
        protected Filter createTransformerFilter(MapEventFilter filterAdd)
        {
            return new MapEventTransformerFilter(transformer == null ? filterAdd : new AndFilter(filterAdd,
                                                                                                 new OrFilter(new MapEventFilter(MapEventFilter
                                                                                                     .E_INSERTED | MapEventFilter
                                                                                                     .E_DELETED),
                                                                                                              new ValueChangeEventFilter(transformer))),
                                                 transformer == null
                                                 ? SemiLiteEventTransformer.INSTANCE
                                                 : new ExtractorEventTransformer(null,
                                                                                 transformer));
        }
    }
}
