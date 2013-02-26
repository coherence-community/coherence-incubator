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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.BinaryWriteBuffer;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                               ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder)
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
            this.connection =
                connectionFactoryBuilder.realize(SystemPropertyParameterProvider.INSTANCE).createConnection();

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
        if (logger.isLoggable(Level.FINER))
        {
            logger.entering(JMSEventDistributor.class.getName(),
                            "establishEventChannelController",
                            new Object[] {dependencies});
        }

        // establish the serializer (if we haven't done so already)
        if (serializer == null)
        {
            serializer = serializerBuilder.realize(parameterProvider);
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
                                                       parameterProvider,
                                                       serializerBuilder,
                                                       connectionFactoryBuilder);

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
     * {@inheritDoc}
     */
    @Override
    public void distribute(Event event)
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
