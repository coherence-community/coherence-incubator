/*
 * File: DelegatingPartitionListener.java
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

package com.oracle.coherence.common.events.dispatching.listeners;

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.partition.PartitionArrivingEvent;
import com.oracle.coherence.common.events.partition.PartitionAssignedEvent;
import com.oracle.coherence.common.events.partition.PartitionDepartingEvent;
import com.oracle.coherence.common.events.partition.PartitionEvent;
import com.oracle.coherence.common.events.partition.PartitionLostEvent;
import com.oracle.coherence.common.events.partition.PartitionRecoveringEvent;
import com.oracle.coherence.environment.Environment;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.PartitionListener;

/**
 * An {@link DelegatingPartitionListener} dispatches {@link PartitionEvent}s (using an {@link EventDispatcher}).
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class DelegatingPartitionListener implements PartitionListener
{
    /**
     * Standard Constructor.
     */
    public DelegatingPartitionListener()
    {
    }


    /**
     * Determine the suitable {@link EventDispatcher} for the specified {@link PartitionedService}.
     *
     * @param partitionedService The {@link PartitionedService} to use
     *
     * @return the requested {@link EventDispatcher}
     */
    protected EventDispatcher getEventDispatcher(PartitionedService partitionedService)
    {
        // use an Environment to locate the EventDispatcher resource
        ConfigurableCacheFactory ccf =
            CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(partitionedService.getContextClassLoader());

        if (ccf instanceof Environment)
        {
            Environment     environment     = (Environment) ccf;
            EventDispatcher eventDispatcher = environment.getResource(EventDispatcher.class);

            if (eventDispatcher == null)
            {
                throw new RuntimeException("Failed to locate the EventDispatcher resource.  Your application appears to be "
                                           + "incorrectly configured or your Environment does not support EventDispatching");
            }
            else
            {
                return eventDispatcher;
            }
        }
        else
        {
            throw new RuntimeException("Can not locate the EventDispatcher resource as the ConfigurableCacheFactory does "
                + "not support Environments. At a minimum you should configure your application to use "
                + "the ExtensibleEnvironment.");
        }
    }


    /**
     * {@inheritDoc}
     */
    public void onPartitionEvent(com.tangosol.net.partition.PartitionEvent partitionEvent)
    {
        PartitionEvent event;

        switch (partitionEvent.getId())
        {
        case com.tangosol.net.partition.PartitionEvent.PARTITION_ASSIGNED :
            event = new PartitionAssignedEvent(partitionEvent);
            break;

        case com.tangosol.net.partition.PartitionEvent.PARTITION_LOST :
            event = new PartitionLostEvent(partitionEvent);
            break;

        case com.tangosol.net.partition.PartitionEvent.PARTITION_RECEIVE_BEGIN :
        case com.tangosol.net.partition.PartitionEvent.PARTITION_RECEIVE_COMMIT :
            if (partitionEvent.getFromMember() == null)
            {
                event = new PartitionRecoveringEvent(partitionEvent);
            }
            else
            {
                event = new PartitionArrivingEvent(partitionEvent);
            }

            break;

        case com.tangosol.net.partition.PartitionEvent.PARTITION_TRANSMIT_BEGIN :
        case com.tangosol.net.partition.PartitionEvent.PARTITION_TRANSMIT_COMMIT :
        case com.tangosol.net.partition.PartitionEvent.PARTITION_TRANSMIT_ROLLBACK :
            event = new PartitionDepartingEvent(partitionEvent);
            break;

        default :
            event = null;
        }

        if (event != null)
        {
            getEventDispatcher(partitionEvent.getService()).dispatchEvent(event);
        }
    }
}
