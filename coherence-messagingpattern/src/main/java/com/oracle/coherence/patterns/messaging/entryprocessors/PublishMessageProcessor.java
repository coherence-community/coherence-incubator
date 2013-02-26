/*
 * File: PublishMessageProcessor.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.messaging.DefaultMessagingSession;
import com.oracle.coherence.patterns.messaging.Destination;
import com.oracle.coherence.patterns.messaging.Message;
import com.oracle.coherence.patterns.messaging.MessageKey.KeyStrategy;
import com.oracle.coherence.patterns.messaging.MessagePublisher;
import com.oracle.coherence.patterns.messaging.MessagePublisherManager;
import com.oracle.coherence.patterns.messaging.PublishRequestIdentifier;
import com.oracle.coherence.patterns.messaging.Queue;
import com.oracle.coherence.patterns.messaging.TopicSubscription;
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
 * A {@link PublishMessageProcessor} is an entry processor that performs the
 * action of publishing a message "payload" by creating a {@link Message} and
 * putting it into the message cache. Once the message is written to the cache,
 * a backing map listener will trigger an event processor to execute
 * asynchronously, exposing the message to either a {@link TopicSubscription} or a {@link Queue}
 * <p>
 * Each {@link DefaultMessagingSession} has a unique identifier, called the publisher key, that is
 * used to invoke the {@link PublishMessageProcessor}.  A {@link KeyStrategy}�is used ensure that messages are
 * written to the same partition as the key that is used when the {@link PublishMessageProcessor} is invoked.
 * This results in an equal distribution of {@link PublishMessageProcessor}s
 * executing across the cluster.  During execution, the {@link PublishMessageProcessor} calls the {@link MessagePublisher}
 * which puts the partition id assigned to the publisher key into the {@link Message}, then writes the
 * {@link Message} directly into the backing map.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class PublishMessageProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The payload of the {@link Message}.
     */
    private Object payload;

    /**
     * The {@link Identifier} of the {@link Destination}.
     */
    private Identifier destinationIdentifier;

    /**
     * The {@link Identifier} of the request identifier used to prevent a
     * message from being put in the message cache twice in the event of a
     * message partition transfer.
     */
    private PublishRequestIdentifier requestIdentifier;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}
     * .
     */
    public PublishMessageProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param destinationIdentifier destination identifier
     * @param requestIdentifier request identifier
     * @param payload message payload
     */
    public PublishMessageProcessor(Identifier               destinationIdentifier,
                                   PublishRequestIdentifier requestIdentifier,
                                   Object                   payload)
    {
        this.destinationIdentifier = destinationIdentifier;
        this.requestIdentifier     = requestIdentifier;
        this.payload               = payload;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        // Get MessagePublisher for this destination/partition pair then key
        // used to invoke this
        // processor is used to determine the partition.
        MessagePublisher publisher = MessagePublisherManager.getInstance().ensurePublisher(destinationIdentifier,
                                                                                           (Identifier) entry.getKey());

        // If this message was published already then ignore it.
        if (publisher.checkRequestExists(requestIdentifier))
        {
            return null;
        }

        // Generate a message key and the message into the message cache.
        publisher.publishMessage(requestIdentifier, payload);

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        destinationIdentifier = (Identifier) ExternalizableHelper.readObject(in);
        requestIdentifier     = (PublishRequestIdentifier) ExternalizableHelper.readObject(in);
        payload               = ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, destinationIdentifier);
        ExternalizableHelper.writeObject(out, requestIdentifier);
        ExternalizableHelper.writeObject(out, payload);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        destinationIdentifier = (Identifier) reader.readObject(0);
        requestIdentifier     = (PublishRequestIdentifier) reader.readObject(1);
        payload               = reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, destinationIdentifier);
        writer.writeObject(1, requestIdentifier);
        writer.writeObject(2, payload);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("PublishMessageProcessor{destinationIdentifier = %s payload=%s} ",
                             destinationIdentifier,
                             payload);
    }
}
