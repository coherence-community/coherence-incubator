/*
 * File: JMSEventChannelController.java
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
import com.oracle.coherence.common.tuples.Pair;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder;
import com.oracle.coherence.patterns.eventdistribution.distributors.AbstractEventChannelController;
import com.tangosol.io.Serializer;
import com.tangosol.util.Binary;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link JMSEventChannelController} is an implementation of a {@link EventChannelController}, specifically for
 * JMS-based {@link EventDistributorBuilder}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class JMSEventChannelController extends AbstractEventChannelController<javax.jms.Message>
    implements JMSEventChannelControllerMBean
{
    /**
     * The {@link Logger} to use for this class.
     */
    private static Logger logger = Logger.getLogger(JMSEventChannelController.class.getName());

    /**
     * A {@link ConnectionFactory} from which we shall establish {@link Connection}s.
     */
    private ConnectionFactory connectionFactory;

    /**
     * The {@link Connection} we'll use to connect to JMS (when <code>null</code> we are not connected).
     */
    private Connection connection;

    /**
     * The {@link Session} we'll use to subscribe to JMS.
     */
    private Session session;

    /**
     * The {@link TopicSubscriber} we'll use to read messages from JMS.
     */
    private TopicSubscriber subscriber;


    /**
     * Standard Constructor.
     */
    public JMSEventChannelController(EventDistributor.Identifier             distributorIdentifier,
                                     EventChannelController.Identifier       controllerIdentifier,
                                     EventChannelController.Dependencies     dependencies,
                                     Environment                             environment,
                                     ParameterProvider                       parameterProvider,
                                     ParameterizedBuilder<Serializer>        serializerBuilder,
                                     ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder)
    {
        super(distributorIdentifier,
              controllerIdentifier,
              dependencies,
              environment,
              parameterProvider,
              serializerBuilder);

        // initialize connection information
        this.connection = null;
        this.session    = null;
        this.subscriber = null;

        // create a connection factory from which we can establish connections (later)
        this.connectionFactory = connectionFactoryBuilder.realize(parameterProvider);
    }


    private void ensureConnected() throws JMSException
    {
        if (connection == null)
        {
            // create a connection
            connection = connectionFactory.createConnection();

            // set the client to be the name of the subscription
            connection.setClientID(controllerIdentifier.getExternalName());

            // start delivery of messages
            connection.start();
        }

        if (session == null)
        {
            // create a session in which we'll explicitly acknowledge message consumption
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        }

        if (subscriber == null)
        {
            // create the topic
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE,
                           "Attempting to create topic {0} for {1}",
                           new Object[] {distributorIdentifier.getExternalName(), distributorIdentifier});
            }

            Topic topic = session.createTopic(distributorIdentifier.getExternalName());

            // create a durable topic subscription
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE,
                           "Attempting to create durable subscription for {0} in {1}",
                           new Object[] {controllerIdentifier, distributorIdentifier});
            }

            subscriber = session.createDurableSubscriber(topic, controllerIdentifier.getExternalName(),
                                                         String.format("symbolicname = '%s'",
                                                                       distributorIdentifier.getSymbolicName()), false);
        }
    }


    private void ensureDisconnected()
    {
        // close and reset the connection, session and subscriber
        if (subscriber != null)
        {
            try
            {
                subscriber.close();
            }
            catch (JMSException ignore)
            {
                // we don't mind if the subscriber can't be closed as we're going to create a new one
            }
            finally
            {
                subscriber = null;
            }
        }

        if (session != null)
        {
            try
            {
                session.close();
            }
            catch (JMSException ignore)
            {
                // we don't mind if the session can't be closed as we're going to create a new one
            }
            finally
            {
                session = null;
            }
        }

        if (connection != null)
        {
            try
            {
                connection.stop();
                connection.close();
            }
            catch (JMSException ignore)
            {
                // we don't mind if the connection can't be closed as we're going to create a new one
            }
            finally
            {
                connection = null;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalEnable()
    {
        try
        {
            ensureConnected();
        }
        catch (JMSException jmsException)
        {
            logger.log(Level.SEVERE,
                       "Failed to enable the EventChannelController {0} due to {1}.",
                       new Object[] {controllerDependencies.getChannelName(), jmsException});

            throw new RuntimeException(jmsException);
        }
    }


    @Override
    protected void internalDisable()
    {
        try
        {
            ensureConnected();

            // close the subscriber so that we can safely unsubscribe
            if (subscriber != null)
            {
                try
                {
                    subscriber.close();
                }
                catch (JMSException ignore)
                {
                    // we don't mind if the subscriber can't be closed we are unsubscribing
                }
                finally
                {
                    subscriber = null;
                }
            }

            // disable by unsubscribing
            session.unsubscribe(controllerIdentifier.getExternalName());

            ensureDisconnected();
        }
        catch (JMSException jmsException)
        {
            logger.log(Level.SEVERE,
                       "Failed to disable the EventChannelController {0} due to {1}.",
                       new Object[] {controllerDependencies.getChannelName(), jmsException});

            throw new RuntimeException(jmsException);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalDrain()
    {
        try
        {
            internalDisable();
        }
        catch (RuntimeException runtimeException)
        {
            logger.log(Level.SEVERE,
                       "Failed to drain the waiting events for EventChannelController {0} due to {1}. Suspending EventChannelController",
                       new Object[] {controllerDependencies.getChannelName(), runtimeException.getCause()});

            // suspend the service
            setState(State.SUSPENDED);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalStart() throws EventChannelNotReadyException
    {
        try
        {
            // ensure we have a connection
            ensureConnected();

            // start the EventChannel
            channel.connect(distributorIdentifier, controllerIdentifier);
        }
        catch (JMSException jmsException)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "The EventChannelController {0} for {1} could not establish subscription {2} to the JMS infrastructure.  Deferring (re)start for {3} ms",
                           new Object[] {controllerDependencies.getChannelName(), distributorIdentifier,
                                         controllerIdentifier.getExternalName(),
                                         controllerDependencies.getRestartDelay()});

                logger.log(Level.FINER, "Exception was:", jmsException);
            }

            // ensure we've disconnected from JMS
            ensureDisconnected();

            // schedule a restart (retry) after the restart delay
            setState(State.PAUSED);
            schedule(new Runnable()
            {
                public void run()
                {
                    start();
                }
            }, controllerDependencies.getRestartDelay(), TimeUnit.MILLISECONDS);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalStop()
    {
        // ensure we've disconnected
        ensureDisconnected();

        channel.disconnect();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<List<Event>, javax.jms.Message> getEventsToDistribute()
    {
        try
        {
            ArrayList<Event>  eventList           = new ArrayList<Event>(controllerDependencies.getBatchSize());

            javax.jms.Message message             = null;
            javax.jms.Message lastReceivedMessage = null;

            do
            {
                message = subscriber.receiveNoWait();

                if (message != null)
                {
                    if (message instanceof BytesMessage)
                    {
                        // remember this as the last received message
                        lastReceivedMessage = message;

                        // retrieve the Event from the message
                        BytesMessage bytesMessage = (BytesMessage) message;

                        try
                        {
                            int    length = bytesMessage.readInt();

                            byte[] bytes  = new byte[length];

                            bytesMessage.readBytes(bytes);

                            Binary binary  = new Binary(bytes);

                            Object payload = getSerializer().deserialize(binary.getBufferInput());

                            if (payload != null && payload instanceof Event)
                            {
                                eventList.add((Event) payload);
                            }
                            else
                            {
                                // TODO: log that there is a foreign event in the message queue
                            }
                        }
                        catch (IOException ioException)
                        {
                            logger.log(Level.SEVERE,
                                       "Failed to retrieve an event from distributor {0} channel {1}."
                                       + "Due to a serialization failure {2}",
                                       new Object[] {getEventDistributorName(), getEventChannelControllerName(),
                                                     ioException});

                            ioException.printStackTrace();
                        }
                    }
                    else
                    {
                        // TODO: not a ByteMessage... this is an error!
                    }
                }

            }
            while (message != null && eventList.size() < controllerDependencies.getBatchSize());

            return new Pair<List<Event>, javax.jms.Message>(eventList, lastReceivedMessage);

        }
        catch (JMSException jmsException)
        {
            throw new RuntimeException(jmsException);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void acknowledgeDistributedEvents(List<Event> events,
                                                Message     lastReceivedMessage)
    {
        try
        {
            // acknowledge the last message we received (to acknowledge all the others as well)
            lastReceivedMessage.acknowledge();
        }
        catch (JMSException jmsException)
        {
            throw new RuntimeException(jmsException);
        }
    }
}
