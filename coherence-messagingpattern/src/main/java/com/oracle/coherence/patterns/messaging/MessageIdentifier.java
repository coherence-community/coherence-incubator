/*
 * File: MessageIdentifier.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link MessageIdentifier} uniquely identifiers a {@link Message} within a {@link Destination}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class MessageIdentifier implements ExternalizableLite, PortableObject
{
    /**
     * Coherence partition identifier.
     */
    private int partitionId;

    /**
     * Monotonically increasing message sequence number.
     */
    private long messageSequenceNumber;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public MessageIdentifier()
    {
    }


    /**
     * Constructor.
     *
     * @param partitionId partition identifier
     * @param messageSequenceNumber message sequence number
     */
    public MessageIdentifier(int  partitionId,
                             long messageSequenceNumber)
    {
        this.partitionId           = partitionId;
        this.messageSequenceNumber = messageSequenceNumber;
    }


    /**
     * Get an identifier that represents a null.
     *
     * @return message identifier
     */
    public static MessageIdentifier getNullIdentifier()
    {
        return new MessageIdentifier(-1, -1);
    }


    /**
     * Check if the identifier is a null identifier.
     *
     * @return true if null identifier
     */
    public boolean isNullIdentifier()
    {
        if (messageSequenceNumber == -1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Get the partition identifier.
     *
     * @return partition identifier
     */
    public int getPartitionId()
    {
        return partitionId;
    }


    /**
     * Get the message sequence number.
     *
     * @return message sequence number
     */
    public long getMessageSequenceNumber()
    {
        return messageSequenceNumber;
    }


    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + (int) (partitionId);
        result = prime * result + (int) (messageSequenceNumber ^ (messageSequenceNumber >>> 32));

        return result;
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

        final MessageIdentifier other = (MessageIdentifier) obj;

        if (partitionId != other.partitionId)
        {
            return false;
        }

        if (messageSequenceNumber != other.messageSequenceNumber)
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        partitionId           = ExternalizableHelper.readInt(in);
        messageSequenceNumber = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeInt(out, partitionId);
        ExternalizableHelper.writeLong(out, messageSequenceNumber);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.partitionId           = reader.readInt(1);
        this.messageSequenceNumber = reader.readLong(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeInt(1, partitionId);
        writer.writeLong(2, messageSequenceNumber);
    }


    /**
     * Return a string version of the {@link MessageIdentifier}.
     *
     * @return message identifier string
     */
    public String toString()
    {
        return String.format("MsgId{%d-%d}", partitionId, messageSequenceNumber);
    }
}
