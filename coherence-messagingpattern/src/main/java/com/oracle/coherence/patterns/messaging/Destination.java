/*
 * File: Destination.java
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

package com.oracle.coherence.patterns.messaging;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.messaging.entryprocessors.CreateSubscriptionProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ExtractorProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link Destination} represents and manages the state of a uniquely named
 * store-and-forward location to which {@link Message}s will be logically
 * delivered and then asynchronously forwarded in-order of arrival to
 * {@link Subscription}s.
 * <p>
 * There are generally two types of {@link Destination}s; {@link Topic}s that
 * are used for one-to-many messaging, and {@link Queue}s that are used for
 * one-to-one messaging.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class Destination implements ExternalizableLite, PortableObject
{
    /**
     * The type of  {@link Destination}.
     */
    public enum DestinationType
    {
        /**
         * Queue destination.
         */
        Queue,

        /**
         * Topic destination.
         */
        Topic
    }


    /**
     * The Coherence cache in which {@link Destination}s will be
     * placed.
     */
    public static final String CACHENAME = "coherence.messagingpattern.destinations";

    /**
     * The {@link Identifier} of the {@link Destination}.
     */
    private Identifier identifier;

    ;

    /**
     * The set of {@link SubscriptionIdentifier}s for {@link Subscription} to
     * the {@link Destination}.
     *
     * {@link Message}s will only be delivered to these {@link Subscription}
     * s.
     */
    private HashSet<SubscriptionIdentifier> subscriptionIdentifiers;

    /**
     * The cluster-wide uniquely generated JMX bean name for the
     * {@link Destination}.
     */
    private String mBeanName;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}
     * .
     */
    public Destination()
    {
        this.subscriptionIdentifiers = new HashSet<SubscriptionIdentifier>();
    }


    /**
     * Standard Constructor.
     *
     * @param identifier The {@link Identifier} of the {@link Destination}
     */
    public Destination(Identifier identifier)
    {
        this();
        this.identifier = identifier;
    }


    /**
     * Standard Constructor.
     *
     * @param destinationName The name of the {@link Destination}
     */
    public Destination(String destinationName)
    {
        this();
        this.identifier = StringBasedIdentifier.newInstance(destinationName);
    }


    /**
     * Returns the cluster-wide uniquely generated JMX bean name for the
     * {@link Destination}.
     *
     * @return MBean name
     */
    public String getMBeanName()
    {
        return mBeanName;
    }


    /**
     * Sets the cluster-wide unique JMX bean name for the {@link Destination}
     * .
     *
     * @param mBeanName name of MBean
     */
    void setMBeanName(String mBeanName)
    {
        this.mBeanName = mBeanName;
    }


    /**
     * Get the name of the {@link Destination}.
     *
     * @return destination name
     */
    public String getName()
    {
        return this.identifier.toString();
    }


    /**
     * Returns a {@link Identifier} for referring to the {@link Destination}
     * .
     *
     * @return destination identifier
     */
    public Identifier getIdentifier()
    {
        return identifier;
    }


    /**
     * Return if the {@link Destination} has any current {@link Subscription}
     * s.
     *
     * @return true if destination has subscriptions
     */
    public boolean hasSubscriptions()
    {
        return subscriptionIdentifiers.size() > 0;
    }


    /**
     * Returns the {@link SubscriptionIdentifier}s of the current
     * {@link Subscription}s for the {@link Destination}.
     *
     * @return set of subscriptions
     */
    public Set<SubscriptionIdentifier> getSubscriptionIdentifiers()
    {
        return subscriptionIdentifiers;
    }


    /**
     * Returns if the {@link Destination} contains a registration for the
     * specified {@link SubscriptionIdentifier}.
     *
     * @param subscriptionIdentifier subscription identifier
     * @return true if destination has subscription
     */
    public boolean containsSubscriptionWithIdentifer(SubscriptionIdentifier subscriptionIdentifier)
    {
        return subscriptionIdentifiers.contains(subscriptionIdentifier);
    }


    /**
     * Adds a new {@link Subscription} {@link SubscriptionIdentifier} to the
     * {@link Destination}.
     *
     * @param subscriptionIdentifier subscription identifier
     *
     * @return <code>true</code> if a {@link Subscription} with the specified
     * {@link SubscriptionIdentifier} already exists on the {@link Destination}
     *
     */
    public boolean addSubscriptionWithIdentifer(SubscriptionIdentifier subscriptionIdentifier)
    {
        return subscriptionIdentifiers.add(subscriptionIdentifier);
    }


    /**
     * Creates a {@link Subscription} to the {@link Destination} identified
     * by the specified {@link SubscriptionIdentifier}.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param subscriptionConfiguration subscription configuration
     * @param subscription The object that will be used to capture the state of
     * the subscription
     */
    public void subscribe(SubscriptionIdentifier    subscriptionIdentifier,
                          SubscriptionConfiguration subscriptionConfiguration,
                          Subscription              subscription)
    {
        // attempt to register the subscription
        if (subscription == null)
        {
            Logger.log(Logger.ERROR,
                       "Can't subscribe to %s with %s as no subscription was provided. Ignoring request to subscribe",
                       getIdentifier(),
                       subscriptionIdentifier);
        }
        else
        {
            // first attempt to place the subscription into the subscriptions
            // cache (if it does not already exist)
            NamedCache subscriptionsCache = CacheFactory.getCache(Subscription.CACHENAME);
            Subscription existingSubscription = (Subscription) subscriptionsCache.invoke(subscription.getIdentifier(),
                                                                                         new CreateSubscriptionProcessor(subscription));

            if (existingSubscription != null)
            {
                if (Logger.isEnabled(Logger.WARN))
                {
                    Logger.log(Logger.WARN,
                               "Subscription %s already exists in subscriptions cache. Ignoring request to create a subscription",
                               subscription.getIdentifier(),
                               getIdentifier());
                }
            }

            // then add the subscription to the destination
            if (!addSubscriptionWithIdentifer(subscription.getIdentifier()))
            {
                if (Logger.isEnabled(Logger.WARN))
                {
                    Logger.log(Logger.WARN,
                               "Subscription %s already registered with the Destination %s. Ignoring request to register the subscription",
                               subscription.getIdentifier(),
                               getIdentifier());
                }
            }
        }
    }


    /**
     * Unsubscribe the specified {@link SubscriptionIdentifier} and
     * appropriately deal with (perhaps rollback) the {@link MessageTracker} of
     * messages.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param messageTracker message tracker
     */
    public void unsubscribe(SubscriptionIdentifier subscriptionIdentifier,
                            MessageTracker         messageTracker)
    {
        // remove the subscriber from the destination
        removeSubscriptionWithIdentifier(subscriptionIdentifier);

        // remove the subscription
        NamedCache subscriptionsCache = CacheFactory.getCache(Subscription.CACHENAME);

        subscriptionsCache.remove(subscriptionIdentifier);
    }


    /**
     * Removes the {@link Subscription} with the specified
     * {@link SubscriptionIdentifier} from the {@link Destination}.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public void removeSubscriptionWithIdentifier(SubscriptionIdentifier subscriptionIdentifier)
    {
        subscriptionIdentifiers.remove(subscriptionIdentifier);
    }


    /**
     * Return true if destination is queue.
     *
     * @param destinationIdentifier destination identifier
     * @return true if destination id queue.
     */
    public static boolean isQueue(Identifier destinationIdentifier)
    {
        return (getClassName(destinationIdentifier).contains("Queue"));
    }


    /**
     * Return true if destination is topic.
     *
     * @param destinationIdentifier destination identifier
     * @return true if destination id topic.
     */
    public static boolean isTopic(Identifier destinationIdentifier)
    {
        return (getClassName(destinationIdentifier).contains("Topic"));
    }


    /**
     * Get the class name of the destination.
     *
     * @param destinationIdentifier destination Identifier
     * @return class name of the destination.
     */
    private static String getClassName(Identifier destinationIdentifier)
    {
        NamedCache destinationsCache = CacheFactory.getCache(Destination.CACHENAME);

        return (String) destinationsCache.invoke(destinationIdentifier,
                                                 new ConditionalProcessor(PresentFilter.INSTANCE,
                                                                          new ExtractorProcessor(new ChainedExtractor("getClass.getName"))));
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        identifier              = (Identifier) ExternalizableHelper.readObject(in);
        subscriptionIdentifiers = new HashSet<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in,
                                            this.subscriptionIdentifiers,
                                            Thread.currentThread().getContextClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, identifier);
        ExternalizableHelper.writeCollection(out, subscriptionIdentifiers);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        identifier              = (Identifier) reader.readObject(0);
        subscriptionIdentifiers = new HashSet<SubscriptionIdentifier>();
        reader.readCollection(1, subscriptionIdentifiers);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, identifier);
        writer.writeCollection(1, subscriptionIdentifiers);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Destination{identifier=%s, subscriptionIdentifiers=%s}",
                             identifier,
                             subscriptionIdentifiers);
    }
}
