/*
 * File: PartitionKey.java
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

package com.oracle.coherence.common.partition;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link PartitionKey} is a key that is assigned to a particular partition.
 * Works together with the {@link PartitionAwareKeyPartitioningStrategy}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class PartitionKey implements PartitionAwareKey, ExternalizableLite, PortableObject
{
    /**
     * The partition this key is assigned to.
     */
    private int partition;

    /**
     * The embedded object that is the "real" key.
     */
    private Object embeddedKey;


    /**
     * Default constructor required by PortableObject.
     */
    public PartitionKey()
    {
    }


    /**
     * Standard constructor.
     *
     * @param partition   the partition this key belongs to
     * @param embeddedKey the real key
     */
    public PartitionKey(int    partition,
                        Object embeddedKey)
    {
        this.partition   = partition;
        this.embeddedKey = embeddedKey;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "PartitionKey [" + (embeddedKey != null ? "embeddedKey=" + embeddedKey + ", " : "") + "partition="
               + partition + "]";
    }


    /**
     * {@inheritDoc}
     */
    public int getPartition()
    {
        return partition;
    }


    /**
     * Returns the real embedded key.
     *
     * @return the real key.
     */
    public Object getKey()
    {
        return embeddedKey;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + embeddedKey.hashCode();
        result = prime * result + partition;

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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

        PartitionKey other = (PartitionKey) obj;

        if (embeddedKey == null)
        {
            if (other.embeddedKey != null)
            {
                return false;
            }
        }
        else
        {
            if (!embeddedKey.equals(other.embeddedKey))
            {
                return false;
            }
        }

        if (partition != other.partition)
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
        partition   = in.readInt();
        embeddedKey = ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        out.writeInt(partition);
        ExternalizableHelper.writeObject(out, embeddedKey);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in) throws IOException
    {
        partition   = in.readInt(0);
        embeddedKey = in.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out) throws IOException
    {
        out.writeInt(0, partition);
        out.writeObject(1, embeddedKey);
    }
}
