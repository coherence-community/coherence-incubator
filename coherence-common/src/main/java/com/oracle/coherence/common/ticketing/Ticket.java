/*
 * File: Ticket.java
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

package com.oracle.coherence.common.ticketing;

import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Ticket} is an specialized {@link Identifier} that represents a
 * ordered "sequence number" issued by an ordered "issuer".  That is,
 * {@link Ticket}s are comparable and thus have a natural order.
 * <p>
 * Internally {@link Ticket}s are represented by two "longs".  The first
 * "long" is used to identify the "issuer".  The second "long" is used to identify
 * the "sequence number" of the {@link Ticket} issued by the "issuer".
 * <p>
 * As "issuers" are represented as longs, "issuers" are comparable.  Thus
 * {@link Ticket}s issued by issuer 0 will occur (be ordered) before
 * {@link Ticket}s issued by issuer 1 etc.
 * <p>
 * A useful way to represent a range of {@link Ticket}s issued by a single
 * "issuer" is to use a {@link TicketBook}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see TicketBook
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Ticket implements Identifier, ExternalizableLite, PortableObject, Comparable<Ticket>
{
    /**
     * A useful {@link Ticket} that may be used to represent "no" ticket.
     */
    public static final Ticket NONE = new Ticket(-1, -1);

    /**
     * The id of the "issuer" that issued the {@link Ticket}.
     */
    private long issuerId;

    /**
     * The sequence number of the {@link Ticket} issued by the issuer.
     */
    private long sequenceNumber;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public Ticket()
    {
    }


    /**
     * Used by the "issuer" to create {@link Ticket}s.
     *
     * @param issuerId The issuerId for the {@link Ticket}
     * @param sequenceNumber The sequenceNumber issued by the issuer
     */
    public Ticket(long issuerId,
                  long sequenceNumber)
    {
        this.issuerId       = issuerId;
        this.sequenceNumber = sequenceNumber;
    }


    /**
     * Returns the id of the "issuer" that issued the {@link Ticket}.
     *
     * @return The id of the "issuer" that issued the {@link Ticket}
     */
    public long getIssuerId()
    {
        return issuerId;
    }


    /**
     * Returns the unique sequence number issued by the "issuer".
     *
     * @return The unique sequence number issued by the "issuer"
     */
    public long getSequenceNumber()
    {
        return sequenceNumber;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        issuerId       = ExternalizableHelper.readLong(in);
        sequenceNumber = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, issuerId);
        ExternalizableHelper.writeLong(out, sequenceNumber);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.issuerId       = reader.readLong(0);
        this.sequenceNumber = reader.readLong(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, issuerId);
        writer.writeLong(1, sequenceNumber);
    }


    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + (int) (issuerId ^ (issuerId >>> 32));
        result = prime * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));

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

        final Ticket other = (Ticket) obj;

        if (issuerId != other.issuerId)
        {
            return false;
        }

        if (sequenceNumber != other.sequenceNumber)
        {
            return false;
        }

        return true;
    }


    /**
     * A {@link Ticket} (x) occurs before (is less than) another {@link Ticket} (y) if
     * the issuerId of (x) is less than (y) or if the issuerIds are the same for (x) and (y),
     * then the sequence number of (x) must be less than (y).
     *
     * {@inheritDoc}
     */
    public int compareTo(Ticket other)
    {
        if (this.getIssuerId() < other.getIssuerId())
        {
            return -1;
        }
        else if (this.getIssuerId() > other.getIssuerId())
        {
            return +1;
        }
        else if (this.getSequenceNumber() < other.getSequenceNumber())
        {
            return -1;
        }
        else if (this.getSequenceNumber() > other.getSequenceNumber())
        {
            return +1;
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        if (this.equals(Ticket.NONE))
        {
            return "Ticket.NONE";
        }
        else
        {
            return String.format("Ticket{%d.%d}", issuerId, sequenceNumber);
        }
    }
}
