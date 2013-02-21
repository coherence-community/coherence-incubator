/*
 * File: MessagesToExpose.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.ranges.Range;

import java.util.HashMap;

/**
 * A {@link MessagesToExpose} class manages a batch of {@link Message}s for a destination that will be exposed to
 * either a {@link Topic} subscriptions or a {@link Queue}. There is a instance of this class per destination
 * so that messages across all partitions for a particular destination can be contained in a single batch.
 * <p>
 * All access to the delivery tracker is synchronized.  There are 3 threads that access this class:
 * <ol>
 *  <li>Backing map listener thread that runs when a {@link Message} is written to the cache.  The {@link MessageEventManager}
 *      will add a message to the tracker.</li>
 *
 *  <li>Partition transfer complete thread.  This thread calls createRangeForPartition and only runs when a
 *      message partition transfer to this member is done.</li>
 *
 *  <li> Event processor thread.  This thread is background thread that exposes the messages to the topic or queue.
 *      It calls getTrackerSnapShot.</li>
 * </ol>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
class MessagesToExpose
{
    /**
     * The singleton instance of the {@link MessagesToExpose}.
     */
    private static MessagesToExpose INSTANCE = new MessagesToExpose();

    /**
     * A map of {@link MessageTracker}s indexed by the destination identifier.  Each {@link MessageTracker}s
     * tracks a batch of {@link Message}s for a destination.
     */
    private HashMap<Identifier, MessageTracker> trackerMap;


    /**
     * The private constructor for the singleton instance.
     */
    private MessagesToExpose()
    {
        // Create the delivery tracker map
        trackerMap = new HashMap<Identifier, MessageTracker>();
    }


    /**
     * Get the singleton instance of the MessageEventManager.
     *
     * @return MessageEventManager
     */
    public static MessagesToExpose getInstance()
    {
        return INSTANCE;
    }


    /**
     * Get a snapshot of the {@link MessageTracker}.
     *
     * @param destinationIdentifier destination identifier
     * @return MessageTracker the messages to expose.
     */
    public synchronized MessageTracker getTrackerSnapShot(Identifier destinationIdentifier)
    {
        MessageTracker tracker = null;

        if (trackerMap.containsKey(destinationIdentifier))
        {
            tracker = trackerMap.get(destinationIdentifier);
            trackerMap.put(destinationIdentifier, new DefaultMessageTracker());
        }

        return tracker;
    }


    /**
     * Add a {@link MessageIdentifier} to the {@link MessageTracker}.
     *
     * @param message message
     */
    public synchronized void add(Message message)
    {
        ensureTracker(message.getDestinationIdentifier()).add(message.getMessageIdentifier());
    }


    /**
     * A partition arrival is done so add the newly created {@link Range} of arrived messages to the
     * the  {@link MessageTracker}.
     *
     * @param destinationIdentifier destination identifier
     * @param partitionId partition identifier
     * @param range message range
     */
    public synchronized void createRangeForPartition(Identifier destinationIdentifier,
                                                     int        partitionId,
                                                     Range      range)
    {
        DefaultMessageTracker tracker = (DefaultMessageTracker) ensureTracker(destinationIdentifier);

        tracker.replaceRange(partitionId, range);
    }


    /**
     * Get the {@link MessageTracker} for the destination.  Create one if it doesn't exist.
     *
     * @param destinationIdentifier destination identifier
     * @return MessageTracker
     */
    private synchronized MessageTracker ensureTracker(Identifier destinationIdentifier)
    {
        if (trackerMap.containsKey(destinationIdentifier))
        {
            return trackerMap.get(destinationIdentifier);
        }
        else
        {
            MessageTracker tracker = new DefaultMessageTracker();

            trackerMap.put(destinationIdentifier, tracker);

            return tracker;
        }
    }
}
