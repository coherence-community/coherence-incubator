/*
 * File: CoherenceEventChannelSubscription.java
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

import com.oracle.coherence.common.backingmaplisteners.Cause;
import com.oracle.coherence.common.backingmaplisteners.LifecycleAwareCacheEntry;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.distributors.EventChannelControllerManager;
import com.oracle.coherence.patterns.messaging.MessageIdentifier;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link CoherenceEventChannelSubscription} is a specialized messaging-layer {@link Subscription}
 * implementation that contains the {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} and
 * {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies} which together enables the
 * realization of an {@link EventChannelController}.
 * <p>
 * Essentially a {@link CoherenceEventChannelSubscription} is used as an end-point in the Messaging Layer.  When a
 * Message (containing an {@link Event}) is delivered to a {@link CoherenceEventChannelSubscription},
 * an update-event occurs on the said {@link CoherenceEventChannelSubscription}, which is then used to trigger
 * a {@link EventChannelController} to commence distributing {@link Event}s.
 * <p>
 * ie: The system is event-driven.  Changes to the state of a {@link CoherenceEventChannelSubscription}
 * cause a {@link EventChannelController} to start/stop {@link Event} distribution as necessary.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"deprecation", "serial"})
public class CoherenceEventChannelSubscription extends Subscription implements LifecycleAwareCacheEntry
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(CoherenceEventChannelSubscription.class.getName());

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} for which the
     * {@link CoherenceEventChannelSubscription} is operating.
     */
    private EventDistributor.Identifier distributorIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} associated with this
     * {@link CoherenceEventChannelSubscription}.
     */
    private EventChannelController.Identifier controllerIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies} for which the
     * {@link CoherenceEventChannelSubscription} has been established.
     */
    private EventChannelController.Dependencies controllerDependencies;

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
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public CoherenceEventChannelSubscription()
    {
    }


    /**
     * Standard Constructor.
     */
    public CoherenceEventChannelSubscription(EventDistributor.Identifier         distributorIdentifier,
                                             EventChannelController.Identifier   controllerIdentifier,
                                             EventChannelController.Dependencies controllerDependencies,
                                             ParameterProvider                   parameterProvider,
                                             ParameterizedBuilder<Serializer>    serializerBuilder)
    {
        super(new SubscriptionIdentifier(StringBasedIdentifier.newInstance(distributorIdentifier.getExternalName()),
                                         controllerIdentifier.getExternalName()),
              controllerDependencies.getStartingMode() == EventChannelController.Mode.DISABLED
              ? Status.DISABLED : Status.ENABLED);

        this.distributorIdentifier  = distributorIdentifier;
        this.controllerIdentifier   = controllerIdentifier;
        this.controllerDependencies = controllerDependencies;
        this.parameterProvider      = parameterProvider;
        this.serializerBuilder      = serializerBuilder;
    }


    /**
     * Determines the {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} in which the
     * {@link CoherenceEventChannelController} is operating.
     *
     * @return A {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     */
    public EventDistributor.Identifier getEventDistributorIdentifier()
    {
        return distributorIdentifier;
    }


    /**
     * Returns the {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} that created the
     * {@link CoherenceEventChannelSubscription}.
     *
     * @return A {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}
     */
    public EventChannelController.Identifier getEventControllerIdentifier()
    {
        return controllerIdentifier;
    }


    /**
     * Determines the {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies} for the
     * {@link CoherenceEventChannelSubscription}.
     *
     * @return A {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies}
     */
    public EventChannelController.Dependencies getEventControllerDependencies()
    {
        return controllerDependencies;
    }


    /**
     * Determines the {@link ParameterProvider} to be used for creating the {@link EventChannel}.
     *
     * @return {@link ParameterProvider}.
     */
    public ParameterProvider getParameterProvider()
    {
        return parameterProvider;
    }


    /**
     * Required for abstract class subscription.  This is not used for event distribution.
     */
    public MessageIdentifier nextMessageToDeliver(long partitionId)
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void onCacheEntryLifecycleEvent(MapEvent mapEvent,
                                           Cause    cause)
    {
        // get the environment in which we're operating
        Environment environment = (Environment) CacheFactory.getConfigurableCacheFactory();

        // get the EventChannelControllerManager to locate our EventChannelController for this EventChannel
        EventChannelControllerManager manager = environment.getResource(EventChannelControllerManager.class);

        // This must be called to let Messaging Subscription handle this event also.
        super.onCacheEntryLifecycleEvent(mapEvent, cause);

        // for newly created subscriptions, schedule the start of an associated EventChannelController
        if (mapEvent.getId() == MapEvent.ENTRY_INSERTED)
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Establishing the EventChannelController for {0}.", new Object[] {this});
            }

            EventChannelController controller = manager.registerEventChannelController(distributorIdentifier,
                                                                                       controllerIdentifier,
                                                                                       controllerDependencies,
                                                                                       new EventChannelControllerBuilder()
            {
                @Override
                public EventChannelController realize(EventDistributor.Identifier       distributorIdentifier,
                                                      EventChannelController.Identifier controllerIdentifier,
                                                      Dependencies                      dependencies,
                                                      Environment                       environment)
                {
                    return new CoherenceEventChannelController(distributorIdentifier,
                                                               controllerIdentifier,
                                                               dependencies,
                                                               environment,
                                                               parameterProvider,
                                                               serializerBuilder);
                }
            });

            if (controllerDependencies.getStartingMode() == EventChannelController.Mode.ENABLED)
            {
                controller.start();
            }

        }
        else if (mapEvent.getId() == MapEvent.ENTRY_DELETED)
        {
            // for deleted subscriptions, schedule the stopping of the associated EventChannelController
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Scheduling the EventChannelController for {0} to stop.", new Object[] {this});
            }

            // remove registered service
            EventChannelController controller =
                (CoherenceEventChannelController) manager.unregisterEventChannelController(distributorIdentifier,
                                                                                           controllerIdentifier);

            controller.stop();

        }
        else if (mapEvent.getId() == MapEvent.ENTRY_UPDATED)
        {
            // when a subscription is update we assume we need to schedule distribution
            final EventChannelController controller = manager.getEventChannelController(distributorIdentifier,
                                                                                        controllerIdentifier);

            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "Scheduling the EventChannelController for {0} to distribute available events.",
                           new Object[] {this});
            }

            if (hasVisibleMessages())
            {
                controller.preempt();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.distributorIdentifier  = (EventDistributor.Identifier) ExternalizableHelper.readObject(in);
        this.controllerIdentifier   = (EventChannelController.Identifier) ExternalizableHelper.readObject(in);
        this.controllerDependencies = (EventChannelController.Dependencies) ExternalizableHelper.readObject(in);
        this.parameterProvider      = (ParameterProvider) ExternalizableHelper.readObject(in);
        this.serializerBuilder      = (ParameterizedBuilder<Serializer>) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, distributorIdentifier);
        ExternalizableHelper.writeObject(out, controllerIdentifier);
        ExternalizableHelper.writeObject(out, controllerDependencies);
        ExternalizableHelper.writeObject(out, parameterProvider);
        ExternalizableHelper.writeObject(out, serializerBuilder);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.distributorIdentifier  = (EventDistributor.Identifier) reader.readObject(101);
        this.controllerIdentifier   = (EventChannelController.Identifier) reader.readObject(102);
        this.controllerDependencies = (EventChannelController.Dependencies) reader.readObject(103);
        this.parameterProvider      = (ParameterProvider) reader.readObject(104);
        this.serializerBuilder      = (ParameterizedBuilder<Serializer>) reader.readObject(105);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(101, distributorIdentifier);
        writer.writeObject(102, controllerIdentifier);
        writer.writeObject(103, controllerDependencies);
        writer.writeObject(104, parameterProvider);
        writer.writeObject(105, serializerBuilder);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String
            .format("CoherenceEventChannelSubscription{%s, distributorIdentifier=%s, controllerIdentifier=%s, controllerDependencies=%s, parameterProvider=%s, serializerBuilder=%s}",
                    super.toString(),
                    distributorIdentifier,
                    controllerIdentifier,
                    controllerDependencies,
                    parameterProvider,
                    serializerBuilder);
    }
}
