/*
 * File: DefaultTicketGenerator.java
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

import java.util.concurrent.atomic.AtomicLong;

/**
 * A default implementation of a {@link TicketGenerator} (uniqueness
 * of the generated {@link Ticket}s dependent on the uniqueness of issuerId
 * provided at construction).
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Ticket
 *
 * @author Brian Oliver
 */
public class DefaultTicketGenerator implements TicketGenerator
{
    /**
     * The issuerId for the generated {@link Ticket}s.
     */
    private long issuerId;

    /**
     * An {@link AtomicLong} for the sequence numbers used
     * to generate {@link Ticket}s.
     */
    private AtomicLong nextSequenceNr;


    /**
     * Standard Constructor.
     *
     * @param issuerId The issuerId of the {@link TicketGenerator}
     */
    public DefaultTicketGenerator(long issuerId)
    {
        this.issuerId       = issuerId;
        this.nextSequenceNr = new AtomicLong(1);
    }


    /**
     * {@inheritDoc}
     */
    public Ticket next()
    {
        return new Ticket(issuerId, nextSequenceNr.getAndIncrement());
    }
}
