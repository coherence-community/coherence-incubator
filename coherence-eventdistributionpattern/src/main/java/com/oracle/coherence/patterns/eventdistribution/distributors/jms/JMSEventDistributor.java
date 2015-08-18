/*
 * File: JMSEventDistributor.java
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

package com.oracle.coherence.patterns.eventdistribution.distributors.jms;

import com.oracle.coherence.common.events.Event;

import com.oracle.coherence.common.expression.SystemPropertyParameterList;

import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.ValueChangeEventFilter;

import com.tangosol.util.transformer.ExtractorEventTransformer;
import com.tangosol.util.transformer.SemiLiteEventTransformer;

import java.io.IOException;

import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * A {@link JMSEventDistributor} is a {@link EventDistributor} implementation that uses a Java Messaging Service (JMS) to manage the
 * distribution of {@link Event}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class JMSEventDistributor implements EventDistributor
{
    /**
     * The {@link Logger} for the class.
     */
    private static final Logger logger = Logger.getLogger(JMSEventDistributor.class.getName());

    /**
     * The {@link Identifier} for the {@link EventDistributor}.
     */
    private EventDistributor.Identifier identifier;

    /**
     * A {@link ParameterizedBuilder} that will realize the {@link Serializer} to (de)serialize {@link Event}s
     * during distribution.
     */
    private ParameterizedBuilder<Serializer> serializerBuilder;

    /**
     * A {@link ParameterizedBuilder} to build {@link ConnectionFactory}s.
     */
    private ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder;

    /**
     * The {@link Serializer} to use for (de)serializing {@link Event}s.
     */
    private Serializer serializer;

    /**
     * The {@link Connection} that will be used to establish a {@link Session} for the {@link EventDistributor}.
     */
    private Connection connection;

    /**
     * The {@link Session} that will be used to establish a {@link MessageProducer} for the {@link EventDistributor}.
     */
    private Session session;

    /**
     * The {@link MessageProducer}s for the {@link EventDistributor}.
     */
    private MessageProducer messageProducer;

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
     * @param connectionFactoryBuilder  The {@link ConnectionFactory} to use for the {@link EventDistributor}.
     */
    public JMSEventDistributor(String                                  symbolicName,
                               String                                  suggestedExternalName,
                               ParameterizedBuilder<Serializer>        serializerBuilder,
                               ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder,
                               ClassLoader                             loader)
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO,
                       String.format("Using JMS-based Event Distributor [%s] (%s).", symbolicName,
                                     suggestedExternalName));
        }

        Base.azzert(symbolicName != null);
        Base.azzert(suggestedExternalName != null);
        Base.azzert(serializerBuilder != null);
        Base.azzert(connectionFactoryBuilder != null);

        // determine an appropriate identifier for the distributor
        // (the external name will be used as the JMS topic name, which in turn we'll use for event distribution)
        this.identifier = new EventDistributor.Identifier(symbolicName,
                                                          getCompliantExternalName(suggestedExternalName));

        this.serializerBuilder        = serializerBuilder;
        this.connectionFactoryBuilder = connectionFactoryBuilder;

        // create the topic to which events will be sent for distribution
        try
        {
            // create the connection
            this.connection = connectionFactoryBuilder.realize(new NullParameterResolver(), loader,
                                                               new SystemPropertyParameterList()).createConnection();

            // create the session
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create the message producer
            this.messageProducer = session.createProducer(session.createTopic(getIdentifier().getExternalName()));
        }
        catch (JMSException jmsException)
        {
            logger.log(Level.SEVERE,
                       "Failed to create the necesssary JMS Infrastructure for the EventDistributor {0}."
                       + "Please ensure that the JMS infrastructure is correctly configured and is available.",
                       getIdentifier());

            throw Base.ensureRuntimeException(jmsException);
        }

        // initially we haven't tracked the starting modes of event channels
        this.eventChannelStartingModes = null;

        // initially we haven't tracked any active event channels
        this.activeEventChannelCount = new AtomicInteger(-1);
    }


    /**
     * Determines a compliant internal name for the {@link EventDistributor} implementation.
     *
     * @param suggestedName The suggested name
     *
     * @return A {@link String}
     */
    protected String getCompliantExternalName(String suggestedName)
    {
        String compliantName = "";

        for (int i = 0; i < suggestedName.length(); i++)
        {
            char character = suggestedName.charAt(i);

            if (Character.isLetterOrDigit(character) || character == '_')
            {
                compliantName += character;
            }
            else
            {
                compliantName += '_';
            }
        }

        return compliantName;
    }


    @Override
    public EventDistributor.Identifier getIdentifier()
    {
        return identifier;
    }


    @Override
    public EventChannelController.Identifier establishEventChannelController(Dependencies      dependencies,
                                                                             ParameterResolver parameterResolver,
                                                                             ClassLoader       loader)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.entering(JMSEventDistributor.class.getName(),
                            "establishEventChannelController",
                            new Object[] {dependencies});
        }

        // establish the NamedCache to track the event channel starting modes
        establishEventChannelStartingMode();

        // establish the serializer (if we haven't done so already)
        if (serializer == null)
        {
            serializer = serializerBuilder.realize(parameterResolver, loader, null);
        }

        // create an EventChannelController identifier (with an external name that is JMS compliant)
        EventChannelController.Identifier controllerIdentifier =
            new EventChannelController.Identifier(dependencies.getChannelName(),
                                                  getCompliantExternalName(dependencies.getExternalName()));

        // create JMSEventChannelControllerConfiguration based on the provided information
        JMSEventChannelControllerConfiguration configuration =
            new JMSEventChannelControllerConfiguration(getIdentifier(),
                                                       controllerIdentifier,
                                                       dependencies,
                                                       parameterResolver,
                                                       serializerBuilder,
                                                       connectionFactoryBuilder,
                                                       loader);

        // place the configuration into the live objects cache
        // (this will kick off establishing the required internal infrastructure for the EventChannelController)
        NamedCache liveObjectsCache = CacheFactory.getCache("coherence.live.objects.distributed");

        liveObjectsCache.invoke(getIdentifier().getSymbolicName() + ":" + controllerIdentifier.getSymbolicName(),
                                new JMSEventChannelControllerInitializationProcessor(configuration));

        if (logger.isLoggable(Level.FINER))
        {
            logger.exiting(JMSEventDistributor.class.getName(), "establishEventChannelController");
        }

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
                        new CustomContinuousQueryCache(CacheFactory.getCache("coherence.live.objects.distributed"),
                                                       new EqualsFilter("getDistributorIdentifier",
                                                                        identifier),
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
            System.out.println("Skipping " + event);

            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "Skipping {0} (all event channels disabled).", event);
            }
        }
        else
        {
            try
            {
                // create bytes message for the event
                BytesMessage      message     = session.createBytesMessage();

                BinaryWriteBuffer writeBuffer = new BinaryWriteBuffer(1024);

                serializer.serialize(writeBuffer.getBufferOutput(), event);

                byte[] bytes = writeBuffer.toByteArray();

                message.writeInt(bytes.length);
                message.writeBytes(bytes);

                // we add the symbolicname property to the message to allow JMS selector use by EventChannelControllers
                // (when all caches are going via the same topic)
                message.setStringProperty("symbolicname", getIdentifier().getSymbolicName());

                if (logger.isLoggable(Level.FINEST))
                {
                    logger.log(Level.FINEST, "Distributing {0} as Message {1}", new Object[] {event, message});
                }

                messageProducer.send(message);
            }
            catch (JMSException jmsException)
            {
                logger.log(Level.SEVERE,
                           "Failed to distribute {0} on the Topic {1}."
                           + "Please ensure that the JMS infrastructure is correctly configured and is available.",
                           new Object[] {event, getIdentifier().getExternalName()});

                throw Base.ensureRuntimeException(jmsException);
            }
            catch (IOException ioException)
            {
                logger.log(Level.SEVERE,
                           "Failed to distribute {0} on the Topic {1}." + "Due to a serialization failure.",
                           new Object[] {event, getIdentifier().getExternalName()});

                throw Base.ensureRuntimeException(ioException);
            }
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
         * The {@link ValueExtractor} to extract the starting mode of an
         * {@link EventChannelController}.
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
