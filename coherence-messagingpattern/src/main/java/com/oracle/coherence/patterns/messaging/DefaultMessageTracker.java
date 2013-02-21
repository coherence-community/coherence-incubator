/*
 * File: DefaultMessageTracker.java
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
import com.oracle.coherence.common.ranges.Ranges;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A {@link DefaultMessageTracker} implements the {@link MessageTracker}
 * interface using a map of ranges indexed by a partition identifier. The
 * {@link MessageTracker} manages a collection of {@link MessageIdentifier}s,
 * each of which contains a partition identifier and message sequence number.
 * That {@link DefaultMessageTracker} uses that information to look up the
 * {@link RangeWrapper} for a particular partition and then access the wrapper
 * using the message sequence number.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <C> subscription configuration
 *
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class DefaultMessageTracker implements MessageTracker
{
    /**
     * A map of ranges indexed by a partition identifier.
     */
    private HashMap<Integer, RangeWrapper> rangeWrapperMap = null;

    /**
     * The name of the message tracker.
     */
    private String name;

    /**
     * The partition identifier of the last message identifier added to the tracker.
     */
    private int lastPartitionId = 0;


    /**
     * Default Constructor.
     */
    public DefaultMessageTracker()
    {
        clear();
        this.name = null;
    }


    /**
     * Constructor with the name of the {@link DefaultMessageTracker}.
     *
     * @param name tracker name
     */
    public DefaultMessageTracker(String name)
    {
        clear();
        this.name = name;

    }


    /**
     * Add a {@link MessageIdentifier} to the {@link DefaultMessageTracker}.  The {@link MessageIdentifier} is
     * not actually stored in the tracker but its partition identifier is used to look up the wrapper then
     * the messages sequence number is added to the wrapper.
     *
     * @param messageIdentifier messages identifier
     * @return true if the message was added
     */
    public boolean add(MessageIdentifier messageIdentifier)
    {
        RangeWrapper wrapper = ensureRangeWrapper(messageIdentifier);

        wrapper.range = wrapper.range.add(messageIdentifier.getMessageSequenceNumber());

        // Update the max sequence number for this partition to prevent
        // duplicates
        if (messageIdentifier.getMessageSequenceNumber() > wrapper.maxSequenceNumber)
        {
            wrapper.maxSequenceNumber = messageIdentifier.getMessageSequenceNumber();
        }

        // Update the last partition of the message received by this tracker.
        lastPartitionId = messageIdentifier.getPartitionId();

        return true;
    }


    /**
     * Add all the contents of a {@link DefaultMessageTracker} to this {@link DefaultMessageTracker}.
     * Iterate through the input {@link DefaultMessageTracker} and add each {@link MessageIdentifier} to
     * this {@link DefaultMessageTracker}.
     *
     * @param inTracker message tracker
     * @return true if the message was added
     */
    public boolean addAll(MessageTracker inTracker)
    {
        boolean                     bAdded = false;
        Iterator<MessageIdentifier> iter   = inTracker.iterator();

        while (iter.hasNext())
        {
            if (add(iter.next()))
            {
                bAdded = true;
            }
        }

        return bAdded;
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        rangeWrapperMap = new HashMap<Integer, RangeWrapper>();
        lastPartitionId = 0;
    }


    /**
     * Get the {@link RangeWrapper} from the map for a given partition.  Create the {@link RangeWrapper} and
     * put it in the map if it doesn't exist.
     *
     * @param partitionId partition identifier
     * @return range wrapper
     */
    private RangeWrapper ensureRangeWrapper(int partitionId)
    {
        RangeWrapper wrapper = null;

        // Create the visible wrapper map for this partition if it doesn't exist
        if (rangeWrapperMap.containsKey(partitionId))
        {
            wrapper = rangeWrapperMap.get(partitionId);
        }
        else
        {
            wrapper = new RangeWrapper(partitionId);
            rangeWrapperMap.put(partitionId, wrapper);
        }

        return wrapper;
    }


    /**
     * Get the {@link RangeWrapper} from the map for a given {@link MessageIdentifier}.  Create the {@link RangeWrapper} and
     * put it in the map if it doesn't exist.
     *
     * @param messageIdentifier messages identifier
     * @return range wrapper
     */
    private RangeWrapper ensureRangeWrapper(MessageIdentifier messageIdentifier)
    {
        return ensureRangeWrapper(messageIdentifier.getPartitionId());
    }


    /**
     * {@inheritDoc}
     */
    public MessageIdentifier getLast()
    {
        RangeWrapper wrapper = ensureRangeWrapper(lastPartitionId);
        long         seqNum  = wrapper.range.getTo();

        return new MessageIdentifier(lastPartitionId, seqNum);
    }


    /**
     * {@inheritDoc}
     */
    public ArrayList<MessageKey> getMessageKeys(Identifier destinationIdentifier)
    {
        ArrayList<MessageKey>       batchMessageKeys = new ArrayList<MessageKey>((int) this.size());
        Iterator<MessageIdentifier> iter             = this.iterator();

        while (iter.hasNext())
        {
            MessageIdentifier messageIdentifier = iter.next();
            MessageKey        messageKey        = Message.getKey(destinationIdentifier, messageIdentifier);

            batchMessageKeys.add(messageKey);
        }

        return batchMessageKeys;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDuplicateMessage(MessageIdentifier messageIdentifier)
    {
        RangeWrapper wrapper = this.ensureRangeWrapper(messageIdentifier);

        return (messageIdentifier.getMessageSequenceNumber() <= wrapper.maxSequenceNumber);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<MessageIdentifier> iterator()
    {
        return new MessageIterator(rangeWrapperMap);
    }


    /**
     * Update the {@link Range}  in the map with a new {@link Range}.
     *
     * @param partitionId partition identifier
     * @param range range
     */
    public void replaceRange(int   partitionId,
                             Range range)

    {
        RangeWrapper wrapper = ensureRangeWrapper(partitionId);

        wrapper.range = range;
    }


    /**
     * Remove a {@link MessageIdentifier} from the {@link DefaultMessageTracker}.  The {@link MessageIdentifier} is
     * not actually stored in the tracker but its partition identifier is used to look up the wrapper then
     * the messages sequence number is removed from the wrapper.
     *
     * @param messageIdentifier messages identifier
     * @return true if the message was removed
     */
    public boolean remove(MessageIdentifier messageIdentifier)
    {
        long         msgSeqNum = messageIdentifier.getMessageSequenceNumber();
        RangeWrapper wrapper   = ensureRangeWrapper(messageIdentifier);

        if (wrapper.range.contains(msgSeqNum))
        {
            wrapper.range = wrapper.range.remove(messageIdentifier.getMessageSequenceNumber());

            // putRangeInMap(newRange, messageIdentifier.getPartitionId());
            return true;
        }

        return false;
    }


    /**
     * Remove all the contents of a {@link DefaultMessageTracker} from this {@link DefaultMessageTracker}.
     * Iterate through the input {@link DefaultMessageTracker} and remove each {@link MessageIdentifier} from
     * this {@link DefaultMessageTracker}.
     *
     * @param inTracker message tracker
     * @return true if the message was removed
     */
    public boolean removeAll(MessageTracker inTracker)
    {
        boolean                     bRemoved = false;
        Iterator<MessageIdentifier> iter     = inTracker.iterator();

        while (iter.hasNext())
        {
            if (remove(iter.next()))
            {
                bRemoved = true;
            }
        }

        return bRemoved;
    }


    /**
     * Get the number of {@link MessageIdentifier}s in the {@link DefaultMessageTracker}.
     *
     * @return number of {@link MessageIdentifier}s
     */
    public long size()
    {
        long                     size   = 0;
        Collection<RangeWrapper> ranges = rangeWrapperMap.values();

        for (RangeWrapper wrapper : ranges)
        {
            size += wrapper.range.size();
        }

        return size;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        ClassLoader loader = getClass().getClassLoader();

        rangeWrapperMap = new HashMap<Integer, RangeWrapper>();
        ExternalizableHelper.readMap(in, rangeWrapperMap, loader);

        lastPartitionId = ExternalizableHelper.readInt(in);
        name            = (String) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeMap(out, rangeWrapperMap);
        ExternalizableHelper.writeInt(out, lastPartitionId);
        ExternalizableHelper.writeObject(out, name);

    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        rangeWrapperMap = new HashMap<Integer, RangeWrapper>();
        reader.readMap(0, rangeWrapperMap);

        lastPartitionId = reader.readInt(1);
        name            = reader.readString(2);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeMap(0, rangeWrapperMap);
        writer.writeInt(1, lastPartitionId);
        writer.writeObject(2, name);

    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuffer                buf  = new StringBuffer("MessageTracker{");
        Iterator<MessageIdentifier> iter = iterator();

        while (iter.hasNext())
        {
            MessageIdentifier id = iter.next();

            buf.append(id.toString() + " ");
        }

        buf.append("}");

        return buf.toString();
    }


    /**
     * A {@link MessageIterator} provides an {@link Iterator} for a {@link DefaultMessageTracker}.
     */
    public static class MessageIterator implements Iterator<MessageIdentifier>
    {
        /**
         * this is used to get next wrapper in the wrapper map.
         */
        private Iterator<RangeWrapper> rangeWrapperMapIterator = null;

        /**
         * this is used to get next message in a wrapper.
         */
        private Iterator<Long> rangeIterator = null;

        /**
         * current wrapper being iterated.
         */
        RangeWrapper currentWrapper = null;


        /**
         * Constructor.
         *
         * @param rangeWrapperMap map of range wrappers
         */
        private MessageIterator(HashMap<Integer, RangeWrapper> rangeWrapperMap)
        {
            Collection<RangeWrapper> ranges = rangeWrapperMap.values();

            rangeWrapperMapIterator = ranges.iterator();
        }


        /**
         * Get the iterator for the next wrapper in the map.
         *
         * @return RangeWrapper
         */
        private Iterator<Long> nextRangeIterator()
        {
            if (rangeWrapperMapIterator.hasNext())
            {
                currentWrapper = this.rangeWrapperMapIterator.next();

                return currentWrapper.range.iterator();
            }
            else
            {
                return null;
            }
        }


        /**
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            do
            {
                // If we have a wrapper iter then see if it has messages
                if (rangeIterator != null)
                {
                    if (rangeIterator.hasNext())
                    {
                        return true;
                    }
                }

                // Go to the next wrapper
                rangeIterator = nextRangeIterator();
            }
            while (rangeIterator != null);

            return false;
        }


        /**
         * {@inheritDoc}
         */
        public MessageIdentifier next()
        {
            if (rangeIterator == null)
            {
                return null;
            }

            return new MessageIdentifier(currentWrapper.partitionId, rangeIterator.next());
        }


        /**
         * {@inheritDoc}
         */
        public void remove()
        {
            rangeIterator.remove();
        }
    }


    /**
     * A {@link RangeWrapper} is a helper class used to manage a {@link Range} of {@link Message}s.
     * for a specific partition
     */
    public static class RangeWrapper implements ExternalizableLite, PortableObject
    {
        /**
         * The {@link Range} containing a set of messages for a specific partition.
         */
        Range range;

        /**
         * The partition Id of the {@link Message}s in the {@link Range}.
         */
        int partitionId;

        /**
         * The maximum sequence number ever received by a
         * range. This is need to prevent duplicates from being added to a range
         * even if the range no longer contains the duplicate. the map is
         * <partitionId, lastMsgSeqNum>
         */
        long maxSequenceNumber;


        /**
         * Needed by ExternalizableLite, PortableObject.
         */
        public RangeWrapper()
        {
        }


        /**
         * Constructor.
         *
         * @param partitionId partition identifier
         */
        RangeWrapper(int partitionId)
        {
            this.partitionId  = partitionId;
            this.range        = Ranges.EMPTY;
            maxSequenceNumber = Long.MIN_VALUE;
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            range             = (Range) ExternalizableHelper.readObject(in);
            partitionId       = ExternalizableHelper.readInt(in);
            maxSequenceNumber = ExternalizableHelper.readLong(in);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeObject(out, range);
            ExternalizableHelper.writeInt(out, partitionId);
            ExternalizableHelper.writeLong(out, maxSequenceNumber);
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            range             = (Range) reader.readObject(0);
            partitionId       = reader.readInt(1);
            maxSequenceNumber = reader.readLong(2);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeObject(0, range);
            writer.writeInt(1, partitionId);
            writer.writeLong(2, maxSequenceNumber);
        }
    }
}
