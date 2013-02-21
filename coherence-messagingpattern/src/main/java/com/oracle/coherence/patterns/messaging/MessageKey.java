/*
 * File: MessageKey.java
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
import com.oracle.coherence.patterns.messaging.entryprocessors.PublishMessageProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link MessageKey} is used as a cache key for a {@link Message}.
 * The key is comprised of a destination {@link Identifier} and {@link MessageIdentifier}
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class MessageKey implements ExternalizableLite, PortableObject
{
    /**
     * The {@link Identifier} of the {@link Destination}.
     */
    private Identifier destinationIdentifier;

    /**
     * The {@link MessageIdentifier} of the {@link Message}.
     */
    private MessageIdentifier messageIdentifier;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public MessageKey()
    {
    }


    /**
     * Constructor.
     *
     * @param destinationIdentifier destination identifier
     * @param messageIdentifier message identifier
     */
    public MessageKey(Identifier        destinationIdentifier,
                      MessageIdentifier messageIdentifier)
    {
        this.destinationIdentifier = destinationIdentifier;
        this.messageIdentifier     = messageIdentifier;
    }


    /**
     * Return the destination identifier.
     *
     * @return destination identifier
     */
    public Identifier getDestinationIdentifier()
    {
        return destinationIdentifier;
    }


    /**
     * Return the {@link MessageIdentifier} .
     *
     * @return message identifier
     */
    public MessageIdentifier getMessageIdentifier()
    {
        return messageIdentifier;
    }


    /**
     * {@inheritDoc}
     */
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

        final MessageKey other = (MessageKey) obj;

        if (!messageIdentifier.equals(other.messageIdentifier))
        {
            return false;
        }

        if (!destinationIdentifier.equals(other.destinationIdentifier))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + destinationIdentifier.hashCode();
        result = prime * result + messageIdentifier.hashCode();

        return result;
    }


    /**
     * Return a string version of the {@link MessageKey}.
     *
     * @return message key string
     */
    public String toString()
    {
        return String.format("%s-%s", destinationIdentifier.toString(), messageIdentifier.toString());

    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        destinationIdentifier = (Identifier) ExternalizableHelper.readObject(in);
        messageIdentifier     = (MessageIdentifier) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, destinationIdentifier);
        ExternalizableHelper.writeObject(out, messageIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        destinationIdentifier = (Identifier) reader.readObject(0);
        messageIdentifier     = (MessageIdentifier) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, destinationIdentifier);
        writer.writeObject(1, messageIdentifier);
    }


    /**
     * A {@link KeyStrategy} ensures that messages are written to the same partition as the key
     * that is used when the {@link PublishMessageProcessor} is invoked.
     * Each {@link DefaultMessagingSession} has a unique identifier, called the publisher key, that is used to invoke the
     * {@link PublishMessageProcessor}.  This results in an equal distribution of {@link PublishMessageProcessor}s
     * executing across the cluster.  During execution, the {@link PublishMessageProcessor} calls the {@link MessagePublisher}
     * which puts the partition id assigned to the publisher key into the {@link Message}, then writes the
     * {@link Message} directly into the backing map.  Coherence will then use this class to get the partition for
     * the {@link Message}.  Since the {@link MessagePublisher} has already put the partition id into the {@link Message},
     * this class simply returns the partition id that is already in the {@link Message}.
     */
    public static class KeyStrategy implements KeyPartitioningStrategy
    {
        /**
         * The  {@link PartitionedService} needed to get partition information.
         */
        PartitionedService service;


        /**
         * Determine the set of partitions that all keys associated with the
         * specified key are assigned to.
         *
         * @param oKey  a key in its Object form
         *
         * @return the PartitionSet associated with the specified key
         */
        public PartitionSet getAssociatedPartitions(Object oKey)
        {
            PartitionSet parts = new PartitionSet(service.getPartitionCount());

            parts.add(getKeyPartition(oKey));

            return parts;
        }


        /**
         * {@inheritDoc}
         */
        public int getKeyPartition(Object key)
        {
            if (key instanceof MessageKey)
            {
                return (int) ((MessageKey) key).getMessageIdentifier().getPartitionId();
            }
            else
            {
                // This must be the publisherId key used to run the
                // PublishMessageEntryProcessor.  Return a partition Id using a
                // hash code.
                int numPartitions = service.getPartitionCount();

                int hash          = key.hashCode();

                if (hash < 0)
                {
                    hash = 0 - hash;
                }

                int partitionId = (hash % numPartitions);

                return partitionId;
            }
        }


        /**
         * {@inheritDoc}
         */
        public void init(PartitionedService service)
        {
            this.service = service;
        }
    }
}
