/*
 * File: DistributableEntry.java
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

package com.oracle.coherence.patterns.eventdistribution.events;

import com.oracle.coherence.common.cluster.ClusterMetaInfo;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.EntryEventProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.Cluster;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map.Entry;

/**
 * A {@link DistributableEntry} is an specialized {@link BinaryEntry} design to be distributed as part of a
 * {@link DistributableEntryEvent}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"serial", "rawtypes"})
public class DistributableEntry implements Entry, BinaryEntry, ExternalizableLite, PortableObject
{
    /**
     * The {@link #CLUSTER_META_INFO_DECORATION_KEY} denotes the {@link ClusterMetaInfo} of the {@link Cluster} in which an
     * {@link Event} occurred. This is the actual source of the {@link Event}, not and indirect source in a cascading
     * set of {@link Event}s. In other words if an {@link Event} at Cluster1 is replicated to
     * Cluster2, the {@link Event} will be decorated with Cluster1.  If the {@link Event} is further replicated to
     * Cluster3 from Cluster2, the {@link Event} will be decorated with Cluster2.
     * <p>
     * The {@link EventChannelController} uses the source of an {@link Event} together with the role of the
     * {@link EventChannel} to perform the appropriate distribution.
     */
    public static final String CLUSTER_META_INFO_DECORATION_KEY = "$src$";

    /**
     * The {@link #MARKED_FOR_ERASE_DECORATION_KEY} is set by the "process" method in {@link EntryEventProcessor} when
     * performing an erase to ensure that the {@link Event} does not cause infinite feedback (ping-pong) between clusters.
     * Basically this decoration is set while processing a remove which does a preliminary put with this key set
     * at the source cluster.
     * <p>
     * This is to make ensure that the entry is properly decorated when a remove is actually performed.
     * The remove is then issued.
     * <p>
     * When this decoration is seen by a cluster it understands that it is purely an artifact of a
     * local erase event and becomes a no-op. (A event in this context does not get passed to the
     * {@link EventDistributorBuilder})
     * <p>
     * When this decoration is not present on an {@link Event}, the {@link Event} a {@link EventDistributorBuilder}.
     */
    public static final String MARKED_FOR_ERASE_DECORATION_KEY = "$erase$";

    /**
     * The {@link Binary} representation of the {@link Entry} key.
     */
    private Binary binaryKey;

    /**
     * The {@link Binary} representation of the {@link Entry} value.
     */
    private Binary binaryValue;

    /**
     * The {@link Binary} representation of the {@link Entry} original value.
     */
    private Binary originalBinaryValue;

    /**
     * The {@link BackingMapManagerContext} of the {@link Entry} (this is not serialized).
     */
    private transient BackingMapManagerContext context;

    /**
     * The {@link Serializer} for the {@link Entry} (this is not serialized).
     */
    private transient Serializer serializer;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject} support.
     */
    public DistributableEntry()
    {
        // SKIP: deliberately empty
    }


    /**
     * Standard Constructor (with a {@link BinaryEntry}).
     *
     * @param binaryEntry The {@link BinaryEntry} on which to base the {@link Entry}
     */
    public DistributableEntry(BinaryEntry binaryEntry)
    {
        this.binaryKey           = binaryEntry.getBinaryKey();
        this.binaryValue         = binaryEntry.getBinaryValue();
        this.originalBinaryValue = binaryEntry.getOriginalBinaryValue();
        this.context             = binaryEntry.getContext();
        this.serializer          = binaryEntry.getSerializer();
    }


    /**
     * Standard Constructor (with key and value binaries).
     *
     * @param binaryKey The {@link Binary} of the entry key
     * @param binaryValue The {@link Binary} of the entry value
     * @param context The {@link BackingMapManagerContext} for the entry
     */
    public DistributableEntry(Binary                   binaryKey,
                              Binary                   binaryValue,
                              Binary                   binaryOriginalValue,
                              BackingMapManagerContext context)
    {
        this.binaryKey           = binaryKey;
        this.binaryValue         = binaryValue;
        this.originalBinaryValue = binaryOriginalValue;
        this.context             = context;
        this.serializer          = context.getCacheService().getSerializer();
    }


    /**
     * {@inheritDoc}
     */
    public Object extract(ValueExtractor extractor)
    {
        return InvocableMapHelper.extractFromEntry(extractor, this);
    }


    /**
     * {@inheritDoc}
     */
    public Binary getBinaryKey()
    {
        return binaryKey;
    }


    /**
     * {@inheritDoc}
     */
    public Binary getBinaryValue()
    {
        return binaryValue;
    }


    /**
     * Returns the Original {@link Binary} Value for the {@link Entry}.
     *
     * @return The original {@link Binary} value of the {@link Entry}
     */
    public Binary getOriginalBinaryValue()
    {
        return originalBinaryValue;
    }


    /**
     * Sets the Original {@link Binary} for the {@link Entry}.
     */
    public void setOriginalBinaryValue(Binary orginalBinaryValue)
    {
        this.originalBinaryValue = orginalBinaryValue;
    }


    /**
     * {@inheritDoc}
     */
    public Object getOriginalValue()
    {
        Object originalValue;

        if (originalBinaryValue == null || originalBinaryValue.length() == 0)
        {
            originalValue = null;
        }
        else
        {
            try
            {
                if (getContext() == null && getSerializer() != null)
                {
                    originalValue = serializer.deserialize(originalBinaryValue.getBufferInput());
                }
                else
                {
                    originalValue = getContext().getValueFromInternalConverter().convert(originalBinaryValue);
                }
            }
            catch (Exception e)
            {
                throw Base.ensureRuntimeException(e);
            }
        }

        // TODO: we should hold a weak reference to the result as we might call this method again.

        return originalValue;
    }


    /**
     * {@inheritDoc}
     */
    public ObservableMap getBackingMap()
    {
        // SKIP: deliberately empty
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public BackingMapManagerContext getContext()
    {
        return context;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public BackingMapContext getBackingMapContext()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void expire(long durationMS)
    {
        // SKIP: deliberately empty
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
     * Sets the {@link BackingMapManagerContext} for the {@link Entry}.
     *
     * @param context The {@link BackingMapManagerContext} to use for the {@link Entry}
     */
    public void setContext(BackingMapManagerContext context)
    {
        this.context = context;

        if (context == null)
        {
            this.serializer = null;
        }
        else
        {
            this.serializer = context.getCacheService().getSerializer();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object getKey()
    {
        Object key;

        if (binaryKey == null || binaryKey.length() == 0)
        {
            key = null;
        }
        else
        {
            try
            {
                if (getContext() == null && getSerializer() != null)
                {
                    key = serializer.deserialize(binaryKey.getBufferInput());
                }
                else
                {
                    key = getContext().getKeyFromInternalConverter().convert(binaryKey);
                }
            }
            catch (Exception e)
            {
                throw Base.ensureRuntimeException(e);
            }
        }

        // TODO: we should hold a weak reference to the result as we might call this method again.

        return key;
    }


    /**
     * {@inheritDoc}
     */
    public Object getValue()
    {
        Object value;

        if (binaryValue == null || binaryValue.length() == 0)
        {
            value = null;
        }
        else
        {
            try
            {
                if (getContext() == null && getSerializer() != null)
                {
                    value = serializer.deserialize(binaryValue.getBufferInput());
                }
                else
                {
                    value = getContext().getValueFromInternalConverter().convert(binaryValue);
                }
            }
            catch (Exception e)
            {
                throw Base.ensureRuntimeException(e);
            }
        }

        // TODO: we should hold a weak reference to the result as we might call this method again.

        return value;
    }


    /**
     * {@inheritDoc}
     */
    public Serializer getSerializer()
    {
        return serializer;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isPresent()
    {
        // NOTE: we always return true as it was always present when distribution began
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public void remove(boolean isSynthetic)
    {
        throw new UnsupportedOperationException("DistributableEntry doesn't support remove(boolean) as it's immutable.");
    }


    /**
     * {@inheritDoc}
     */
    public Object setValue(Object value)
    {
        // we need to return the previous value
        Object oPrevious = getValue();

        // remember the existing custom decorations as we'll need to re-decorate them after we change the value
        Object oDecorations = context.getInternalValueDecoration(binaryValue, BackingMapManagerContext.DECO_CUSTOM);

        if (context == null)
        {
            throw new UnsupportedOperationException("DistributableEntry doesn't have a BackingMapContext so unable to serialize the new value.");
        }
        else
        {
            // serialize/convert the new value to a binary
            binaryValue = (Binary) context.getValueToInternalConverter().convert(value);

            // re-add the custom decorations
            binaryValue = (Binary) context.addInternalValueDecoration(binaryValue,
                                                                      BackingMapManagerContext.DECO_CUSTOM,
                                                                      oDecorations);

            return oPrevious;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void setValue(Object  value,
                         boolean isSynthetic)
    {
        setValue(value);
    }


    /**
     * {@inheritDoc}
     */
    public void update(ValueUpdater valueUpdater,
                       Object       value)
    {
        throw new UnsupportedOperationException("DistributableEntry doesn't support update(ValueUpdater, Object).");
    }


    /**
     * {@inheritDoc}
     */
    public void updateBinaryValue(Binary binary)
    {
        throw new UnsupportedOperationException("DistributableEntry doesn't support updateBinaryValue(Binary).");
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.binaryKey           = (Binary) ExternalizableHelper.readObject(in);
        this.binaryValue         = (Binary) ExternalizableHelper.readObject(in);
        this.originalBinaryValue = (Binary) ExternalizableHelper.readObject(in);
        this.context             = null;
        this.serializer          = null;
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, binaryKey);
        ExternalizableHelper.writeObject(out, binaryValue);
        ExternalizableHelper.writeObject(out, originalBinaryValue);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.binaryKey           = reader.readBinary(0);
        this.binaryValue         = reader.readBinary(1);
        this.originalBinaryValue = reader.readBinary(2);
        this.context             = null;
        this.serializer          = null;
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeBinary(0, binaryKey);
        writer.writeBinary(1, binaryValue);
        writer.writeBinary(2, originalBinaryValue);
    }


    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return binaryKey.hashCode();
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

        if (!(obj instanceof DistributableEntry))
        {
            return false;
        }

        DistributableEntry other = (DistributableEntry) obj;

        if (binaryKey == null)
        {
            if (other.binaryKey != null)
            {
                return false;
            }
        }
        else if (!binaryKey.equals(other.binaryKey))
        {
            return false;
        }

        if (binaryValue == null)
        {
            if (other.binaryValue != null)
            {
                return false;
            }
        }
        else if (!binaryValue.equals(other.binaryValue))
        {
            return false;
        }

        if (originalBinaryValue == null)
        {
            if (other.originalBinaryValue != null)
            {
                return false;
            }
        }
        else if (!originalBinaryValue.equals(other.originalBinaryValue))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        Object key           = null;
        Object value         = null;
        Object originalValue = null;

        if (context != null)
        {
            try
            {
                key           = getKey();
                value         = getValue();
                originalValue = getOriginalValue();
            }
            catch (RuntimeException e)
            {
                // no op we just can deserialize the key or the value we'll use the binary version
            }
        }

        return String.format("DistributableEntry{key=%s, value=%s, originalValue=%s}",
                             (key == null ? getBinaryKey() : key),
                             (value == null ? getBinaryValue() : value),
                             (originalValue == null ? getOriginalBinaryValue() : originalValue));
    }
}
