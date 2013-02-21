/*
 * File: MessagePublisher.java
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
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.common.ranges.Range;
import com.oracle.coherence.common.ranges.Ranges;
import com.oracle.coherence.common.ticketing.Ticket;
import com.oracle.coherence.common.ticketing.TicketBook;
import com.oracle.coherence.patterns.messaging.Destination.DestinationType;
import com.oracle.coherence.patterns.messaging.entryprocessors.PublishMessageProcessor;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.processor.ExtractorProcessor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The {@link MessagePublisher} is responsible for generating {@link MessageIdentifier}s and putting a
 * message in the message cache. The {@link MessagePublisher} is a JVM only object, it is not
 * stored in the cache. It is created on demand the first time it is needed and exists for the life
 * of the JVM or until the message partition it services is transferred to another member.
 * There is a {@link MessagePublisher} created for each destination/partition pair.
 * When the {@link PublishMessageProcessor} .process() method executes, it calls {@link MessagePublisherManager} to
 * get the publisher.  The {@link MessagePublisherManager} looks up the publisher in a local, static map.
 * If the publisher doesn't exist, it is created and put in the map. That publisher exists for the life of the JVM.
 * <p>
 * In the case of a member failure the entire map will be recreated.  If the message partition moves to another member, then
 * the Publisher will need to be recreated and rebuilt.  A Message backing map listener will generate the events needed
 * to rebuild the entry (entry events, and partition events).
 * <p>
 * To publisher a message, the client must call MessagingSession.publisherMessage(..).  Every MessagingSession
 * has a unique publisher identifier that is used as a key when invoking the PublishMessageProcesser.  This
 * forces the processor to execute in the member that owns that key, even though there isn't yet an
 * entry in the cache for that key. This class then gets the partitionId assigned to that key and puts
 * the message into the partition.  The message key used to store the message is a combination of destinationId,
 * partitionId and a sequence number.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class MessagePublisher
{
    /**
     * The identifier of the partition serviced by this {@link MessagePublisher}.
     */
    private int partitionId = 0;

    /**
     * Type of destination.
     */
    private Destination.DestinationType destinationType = null;

    /**
     * Destination {@link Identifier} used as a cache key.
     */
    private Identifier destinationIdentifier = null;

    /**
     * {@link TicketBook} used to generate monotonically increasing message sequence numbers.
     */
    private TicketBook messageSequenceNumberTicketBook = new TicketBook(partitionId);

    /**
     * Minimum message sequence number for all messages that arriving as a result
     * of a partition transfer to this member for the partition/destination managed
     * by this {@link MessagePublisher}.
     */
    private long minArrivingSeqNum;

    /**
     * Maximum message sequence number for all messages that arriving as a result
     * of a partition transfer to this member for the partition/destination managed
     * by this {@link MessagePublisher}.
     */
    private long maxArrivingSeqNum;

    /**
     * Minimum message sequence number for all messages that arriving that need additional
     * processing by an event processor.
     */
    private long minDeliverySeqNum;

    /**
     * Maximum message sequence number for all messages that arriving that need additional
     * processing by an event processor.
     */
    private long maxDeliverySeqNum;

    /**
     * A map of the highest request sequence number for each publisher indexed by the publisher id.  This
     * is used to prevent putting a message in the cache in the case of an entry processor failure.
     */
    private HashMap<Identifier, Long> requestIdentifierMap = new HashMap<Identifier, Long>();


    /**
     * Constructor.  Initialize variables and create a new {@link TicketBook}
     *
     * @param destinationIdentifier destination identifier
     * @param partitionId partition identifier
     */
    MessagePublisher(Identifier destinationIdentifier,
                     int        partitionId)
    {
        this.destinationIdentifier = destinationIdentifier;
        this.partitionId           = partitionId;
        initDestinationType();
        reset();
    }


    /**
     * Publish the message by creating a {@link Message} object and putting it directly into
     * the backing map. See {@link PublishMessageProcessor} for further explanation.
     * <p>
     * Note: This method must be synchronized since multiple publishers may be writing to the
     * same partition.  It is imperative that the backing map event is called is the message sequence
     * number order.  This ensures that {@link MessageEventManager} getEventProcessor method exposes the
     * messages in monotonically increasing order.
     *
     * @param requestId request identifier
     * @param payload message payload
     */
    @SuppressWarnings("unchecked")
    public synchronized void publishMessage(PublishRequestIdentifier requestId,
                                            Object                   payload)
    {
        // Note: if the destination is a topic with no subscriptions then the
        // message is deleted in the event processor. This allows the message to
        // be put into the cache.
        // without getting the topic from the destination cache.
        Message message = new Message(this.destinationIdentifier, requestId, generateMessageId(), payload);

        // Get the backing map manager context.
        NamedCache               cache   = CacheFactory.getCache(Message.CACHENAME);
        CacheService             service = cache.getCacheService();
        BackingMapManager        manager = service.getBackingMapManager();
        BackingMapManagerContext context = manager.getContext();

        // Put the message into the cache via the backing map. This will
        // generate an event where we will inform all the subscribers that the
        // message has arrived.
        Map<Object, Object> backingMap = (Map<Object, Object>) context.getBackingMap(Message.CACHENAME);
        Object              binaryKey  = context.getKeyToInternalConverter().convert(message.getKey());
        Object              binaryVal  = context.getValueToInternalConverter().convert(message);

        backingMap.put(binaryKey, binaryVal);
    }


    /**
     * Check if a {@link PublishRequestIdentifier} has already been seen by this publisher. If a partition is
     * transferred after {@link Message} has been put into the cache but before the {@link PublishMessageProcessor}
     * completes, then Coherence will re-run the {@link PublishMessageProcessor}.  Without this check, the same
     * {@link Message} would be written to the cache again, violating the JMS contract.
     * The map is populated during partition transfer and is only needed by the first {@link PublishMessageProcessor}
     * executing against a partition after the partition has been transferred.  That is why the map entry is
     * removed if it is found.
     *
     * @param requestId request identifier
     * @return boolean true if the request exists.
     */
    public synchronized boolean checkRequestExists(PublishRequestIdentifier requestId)
    {
        Identifier key = requestId.getPublisherIdentifier();

        if (requestIdentifierMap.containsKey(key))
        {
            long lastSeqNum = requestIdentifierMap.get(key).longValue();

            requestIdentifierMap.remove(key);

            if (lastSeqNum == requestId.getMessageSequenceNumber())
            {
                return true;
            }
            else if (lastSeqNum > requestId.getMessageSequenceNumber())
            {
                Logger.log(Logger.ERROR, "MessagePublisher.checkRequestExists found an invalid sequence number.\n");
            }
        }

        return false;
    }


    /**
     * Save the maximum sequence number for a given publisher.  This is used to determine if the
     * {@link PublishMessageProcessor} is trying to re=publish the same message.
     *
     * @param requestId request identifier
     */
    synchronized void saveRequest(PublishRequestIdentifier requestId)
    {
        Identifier key = requestId.getPublisherIdentifier();

        if (requestIdentifierMap.containsKey(key))
        {
            long maxSeqNum = requestIdentifierMap.get(key).longValue();

            if (maxSeqNum >= requestId.getMessageSequenceNumber())
            {
                return;    // maximum sequence number for this publisher is already

                // in the map
            }
        }

        // Save the maximum sequence number for this publisher;
        this.requestIdentifierMap.put(key, requestId.getMessageSequenceNumber());
    }


    /**
     * Reset the variables to the initial state.
     */
    void reset()
    {
        minArrivingSeqNum = Long.MAX_VALUE;
        maxArrivingSeqNum = Long.MIN_VALUE;

        minDeliverySeqNum = Long.MAX_VALUE;
        maxDeliverySeqNum = Long.MIN_VALUE;

    }


    /**
     * Check if the {@link Destination} used by this publisher is a {@link Topic}.
     *
     * @return true if destination is a topic
     */
    boolean isTopic()
    {
        return destinationType == Destination.DestinationType.Topic;
    }


    /**
     * Check if the {@link Destination} used by this publisher is a {@link Queue}.
     *
     * @return true if destination is a queue
     */
    boolean isQueue()
    {
        return destinationType == Destination.DestinationType.Queue;
    }


    /**
     * Check if the {@link Destination} used by this publisher is a {@link Queue}.
     *
     * @return true if destination is a queue
     */
    int getPartitionId()
    {
        return partitionId;
    }


    /**
     * Get the {@link Identifier} of the {@link Destination} used by this publisher.
     *
     * @return destination identifier
     */
    Identifier getDestinationIdentifier()
    {
        return destinationIdentifier;
    }


    /**
     * A message is arriving to this member due to a partition transfer.
     *
     * @param message messages
     */
    void saveArrivingMessage(Message message)
    {
        // Add this message requestId to the list so that we can prevent
        // duplicate publishing.
        this.saveRequest(message.getRequestIdentifier());

        // Update the minimum and maximum sequence numbers so that the {@link TicketBook}
        // can be recreated when the transfer is done.
        long seqNum = message.getMessageIdentifier().getMessageSequenceNumber();

        if (seqNum < minArrivingSeqNum)
        {
            minArrivingSeqNum = seqNum;
        }

        if (seqNum > maxArrivingSeqNum)
        {
            maxArrivingSeqNum = seqNum;
        }
    }


    /**
     * A message is arriving to this member due to a partition transfer and the message
     * needs to be exposed by an event processor.
     *
     * @param messageIdentifier message identifier
     */
    void saveMessageToBeExposed(MessageIdentifier messageIdentifier)
    {
        // Update the minimum sequence numbers so that the {@link Range}
        // can be recreated when the transfer is done.
        long seqNum = messageIdentifier.getMessageSequenceNumber();

        if (seqNum < minDeliverySeqNum)
        {
            minDeliverySeqNum = seqNum;
        }

        if (seqNum > maxDeliverySeqNum)
        {
            maxDeliverySeqNum = seqNum;
        }
    }


    /**
     * The partition transfer to this member is done.  Create a {@link TicketBook} for generating
     * message sequence numbers.
     */
    void recreateTicketBook()
    {
        createTicketBook(minArrivingSeqNum, maxArrivingSeqNum);
    }


    /**
     * Create a {@link TicketBook} using a range
     */
    private void createTicketBook(long from,
                                  long to)
    {
        // Create a new ticket book. If no messages were transferred then
        //
        if (from == Long.MAX_VALUE)
        {
            messageSequenceNumberTicketBook = new TicketBook(partitionId);
        }
        else
        {
            messageSequenceNumberTicketBook = new TicketBook(partitionId, from, to);
        }
    }


    /**
     * Create the new range for this partition containing messages that need to be exposed.
     *
     * @return range
     */
    Range getPendingMessagesToExpose()
    {
        // Create a new range for the batch delivery tracker
        //
        Range range;

        if (minDeliverySeqNum == Long.MAX_VALUE)
        {
            range = Ranges.EMPTY;
        }
        else
        {
            range = Ranges.newRange(minDeliverySeqNum, maxDeliverySeqNum);
        }

        return range;

    }


    /**
     * Generate the {@link MessageIdentifier} using the destination id and a monotonically increasing
     * message sequence number for the next {@link Message} to publish. The {@link MessageIdentifier}
     * will guarantee ordering for each message sent by a {@link MessagingSession} to a specific destination.
     * <p>
     * Each {@link MessagePublisher} contains a {@link TicketBook} which is used to generate the
     * message sequence numbers. It is critical that message ordering is maintained per client
     * session, i.e. {@link MessagingSession}.  When an message partition is transferred to a new member,
     * the last message sequence number is tracked and we recreate the {@link TicketBook} once the transfer
     * is done. See {@link MessageEventManager}.  However, if the partition is empty there are
     * no arriving messages, the next sequence number is not known. This can happen if message consumption
     * is faster than message production.  In that case, the code gets the sequence number from the destination or
     * subscription.
     *
     * @return message identifier
     */
    private MessageIdentifier generateMessageId()
    {
        // Get a new ticket. Since multiple publishers may be sharing a single
        // partition, we need to lock the book.
        Ticket ticket = null;

        synchronized (messageSequenceNumberTicketBook)
        {
            // INC-529 fix
            // If the ticket book is empty that means that either this is the first message
            // put into the message partition or there was a fail over when the message partition
            // was empty.  Either case is very rare but must be handled or else the
            // sequence number will restart at 1 and be incorrect.
            if (messageSequenceNumberTicketBook.isEmpty())
            {
                // Set up Extractor entry processor to get sequence number
                Integer[] params = new Integer[1];

                params[0] = Integer.valueOf(partitionId);

                EntryProcessor processor =
                    new ExtractorProcessor(new ReflectionExtractor("getLastMessageSequenceNumber",
                                                                   params));

                // The queue or topic subscription keeps track of the last message received for each partition.
                long seqNum = 0;

                if (isTopic())
                {
                    seqNum = getSequenceNumberForTopic(processor);
                }
                else
                {
                    // Invoke the extractor processor against the queue
                    Long seqNumLong =
                        (Long) CacheFactory.getCache(Destination.CACHENAME).invoke(this.destinationIdentifier,
                                                                                   processor);

                    seqNum = seqNumLong.longValue();
                }

                // If we got a non-zero sequnce number then
                // Recreate the ticket book starting at the last sequence number.
                if (seqNum > 0)
                {
                    this.createTicketBook(seqNum, seqNum);
                }
            }

            // Get a ticket with the next sequence number
            ticket = messageSequenceNumberTicketBook.extend();
        }

        return new MessageIdentifier(partitionId, ticket.getSequenceNumber());
    }


    /**
     * Get the maximum message sequence number from one of the subscriptions in a topic.
     *
     * @param processor entry processor to invoke against subscription
     * @return long sequence number. A value of zero means the sequence number was not retrieved.
     */
    private long getSequenceNumberForTopic(EntryProcessor processor)
    {
        // Get the topic
        NamedCache  destinationsCache = CacheFactory.getCache(Destination.CACHENAME);
        Destination destination       = (Destination) destinationsCache.get(destinationIdentifier);

        if (destination == null)
        {
            String error = "Error generating message identifier. Destination " + this.destinationIdentifier
                           + " is not found.";

            Logger.log(Logger.ERROR, error);

            throw new IllegalStateException(error);
        }

        // Get the last message sequence number seen for this partition by any topic subscription.
        Set<SubscriptionIdentifier> subscriptions = destination.getSubscriptionIdentifiers();

        if (subscriptions == null)
        {
            return 0;
        }

        // Loop until a successful call is made or no more subscriptions.
        // A subscription might be removed from the cache by the time we call it.
        long                             seqNum  = 0;
        boolean                          success = false;
        Iterator<SubscriptionIdentifier> iter    = subscriptions.iterator();

        while (iter.hasNext() && (!success))
        {
            SubscriptionIdentifier subscriptionIdentifier = iter.next();

            // Call the subscription. If null is returned then try the next one.
            Long seqNumLong = (Long) CacheFactory.getCache(Subscription.CACHENAME).invoke(subscriptionIdentifier,
                                                                                          processor);

            if (seqNumLong != null)
            {
                seqNum  = seqNumLong.longValue();
                success = true;
            }
        }

        return seqNum;
    }


    /**
     * Load the destination object if we don't know the type of destination.
     */
    private void initDestinationType()
    {
        if (this.destinationType == null)
        {
            NamedCache  destinationsCache = CacheFactory.getCache(Destination.CACHENAME);
            Destination destination       = (Destination) destinationsCache.get(destinationIdentifier);

            if (destination == null)
            {
                Logger.log(Logger.ERROR, "Destination " + this.destinationIdentifier + " is not found.");

                return;
            }

            if (destination instanceof Queue)
            {
                destinationType = DestinationType.Queue;
            }
            else
            {
                destinationType = DestinationType.Topic;
            }
        }
    }


    /**
     * The {@link Key} is used to identify a {@link MessagePublisher} for
     * a specific partition in a given destination.  The key is composed of a partition identifier
     * plus a destination identifier.
     */
    public static class Key implements Serializable
    {
        /**
         * Destination {@link Identifier}.
         */
        Identifier destinationIdentifier;

        /**
         * Partition identifier.
         */
        int partitionId;


        /**
         * Constructor.
         *
         * @param destinationIdentifier destination identifier
         * @param partitionId partition identifier
         */
        Key(Identifier destinationIdentifier,
            int        partitionId)
        {
            this.destinationIdentifier = destinationIdentifier;
            this.partitionId           = partitionId;
        }


        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (obj == null)
            {
                return false;
            }

            if (getClass() != obj.getClass())
            {
                return false;
            }

            final MessagePublisher.Key other = (MessagePublisher.Key) obj;

            if (partitionId != other.partitionId)
            {
                return false;
            }

            if (!destinationIdentifier.equals(other.destinationIdentifier))
            {
                return false;
            }

            return true;
        }


        /**
         * {@inheritDoc}
         */
        public int hashCode()
        {
            final int prime  = 31;
            int       result = 1;

            result = prime * result + destinationIdentifier.hashCode();
            result = prime * result + (int) (partitionId);

            return result;
        }


        /**
         * Generate a string representation of this key.
         *
         * @return string
         */
        public String toString()
        {
            return String.format("MessagePublsiher{destinationIdentifier=%s, partitionId=%d, }",
                                 destinationIdentifier,
                                 partitionId);
        }
    }
}
