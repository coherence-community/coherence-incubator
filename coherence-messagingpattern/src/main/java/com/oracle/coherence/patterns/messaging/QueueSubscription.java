/*
 * File: QueueSubscription.java
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
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.common.logging.Logger;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.processor.UpdaterProcessor;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A {@link QueueSubscription} represents the <strong>state</strong> of an individual subscription
 * to a {@link Queue}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class QueueSubscription extends LeasedSubscription
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public QueueSubscription()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param leasedSubscriptionConfiguration configuration
     * @param creationTime The time (since in the epoc in milliseconds) when the subscription was created.
     */
    public QueueSubscription(SubscriptionIdentifier          subscriptionIdentifier,
                             Status                          status,
                             LeasedSubscriptionConfiguration leasedSubscriptionConfiguration,
                             long                            creationTime)
    {
        super(subscriptionIdentifier, status, leasedSubscriptionConfiguration, creationTime);
    }


    /**
     * Standard Constructor.
     *
     * @param destinationIdentifier destination identifier
     * @param subscriberName name
     * @param leasedSubscriptionConfiguration configuration
     * @param creationTime The time (since in the epoc in milliseconds) when the subscription was created.
     */
    public QueueSubscription(Identifier                      destinationIdentifier,
                             String                          subscriberName,
                             Status                          status,
                             LeasedSubscriptionConfiguration leasedSubscriptionConfiguration,
                             long                            creationTime)
    {
        this(new SubscriptionIdentifier(destinationIdentifier,
                                        subscriberName), status, leasedSubscriptionConfiguration, creationTime);
    }


    /**
     * Roll back all messages that have been delivered but not acknowledged.
     */
    @Override
    public void rollback()
    {
        // Get the message keys to rollback
        ArrayList<MessageKey> messageKeys =
            getVisibleMessageTracker().getMessageKeys(getIdentifier().getDestinationIdentifier());

        if (messageKeys.size() > 0)
        {
            // Tell the {@link Message} to remove the subscription from its visible
            // list
            CacheFactory.getCache(Message.CACHENAME).invokeAll(messageKeys,
                                                               new UpdaterProcessor("makeInvisibleTo",
                                                                                    getIdentifier()));
        }

        // Remove the messages that have been delivered but not acknowledge from the accepted messages list.
        // This is done so that the Queue object can re-deliver the message to this subscription.
        //
        Iterator<MessageIdentifier> iter = getVisibleMessageTracker().iterator();

        while (iter.hasNext())
        {
            rollbackAcceptedMessage(iter.next());
        }

        // Create a new visible message tracker.
        createVisibleMessageTracker();
    }


    /**
     * {@inheritDoc}
     */
    public void onLeaseSuspended(Object leaseOwner,
                                 Lease  lease)
    {
        Logger.log(Logger.ERROR,
                   "Unexpected %s Lease %s was suspended.  This should never happen! Ignoring request",
                   leaseOwner,
                   lease);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("QueueSubscription{%s}", super.toString());
    }
}
