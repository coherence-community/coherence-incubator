/*
 * File: RegisterSubscriptionsWithMessageProcessor.java
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
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifierSet;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link RegisterSubscriptionsWithMessageProcessor} is used to indicate that a
 * {@link Message} has been made visible to a set of subscriptions.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class RegisterSubscriptionsWithMessageProcessor extends AbstractProcessor implements ExternalizableLite,
                                                                                            PortableObject
{
    /**
     * The {@link SubscriptionIdentifier} of the subscriber receiving the message.
     */
    private SubscriptionIdentifierSet subscriptionIdentifiers;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public RegisterSubscriptionsWithMessageProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier of the subscriber that received the message
     */
    public RegisterSubscriptionsWithMessageProcessor(SubscriptionIdentifierSet subscriptionIdentifiers)
    {
        this.subscriptionIdentifiers = subscriptionIdentifiers;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent())
        {
            Message message = (Message) entry.getValue();

            message.makeVisibleTo(subscriptionIdentifiers);

            // INC-1033 There is a race condition where the subscription could ack the message before
            // the message is visible.  If that happened then the message is no longer needed so remove it.
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
        SubscriptionIdentifierSet subscriptionIdentifiers = new SubscriptionIdentifierSet();

        subscriptionIdentifiers.readExternal(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        subscriptionIdentifiers.writeExternal(out);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        SubscriptionIdentifierSet subscriptionIdentifiers = new SubscriptionIdentifierSet();

        subscriptionIdentifiers.readExternal(reader);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        subscriptionIdentifiers.writeExternal(writer);
    }
}
