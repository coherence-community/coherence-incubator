/*
 * File: MessageCacheInterceptor.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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
import com.oracle.coherence.common.ranges.Ranges;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ResourceRegistry;

import java.util.Iterator;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MessageCacheInterceptor handles several responsibilities related to message caching.
 * <p>
 * This interceptor captures the INSERTED and ARRIVED events raised by Live Events:
 *
 * <p>
 * This interceptor is instantiated and owned by the MessageCache.
 *
 * @author der  2013.6.29
 */
@SuppressWarnings("rawtypes")
public class MessageCacheInterceptor implements EventInterceptor
{
    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(MessageCacheInterceptor.class.getName());


    /**
     * Create a {@link MessageCacheInterceptor}.
     */
    public MessageCacheInterceptor()
    {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(Event event)
    {
        if (event instanceof EntryEvent)
        {
            if (event.getType() == EntryEvent.Type.INSERTED)
            {
                onCacheEntryInserted((EntryEvent) event);
            }
            else if (event.getType() == EntryEvent.Type.REMOVED)
            {
                onCacheEntryRemoved((EntryEvent) event);
            }
        }
        else if (event instanceof TransferEvent)
        {
            int partitionId = ((TransferEvent) event).getPartitionId();

            if (event.getType() == TransferEvent.Type.ARRIVED)
            {
                // Cache entry event processing
                onCacheEntryArrived((TransferEvent) event);

                // Partition event processing
                onPartitionArrived(partitionId);
            }
            else if (event.getType() == TransferEvent.Type.DEPARTING)
            {
                // Cache entry event processing
                onCacheEntryDeparting((TransferEvent) event);

                // Partition event processing
                onPartitionDeparting(partitionId);
            }
        }
    }


    /**
     * Event processing for Cache Entry when message is inserted into cache.
     *
     * @param event Entry event triggered by cache insert.
     */
    private void onCacheEntryInserted(EntryEvent event)
    {
        for (BinaryEntry binaryEntry : event.getEntrySet())
        {
            Message message = (Message) binaryEntry.getValue();

            // Nothing else to do if message is already visible (exposed to topic or queue)
            if (!message.isVisible())
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "MessageCacheInterceptor:onCacheEntryInserted identifier: {0}",
                               message.getDestinationIdentifier());
                }

                MessagePublisher publisher = MessagePublisherManager.getInstance().ensurePublisher(message);

                // The message has just be inserted in the cache and needs to be exposed.
                // Add to the set of messages to be exposed.
                MessagesToExpose.getInstance().add(message);

                Identifier id = message.getDestinationIdentifier();

                MessageEngine mEngine =
                    CacheFactory.getConfigurableCacheFactory().getResourceRegistry().getResource(MessageEngine.class,
                                                                                                 id.toString());

                if (mEngine == null)
                {
                    mEngine = new MessageEngine(id);
                    CacheFactory.getConfigurableCacheFactory().getResourceRegistry()
                        .registerResource(MessageEngine.class,
                                          id.toString(),
                                          mEngine);
                }

                mEngine.processRunEvent(publisher);
            }
        }
    }


    /**
     * Event processing for Cache Entry when message arrive via partition transfer.
     *
     * @param event Transfer event triggered by partition transfer.
     */
    private void onCacheEntryArrived(TransferEvent event)
    {
        Set<BinaryEntry> binaryEntries = event.getEntries().get(Message.CACHENAME);

        if (binaryEntries != null)
        {
            for (BinaryEntry binaryEntry : binaryEntries)
            {
                Message          message   = (Message) binaryEntry.getValue();
                MessagePublisher publisher = MessagePublisherManager.getInstance().ensurePublisher(message);

                publisher.saveArrivingMessage(message);

                // Nothing else to do if message is already visible (exposed to topic or queue)
                if (!message.isVisible())
                {
                    if (logger.isLoggable(Level.FINER))
                    {
                        logger.log(Level.FINER,
                                   "MessageCacheInterceptor:onCacheEntryArrived identifier: {0}",
                                   message.getDestinationIdentifier());
                    }

                    // The message is being transferred to this member and it hasn't been
                    // exposed. Because messages are arriving in random order it cannot
                    // be added to MessagesToExpose object until the transfer.  When the
                    // transfer is done, the newly arrived messages will be added.
                    // See onPartitionArrivalDone()
                    publisher.saveMessageToBeExposed(message.getMessageIdentifier());

                    // The finite state machine will coalesce processor requests.  Without coalescing the requests
                    // the event queue backs up eventually causing an out of memory error.
                    MessageEngine mEngine = new MessageEngine(publisher.getDestinationIdentifier());

                    CacheFactory.getConfigurableCacheFactory().getResourceRegistry().registerResource(MessageEngine.class,
                                                                                                      message
                                                                                                      .getDestinationIdentifier()
                                                                                                          .toString(),
                                                                                                      mEngine);

                    mEngine.processRunEvent(publisher);

                }
            }
        }
    }


    /**
     * Event processing for Cache Entry when message is removed from cache.
     *
     * @param event Entry event triggered by cache removal.
     */
    private void onCacheEntryRemoved(EntryEvent event)
    {
        for (BinaryEntry binaryEntry : event.getEntrySet())
        {
            onRemoved(binaryEntry);
        }
    }


    /**
     * Event processing for Cache Entry when message departing via partition transfer.
     *
     * @param event Transfer event triggered by partition transfer.
     */
    private void onCacheEntryDeparting(TransferEvent event)
    {
        Set<BinaryEntry> binaryEntries = event.getEntries().get(Message.CACHENAME);

        if (binaryEntries != null)
        {
            for (BinaryEntry binaryEntry : binaryEntries)
            {
                onRemoved(binaryEntry);
            }
        }
    }


    /**
     * Common logic for performing cleanup on a removed entry, either from a removed entry event or
     * from a departing entry event. Cleanup the FSM that was used for coalescing events.
     *
     * @param binaryEntry BinaryEntry for the event to be processed.
     */
    private void onRemoved(BinaryEntry binaryEntry)
    {
        Message message = (Message) binaryEntry.getOriginalValue();

        if (message == null)
        {
            return;
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER,
                       "MessageCacheInterceptor:onRemoved identifier: {0}",
                       message.getDestinationIdentifier());
        }

        // Shutdown the Engine which contains the finite state machine, used to coalesce queue event processing.
        ResourceRegistry registry = CacheFactory.getConfigurableCacheFactory().getResourceRegistry();
        MessageEngine mEngine = registry.getResource(MessageEngine.class,
                                                     message.getDestinationIdentifier().toString());

        if (mEngine != null)
        {
            registry.unregisterResource(MessageEngine.class, message.getDestinationIdentifier().toString());
            mEngine.dispose();
        }
    }


    /**
     * Event processing when partition is arrived at the member.
     * Recreate the publisher ticket book and update the messages to be exposed.
     *
     * @param partitionId id of arriving partition.
     */
    private void onPartitionArrived(int partitionId)
    {
        // Recreate the publisher ticket book and update the messages to be exposed.
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
     * Event processing when partition is departing at the member.
     * The partition has finished departing from the member.  Clean up the messages to expose
     *  tracker and remove any publisher in this member with a matching partition id.
     *
     * @param partitionId the departing partition
     */
    private void onPartitionDeparting(int partitionId)
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
}
