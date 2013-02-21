/*
 * File: LeasedSubscription.java
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

import com.oracle.coherence.common.backingmaplisteners.Cause;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.common.leasing.LeaseExpiryCoordinator;
import com.oracle.coherence.common.leasing.LeaseListener;
import com.oracle.coherence.common.leasing.Leased;
import com.oracle.coherence.common.leasing.Leasing;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.messaging.entryprocessors.UnsubscribeProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link LeasedSubscription} is a {@link Subscription} that has a
 * specific life-time defined by a {@link Lease}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"serial"})
public abstract class LeasedSubscription extends ConfigurableSubscription<LeasedSubscriptionConfiguration>
    implements Leased,
               LeaseListener
{
    /**
     * The {@link Lease} that defines the life-time of the {@link Subscription}.
     * When the {@link Lease} expires, the {@link Subscription} is no longer valid.
     */
    private Lease lease;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public LeasedSubscription()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param leasedSubscriptionConfiguration leasedSubscription configuration
     * @param creationTime The time (since in the epoc in milliseconds) when the {@link Subscription} was created.
     */
    public LeasedSubscription(SubscriptionIdentifier          subscriptionIdentifier,
                              Status                          status,
                              LeasedSubscriptionConfiguration leasedSubscriptionConfiguration,
                              long                            creationTime)
    {
        super(subscriptionIdentifier, status, leasedSubscriptionConfiguration);
        this.lease = Leasing.newLease(creationTime, leasedSubscriptionConfiguration.getLeaseDuration());
    }


    /**
     * {@inheritDoc}
     */
    public Lease getLease()
    {
        return lease;
    }


    /**
     * {@inheritDoc}
     */
    public void onLeaseCanceled(Object leaseOwner,
                                Lease  lease)
    {
        if (Logger.isEnabled(Logger.QUIET))
        {
            Logger.log(Logger.DEBUG, "The lease for %s has been canceled. Requesting to unsubscribe", this);
        }

        unsubscribe();
    }


    /**
     * {@inheritDoc}
     */
    public void onLeaseExpiry(Object leaseOwner,
                              Lease  lease)
    {
        if (Logger.isEnabled(Logger.QUIET))
        {
            Logger.log(Logger.DEBUG, "The lease for %s has expired. Requesting to unsubscribe", this);
        }

        unsubscribe();
    }


    /**
     * Extend the lease.
     *
     * @param duration number of milliseconds to extend the lease.  If the input duration is 0
     *   then use the lease duration passed in to the constructor.
     */
    public void extendLease(long duration)
    {
        if (duration == 0)
        {
            duration = getConfiguration().getLeaseDuration();
        }

        getLease().extend(duration);
    }


    /**
     * Unsubscribe from the destination.
     */
    private void unsubscribe()
    {
        SubscriptionIdentifier subscriptionId   = getIdentifier();
        NamedCache             destinationCache = CacheFactory.getCache(Destination.CACHENAME);

        destinationCache.invoke(subscriptionId.getDestinationIdentifier(), new UnsubscribeProcessor(subscriptionId));
    }


    /**
     * Register or deregister the lease.
     *
     * @param mapEvent map event
     * @param cause cause of event
     */
    public void onCacheEntryEvent(MapEvent mapEvent,
                                  Cause    cause)
    {
        if (mapEvent.getId() == MapEvent.ENTRY_INSERTED || mapEvent.getId() == MapEvent.ENTRY_UPDATED)
        {
            // update the lease coordinator with the lease for this subscription
            LeaseExpiryCoordinator.INSTANCE.registerLease(getIdentifier(), getLease(), this);
        }
        else
        {
            // notify the lease coordinator that this subscription has been
            // removed
            LeaseExpiryCoordinator.INSTANCE.deregisterLease(getIdentifier());
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        lease = (Lease) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, lease);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        lease = (Lease) reader.readObject(200);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(200, lease);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("LeasedSubscription{%s, lease=%s}", super.toString(), lease);
    }
}
