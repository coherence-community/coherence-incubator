/*
 * File: MessagePartitionListener.java
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

package com.oracle.coherence.patterns.messaging;

import com.tangosol.net.partition.PartitionEvent;
import com.tangosol.net.partition.PartitionListener;
import com.tangosol.net.partition.PartitionSet;

/**
 * The {@link MessagePartitionListener} listens for partition events on the message cache.  This class is
 * needed to handle message cache partition transfers.  This class is registered in the messaging pattern
 * cache configuration files.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
public class MessagePartitionListener implements PartitionListener
{
    /**
     * Default Constructor.
     */
    public MessagePartitionListener()
    {
    }


    /**
     * {@inheritDoc}
     */
    public void onPartitionEvent(PartitionEvent event)
    {
        // Loop through all of the partitionIds and inform each partition about
        // the event.
        //
        PartitionSet partitionSet   = event.getPartitionSet();
        int[]        partitionArray = partitionSet.toArray();

        for (int i = 0; i < partitionArray.length; i++)
        {
            onEvent(event, partitionArray[i]);
        }
    }


    /**
     * Handle an event for a single partition.  Call the {@link MessageEventManager} method
     * associated with each event.
     *
     * @param event partition event
     * @param partitionId partition identifier
     */
    private void onEvent(PartitionEvent event,
                         int            partitionId)
    {
        switch (event.getId())
        {
        case PartitionEvent.PARTITION_LOST :
            break;

        case PartitionEvent.PARTITION_RECEIVE_BEGIN :
            MessageEventManager.getInstance().onPartitionArrivalBegin(partitionId);
            break;

        case PartitionEvent.PARTITION_RECEIVE_COMMIT :
            MessageEventManager.getInstance().onPartitionArrivalDone(partitionId);
            break;

        case PartitionEvent.PARTITION_TRANSMIT_BEGIN :
            MessageEventManager.getInstance().onPartitionDepartureBegin(partitionId);
            break;

        case PartitionEvent.PARTITION_TRANSMIT_ROLLBACK :
            MessageEventManager.getInstance().onPartitionDepartureAbort(partitionId);
            break;

        case PartitionEvent.PARTITION_TRANSMIT_COMMIT :
            MessageEventManager.getInstance().onPartitionDepartureDone(partitionId);
            break;

        default :
            break;
        }

    }
}
