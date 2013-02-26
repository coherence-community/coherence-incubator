/*
 * File: StdErrEventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;

import java.util.Iterator;

/**
 * A simple {@link EventChannel} implementation that outputs all {@link Event}s to stderr.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class StdErrEventChannel implements EventChannel
{
    /**
     * Standard Constructor.
     */
    public StdErrEventChannel()
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    public void connect(EventDistributor.Identifier       distributorIdentifier,
                        EventChannelController.Identifier controllerIdentifier) throws EventChannelNotReadyException
    {
        System.err.printf("Connected: StdErrEventChannel\n");
    }


    /**
     * {@inheritDoc}
     */
    public void disconnect()
    {
        System.err.printf("Disconnected: StdErrEventChannel\n");
    }


    /**
     * {@inheritDoc}
     */
    public int send(Iterator<Event> events)
    {
        int distributionCount = 0;

        while (events.hasNext())
        {
            Event event = events.next();

            distributionCount++;

            System.err.printf("StdErrEventChannel: event=%s\n", event);
        }

        return distributionCount;
    }
}
