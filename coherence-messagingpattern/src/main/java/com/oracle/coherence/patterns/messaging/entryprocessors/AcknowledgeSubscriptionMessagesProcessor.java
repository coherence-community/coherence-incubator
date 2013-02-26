/*
 * File: AcknowledgeSubscriptionMessagesProcessor.java
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
import com.oracle.coherence.patterns.messaging.Subscription;
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
 * The {@link AcknowledgeSubscriptionMessagesProcessor} is used record that a
 * set of {@link Message}s have been acknowledged by a {@link Subscription}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class AcknowledgeSubscriptionMessagesProcessor extends AbstractProcessor implements ExternalizableLite,
                                                                                           PortableObject
{
    /**
     * The {@link MessageTracker} containing the set of {@link MessageIdentifier}s being acknowledged.
     */
    private MessageTracker messageTracker;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public AcknowledgeSubscriptionMessagesProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param messageTracker contains the set of messages being acknowledged.
     */
    public AcknowledgeSubscriptionMessagesProcessor(MessageTracker messageTracker)
    {
        this.messageTracker = messageTracker;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent() && entry.getValue() instanceof Subscription)
        {
            Subscription subscription = (Subscription) entry.getValue();

            subscription.onAcknowledgeMessages(messageTracker);
            entry.setValue(subscription, true);
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.messageTracker = (MessageTracker) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, messageTracker);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.messageTracker = (MessageTracker) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, messageTracker);
    }
}
