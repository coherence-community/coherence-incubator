/*
 * File: RemoteClusterEventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.cluster.ClusterMetaInfo;
import com.oracle.coherence.common.cluster.LocalClusterMetaInfo;
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.resourcing.InvocationServiceSupervisedResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceProviderManager;
import com.oracle.coherence.common.resourcing.SupervisedResourceProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.tools.deferred.InstanceUnavailableException;
import com.oracle.tools.deferred.Supervised;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.internal.util.ObjectFormatter;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ResourceRegistry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link RemoteClusterEventChannel} is a {@link EventChannel} implementation that uses a Remote
 * {@link InvocationService} in which to distribute batches of {@link Event}s to a remote cluster, at which
 * another {@link EventChannel} (called the remote {@link EventChannel}) will perform the actual distribution into
 * the said cluster
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class RemoteClusterEventChannel extends AbstractInterClusterEventChannel
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(RemoteClusterEventChannel.class.getName());

    /**
     *     The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier} in which the
     *     {@link EventChannel} operates.
     */
    private EventDistributor.Identifier m_distributorIdentifier;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} of the
     * {@link EventChannelController} that owns the {@link EventChannel}.
     */
    private EventChannelController.Identifier m_controllerIdentifier;

    /**
     * The name of the remote {@link InvocationService} that we'll use to distribute {@link Event}s to the
     * remote cluster.
     */
    private String remoteInvocationServiceName;

    /**
     * The {@link ParameterResolver} that must be used to construct remote {@link EventChannel}s.
     */
    private ParameterResolver resolver;

    /**
     * The ResourceRegistry.
     */
    private ResourceRegistry registry;

    /**
     * The {@link EventChannelBuilder} that will be used to build an {@link EventChannel} that is responsible
     * for distributing {@link Event}s in the the remote cluster.
     */
    private EventChannelBuilder remoteEventChannelBuilder;

    /**
     * The {@link InvocationService} that we'll use to distribute {@link Event}s to a remote cluster.
     * <p>
     * When this is <code>null</code> the remote {@link InvocationService} is unavailable.
     */
    private InvocationService remoteInvocationService;

    /**
     * The millis needed to do the apply.
     */
    private volatile long m_applyMillis;


    /**
     * Standard Constructor.
     *
     * @param distributionRole
     * @param remoteInvocationServiceName   The name of the remote {@link InvocationService} to use for distributing
     *                                      {@link Event}s to the target cluster
     * @param remoteEventChannelBuilder     The {@link EventChannelBuilder} to realize an {@link EventChannel} at the
     *                                      target cluster to apply {@link EntryEvent}s.
     */
    public RemoteClusterEventChannel(DistributionRole    distributionRole,
                                     String              remoteInvocationServiceName,
                                     EventChannelBuilder remoteEventChannelBuilder,
                                     ResourceRegistry    registry,
                                     ParameterResolver   resolver)
    {
        super(distributionRole);
        this.remoteInvocationServiceName = remoteInvocationServiceName;
        this.remoteEventChannelBuilder   = remoteEventChannelBuilder;
        this.remoteInvocationService     = null;
        this.resolver                    = resolver;
        this.registry                    = registry;
    }


    /**
     * Returns the {@link Supervised} for the remote {@link InvocationService}.
     *
     * @return The {@link Supervised} for the remote {@link InvocationService}
     */
    private SupervisedResourceProvider<InvocationService> getSupervisedResourceProvider()
    {
        // register a Supervised for the remote invocation scheme
        ResourceProviderManager manager =  registry.getResource(ResourceProviderManager.class);

        return  (SupervisedResourceProvider<InvocationService>) manager.getResourceProvider(InvocationService.class,
                remoteInvocationServiceName);

    }


    /**
     * {@inheritDoc}
     */
    public void connect(EventDistributor.Identifier       distributorIdentifier,
                        EventChannelController.Identifier controllerIdentifier) throws EventChannelNotReadyException
    {
        // remember the identifiers as we'll need to pass them to the remote event channel
        this.m_distributorIdentifier = distributorIdentifier;
        this.m_controllerIdentifier  = controllerIdentifier;

        // assume the remote invocation service is unavailable
        remoteInvocationService = null;

        // get the SupervisedResourceProvider that controls access to the underlying remote Invocation Service
        SupervisedResourceProvider<InvocationService> resourceProvider = getSupervisedResourceProvider();

        if (resourceProvider == null)
        {
            if (resourceProvider == null)
            {
                // register a SupervisedResourceProvider for the remote invocation scheme
                registry.getResource(ResourceProviderManager.class)
                        .registerResourceProvider(InvocationService.class,
                                remoteInvocationServiceName,
                                new InvocationServiceSupervisedResourceProvider(
                                        remoteInvocationServiceName));

                // now ask for it again (just in case it was replaced by the supervisor)
                resourceProvider = getSupervisedResourceProvider();
            }
        }

        if (resourceProvider.isResourceAccessible())
        {
            try
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE,
                               "Attempting to connect to Remote Invocation Service {0} in {1} for {2}",
                               new Object[] {remoteInvocationServiceName, distributorIdentifier,
                                             controllerIdentifier});
                }

                // attempt to access the remote InvocationService (this will make the connection)
                remoteInvocationService = resourceProvider.getResource();

                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE,
                               "Connected to Remote Invocation Service {0} in {1} for {2}",
                               new Object[] {remoteInvocationServiceName, distributorIdentifier,
                                             controllerIdentifier});
                }

                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE,
                               "Attempting to detect Remote Cluster Name using Remote Invocation Service {0}",
                               remoteInvocationServiceName);
                }

                // attempt to auto-detect the remote cluster info
                setTargetClusterMetaInfo((ClusterMetaInfo) remoteInvocationService
                    .query(new GetClusterMetaInfoAgent(), null).values().iterator().next());

                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE,
                               "Connected to Remote Cluster {0}",
                               getTargetClusterMetaInfo().getUniqueName());
                }
            }
            catch (InstanceUnavailableException resourceUnavailableException)
            {
                // when the resource is unavailable this means that the channel is not ready
                throw new EventChannelNotReadyException(this);
            }
            catch (RuntimeException runtimeException)
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               "Failed to connect to Remote Invocation Service {0} in {1} for {2}",
                               new Object[] {remoteInvocationServiceName, distributorIdentifier,
                                             controllerIdentifier});
                    logger.log(Level.WARNING, "Causing exception was:", runtimeException);
                }

                // let the SupervisedResourceProvider for our remote Invocation Service that the said service is not available.
                resourceProvider.resourceNoLongerAvailable();

                throw runtimeException;
            }
        }
        else
        {
            // the channel is not ready as the remote invocation service is unavailable
            throw new EventChannelNotReadyException(this);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void disconnect()
    {
        if (remoteInvocationService != null)
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE,
                           "Disconnected from Remote Invocation Service {0} in {1} for {2}",
                           new Object[] {remoteInvocationServiceName, m_distributorIdentifier,
                                         m_controllerIdentifier});
            }

            remoteInvocationService = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int send(Iterator<Event> events)
    {
        if (remoteInvocationService == null)
        {
            throw new IllegalStateException(String
                .format("Attempted to distribute using an unavailable Remote Invocation Service %s",
                        remoteInvocationServiceName));
        }
        else
        {
            List<Event> eventsToDistribute = getEventsToDistribute(events);

            try
            {

                remoteInvocationService.query(new RemoteDistributionAgent(m_distributorIdentifier,
                                                                            m_controllerIdentifier,
                                                                            eventsToDistribute,
                                                                            remoteEventChannelBuilder,
                                                                            resolver), null);
            }
            catch (RuntimeException runtimeException)
            {
                // on any failure we must tell the resource supervisor that the remote invocation service is faulty
                getSupervisedResourceProvider().resourceNoLongerAvailable();

                throw runtimeException;
            }

            return eventsToDistribute.size();
        }
    }


    /**
     * A {@link GetClusterMetaInfoAgent} determines and returns the {@link ClusterMetaInfo} for a remote {@link Cluster}
     * using an Invocation Service.
     *
     * @see InterClusterEventChannel
     */
    public static class GetClusterMetaInfoAgent extends AbstractInvocable implements ExternalizableLite, PortableObject
    {
        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}
         */
        public GetClusterMetaInfoAgent()
        {
            // deliberately empty
        }


        /**
         * {@inheritDoc}
         */
        public void run()
        {
            setResult(LocalClusterMetaInfo.getInstance());
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            // deliberately empty - nothing to read
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            // deliberately empty - nothing to write
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            // deliberately empty - nothing to read
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            // deliberately empty - nothing to write
        }
    }


    /**
     * A {@link RemoteDistributionAgent} is used to distribute a batch (as a ordered list) of
     * {@link Event}s using an {@link InvocationService} to a remote site for local distribution.
     */
    public static class RemoteDistributionAgent extends AbstractInvocable implements ExternalizableLite, PortableObject
    {
        private EventDistributor.Identifier       distributorIdentifier;
        private EventChannelController.Identifier controllerIdentifier;
        private List<Event>                       events;
        private EventChannelBuilder               remoteEventChannelBuilder;
        private ParameterResolver                 resolver;


        /**
         * Required {@link ExternalizableLite} and {@link PortableObject}
         */
        public RemoteDistributionAgent()
        {
        }


        /**
         * Standard Constructor.
         */
        public RemoteDistributionAgent(EventDistributor.Identifier       distributorIdentifier,
                                       EventChannelController.Identifier controllerIdentifier,
                                       List<Event>                       events,
                                       EventChannelBuilder               remoteEventChannelBuilder,
                                       ParameterResolver                 resolver)
        {
            this.distributorIdentifier     = distributorIdentifier;
            this.controllerIdentifier      = controllerIdentifier;
            this.events                    = events;
            this.remoteEventChannelBuilder = remoteEventChannelBuilder;
            this.resolver                  = resolver;
        }


        /**
         *     REMEMBER: The following method is executed in the remote cluster!
         */
        public void run()
        {
            EventChannel channel = remoteEventChannelBuilder.realize(resolver,
                    ParameterResolver.class.getClassLoader(), null);

            try
            {
                long ldtStart = Base.getSafeTimeMillis();

                channel.connect(distributorIdentifier, controllerIdentifier);

                int cEvents = channel.send(events.iterator());

                setResult(new Integer(cEvents));

                channel.disconnect();

            }
            catch (Exception exception)
            {
                throw ensureRuntimeException(exception,
                                             String.format("Failed to distribute events using [%s] in [%s]",
                                                           controllerIdentifier, distributorIdentifier));
            }
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            this.distributorIdentifier = (EventDistributor.Identifier) ExternalizableHelper.readObject(in);
            this.controllerIdentifier  = (EventChannelController.Identifier) ExternalizableHelper.readObject(in);
            this.events                = new LinkedList<Event>();
            ExternalizableHelper.readCollection(in, events, Thread.currentThread().getContextClassLoader());
            this.remoteEventChannelBuilder = (EventChannelBuilder) ExternalizableHelper.readObject(in);
            this.resolver               = (ParameterResolver) ExternalizableHelper.readObject(in);

        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeObject(out, distributorIdentifier);
            ExternalizableHelper.writeObject(out, controllerIdentifier);
            ExternalizableHelper.writeCollection(out, events);
            ExternalizableHelper.writeObject(out, remoteEventChannelBuilder);
            ExternalizableHelper.writeObject(out, resolver);
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            this.distributorIdentifier = (EventDistributor.Identifier) reader.readObject(1);
            this.controllerIdentifier  = (EventChannelController.Identifier) reader.readObject(2);
            this.events                = new LinkedList<Event>();
            reader.readCollection(3, events);
            this.remoteEventChannelBuilder = (EventChannelBuilder) reader.readObject(4);
            this.resolver                 = (ParameterResolver) reader.readObject(5);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeObject(1, distributorIdentifier);
            writer.writeObject(2, controllerIdentifier);
            writer.writeCollection(3, events);
            writer.writeObject(4, remoteEventChannelBuilder);
            writer.writeObject(5, resolver);
        }
    }
}
