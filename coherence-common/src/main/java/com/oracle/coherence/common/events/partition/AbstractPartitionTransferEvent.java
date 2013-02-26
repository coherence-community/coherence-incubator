/*
 * File: AbstractPartitionTransferEvent.java
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

package com.oracle.coherence.common.events.partition;

import com.tangosol.net.Member;
import com.tangosol.net.partition.PartitionEvent;

/**
 * A base implementation for {@link PartitionTransferEvent}s.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
abstract class AbstractPartitionTransferEvent extends AbstractPartitionEvent implements PartitionTransferEvent
{
    /**
     * The {@link Member} from which the partition transfer occurred.
     */
    private Member memberFrom;

    /**
     * The {@link Member} to which the partition transfer occurred.
     */
    private Member memberTo;


    /**
     * Standard Constructor for Tangosol-based {@link PartitionEvent}s.
     *
     * @param partitionEvent The Tangosol based {@link PartitionEvent}
     */
    protected AbstractPartitionTransferEvent(PartitionEvent partitionEvent)
    {
        super(partitionEvent);
        this.memberFrom = partitionEvent.getFromMember();
        this.memberTo   = partitionEvent.getToMember();
    }


    /**
     * {@inheritDoc}
     */
    public Member getMemberFrom()
    {
        return memberFrom;
    }


    /**
     * {@inheritDoc}
     */
    public Member getMemberTo()
    {
        return memberTo;
    }
}
