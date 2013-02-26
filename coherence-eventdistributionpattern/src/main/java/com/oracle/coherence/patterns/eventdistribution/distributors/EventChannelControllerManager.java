/*
 * File: EventChannelControllerManager.java
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

package com.oracle.coherence.patterns.eventdistribution.distributors;

import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerMBean;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.Registry;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link EventChannelControllerManager} is an internal class responsible for managing a collection
 * of {@link EventChannelController}s by {@link EventDistributor}s.  Included in this responsibility is
 * the management of JMX infrastructure registrations.
 * <p>
 * An instance of this class may be acquired via an {@link Environment}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventChannelControllerManager
{
    /**
     * The {@link Environment} in which this {@link EventChannelControllerManager} is operating.
     */
    private Environment environment;

    /**
     * The collection of {@link EventChannelController}s being managed.
     */
    private ConcurrentHashMap<EventDistributor.Identifier, ConcurrentHashMap<EventChannelController.Identifier, EventChannelController>> controllersByDistributor;


    /**
     * Standard Constructor.
     *
     * @param environment The {@link Environment} in which the {@link EventChannelControllerManager} is operating
     */
    public EventChannelControllerManager(Environment environment)
    {
        this.environment = environment;
        this.controllersByDistributor = new ConcurrentHashMap<EventDistributor.Identifier,
                                                              ConcurrentHashMap<EventChannelController.Identifier,
                                                                                EventChannelController>>();
    }


    /**
     * Registers a {@link EventChannelController} for the specified {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     * with {@link EventChannelControllerManager}.  If the {@link EventChannelController} with the specified
     * {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier} is already known to the
     * {@link EventChannelControllerManager}, the existing one is returned.  Otherwise a {@link EventChannelController}
     * is realized using the specified {@link EventChannelControllerBuilder} and
     * {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies}.
     *
     * @param distributorIdentifier The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     *                              in which to register/create/realize a {@link EventChannelController}.
     * @param controllerIdentifier  The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}
     *                              for the proposed {@link EventChannelController}.
     * @param dependencies          The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies}
     *                              for the proposed {@link EventChannelController}.
     * @param builder               The {@link EventChannelControllerBuilder} to create the {@link EventChannelController}
     *                              (if it's not already known to the {@link EventChannelControllerManager}).
     *
     * @return A {@link EventChannelController}.
     */
    public EventChannelController registerEventChannelController(EventDistributor.Identifier         distributorIdentifier,
                                                                 EventChannelController.Identifier   controllerIdentifier,
                                                                 EventChannelController.Dependencies dependencies,
                                                                 EventChannelControllerBuilder       builder)
    {
        ConcurrentHashMap<EventChannelController.Identifier, EventChannelController> controllers;

        if (!controllersByDistributor.containsKey(distributorIdentifier))
        {
            controllers = controllersByDistributor.putIfAbsent(distributorIdentifier,
                                                               new ConcurrentHashMap<EventChannelController.Identifier,
                                                                                     EventChannelController>());
        }

        controllers = controllersByDistributor.get(distributorIdentifier);

        EventChannelController controller;

        if (controllers.containsKey(controllerIdentifier))
        {
            controller = controllers.get(controllerIdentifier);
        }
        else
        {
            synchronized (controllers)
            {
                controller = builder.realize(distributorIdentifier, controllerIdentifier, dependencies, environment);

                controllers.put(controllerIdentifier, controller);

                // attempt to register the MBean for the EventChannelController (if it supports the MBean)
                if (controller instanceof EventChannelControllerMBean)
                {
                    Registry registry = CacheFactory.getCluster().getManagement();

                    if (registry != null)
                    {
                        String mBeanName = registry.ensureGlobalName("type=EventChannels,id="
                                                                     + distributorIdentifier.getSymbolicName() + "-"
                                                                     + controllerIdentifier.getSymbolicName());

                        registry.register(mBeanName, controller);
                    }
                }
            }
        }

        return controller;
    }


    /**
     * Unregisters the {@link EventChannelController} for the specified {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     * and {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}.
     *
     * @param distributorIdentifier   The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     *                                in which to unregister the {@link EventChannelController}.
     * @param controllerIdentifier    The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}
     *                                for the {@link EventChannelController}.
     */
    public EventChannelController unregisterEventChannelController(EventDistributor.Identifier distributorIdentifier,
                                                                   EventChannelController
                                                                       .Identifier             controllerIdentifier)
    {
        EventChannelController controller = null;

        if (controllersByDistributor.containsKey(distributorIdentifier))
        {
            ConcurrentHashMap<EventChannelController.Identifier, EventChannelController> controllers =
                controllersByDistributor.get(distributorIdentifier);

            controller = controllers.remove(controllerIdentifier);

            if (controller != null)
            {
                // attempt to unregister the MBean for the EventChannelController (if it supports the MBean)
                if (controller instanceof EventChannelControllerMBean)
                {
                    Registry registry = CacheFactory.getCluster().getManagement();

                    if (registry != null)
                    {
                        String mBeanName = registry.ensureGlobalName("type=EventChannels,id="
                                                                     + distributorIdentifier.getSymbolicName() + "-"
                                                                     + controllerIdentifier.getSymbolicName());

                        registry.unregister(mBeanName);
                    }
                }
            }

            synchronized (controllers)
            {
                if (controllers.isEmpty())
                {
                    controllersByDistributor.remove(distributorIdentifier);
                }
            }
        }

        return controller;
    }


    /**
     * Determines the registered {@link EventChannelController} for the specified {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     * and {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}
     *
     * @param distributorIdentifier The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributor.Identifier}
     *                              in which to locate the {@link EventChannelController}.
     * @param controllerIdentifier  The {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Identifier}
     *                              for the {@link EventChannelController}.
     *
     * @return The registered {@link EventChannelController} or <code>null</code> if unknown.
     */
    public EventChannelController getEventChannelController(EventDistributor.Identifier       distributorIdentifier,
                                                            EventChannelController.Identifier controllerIdentifier)
    {
        if (controllersByDistributor.containsKey(distributorIdentifier))
        {
            return controllersByDistributor.get(distributorIdentifier).get(controllerIdentifier);
        }
        else
        {
            return null;
        }
    }
}
