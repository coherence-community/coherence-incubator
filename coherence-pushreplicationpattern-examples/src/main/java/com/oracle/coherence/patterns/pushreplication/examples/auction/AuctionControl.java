/*
 * File: AuctionControl.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * The {@link AuctionControl} is used to control distributed bidding
 * across clusters.  The control has a simple boolean which indicates whether
 * or not bidding is permitted (the auction is active).
 * <p>
 * The {@link AuctionControl} also provides counter to indicate the number of
 * items in the auction.
 * <p>
 * During an auction, the {@link Application} exclusively updates
 * {@link AuctionControl} (which is then replicated to other sites) to
 * detail the status of the auction.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Robert Hanckel
 */
public class AuctionControl implements Serializable, ExternalizableLite, PortableObject
{
    /**
     * The name of the control cache.
     */
    public static final String CACHE_NAME            = "control-cache";
    private static final long  serialVersionUID      = -2693195437627128596L;

    private long               m_TotalItems          = 0;
    private long               m_AuctionDurationMS   = 0;
    private long               m_DurationRemainingMS = 0;
    private boolean            m_IsActive            = false;


    /**
     * Constructs an {@link AuctionControl}.
     */
    public AuctionControl()
    {
    }


    /**
     * Constructs {@link AuctionControl} with a specified number of items
     * (for the auction).
     *
     * @param totalItems
     */
    public AuctionControl(long totalItems)
    {
        m_TotalItems          = totalItems;
        m_AuctionDurationMS   = 0;
        m_DurationRemainingMS = 0;
        m_IsActive            = false;
    }


    /**
     * Sets the duration of the auction.
     *
     * @param auctionDurationMS
     */
    public void startBidding(long auctionDurationMS)
    {
        m_IsActive            = true;
        m_AuctionDurationMS   = auctionDurationMS;
        m_DurationRemainingMS = auctionDurationMS;
    }


    /**
     * Resumes the auction (if it was stopped).
     */
    public void resumeBidding()
    {
        m_IsActive = m_DurationRemainingMS > 0;
    }


    /**
     * Suspends the auction.
     */
    public void suspendBidding()
    {
        m_IsActive = false;
    }


    /**
     * Stops the auction.
     */
    public void stopBidding()
    {
        m_IsActive            = false;
        m_DurationRemainingMS = 0;
    }


    /**
     * Determine if the bidding is permitted in the auction.
     *
     * @return if bidding is permitted.
     */
    public boolean isActive()
    {
        return m_IsActive && m_DurationRemainingMS > 0;
    }


    /**
     * Obtain the number of items in the auction.
     *
     * @return the number of items
     */
    public long getTotalItems()
    {
        return m_TotalItems;
    }


    /**
     * Obtains duration of the auction (in MS)
     *
     * @return the duration of the auction
     */
    public long getAuctionDuration()
    {
        return m_AuctionDurationMS;
    }


    /**
     * Obtains the remaining duration (in MS) of the auction.
     *
     * @return the remaining MS for the auction
     */
    public long getRemainingDuration()
    {
        return m_DurationRemainingMS;
    }


    /**
     * Reduces the amount of time (in MS) remaining in the auction.
     *
     * @param durationMS
     */
    public void reduceDurationRemaining(long durationMS)
    {
        m_DurationRemainingMS = Math.max(0, m_DurationRemainingMS - durationMS);
        m_IsActive            = m_IsActive && m_DurationRemainingMS > 0;
    }


    /**
     * Determines the percentage of time remaining in the auction.
     *
     * @return the percentage of time remaining in the auction
     */
    public int getPercentageRemaining()
    {
        return m_DurationRemainingMS > 0
               ? 100 - (int) ((double) m_DurationRemainingMS / (double) m_AuctionDurationMS * 100.0) : 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput reader) throws IOException
    {
        this.m_TotalItems          = reader.readLong();
        this.m_AuctionDurationMS   = reader.readLong();
        this.m_DurationRemainingMS = reader.readLong();
        this.m_IsActive            = reader.readBoolean();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        writer.writeLong(m_TotalItems);
        writer.writeLong(m_AuctionDurationMS);
        writer.writeLong(m_DurationRemainingMS);
        writer.writeBoolean(m_IsActive);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.m_TotalItems          = reader.readLong(0);
        this.m_AuctionDurationMS   = reader.readLong(1);
        this.m_DurationRemainingMS = reader.readLong(2);
        this.m_IsActive            = reader.readBoolean(3);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, m_TotalItems);
        writer.writeLong(1, m_AuctionDurationMS);
        writer.writeLong(2, m_DurationRemainingMS);
        writer.writeBoolean(3, m_IsActive);
    }
}
