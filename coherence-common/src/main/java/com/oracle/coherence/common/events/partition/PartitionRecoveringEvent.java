/*
 * File: PartitionRecoveringEvent.java
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

import com.oracle.coherence.common.events.PhasedEvent;
import com.tangosol.net.partition.PartitionEvent;

/**
 * A {@link PartitionRecoveringEvent} is a phased {@link PhasedEvent} that captures the information concerning the
 * recovery (and thus ownership responsibility) of a set of partitions.
 * <p>
 * Such events typically occur as a response to a storage-enabled member being terminated.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class PartitionRecoveringEvent extends AbstractPartitionEvent implements PhasedEvent
{
    /**
     * The {@link Phase} of the {@link PartitionRecoveringEvent}.
     */
    private Phase phase;


    /**
     * Standard Constructor for Tangosol-based {@link PartitionEvent}s.
     *
     * @param partitionEvent the Tangosol based {@link PartitionEvent}
     */
    public PartitionRecoveringEvent(PartitionEvent partitionEvent)
    {
        super(partitionEvent);
        this.phase = partitionEvent.getId() == PartitionEvent.PARTITION_RECEIVE_BEGIN
                     ? Phase.Commenced : Phase.Completed;
    }


    /**
     * {@inheritDoc}
     */
    public Phase getPhase()
    {
        return phase;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("PartitionRecoveryingEvent{partitionSet=%s, phase=%s}", getPartitionSet(), getPhase());
    }
}
