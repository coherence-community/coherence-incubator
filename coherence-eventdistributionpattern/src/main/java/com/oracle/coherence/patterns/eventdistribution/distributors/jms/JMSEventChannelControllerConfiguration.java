/*
 * File: JMSEventChannelControllerConfiguration.java
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
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.processing.LiveObject;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Mode;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.distributors.EventChannelControllerManager;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;

import javax.jms.ConnectionFactory;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link JMSEventChannelControllerConfiguration} is a {@link LiveObject} that holds the configuration of a
 * {@link JMSEventChannelController}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class JMSEventChannelControllerConfiguration implements LiveObject<DistributableEntry>,
                                                               ExternalizableLite,
                                                               PortableObject
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(JMSEventChannelControllerConfiguration.class.getName());

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} in which the
     * {@link JMSEventChannelController} will operate.
     */
    private EventDistributor.Identifier distributorIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} for the
     * {@link JMSEventChannelControllerConfiguration}.
     */
    private EventChannelController.Identifier controllerIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies} for the
     * {@link JMSEventChannelControllerConfiguration} to establish.
     */
    private EventChannelController.Dependencies dependencies;

    /**
     * The {@link ParameterProvider} to use to create the {@link EventChannel}.
     */
    private ParameterProvider parameterProvider;

    /**
     * A {@link ParameterizedBuilder} that will realize the {@link Serializer} to (de)serialize {@link Event}s
     * during distribution.
     */
    private ParameterizedBuilder<Serializer> serializerBuilder;

    /**
     * A {@link ParameterizedBuilder} that we can use to build {@link ConnectionFactory}s.
     */
    private ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public JMSEventChannelControllerConfiguration()
    {
        // SKIP: deliberately empty
    }


    /**
     * Standard Constructor.
     */
    public JMSEventChannelControllerConfiguration(EventDistributor.Identifier             distributorIdentifier,
                                                  EventChannelController.Identifier       controllerIdentifier,
                                                  EventChannelController.Dependencies     dependencies,
                                                  ParameterProvider                       parameterProvider,
                                                  ParameterizedBuilder<Serializer>        serializerBuilder,
                                                  ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder)
    {
        this.distributorIdentifier    = distributorIdentifier;
        this.controllerIdentifier     = controllerIdentifier;
        this.dependencies             = dependencies;
        this.parameterProvider        = parameterProvider;
        this.serializerBuilder        = serializerBuilder;
        this.connectionFactoryBuilder = connectionFactoryBuilder;
    }


    /**
     * Determines the {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} in which the
     * {@link JMSEventChannelControllerConfiguration} will operate.
     *
     * @return The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     */
    public EventDistributor.Identifier getDistributorIdentifier()
    {
        return distributorIdentifier;
    }


    /**
     * Returns the {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} that
     * created the {@link JMSEventChannelControllerConfiguration}.
     *
     * @return A {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}
     */
    public EventChannelController.Identifier getEventChannelControllerIdentifier()
    {
        return controllerIdentifier;
    }


    /**
     * Determines the {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies} for the
     * {@link JMSEventChannelControllerConfiguration}.
     *
     * @return A {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies}
     */
    public EventChannelController.Dependencies getDependencies()
    {
        return dependencies;
    }


    /**
     * Determines the {@link ParameterizedBuilder} for {@link ConnectionFactory}s that may be used to realize
     * a {@link ConnectionFactory}.
     *
     * @return A {@link ParameterizedBuilder} for {@link ConnectionFactory}s.
     */
    public ParameterizedBuilder<ConnectionFactory> getConnectionFactoryBuilder()
    {
        return connectionFactoryBuilder;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void process(EventDispatcher                eventDispatcher,
                        EntryEvent<DistributableEntry> event)
    {
        // get the environment in which we're operating
        Environment environment = (Environment) CacheFactory.getConfigurableCacheFactory();

        // get the EventChannelControllerManager to locate our EventChannelController for this EventChannel
        EventChannelControllerManager manager = environment.getResource(EventChannelControllerManager.class);

        if (event instanceof EntryInsertedEvent)
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Establishing the EventChannelController for {0}.", new Object[] {this});
            }

            EventChannelController controller = manager.registerEventChannelController(distributorIdentifier,
                                                                                       controllerIdentifier,
                                                                                       dependencies,
                                                                                       new EventChannelControllerBuilder()
            {
                @Override
                public EventChannelController realize(EventDistributor.Identifier distributorIdentifier,
                                                      Identifier                  controllerIdentifier,
                                                      Dependencies                dependencies,
                                                      Environment                 environment)
                {
                    return new JMSEventChannelController(distributorIdentifier,
                                                         controllerIdentifier,
                                                         dependencies,
                                                         environment,
                                                         parameterProvider,
                                                         serializerBuilder,
                                                         connectionFactoryBuilder);
                }
            });

            if (dependencies.getStartingMode() == Mode.ENABLED)
            {
                controller.start();
            }

        }
        else if (event instanceof EntryRemovedEvent)
        {
            // for deleted subscriptions, schedule the stopping of the associated EventChannelController

            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Scheduling the EventChannelController for {0} to stop.", new Object[] {this});
            }

            // remove registered service
            EventChannelController controller = manager.unregisterEventChannelController(distributorIdentifier,
                                                                                         controllerIdentifier);

            controller.stop();
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.distributorIdentifier    = (EventDistributor.Identifier) ExternalizableHelper.readObject(in);
        this.controllerIdentifier     = (EventChannelController.Identifier) ExternalizableHelper.readObject(in);
        this.dependencies             = (EventChannelController.Dependencies) ExternalizableHelper.readObject(in);
        this.parameterProvider        = (ParameterProvider) ExternalizableHelper.readObject(in);
        this.serializerBuilder        = (ParameterizedBuilder<Serializer>) ExternalizableHelper.readObject(in);
        this.connectionFactoryBuilder = (ParameterizedBuilder<ConnectionFactory>) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, distributorIdentifier);
        ExternalizableHelper.writeObject(out, controllerIdentifier);
        ExternalizableHelper.writeObject(out, dependencies);
        ExternalizableHelper.writeObject(out, parameterProvider);
        ExternalizableHelper.writeObject(out, serializerBuilder);
        ExternalizableHelper.writeObject(out, connectionFactoryBuilder);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.distributorIdentifier    = (EventDistributor.Identifier) reader.readObject(1);
        this.controllerIdentifier     = (EventChannelController.Identifier) reader.readObject(2);
        this.dependencies             = (EventChannelController.Dependencies) reader.readObject(3);
        this.parameterProvider        = (ParameterProvider) reader.readObject(4);
        this.serializerBuilder        = (ParameterizedBuilder<Serializer>) reader.readObject(5);
        this.connectionFactoryBuilder = (ParameterizedBuilder<ConnectionFactory>) reader.readObject(6);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, distributorIdentifier);
        writer.writeObject(2, controllerIdentifier);
        writer.writeObject(3, dependencies);
        writer.writeObject(4, parameterProvider);
        writer.writeObject(5, serializerBuilder);
        writer.writeObject(6, connectionFactoryBuilder);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String
            .format("JMSEventChannelControllerConfiguration{distributorIdentifier=%s, controllerIdentifier=%s, dependencies=%s, serializerBuilder=%s, connectionFactoryBuilder=%s}",
                    distributorIdentifier,
                    controllerIdentifier,
                    dependencies,
                    parameterProvider,
                    serializerBuilder,
                    connectionFactoryBuilder);
    }
}
