/*
 * File: PlaceBidProcessor.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.pushreplication.examples.auction;

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
 * An entry processor that places a bid.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author  Brian Oliver
 */
@SuppressWarnings("serial")
public class PlaceBidProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    private CustomerReference m_Bidder;
    private long              m_Price;


    /**
     * Constructs a {@link PlaceBidProcessor}.
     */
    public PlaceBidProcessor()
    {
    }


    /**
     * Constructs a {@link PlaceBidProcessor} for a specific bidder and price.
     *
     * @param bidder
     * @param price
     */
    public PlaceBidProcessor(CustomerReference bidder,
                             long              price)
    {
        m_Bidder = bidder;
        m_Price  = price;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(final Entry entry)
    {
        if (entry.isPresent() && entry.getValue() instanceof BiddingItem)
        {
            BiddingItem biddingItem = (BiddingItem) entry.getValue();

            biddingItem.placeBid(m_Bidder, m_Price);

            entry.setValue(biddingItem);
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput reader) throws IOException
    {
        m_Bidder = (CustomerReference) ExternalizableHelper.readObject(reader);
        m_Price  = reader.readLong();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        ExternalizableHelper.writeObject(writer, m_Bidder);
        writer.writeLong(m_Price);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_Bidder = (CustomerReference) reader.readObject(0);
        m_Price  = reader.readLong(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, m_Bidder);
        writer.writeLong(1, m_Price);
    }
}
