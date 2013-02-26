/*
 * File: PartitionAssignedEvent.java
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

import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.PartitionEvent;
import com.tangosol.net.partition.PartitionSet;

/**
 * A {@link PartitionAssignedEvent} captures the information concerning one or more partitions that have been
 * assigned to a storage enabled member (due to initialization at the member or the member taking ownership due to
 * loss).
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class PartitionAssignedEvent extends AbstractPartitionEvent
{
    /**
     * Standard Constructor for Tangosol-based {@link PartitionEvent}s.
     *
     * @param partitionEvent the Tangosol based {@link PartitionEvent}
     */
    public PartitionAssignedEvent(PartitionEvent partitionEvent)
    {
        super(partitionEvent);
    }


    /**
     * Standard Constructor when using explicit {@link PartitionedService} and {@link PartitionSet}s.
     *
     * @param partitionedService The {@link PartitionedService}
     * @param partitionSet       The {@link PartitionSet}
     */
    public PartitionAssignedEvent(PartitionedService partitionedService,
                                  PartitionSet       partitionSet)
    {
        super(partitionedService, partitionSet);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("PartitionAssignedEvent{partitionSet=%s}", getPartitionSet());
    }
}
