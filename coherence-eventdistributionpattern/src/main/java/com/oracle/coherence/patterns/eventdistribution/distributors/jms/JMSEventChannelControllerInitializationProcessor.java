/*
 * File: JMSEventChannelControllerInitializationProcessor.java
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

import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Mode;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.processor.AbstractProcessor;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link JMSEventChannelControllerInitializationProcessor} is an {@link EntryProcessor} that creates and initializes
 * a {@link JMSEventChannelControllerConfiguration}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class JMSEventChannelControllerInitializationProcessor extends AbstractProcessor implements PortableObject,
                                                                                                   ExternalizableLite
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger =
        Logger.getLogger(JMSEventChannelControllerInitializationProcessor.class.getName());

    /**
     * The {@link JMSEventChannelControllerConfiguration} to be initialized.
     */
    private JMSEventChannelControllerConfiguration configuration;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public JMSEventChannelControllerInitializationProcessor()
    {
        // SKIP: deliberately empty
    }


    /**
     * Standard Constructor.
     *
     * @param configuration The {@link JMSEventChannelControllerConfiguration} that will be used to
     *                      initialize the {@link JMSEventChannelController}.
     */
    public JMSEventChannelControllerInitializationProcessor(JMSEventChannelControllerConfiguration configuration)
    {
        this.configuration = configuration;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(Entry entry)
    {
        if (!entry.isPresent())
        {
            // IMPORTANT: Before we setup the configuration (which occurs asynchronously after the entry is set),
            // let's make sure we've got a durable subscription for them so that we don't miss any messages!
            try
            {
                if (logger.isLoggable(Level.INFO))
                {
                    logger.info(String.format("Starting Mode for %s is %s.",
                                              configuration.getEventChannelControllerIdentifier(),
                                              configuration.getDependencies().getStartingMode()));
                }

                if (configuration.getDependencies().getStartingMode() != Mode.DISABLED)
                {
                    // create a connection factory from the configuration
                    ConnectionFactory connectionFactory =
                        configuration.getConnectionFactoryBuilder().realize(SystemPropertyParameterProvider.INSTANCE);

                    // create a new connection for the subscription
                    Connection connection = connectionFactory.createConnection();

                    // set the client to be the name of the subscription
                    connection.setClientID(configuration.getEventChannelControllerIdentifier().getExternalName());

                    // start the connection
                    connection.start();

                    // create a session in which we'll explicitly acknowledge message consumption
                    Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

                    // ensure we have the topic for the distributor
                    Topic topic = session.createTopic(configuration.getDistributorIdentifier().getExternalName());

                    // create the durable subscription on the topic for the EventChannelController restricted to
                    // the symbolicname of the distributor
                    TopicSubscriber subscriber = session.createDurableSubscriber(topic,
                                                                                 configuration
                                                                                     .getEventChannelControllerIdentifier()
                                                                                     .getExternalName(),
                                                                                 String.format("symbolicname = '%s'",
                                                                                     configuration
                                                                                         .getDistributorIdentifier()
                                                                                         .getSymbolicName()),
                                                                                 false);

                    // now close the subscriber, session and connection (so that a EventChannelController can subscribe)
                    subscriber.close();
                    session.close();
                    connection.close();
                }
            }
            catch (JMSException jmsException)
            {
                logger.log(Level.SEVERE,
                           "Failed to establish the Subscription {0} for {1} for EventChannel {2}."
                           + "Please ensure that the JMS infrastructure is correctly configured and is available.",
                           new Object[] {configuration.getEventChannelControllerIdentifier().getExternalName(),
                                         configuration.getDistributorIdentifier(),
                                         configuration.getDependencies().getChannelName()});

                throw Base.ensureRuntimeException(jmsException);
            }

            // now set the configuration (Live Object)
            entry.setValue(configuration);
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.configuration = (JMSEventChannelControllerConfiguration) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, configuration);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.configuration = (JMSEventChannelControllerConfiguration) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, configuration);
    }
}
