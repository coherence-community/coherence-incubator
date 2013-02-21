/*
 * File: TicketAggregator.java
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

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.AbstractAggregator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The {@link TicketAggregator} is a parallel {@link AbstractAggregator} that
 * produces an ordered {@link LinkedList} of {@link TicketBook}s (based on natural ordering
 * of {@link TicketBook}s).  Each {@link TicketBook} optimally represents an ordered
 * range of {@link Ticket}s that have been aggregated for each "issuer".
 * <p>
 * This aggregator is useful for constructing ranges of {@link Ticket}s from across
 * a cluster.
 * <p>
 * This aggregator has two passes.  The first pass (non-final) collects all of the {@link Ticket}s
 * for each "issuer" to produce a set of {@link TicketBook}s.  The second pass (final)
 * combines the {@link TicketBook}s and produces a {@link LinkedList} of {@link TicketBook}s,
 * representing all of the {@link Ticket}s that have been aggregated.
 * <p>
 * When using this aggregator constructor, you must specify either the name of the method
 * that returns a {@link Ticket} OR a {@link ValueExtractor} that produces a {@link Ticket}
 * to be aggregated.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Ticket
 * @see TicketBook
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class TicketAggregator extends AbstractAggregator
{
    /**
     * A map of ordered {@link Ticket}s by issuer id.
     * This is used for the "first pass" of the aggregation
     * to organize {@link Ticket}s into {@link TicketBook}s.
     */
    private transient TreeMap<Long, TreeSet<Ticket>> ticketsByIssuer;

    /**
     * An ordered set of {@link TicketBook}s.  This is
     * used for the "second pass" of the aggregation
     * to organize and collapse the collected {@link TicketBook}s.
     */
    private transient TreeSet<TicketBook> ticketBooks;


    /**
     * Required for ExternalizableLite and/or PortableObject.
     */
    public TicketAggregator()
    {
        super();
    }


    /**
     * Standard Constructor (when providing the name of the method that returns
     * a {@link Ticket} for aggregation).
     *
     * @param methodName The name of the method to use to extract a {@link Ticket} for aggregation
     */
    public TicketAggregator(String methodName)
    {
        super(methodName);
    }


    /**
     * Standard Constructor (when providing a {@link ValueExtractor} that returns
     * a {@link Ticket} for aggregation).
     *
     * @param extractor The {@link ValueExtractor} to extract a {@link Ticket} for aggregation
     */
    public TicketAggregator(ValueExtractor extractor)
    {
        super(extractor);
    }


    /**
     * {@inheritDoc}
     */
    protected void init(boolean isFinal)
    {
        if (isFinal)
        {
            this.ticketBooks = new TreeSet<TicketBook>();
        }
        else
        {
            this.ticketsByIssuer = new TreeMap<Long, TreeSet<Ticket>>();
        }
    }


    /**
     * {@inheritDoc}
     */
    protected Object finalizeResult(boolean isFinal)
    {
        if (isFinal)
        {
            LinkedList<TicketBook> result     = new LinkedList<TicketBook>();
            TicketBook             ticketBook = null;

            for (TicketBook aTicketBook : ticketBooks)
            {
                if (ticketBook == null ||!ticketBook.combine(aTicketBook))
                {
                    ticketBook = aTicketBook;
                    result.add(ticketBook);
                }
            }

            return result;
        }
        else
        {
            TreeSet<TicketBook> result = new TreeSet<TicketBook>();

            for (long issuerId : ticketsByIssuer.keySet())
            {
                TicketBook ticketBook = null;

                for (Ticket ticket : ticketsByIssuer.get(issuerId))
                {
                    if (ticketBook == null ||!ticketBook.add(ticket))
                    {
                        ticketBook = new TicketBook(ticket);
                        result.add(ticketBook);
                    }
                }
            }

            return result;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    protected void process(Object  object,
                           boolean isFinal)
    {
        if (isFinal)
        {
            ticketBooks.addAll((Collection<TicketBook>) object);
        }
        else
        {
            if (object instanceof Ticket)
            {
                Ticket          ticket = (Ticket) object;
                TreeSet<Ticket> tickets;

                if (ticketsByIssuer.containsKey(ticket.getIssuerId()))
                {
                    tickets = ticketsByIssuer.get(ticket.getIssuerId());
                }
                else
                {
                    tickets = new TreeSet<Ticket>();
                    ticketsByIssuer.put(ticket.getIssuerId(), tickets);
                }

                tickets.add(ticket);
            }
            else
            {
                // we can't aggregate the object as it's not a Ticket
            }
        }
    }
}
