/*
 * File: SubscriptionIdentifier.java
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
import com.oracle.coherence.common.identifiers.UUIDBasedIdentifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SubscriptionIdentifier} identifies a {@link Subscription} within a cache.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SubscriptionIdentifier implements Identifier, ExternalizableLite, PortableObject
{
    /**
     * The {@link Identifier} of the {@link Destination} that owns the {@link Subscription}.
     */
    private Identifier destinationIdentifier;

    /**
     * The {@link Identifier} of the {@link Subscription}.
     */
    private Identifier subscriberIdentifier;


    /**
     * Required for {@link ExternalizableLite}.
     */
    public SubscriptionIdentifier()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param destinationIdentifier destination identifier
     */
    public SubscriptionIdentifier(Identifier destinationIdentifier)
    {
        this.destinationIdentifier = destinationIdentifier;
        this.subscriberIdentifier  = UUIDBasedIdentifier.newInstance();
    }


    /**
     * Standard Constructor.
     *
     * @param destinationIdentifier destination identifier
     * @param subscriberIdentifier subscriber identifier
     */
    public SubscriptionIdentifier(Identifier destinationIdentifier,
                                  Identifier subscriberIdentifier)
    {
        this.destinationIdentifier = destinationIdentifier;
        this.subscriberIdentifier  = subscriberIdentifier;
    }


    /**
     * Standard Constructor.
     *
     * @param destinationIdentifier destination identifier
     * @param subscriberName subscriber name
     */
    public SubscriptionIdentifier(Identifier destinationIdentifier,
                                  String     subscriberName)
    {
        this.destinationIdentifier = destinationIdentifier;
        this.subscriberIdentifier  = StringBasedIdentifier.newInstance(subscriberName);
    }


    /**
     * Returns the {@link Identifier} of the {@link Destination}
     * to which the identified subscriber is subscribed.
     *
     * @return destination identifier
     */
    public Identifier getDestinationIdentifier()
    {
        return destinationIdentifier;
    }


    /**
     * Returns the {@link Identifier} of the subscriber that
     * is subscribed to the identified {@link Destination}.
     *
     * @return subscriber identifier
     */
    public Identifier getSubscriberIdentifier()
    {
        return subscriberIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((destinationIdentifier == null) ? 0 : destinationIdentifier.hashCode());
        result = prime * result + ((subscriberIdentifier == null) ? 0 : subscriberIdentifier.hashCode());

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        final SubscriptionIdentifier other = (SubscriptionIdentifier) obj;

        if (destinationIdentifier == null)
        {
            if (other.destinationIdentifier != null)
            {
                return false;
            }
        }
        else if (!destinationIdentifier.equals(other.destinationIdentifier))
        {
            return false;
        }

        if (subscriberIdentifier == null)
        {
            if (other.subscriberIdentifier != null)
            {
                return false;
            }
        }
        else if (!subscriberIdentifier.equals(other.subscriberIdentifier))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("SubscriptionIdentifier{destinationIdentifier=%s, subscriberIdentifier=%s}",
                             destinationIdentifier,
                             subscriberIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        destinationIdentifier = (Identifier) ExternalizableHelper.readObject(in);
        subscriberIdentifier  = (Identifier) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, destinationIdentifier);
        ExternalizableHelper.writeObject(out, subscriberIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        destinationIdentifier = (Identifier) reader.readObject(0);
        subscriberIdentifier  = (Identifier) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, destinationIdentifier);
        writer.writeObject(1, subscriberIdentifier);
    }
}
