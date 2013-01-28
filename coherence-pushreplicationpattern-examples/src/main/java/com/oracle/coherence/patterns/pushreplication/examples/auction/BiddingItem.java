/*
 * File: BiddingItem.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * The {@link BiddingItem} represents a bidding item that is put into cache
 * that is replicated between clusters.
 * It is updated by bidders running concurrently within clusters.
 * <p>
 * An item consists of a bidding price, and a customer id.  For simplicity,
 * the customerId is set to the siteName passed into the {@link Application}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Robert Hanckel
 */
@SuppressWarnings("serial")
public class BiddingItem implements Serializable, ExternalizableLite, PortableObject
{
    /**
     * The cache in which {@link BiddingItem}s will be placed.
     */
    public static final String CACHE_NAME = "bidding-cache";

    private CustomerReference  m_CustomerReference;
    private long               m_Price = 0;


    /**
     * Constructs a {@link BiddingItem}.
     */
    public BiddingItem()
    {
    }


    /**
     * Constructs a {@link BiddingItem} with an initial price.
     *
     * @param initialPrice
     */
    public BiddingItem(long initialPrice)
    {
        m_CustomerReference = null;
        m_Price             = initialPrice;
    }


    /**
     * Obtains the current bid (price) of the auction item.
     *
     * @return the current bid
     */
    public long getPrice()
    {
        return m_Price;
    }


    /**
     * Obtains the {@link CustomerReference} that made the
     * bid.
     *
     * @return  the {@link CustomerReference} owning the bid
     */
    public CustomerReference getCustomerReference()
    {
        return m_CustomerReference;
    }


    /**
     * Determines if the current price is from a customer.
     *
     * @return if the current bid is from a customer
     */
    public boolean hasCustomerReference()
    {
        return m_CustomerReference != null;
    }


    /**
     * Attempts to update the item with a new bid by a customer.  Should
     * the bid be higher than the existing bid, the new bid is accepted
     * and the customer is remembered.
     *
     * @param bidder
     * @param bidPrice
     */
    public void placeBid(CustomerReference bidder,
                         long              bidPrice)
    {
        if (bidPrice > m_Price)
        {
            m_CustomerReference = bidder;
            m_Price             = bidPrice;
        }
        else if (bidPrice == m_Price)
        {
            if (this.getCustomerReference().compareTo(bidder) < 0)
            {
                m_CustomerReference = bidder;
            }
        }
    }


    /**
     * Given another {@link BiddingItem}, updates this {@link BiddingItem}
     * if the other {@link BiddingItem} is better (a higher bid).
     *
     * @param other
     *
     * @return <code>true</code> if the other item is a higher/better bid or
     *         <code>false</code> if the current bid is better.
     */
    public boolean merge(BiddingItem other)
    {
        if (this.getPrice() < other.getPrice())
        {
            m_Price             = other.getPrice();
            m_CustomerReference = other.getCustomerReference();

            return true;

        }
        else if (this.getPrice() == other.getPrice())
        {
            if (this.getCustomerReference() == null && other.getCustomerReference() == null)
            {
                return false;
            }
            else if (this.getCustomerReference() == null)
            {
                m_CustomerReference = other.getCustomerReference();

                return true;
            }
            else if (other.getCustomerReference() == null)
            {
                return false;
            }
            else if (this.getCustomerReference().compareTo(other.getCustomerReference()) < 0)
            {
                m_CustomerReference = other.getCustomerReference();

                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput reader) throws IOException
    {
        m_CustomerReference = (CustomerReference) ExternalizableHelper.readObject(reader);
        m_Price             = reader.readLong();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        ExternalizableHelper.writeObject(writer, m_CustomerReference);
        writer.writeLong(m_Price);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_CustomerReference = (CustomerReference) reader.readObject(0);
        m_Price             = reader.readLong(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, m_CustomerReference);
        writer.writeLong(1, m_Price);
    }
}
