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

import com.oracle.coherence.common.liveobjects.OnArrived;
import com.oracle.coherence.common.liveobjects.OnInserted;
import com.oracle.coherence.common.liveobjects.OnUpdated;
import com.oracle.coherence.patterns.processing.internal.Environment;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.management.Registry;
import com.tangosol.util.BinaryEntry;

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
public class TaskProcessorMBeanManager
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(TaskProcessorMBeanManager.class.getName());

    /**
     * HashMap keeping track of the {@link TaskProcessorMediatorProxy} objects.
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
        DefaultTaskProcessorMediator.setMBeanManager(this);
    }

    /**
     * Adds a {@link TaskProcessorMediatorProxyMBean} and register it with the MBeanServer.
     *
     * @param key the {@link TaskProcessorMediatorKey} this {@link TaskProcessorMediatorProxyMBean} represents.
     */
    public void addMBean(TaskProcessorMediatorKey   key)
    {

        // create a new MBean and register it
        TaskProcessorMediatorProxy proxy = new TaskProcessorMediatorProxy(key);

        localMBeans.put(key, proxy);
        registerMBean(key, proxy);
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
     * @param key   the TaskProcessorMediatorProxyMBean key
     * @param bean  the MBean
     */
    @SuppressWarnings("rawtypes")
    public void updateMBean(Object key, TaskProcessorMediatorProxyMBean bean)
    {
        TaskProcessorMediatorProxy proxy = localMBeans.get(key);

        if (proxy != null)
        {
            proxy.setValue(bean);
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
     * Return true if shutting down.
     *
     * @return  true if shutting down
     */
    public boolean isShuttingDown()
    {
        return shuttingDown;
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
