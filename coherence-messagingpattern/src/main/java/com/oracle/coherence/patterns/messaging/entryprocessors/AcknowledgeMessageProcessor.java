/*
 * File: AcknowledgeMessageProcessor.java
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
 * The {@link AcknowledgeMessageProcessor} is used to indicate that a
 * {@link Message} has been acknowledged by a {@link Subscription}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class AcknowledgeMessageProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link SubscriptionIdentifier} of the subscriber acknowledging the message.
     */
    private SubscriptionIdentifier subscriptionIdentifier;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public AcknowledgeMessageProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier of the subscriber acknowledging the message
     */
    public AcknowledgeMessageProcessor(SubscriptionIdentifier subscriptionIdentifier)
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
            // Tell the message that the subscription has delivered and acknowledged the message.
            // If the message have been acknowledged by all subscriptions then delete it.
            //
            Message message = (Message) entry.getValue();

            message.deliveredTo(subscriptionIdentifier);
            message.acknowledgedBy(subscriptionIdentifier);

            if (message.isAcknowledged())
            {
                entry.remove(false);
            }
            else
            {
                entry.setValue(message, true);
            }

            return message;

        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.subscriptionIdentifier = (SubscriptionIdentifier) ExternalizableHelper.readExternalizableLite(in);
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
        this.subscriptionIdentifier = (SubscriptionIdentifier) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, subscriptionIdentifier);
    }
}
