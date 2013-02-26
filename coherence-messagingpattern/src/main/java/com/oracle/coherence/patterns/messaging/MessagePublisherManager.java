/*
 * File: MessagePublisherManager.java
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
import com.oracle.coherence.patterns.messaging.entryprocessors.PublishMessageProcessor;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * A {@link MessagePublisherManager} manages a map of {@link MessagePublisher}s indexed by
 * a composite key of destination identifier and partition identifier.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
public class MessagePublisherManager
{
    /**
     * This is the singleton object for this class.
     */
    private static MessagePublisherManager INSTANCE = new MessagePublisherManager();

    /**
     * This is a map of MessagePublishers that are indexed by a destinationId/partitionId pair.
     * This map is not saved in the cache.  During recovery, we rebuild the map for each partition since
     * we know what the next message id is by looking the message id insert events on
     * the new owner member as existing messages are moved to the backup partition.
     */
    private HashMap<String, MessagePublisher> messagePublisherMap = null;


    /**
     * Constructor is private to enforce singleton.
     */
    private MessagePublisherManager()
    {
        messagePublisherMap = new HashMap<String, MessagePublisher>();
    }


    /**
     * Get the singleton instance of the {@link MessagePublisherManager}.
     *
     * @return MessagePublisherManager
     */
    public static MessagePublisherManager getInstance()
    {
        return INSTANCE;
    }


    /**
     * Get the message publisher for the specified destination/partition pair.  This method is
     * called when the message is being published.  See {@link PublishMessageProcessor}
     *
     * @param destinationId destination identifier
     * @param publisherId publisher identifier
     * @return message publisher
     */
    public MessagePublisher ensurePublisher(Identifier destinationId,
                                            Identifier publisherId)
    {
        // Get the context so that the partitionId can be retrieved
        NamedCache               cache   = CacheFactory.getCache(Message.CACHENAME);
        CacheService             service = cache.getCacheService();
        BackingMapManager        manager = service.getBackingMapManager();
        BackingMapManagerContext context = manager.getContext();

        // Get the partition that owns this publisher key.
        Object binaryPublisherId = context.getKeyToInternalConverter().convert(publisherId);
        int    partitionId       = context.getKeyPartition(binaryPublisherId);

        return getPublisher(destinationId, partitionId, true);
    }


    /**
     * Get the MessagePublisher responsible for publishing the {@link Message}.  The publisher
     * may not exist if there was a partition transfer so create it in that case.
     *
     * @param message message
     * @return MessagePublisher
     */
    MessagePublisher ensurePublisher(Message message)
    {
        return getPublisher(message.getDestinationIdentifier(),
                            (int) message.getMessageIdentifier().getPartitionId(),
                            true);
    }


    /**
     * Get the MessagePublisher responsible for publishing the {@link Message}.
     *
     * @param message message
     * @return MessagePublisher or null if the publisher doesn't exist
     */
    MessagePublisher getPublisher(Message message)
    {
        return getPublisher(message.getDestinationIdentifier(),
                            (int) message.getMessageIdentifier().getPartitionId(),
                            false);
    }


    /**
     * Get the MessagePublisher responsible for publishing {@link Message}s for the
     * given destination and partition.
     *
     * @param destinationIdentifier destination identifier
     * @param partitionId partition identifier
     *
     * @return MessagePublisher or null if the publisher doesn't exist
     */
    MessagePublisher getPublisher(Identifier destinationIdentifier,
                                  int        partitionId)
    {
        return getPublisher(destinationIdentifier, partitionId, false);
    }


    /**
     * Get the publishers responsible for the partition .
     *
     * @param partitionId partition identifier
     * @return publisher list
     */
    synchronized LinkedList<MessagePublisher> getPublishersForPartition(int partitionId)
    {
        LinkedList<MessagePublisher>              list = new LinkedList<MessagePublisher>();
        Iterator<Entry<String, MessagePublisher>> iter = messagePublisherMap.entrySet().iterator();

        while (iter.hasNext())
        {
            Entry<String, MessagePublisher> entry     = (Entry<String, MessagePublisher>) iter.next();
            MessagePublisher                publisher = entry.getValue();

            if (publisher.getPartitionId() == partitionId)
            {
                list.add(publisher);
            }
        }

        return list;
    }


    /**
     * Get the message publisher destinationId and partitionId as the composite key.
     * If the publisher doesnt't exist then create it if the bCreate flag is true.
     *
     * @param destinationIdentifier destination identifier
     * @param partitionId partition identifier
     * @param bCreate if true the create the publisher if it doesn't exist.
     * @return message publisher
     *
     */
    private synchronized MessagePublisher getPublisher(Identifier destinationIdentifier,
                                                       int        partitionId,
                                                       boolean    bCreate)
    {
        // Get the message publisher using the destinationId and partitionId as
        // the composite key.
        //
        MessagePublisher     publisher = null;
        MessagePublisher.Key key       = new MessagePublisher.Key(destinationIdentifier, partitionId);

        if (messagePublisherMap.containsKey(key.toString()))
        {
            publisher = messagePublisherMap.get(key.toString());
        }
        else if (bCreate)
        {
            publisher = new MessagePublisher(destinationIdentifier, partitionId);
            messagePublisherMap.put(key.toString(), publisher);
        }

        return publisher;
    }


    /**
     *  Remove any publisher with a matching partition id.
     *
     * @param partitionId partition identifier
     */
    synchronized void removePublishersForPartition(int partitionId)
    {
        Iterator<Entry<String, MessagePublisher>> iter = messagePublisherMap.entrySet().iterator();

        while (iter.hasNext())
        {
            Entry<String, MessagePublisher> entry     = (Entry<String, MessagePublisher>) iter.next();
            MessagePublisher                publisher = entry.getValue();

            if (publisher.getPartitionId() == partitionId)
            {
                iter.remove();
            }
        }
    }
}
