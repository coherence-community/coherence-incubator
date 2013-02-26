/*
 * File: AbstractBackingMapEntryEvent.java
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

package com.oracle.coherence.common.events.backingmap;

import com.tangosol.io.Serializer;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A base implementation for {@link BackingMapEntryEvent}s.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractBackingMapEntryEvent implements BackingMapEntryEvent
{
    /**
     * <p>The {@link BackingMapManagerContext} in which the {@link BackingMapEntryEvent} occurred.</p>
     */
    protected final BackingMapManagerContext backingMapManagerContext;

    /**
     * <p>The name of the {@link com.tangosol.net.NamedCache} on which the {@link BackingMapEntryEvent} occurred.</p>
     */
    private final String cacheName;

    /**
     * <p>The internal presentation of the {@link Entry} for the {@link BackingMapEntryEvent}.
     */
    private final EventEntry entry;


    /**
     * <p>Standard Constructor (when using {@link Entry}s).</p>
     *
     * @param backingMapManagerContext The BackingMapManagerContext associated with this event
     * @param cacheName                The name of the cache where this event was triggered
     * @param entry                    The {@link Entry} associated with this event
     */
    public AbstractBackingMapEntryEvent(final BackingMapManagerContext backingMapManagerContext,
                                        final String                   cacheName,
                                        final Map.Entry<?, ?>          entry)
    {
        this.backingMapManagerContext = backingMapManagerContext;
        this.cacheName                = cacheName;

        if (entry instanceof EventEntry)
        {
            this.entry = (EventEntry) entry;
        }
        else if (entry instanceof BinaryEntry)
        {
            this.entry = new EventEntry((BinaryEntry) entry);
        }
        else
        {
            this.entry = new EventEntry(entry.getKey(), entry.getValue());
        }
    }


    /**
     * <p>Standard Constructor (when using key, value pairs).</p>
     *
     * @param backingMapManagerContext The BackingMapManagerContext associated with this event
     * @param cacheName                The name of the cache where this event was triggered
     * @param key                      The key associated with this event
     * @param value                    The value associated with this event
     */
    public AbstractBackingMapEntryEvent(BackingMapManagerContext backingMapManagerContext,
                                        String                   cacheName,
                                        Object                   key,
                                        Object                   value)
    {
        this.backingMapManagerContext = backingMapManagerContext;
        this.cacheName                = cacheName;

        this.entry                    = new EventEntry(key, value);
    }


    /**
     * {@inheritDoc}
     */
    public BackingMapManagerContext getContext()
    {
        return backingMapManagerContext;
    }


    /**
     * {@inheritDoc}
     */
    public String getCacheName()
    {
        return cacheName;
    }


    /**
     * {@inheritDoc}
     */
    public BinaryEntry getEntry()
    {
        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("%s{cacheName=%s, entry=%s}", getClass().getName(), getCacheName(), getEntry());
    }


    /**
     *
     * <p>A {@link EventEntry} EventEntry is an immutable representation of an Entry for use by the events system.</p>
     *
     * @author Brian Oliver
     */
    @SuppressWarnings("rawtypes")
    protected class EventEntry implements Map.Entry, BinaryEntry
    {
        /**
         * <p>The {@link Binary} version of the key.</p>
         */
        private Binary binaryKey;

        /**
         * <p>The {@link Binary} version of the key.</p>
         */
        private Binary binaryValue;

        /**
         * <p>The deserialized key.</p>
         */
        private Object deserializedKey;

        /**
         * <p>The deserialized value.</p>
         */
        private Object deserializedValue;


        /**
         * <p>{@link BinaryEntry} based constructor.</p>
         *
         * @param binaryEntry The {@link BinaryEntry} to build this EventEntry out of
         */
        public EventEntry(BinaryEntry binaryEntry)
        {
            this.binaryKey         = binaryEntry.getBinaryKey();
            this.binaryValue       = binaryEntry.getBinaryValue();
            this.deserializedKey   = Void.TYPE;
            this.deserializedValue = Void.TYPE;
        }


        /**
         * <p>Object based constructor.</p>
         *
         * @param key   The key of this {@link EventEntry}
         * @param value The value of this {@link EventEntry}
         */
        public EventEntry(Object key,
                          Object value)
        {
            if (key instanceof Binary)
            {
                this.binaryKey       = (Binary) key;
                this.deserializedKey = Void.TYPE;           // Void.Type indicates not yet deserialized
            }
            else
            {
                this.binaryKey       = Binary.NO_BINARY;    // Binary.NO_BINARY indicates not yet serialized
                this.deserializedKey = key;
            }

            if (value instanceof Binary)
            {
                this.binaryValue       = (Binary) value;
                this.deserializedValue = Void.TYPE;           // Void.Type indicates not yet deserialized
            }
            else
            {
                this.binaryValue       = Binary.NO_BINARY;    // Binary.NO_BINARY indicates not yet serialized
                this.deserializedValue = value;
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isReadOnly()
        {
            return true;
        }


        /**
         * {@inheritDoc}
         */
        public Serializer getSerializer()
        {
            return getContext().getCacheService().getSerializer();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ObservableMap getBackingMap()
        {
            return getBackingMapContext().getBackingMap();
        }


        /**
         * {@inheritDoc}
         */
        public BackingMapManagerContext getContext()
        {
            return backingMapManagerContext;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public BackingMapContext getBackingMapContext()
        {
            return backingMapManagerContext.getBackingMapContext(getCacheName());
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void expire(long durationMS)
        {
            // SKIP: nothing to do here
        }


        /**
         * {@inheritDoc}
         */
        public Object getKey()
        {
            if (deserializedKey == Void.TYPE)
            {
                deserializedKey = getContext().getKeyFromInternalConverter().convert(binaryKey);
            }

            return deserializedKey;
        }


        /**
         * {@inheritDoc}
         */
        public Binary getBinaryKey()
        {
            if (binaryKey == Binary.NO_BINARY)
            {
                binaryKey = (Binary) getContext().getKeyToInternalConverter().convert(deserializedKey);

            }

            return binaryKey;
        }


        /**
         * {@inheritDoc}
         */
        public Object getValue()
        {
            if (deserializedValue == Void.TYPE)
            {
                deserializedValue = getContext().getValueFromInternalConverter().convert(binaryValue);
            }

            return deserializedValue;
        }


        /**
         * {@inheritDoc}
         */
        public Binary getBinaryValue()
        {
            if (binaryValue == Binary.NO_BINARY)
            {
                binaryValue = (Binary) getContext().getValueToInternalConverter().convert(deserializedValue);

            }

            return binaryValue;
        }


        /**
         * {@inheritDoc}
         */
        public Object extract(ValueExtractor extractor)
        {
            return InvocableMapHelper.extractFromEntry(extractor, entry);
        }


        /**
         * {@inheritDoc}
         */
        public Object setValue(Object value)
        {
            throw new UnsupportedOperationException("Can not call setValue(Object) an an EventEntry as they are immutable");
        }


        /**
         * {@inheritDoc}
         */
        public void updateBinaryValue(Binary binary)
        {
            throw new UnsupportedOperationException("Can not call updateBinaryValue(Binary) an an EventEntry as they are immutable");
        }


        /**
         * {@inheritDoc}
         */
        public boolean isPresent()
        {
            // NOTE: always returns true as when the event was raises the entry was present
            return true;
        }


        /**
         * {@inheritDoc}
         */
        public void remove(boolean synthetic)
        {
            throw new UnsupportedOperationException("Can not call remove(boolean) an an EventEntry as they are immutable");
        }


        /**
         * {@inheritDoc}
         */
        public void setValue(Object  value,
                             boolean synthetic)
        {
            throw new UnsupportedOperationException("Can not call setValue(Object, boolean) an an EventEntry as they are immutable");
        }


        /**
         * {@inheritDoc}
         */
        public void update(ValueUpdater valueUpdater,
                           Object       object)
        {
            throw new UnsupportedOperationException("Can not call update(ValueUpdater, Object) an an EventEntry as they are immutable");
        }


        /**
         * EventEntries are immutable so there will not be an original value.
         *
         * @return null
         */
        public Binary getOriginalBinaryValue()
        {
            return null;
        }


        /**
         * EventEntries are immutable so there will not be an original value.
         *
         * @return null
         */
        public Object getOriginalValue()
        {
            return null;
        }


        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return String.format("%s{key=%s, value=%s}", getClass().getName(), getKey(), getValue());
        }
    }
}
