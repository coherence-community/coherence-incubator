/*
 * File: UnsubscribeProcessor.java
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

import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.messaging.Destination;
import com.oracle.coherence.patterns.messaging.MessageTracker;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.ExtractorProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link UnsubscribeProcessor} is used to remove a {@link Subscription} to
 * a {@link Destination}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class UnsubscribeProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link SubscriptionIdentifier} of the {@link Subscription} to the {@link Destination}.
     */
    private SubscriptionIdentifier subscriptionIdentifier;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public UnsubscribeProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier the identifier for the {@link Subscription}
     */
    public UnsubscribeProcessor(SubscriptionIdentifier subscriptionIdentifier)
    {
        this.subscriptionIdentifier = subscriptionIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        Destination destination = (Destination) entry.getValue();

        if (destination == null)
        {
            return null;
        }

        if (Logger.isEnabled(Logger.QUIET))
        {
            Logger.log(Logger.QUIET, "Unsubscribing %s", subscriptionIdentifier);
        }

        // get the visible message range from either environment or subscription
        // (INC-165)
        MessageTracker messageTracker =
            (MessageTracker) CacheFactory.getCache(Subscription.CACHENAME).invoke(subscriptionIdentifier,
                                                                                  new ExtractorProcessor("getVisibleMessageTracker"));

        // delegate the implementation to the underlying destination
        destination.unsubscribe(subscriptionIdentifier, messageTracker);

        entry.setValue(destination);

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        subscriptionIdentifier = (SubscriptionIdentifier) ExternalizableHelper.readExternalizableLite(in);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeExternalizableLite(out, subscriptionIdentifier);

    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        subscriptionIdentifier = (SubscriptionIdentifier) reader.readObject(0);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, subscriptionIdentifier);

    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("UnsubscribeProcessor{subscriptionIdentifier=%s}", subscriptionIdentifier);
    }
}
