/*
 * File: CreateSubscriptionProcessor.java
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

package com.oracle.coherence.patterns.messaging.entryprocessors;

import com.oracle.coherence.patterns.messaging.LeasedSubscription;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link EntryProcessor} that will place an {@link Subscription} into the
 * {@link Subscription#CACHENAME} Coherence Cache, if it does not already
 * exist.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class CreateSubscriptionProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link Subscription} to be created.
     */
    private Subscription subscription;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public CreateSubscriptionProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscription subscription
     */
    public CreateSubscriptionProcessor(Subscription subscription)
    {
        this.subscription = subscription;

    }


    /**
     * Create the subscription if it doesn't exist and set its new value in the
     * cache. Return the subscription to the caller.
     *
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        Subscription existingSubscription = null;

        // Create the subscription if it doesn't exist.
        if (!entry.isPresent())
        {
            entry.setValue(subscription);
        }
        else
        {
            // If leased subscription already exists then set the lease.
            existingSubscription = (Subscription) entry.getValue();

            if (existingSubscription instanceof LeasedSubscription)
            {
                long duration = ((LeasedSubscription) existingSubscription).getConfiguration().getLeaseDuration();

                ((LeasedSubscription) existingSubscription).extendLease(duration);
                entry.setValue(existingSubscription);
            }
        }

        // Return the existing subscription or null if the subscription didn't exist.
        return existingSubscription;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.subscription = (Subscription) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, subscription);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.subscription = (Subscription) reader.readObject(0);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, subscription);

    }
}
