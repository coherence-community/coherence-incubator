/*
 * File: RequestMessageFromQueueProcessor.java
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

import com.oracle.coherence.patterns.messaging.Message;
import com.oracle.coherence.patterns.messaging.Queue;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link RequestMessageFromQueueProcessor} is used to request a
 * {@link Message} for a queue {@link Subscription}.
 * <p>
 * The side-effect is that the {@link SubscriptionIdentifier} of the {@link Subscription}
 * is added to deliveredTo state of the {@link Message}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class RequestMessageFromQueueProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link SubscriptionIdentifier} of the subscriber that is requesting the message.
     */
    private SubscriptionIdentifier subscriptionIdentifier;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public RequestMessageFromQueueProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     */
    public RequestMessageFromQueueProcessor(SubscriptionIdentifier subscriptionIdentifier)
    {
        this.subscriptionIdentifier = subscriptionIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent())
        {
            Queue queue = (Queue) entry.getValue();

            queue.requestMessage(subscriptionIdentifier);
            entry.setValue(queue, true);
        }

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
}
