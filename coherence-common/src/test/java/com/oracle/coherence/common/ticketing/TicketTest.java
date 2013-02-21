/*
 * File: TicketTest.java
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

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link Ticket} implementation.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class TicketTest
{
    /**
     * Test {@link Ticket} creation.
     */
    @Test
    public void testTicketCreation()
    {
        Ticket ticket = new Ticket();

        assertTrue(ticket != null);
        assertTrue(ticket.getIssuerId() == 0);
        assertTrue(ticket.getSequenceNumber() == 0);

    }


    /**
     * Test {@link Ticket} creation.
     */
    @Test
    public void testTicketCreationLongLong()
    {
        Ticket ticket = new Ticket(1, 1000);

        assertTrue(ticket != null);
        assertTrue(ticket.getIssuerId() == 1);
        assertTrue(ticket.getSequenceNumber() == 1000);
    }


    /**
     * Test equals.
     */
    @Test
    public void testTicketEquals()
    {
        Ticket ticket = new Ticket(1, 1000);
        Ticket other  = new Ticket(1, 1000);

        assertTrue(ticket.equals(other));
        other = new Ticket(0, 1000);
        assertTrue(!ticket.equals(other));
        other = new Ticket(1, 999);
        assertTrue(!ticket.equals(other));
        assertTrue(ticket.equals(ticket));
        assertTrue(!ticket.equals(null));

    }


    /**
     * Test compareTo.
     */
    @Test
    public void testTicketCompareTo()
    {
        Ticket ticket = new Ticket(1, 1000);
        Ticket other  = new Ticket(1, 1000);

        assertTrue(ticket.compareTo(other) == 0);

        other = new Ticket(0, 1000);
        assertTrue(ticket.compareTo(other) == 1);

        other = new Ticket(2, 0);
        assertTrue(ticket.compareTo(other) == -1);

        other = new Ticket(1, 1001);
        assertTrue(ticket.compareTo(other) == -1);

        other = new Ticket(1, 999);
        assertTrue(ticket.compareTo(other) == 1);

    }
}
