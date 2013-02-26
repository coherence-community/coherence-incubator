/*
 * File: MessagingMBeanManager.java
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

package com.oracle.coherence.patterns.messaging.management;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.messaging.Destination;
import com.oracle.coherence.patterns.messaging.Queue;
import com.oracle.coherence.patterns.messaging.QueueSubscription;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.oracle.coherence.patterns.messaging.Topic;
import com.oracle.coherence.patterns.messaging.TopicSubscription;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.Registry;

import java.util.HashMap;

/**
 * The {@link MessagingMBeanManager} is used to register and unregister messaging MBeans with the MBean Server.
 * MBeans can represent cache objects. {@link MessagingMBeanProxy} is used to expose the object to the MBean server.
 * Whenever the cache object is modified, the backing map listener or event processor calls the {@link MessagingMBeanManager}
 * to register the MBean.  If the MBean is already registered, then the the {@link MessagingMBeanProxy} is updated with
 * the reference to the newly updated object. The {@link MessagingMBeanManager} maintains a map of {@link MessagingMBeanProxy}s
 * indexed by the MBean object name.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
public class MessagingMBeanManager
{
    /**
     * The singleton instance of the {@link MessagingMBeanManager}.
     */
    private static MessagingMBeanManager INSTANCE = new MessagingMBeanManager();

    /**
     * HashMap keeping track of the {@link MBean} objects.
     */
    private HashMap<String, MessagingMBeanProxy> mbeanMap;


    /**
     *  Constructor is private to enforce singleton class.
     */
    private MessagingMBeanManager()
    {
        mbeanMap = new HashMap<String, MessagingMBeanProxy>();
    }


    /**
     * Get the singleton {@link MessagingMBeanManager} instance.
     *
     * @return MessagingMBeanManager
     */
    public static MessagingMBeanManager getInstance()
    {
        return INSTANCE;
    }


    /**
     * Register the MBean with the MBean server.  If the MBean already exists then object the proxy with the
     * new instance of the object. If the MBean doesn't exist then create a proxy for the object and store the
     * proxy in the MBean map.
     *
     * @param object the managed object the object to be registered.
     * @param proxyClass the proxy class that acts as an MBean wrapper for the object.
     * @param mbeanName the MBean name of the object
     * @return mbean name
     */
    @SuppressWarnings("unchecked")
    public String registerMBean(Object object,
                                Class  proxyClass,
                                String mbeanName)
    {
        Registry registry;

        try
        {
            registry = CacheFactory.getCluster().getManagement();

            if (registry == null)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            // Ignore any exceptions since the cluster may be shutting down.
            return null;
        }

        String globalName = registry.ensureGlobalName(mbeanName);

        addMBean(registry, proxyClass, globalName, object);

        return globalName;

    }


    /**
     * Unregister the MBean with the MBean server.
     *
     * @param object the managed object the object to be unregistered.
     * @param mbeanName the MBean name of the object
     */
    public void unregisterMBean(Object object,
                                String mbeanName)
    {
        try
        {
            Registry registry = CacheFactory.getCluster().getManagement();

            if (registry == null)
            {
                return;
            }

            String globalName = registry.ensureGlobalName(mbeanName);

            removeMBean(registry, globalName);
        }
        catch (Exception e)
        {
            ;    // Ignore any exceptions since the cluster may be shutting down.
        }
    }


    /**
     * Add the proxy to the MBean map.
     *
     * @param registry Coherence MBean registry
     * @param proxyClass class name of MBean proxy
     * @param key map key
     * @param managedObject object to be managed
     */
    @SuppressWarnings("unchecked")
    private synchronized void addMBean(Registry registry,
                                       Class    proxyClass,
                                       String   key,
                                       Object   managedObject)
    {
        try
        {
            if (mbeanMap.containsKey(key))
            {
                MessagingMBeanProxy queueProxy = mbeanMap.get(key);

                queueProxy.setObject(managedObject);
            }
            else
            {
                MessagingMBeanProxy mbean = (MessagingMBeanProxy) proxyClass.newInstance();

                mbean.setObject(managedObject);
                registry.register(key, mbean);
                mbeanMap.put(key, mbean);
            }
        }
        catch (Throwable t)
        {
            Logger.log(Logger.ERROR, "Error adding MBean for key %s " + key);
        }
    }


    /**
     * Remove the MBean from the MBean map.
     *
     * @param registry Coherence MBean registry
     * @param key map key
     */
    private synchronized void removeMBean(Registry registry,
                                          String   key)
    {
        Object mbean = mbeanMap.get(key);

        if (mbean != null)
        {
            registry.unregister(key);
            mbeanMap.remove(key);
        }

    }


    /**
     * Build the MBean name for a {@link Queue}.
     *
     * @param destinationIdentifier destination identifier
     * @return MBean name
     */
    public String buildQueueMBeanName(Identifier destinationIdentifier)
    {
        return buildDestinationMBeanName(destinationIdentifier, "Queues");
    }


    /**
     * Build the MBean name for a {@link Topic}.
     *
     * @param destinationIdentifier destination identifier
     * @return MBean name
     */
    public String buildTopicMBeanName(Identifier destinationIdentifier)
    {
        return buildDestinationMBeanName(destinationIdentifier, "Topics");
    }


    /**
     * Build the MBean name for a {@link Destination}.
     *
     * @param destinationIdentifier destination identifier
     * @param destinationType type of destination
     * @return MBean name
     */
    private String buildDestinationMBeanName(Identifier destinationIdentifier,
                                             String     destinationType)
    {
        return String.format("type=Messaging" + ",destination=" + destinationType + ",destinationId=%s",
                             destinationIdentifier);
    }


    /**
     * Build the MBean name for a {@link QueueSubscription}.
     *
     * @param subscriptionIdentifier subscription identifier
     * @return MBean name
     */
    public String buildQueueSubscriptionMBeanName(SubscriptionIdentifier subscriptionIdentifier)
    {
        String destMbeanName = buildQueueMBeanName(subscriptionIdentifier.getDestinationIdentifier());

        return buildSubscriptionMBeanName(destMbeanName, subscriptionIdentifier);
    }


    /**
     * Build the MBean name for a {@link TopicSubscription}.
     *
     * @param subscriptionIdentifier subscription identifier
     * @return MBean name
     */
    public String buildTopicSubscriptionMBeanName(SubscriptionIdentifier subscriptionIdentifier)
    {
        String destMbeanName = buildTopicMBeanName(subscriptionIdentifier.getDestinationIdentifier());

        return buildSubscriptionMBeanName(destMbeanName, subscriptionIdentifier);
    }


    /**
     * Build the MBean name for a {@link Subscription}.
     *
     * @param destinationMbeanName mbean name
     * @param subscriptionIdentifier subscription identifier
     * @return MBean name
     */
    private String buildSubscriptionMBeanName(String                 destinationMbeanName,
                                              SubscriptionIdentifier subscriptionIdentifier)
    {
        // Replace all commas with dashes to prevent the jconsole tree from
        // displaying an extra child
        String subId = subscriptionIdentifier.toString().replace(',', '-');
        String name  = String.format(destinationMbeanName + ",subscription=Subscriptions" + ",subscriptionId=%s",
                                     subId);

        return name;
    }
}
