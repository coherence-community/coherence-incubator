/*
 * File: TaskProcessorMBeanManager.java
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

package com.oracle.coherence.patterns.processing.internal.task;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryArrivedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryDepartedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryInsertedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryRemovedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryUpdatedEvent;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.lifecycle.NamedCacheStorageReleasedEvent;
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.LifecycleEventFilter;
import com.oracle.coherence.environment.extensible.dependencies.DependencyReference;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.dependencies.EnvironmentReference;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.Registry;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link TaskProcessorMBeanManager} manages MBeans that represent {@link TaskProcessorMediator} objects in the
 * grid.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class TaskProcessorMBeanManager implements DependentResource, EventProcessor<Event>
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(TaskProcessorMBeanManager.class.getName());

    /**
     * HashMap keeping track of the {@link TaskProcessorMBeanProxy} objects.
     */
    private ConcurrentHashMap<TaskProcessorMediatorKey, TaskProcessorMediatorProxy> localMBeans;

    /**
     * Set if we are shutting down. If we are, lets not process any more events
     */
    private volatile boolean shuttingDown;


    /**
     * Default constructor.
     */
    public TaskProcessorMBeanManager()
    {
        localMBeans  = new ConcurrentHashMap<TaskProcessorMediatorKey, TaskProcessorMediatorProxy>();
        shuttingDown = false;
        DefaultTaskProcessorMediator.setEventProcessor(this);
    }


    /**
     * Adds a {@link TaskProcessorMediatorProxyMBean} and register it with the MBeanServer.
     *
     * @param key the {@link TaskProcessorMediatorKey} this {@link TaskProcessorMediatorProxyMBean} represents.
     * @param mBeanProxy the {@link TaskProcessorMediatorProxy} that is to be registered
     */
    private void addMBean(TaskProcessorMediatorKey   key,
                          TaskProcessorMediatorProxy mBeanProxy)
    {
        localMBeans.put(key, mBeanProxy);
        registerMBean(key, mBeanProxy);
    }


    /**
     * Removes the MBean associated with the key.
     *
     * @param key the {@link TaskProcessorMediatorKey} for which we want to unregister the MBean.
     */
    private void removeMBean(TaskProcessorMediatorKey key)
    {
        TaskProcessorMediatorProxy proxy = localMBeans.get(key);

        if (proxy != null)
        {
            unregisterMBean(proxy);
        }
    }


    /**
     * Updates the MBean associated with the entry.
     *
     * @param entry the updated {@link Entry}
     */
    @SuppressWarnings("rawtypes")
    public void updateMBean(Entry entry)
    {
        TaskProcessorMediatorProxy proxy = localMBeans.get(entry.getKey());

        if (proxy != null)
        {
            proxy.setValue((TaskProcessorMediatorProxyMBean) entry.getValue());
        }
    }


    /**
     * Helper method registering an MBean.
     *
     * @param key the {@link TaskProcessorMediatorKey}
     * @param mBeanProxy the {@link TaskProcessorMediatorProxy} we are registering
     */
    protected void registerMBean(TaskProcessorMediatorKey   key,
                                 TaskProcessorMediatorProxy mBeanProxy)
    {
        if (!shuttingDown)
        {
            Registry registry = CacheFactory.ensureCluster().getManagement();

            if (registry != null)
            {
                mBeanProxy
                    .setMBeanName(registry
                        .ensureGlobalName(String
                            .format("type=ProcessingPattern,subType=TaskProcessor,id=%s",
                                    key.getTaskProcessorDefinitionIdentifier().toString() + ":" + key.getMemberId()
                                    + ":" + +CacheFactory.ensureCluster().getLocalMember().getId())));

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Registering JMX management extensions for TaskProcessorMediator {0}",
                               mBeanProxy.getMBeanName());
                }

                registry.register(mBeanProxy.getMBeanName(), mBeanProxy);
            }
        }
    }


    /**
     * Helper method to unregister an MBean.
     *
     * @param mBeanProxy the {@link TaskProcessorMediatorProxy}
     */
    protected void unregisterMBean(TaskProcessorMediatorProxy mBeanProxy)
    {
        if (!shuttingDown)
        {
            Registry registry = CacheFactory.ensureCluster().getManagement();

            if (registry != null)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Unregistering JMX management extensions for TaskProcessorMediator %s",
                               mBeanProxy.getMBeanName());
                }

                registry.unregister(mBeanProxy.getMBeanName());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public void process(EventDispatcher eventDispatcher,
                        Event           event)
    {
        if (!shuttingDown)
        {
            if (event instanceof NamedCacheStorageReleasedEvent)
            {
                if (DefaultTaskProcessorMediator.CACHENAME
                    .equals(((NamedCacheStorageReleasedEvent) event).getCacheName()))
                {
                    onDependenciesViolated(eventDispatcher.getEnvironment());
                }
            }
            else
            {
                if (event instanceof BackingMapEntryUpdatedEvent)
                {
                    updateMBean(((EntryEvent) event).getEntry());

                }
                else
                {
                    if ((event instanceof BackingMapEntryInsertedEvent)
                        || (event instanceof BackingMapEntryArrivedEvent))
                    {
                        if (event instanceof BackingMapEntryArrivedEvent)
                        {
                            ((TaskProcessorMediator) ((EntryEvent) event).getEntry().getValue()).entryArrived();
                        }

                        // We now create a new MBean and register it.
                        TaskProcessorMediatorProxy proxy =
                            new TaskProcessorMediatorProxy((TaskProcessorMediatorKey) ((EntryEvent) event).getEntry()
                                .getKey());

                        addMBean((TaskProcessorMediatorKey) ((EntryEvent) event).getEntry().getKey(), proxy);
                    }
                    else
                    {
                        if ((event instanceof BackingMapEntryRemovedEvent)
                            || (event instanceof BackingMapEntryDepartedEvent))
                        {
                            if (event instanceof BackingMapEntryDepartedEvent)
                            {
                                ((TaskProcessorMediator) ((EntryEvent) event).getEntry().getValue()).entryDeparted();
                            }

                            // We now remove the registered MBean.
                            // removeMBean((TaskProcessorMediatorKey) ((EntryEvent) event).getEntry().getKey());
                        }
                    }
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Set<DependencyReference> getDependencyReferences()
    {
        return Collections.singleton((DependencyReference) new EnvironmentReference());
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesSatisfied(Environment environment)
    {
        // Start listening for lifecycle events since we want to know when we are shutting down
        environment.getResource(EventDispatcher.class).registerEventProcessor(LifecycleEventFilter.INSTANCE, this);
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesViolated(Environment environment)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Shutting down TaskProcessorMBeanManager.");
        }

        shuttingDown = true;
    }
}
