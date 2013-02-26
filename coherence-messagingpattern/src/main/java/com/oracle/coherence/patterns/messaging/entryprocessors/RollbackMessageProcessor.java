/*
 * File: RollbackMessageProcessor.java
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
import com.oracle.coherence.patterns.messaging.MessageIdentifier;
import com.oracle.coherence.patterns.messaging.MessageTracker;
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
 * The {@link RollbackMessageProcessor} is used to roll back {@link Message}s for a {@link Queue}
 * {@link Message} for a {@link Subscription}.
 * <p>
 * The side-effect is that the {@link SubscriptionIdentifier} of the {@link Subscription}
 * is added to deliveredTo state of the {@link Message}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class RollbackMessageProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link SubscriptionIdentifier} of the subscriber that is rolling back the messages.
     */
    private SubscriptionIdentifier subscriptionIdentifier;

    /**
     * The {@link MessageTracker} containing the set of {@link MessageIdentifier}s to roll back.
     */
    private MessageTracker messageTracker;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public RollbackMessageProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier the subscription that is rolling back the messages.
     * @param messageTracker the message identifiers of messages to roll back.
     */
    public RollbackMessageProcessor(SubscriptionIdentifier subscriptionIdentifier,
                                    MessageTracker         messageTracker)
    {
        this.subscriptionIdentifier = subscriptionIdentifier;
        this.messageTracker         = messageTracker;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent())
        {
            Queue queue = (Queue) entry.getValue();

            queue.rollbackMessages(subscriptionIdentifier, messageTracker);
            entry.setValue(queue);
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        subscriptionIdentifier = (SubscriptionIdentifier) ExternalizableHelper.readExternalizableLite(in);
        messageTracker         = (MessageTracker) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeExternalizableLite(out, subscriptionIdentifier);
        ExternalizableHelper.writeObject(out, messageTracker);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        subscriptionIdentifier = (SubscriptionIdentifier) reader.readObject(0);
        messageTracker         = (MessageTracker) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, subscriptionIdentifier);
        writer.writeObject(1, messageTracker);
    }
}
