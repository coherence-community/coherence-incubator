/*
 * File: MessageEventManager.java
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

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryArrivedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryInsertedEvent;
import com.oracle.coherence.common.events.dispatching.listeners.DelegatingBackingMapListener;
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.ranges.Range;
import com.oracle.coherence.common.ranges.Ranges;
import com.oracle.coherence.common.ticketing.TicketBook;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.Filter;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.filter.EqualsFilter;

import java.util.Iterator;

/**
 * A {@link MessageEventManager} is a singleton class responsible for all asynchronous processing of a {@link Message}
 * as a result of a message cache event.  The {@link MessageEventManager} object is created when the JVM starts and exists
 * for the life of the process.  The {@link MessageEventManager} is called by the the {@link DelegatingBackingMapListener}
 * to create an {@link EventProcessor}, which will expose the message to a {@link Topic} or {@link Queue}.
 * The {@link DelegatingBackingMapListener} then schedules the {@link EventProcessor} to run on a background thread.
 * <p>
 * When a message partition is moved to this member, this class receives an EntryArriving event in random order for each
 * message in the partition.  The {@link MessagePublisher} is used to keep track of the arriving message sequence numbers.
 * When the partition transfer is complete, two things are done: 1) the {@link MessagePublisher} recreates its {@link TicketBook}
 * and 2) messages that just arrived, but have never been exposed, are handled by the {@link MessagesToExpose}.
 * During partition transfer, Coherence prevents blocks any new message activity for the partition until the transfer is
 * complete.  Because of this, all arriving messages will be put into the  batch before any new
 * ones thus ensuring correct message ordering.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
class MessageEventManager
{
    /**
     * The {@link ProcessorStateManager} manages the state of the queue processor so that
     * the getEntryProcessor method will know whether a processor should be queued up.
     */
    private static ProcessorStateManager stateManager = new ProcessorStateManager();

    /**
     * The singleton instance of the {@link MessageEventManager}.
     */
    private static MessageEventManager INSTANCE = new MessageEventManager();

    /**
     * A continuous query cache used to optimize access to the {@link Topic} subscription lists.
     */
    private ContinuousQueryCache cqcTopicCache;


    /**
     * The private constructor for the singleton instance.
     */
    private MessageEventManager()
    {
        // Create a continuous query cache for the topic so that the subscription list can be retrieved quickly
        NamedCache cache  = CacheFactory.getCache(Destination.CACHENAME);
        Filter     filter = new EqualsFilter(new ChainedExtractor("getClass.getName"), Topic.class.getName());

        cqcTopicCache = new ContinuousQueryCache(cache, filter);
    }


    /**
     * Get the {@link ProcessorStateManager}.
     *
     * @return {@link ProcessorStateManager}
     */
    ProcessorStateManager getStateManager()
    {
        return stateManager;
    }


    /**
     * Get the continuous query cache for the  {@link Topic}.
     *
     * @return continuous query cache topic cache
     */
    ContinuousQueryCache getDestinationCache()
    {
        return cqcTopicCache;
    }


    /**
     * Get the singleton instance of the MessageEventManager.
     *
     * @return MessageEventManager
     */
    static MessageEventManager getInstance()
    {
        return INSTANCE;
    }


    /**
     * The partition starting to depart from this member. There is nothing to do.
     *
     * @param partitionId partition identifier
     */
    synchronized void onPartitionDepartureBegin(int partitionId)
    {
    }


    /**
     * The partition aborted departure from this member. There is nothing to do.
     *
     * @param partitionId partition identifier
     */
    synchronized void onPartitionDepartureAbort(int partitionId)
    {
    }


    /**
     * The partition has finished departing from the member.  Clean up the messages to expose tracker and
     * remove any publisher in this member with a matching partition id.
     *
     * @param partitionId partition identifier
     */
    synchronized void onPartitionDepartureDone(int partitionId)
    {
        Iterator<MessagePublisher> iter =
            MessagePublisherManager.getInstance().getPublishersForPartition(partitionId).iterator();

        while (iter.hasNext())
        {
            MessagePublisher publisher = iter.next();

            // Remove the partition that just left this member from the tracker.
            MessagesToExpose.getInstance().createRangeForPartition(publisher.getDestinationIdentifier(),
                                                                   partitionId,
                                                                   Ranges.EMPTY);
        }

        // Remove all the message publishers for this partition.
        MessagePublisherManager.getInstance().removePublishersForPartition(partitionId);
    }


    /**
     * The partition has started arriving to the member.  This event is
     * ignored.  Instead the message arriving event is used to handle
     * and arriving partition on a per message basis.  See {@link MessageEventManager}
     *
     * @param partitionId partition identifier
     */
    synchronized void onPartitionArrivalBegin(int partitionId)
    {
    }


    /**
     * The partition is done arriving at the member.
     * Recreate the publisher ticket book and update the messages to be exposed.
     *
     * @param partitionId partition identifier
     *
     */
    synchronized void onPartitionArrivalDone(int partitionId)
    {
        Iterator<MessagePublisher> iter =
            MessagePublisherManager.getInstance().getPublishersForPartition(partitionId).iterator();

        while (iter.hasNext())
        {
            MessagePublisher publisher = iter.next();

            // Recreate the ticket book for the publisher so newly inserted message have the correct sequence number
            publisher.recreateTicketBook();

            // Add any newly arrived messages that need to be exposed.
            Range range = publisher.getPendingMessagesToExpose();

            MessagesToExpose.getInstance().createRangeForPartition(publisher.getDestinationIdentifier(),
                                                                   partitionId,
                                                                   range);

            // Finally reset the publisher
            publisher.reset();
        }
    }


    /**
     * The event process will be scheduled to run on a background thread.
     * This method is called on the service thread by the message cache
     * {@link DelegatingBackingMapListener}.
     *
     * @param e event
     * @param message message
     * @return event processor or null
     */
    public EventProcessor<EntryEvent> getEventProcessor(Event   e,
                                                        Message message)
    {
        MessagePublisher publisher = MessagePublisherManager.getInstance().ensurePublisher(message);

        // If this message is arriving due to partition transfer then tell the publisher
        // so that it can recreate its ticket book once the transfer is done.
        if (e instanceof BackingMapEntryArrivedEvent)
        {
            publisher.saveArrivingMessage(message);
        }

        // Nothing else to do if message is already visible (exposed to topic or queue)
        if (message.isVisible())
        {
            return null;
        }

        if (e instanceof BackingMapEntryArrivedEvent)
        {
            // The message is being transferred to this member and it hasn't been
            // exposed. Because messages are arriving in random order it cannot
            // be added to MessagesToExpose object until the transfer.  When the
            // transfer is done, the newly arrived messages will be added.
            // See onPartitionArrivalDone()
            publisher.saveMessageToBeExposed(message.getMessageIdentifier());
        }
        else if (e instanceof BackingMapEntryInsertedEvent)
        {
            // The message has just be inserted in the cache and needs to be exposed.
            // Add to the set of messages to be exposed.
            MessagesToExpose.getInstance().add(message);
        }
        else
        {
            return null;
        }

        // An event processor needs to run to expose the batch of messages.  If one is already
        // scheduled for this destination then it will process this message, otherwise schedule one.
        EventProcessor<EntryEvent> proc = null;

        if (!stateManager.isScheduled(message.getDestinationIdentifier()))
        {
            proc = createEventProcessor(publisher, e);
            stateManager.setScheduled(message.getDestinationIdentifier());
        }

        return proc;

    }


    /**
     * Create a processor to expose the messages to a queue to topic.
     *
     * @param publisher messages publisher
     * @param e event
     *
     * @return EventProcessor
     */
    private EventProcessor<EntryEvent> createEventProcessor(MessagePublisher publisher,
                                                            Event            e)
    {
        EventProcessor<EntryEvent> proc = null;

        if (publisher.isQueue())
        {
            proc = new MessageEventProcessors.ExposeToQueueProcessor(publisher.getDestinationIdentifier());
        }
        else if (publisher.isTopic())
        {
            proc = new MessageEventProcessors.ExposeToTopicProcessor(publisher.getDestinationIdentifier());
        }

        return proc;
    }
}
