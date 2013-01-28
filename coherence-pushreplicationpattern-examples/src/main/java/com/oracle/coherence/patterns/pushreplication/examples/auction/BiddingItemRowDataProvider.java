/*
 * File: BiddingItemRowDataProvider.java
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

import com.oracle.coherence.patterns.pushreplication.examples.NamedCacheModel.RowDataProvider;

/**
 * A {@link BiddingItemRowDataProvider} is an {@link RowDataProvider} for {@link BiddingItem}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class BiddingItemRowDataProvider implements RowDataProvider<String, BiddingItem>
{
    private final static String[]     columns = {"Item", "Price", "Highest Bidder", "From"};

    private CustomerReferenceRenderer bidderNameRenderer;
    private SiteNameRenderer          siteNameRenderer;


    /**
     * Constructs a {@link BiddingItemRowDataProvider}.
     *
     * @param bidderNameRenderer
     * @param siteNameRenderer
     */
    public BiddingItemRowDataProvider(CustomerReferenceRenderer bidderNameRenderer,
                                      SiteNameRenderer          siteNameRenderer)
    {
        this.bidderNameRenderer = bidderNameRenderer;
        this.siteNameRenderer   = siteNameRenderer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount()
    {
        return columns.length;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(int column)
    {
        return String.class;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(int column)
    {
        return columns[column];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(String      itemNumber,
                             BiddingItem biddingItem,
                             int         column)
    {
        if (biddingItem == null)
        {
            return null;
        }
        else
        {
            switch (column)
            {
            case 0 :
                return itemNumber;

            case 1 :
                return biddingItem.getPrice();

            case 2 :
                return bidderNameRenderer.getDisplayName(biddingItem.getCustomerReference());

            case 3 :
                return biddingItem.hasCustomerReference()
                       ? siteNameRenderer.getDisplayName(biddingItem.getCustomerReference().getSite()) : null;

            default :
                return null;
            }
        }
    }
}
