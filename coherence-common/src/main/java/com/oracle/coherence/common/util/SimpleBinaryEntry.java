/*
 * File: SimpleBinaryEntry.java
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

package com.oracle.coherence.common.util;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.extractor.AbstractUpdater;

import java.io.IOException;

/**
 * A {@link SimpleBinaryEntry} is an implementation of {@link BinaryEntry}, mainly for the purposes of testing
 * interfaces that require a {@link BinaryEntry}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class SimpleBinaryEntry implements BinaryEntry
{
    /**
     * The {@link Binary} representation of the Key for the {@link BinaryEntry}.
     */
    private Binary binaryKey;

    /**
     * The {@link Binary} representation of the Value for the {@link BinaryEntry}.
     */
    private Binary binaryValue;

    /**
     * The {@link Binary} representation of the Original Value for the {@link BinaryEntry}.
     */
    private Binary binaryOriginalValue;

    /**
     * The {@link Serializer} that may be used to serialize and deserialize the {@link BinaryEntry}.
     */
    private Serializer serializer;


    /**
     * Standard Constructor (using the {@link DefaultSerializer}).
     *
     * @param binaryKey             The {@link Binary} Key
     * @param binaryValue           The {@link Binary} Value
     */
    public SimpleBinaryEntry(Binary binaryKey,
                             Binary binaryValue)
    {
        this(new DefaultSerializer(), binaryKey, binaryValue, null);
    }


    /**
     * Standard Constructor (using natural objects, unserialized)
     *
     * @param key               The key
     * @param value             The value
     */
    public SimpleBinaryEntry(Object key,
                             Object value)
    {
        this(key, value, null);
    }


    /**
     * Standard Constructor (using the {@link DefaultSerializer}).
     *
     * @param binaryKey             The {@link Binary} Key
     * @param binaryValue           The {@link Binary} Value
     * @param binaryOriginalValue   The {@link Binary} Original Value
     */
    public SimpleBinaryEntry(Binary binaryKey,
                             Binary binaryValue,
                             Binary binaryOriginalValue)
    {
        this(new DefaultSerializer(), binaryKey, binaryValue, binaryOriginalValue);
    }


    /**
     * Standard Constructor (using natural objects, unserialized)
     *
     * @param key               The key
     * @param value             The value
     * @param originalValue     The original value
     */
    public SimpleBinaryEntry(Object key,
                             Object value,
                             Object originalValue)
    {
        this.serializer = new DefaultSerializer();

        try
        {
            BinaryWriteBuffer buffer = new BinaryWriteBuffer(200);

            serializer.serialize(buffer.getBufferOutput(), key);
            this.binaryKey = buffer.toBinary();

            buffer         = new BinaryWriteBuffer(200);
            serializer.serialize(buffer.getBufferOutput(), value);
            this.binaryValue = buffer.toBinary();

            buffer           = new BinaryWriteBuffer(200);
            serializer.serialize(buffer.getBufferOutput(), originalValue);
            this.binaryOriginalValue = buffer.toBinary();
        }
        catch (IOException e)
        {
            throw Base.ensureRuntimeException(e, "Failed to Serialize the SimpleBinaryEntry parameters");
        }
    }


    /**
     * Standard Constructor.
     *
     * @param serializer            The {@link Serializer}
     * @param binaryKey             The {@link Binary} Key
     * @param binaryValue           The {@link Binary} Value
     */
    public SimpleBinaryEntry(Serializer serializer,
                             Binary     binaryKey,
                             Binary     binaryValue)
    {
        this(serializer, binaryKey, binaryValue, null);
    }


    /**
     * Standard Constructor.
     *
     * @param serializer            The {@link Serializer}
     * @param binaryKey             The {@link Binary} Key
     * @param binaryValue           The {@link Binary} Value
     * @param binaryOriginalValue   The {@link Binary} Original Value
     */
    public SimpleBinaryEntry(Serializer serializer,
                             Binary     binaryKey,
                             Binary     binaryValue,
                             Binary     binaryOriginalValue)
    {
        this.serializer          = serializer;
        this.binaryKey           = binaryKey;
        this.binaryValue         = binaryValue;
        this.binaryOriginalValue = binaryOriginalValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Binary getBinaryKey()
    {
        return binaryKey;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Binary getBinaryValue()
    {
        return binaryValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Binary getOriginalBinaryValue()
    {
        return binaryOriginalValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Serializer getSerializer()
    {
        return serializer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
    {
        return false;
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
    @Override
    public BackingMapContext getBackingMapContext()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPresent()
    {
        return getValue() != null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object getKey()
    {
        try
        {
            if (binaryKey == null)
            {
                return null;
            }
            else
            {
                return serializer.deserialize(binaryKey.getBufferInput());
            }
        }
        catch (IOException e)
        {
            throw Base.ensureRuntimeException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue()
    {
        try
        {
            if (binaryValue == null)
            {
                return null;
            }
            else
            {
                return serializer.deserialize(binaryValue.getBufferInput());
            }
        }
        catch (IOException e)
        {
            throw Base.ensureRuntimeException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object getOriginalValue()
    {
        try
        {
            if (binaryOriginalValue == null)
            {
                return null;
            }
            else
            {
                return serializer.deserialize(binaryOriginalValue.getBufferInput());
            }
        }
        catch (IOException e)
        {
            throw Base.ensureRuntimeException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(boolean isSynthetic)
    {
        setValue(null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object setValue(Object value)
    {
        Object currentValue = getValue();

        try
        {
            BinaryWriteBuffer buffer = new BinaryWriteBuffer(200);

            serializer.serialize(buffer.getBufferOutput(), value);
            this.binaryValue = buffer.toBinary();
        }
        catch (IOException e)
        {
            throw Base.ensureRuntimeException(e);
        }

        return currentValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Object  value,
                         boolean isSynthetic)
    {
        setValue(value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object extract(ValueExtractor extractor)
    {
        return InvocableMapHelper.extractFromEntry(extractor, this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBinaryValue(Binary binaryValue)
    {
        this.binaryValue = binaryValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void update(ValueUpdater valueUpdater,
                       Object       value)
    {
        if (valueUpdater instanceof AbstractUpdater)
        {
            ((AbstractUpdater) valueUpdater).updateEntry(this, value);
        }
        else
        {
            valueUpdater.update(getValue(), value);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ObservableMap getBackingMap()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public BackingMapManagerContext getContext()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int result = 31 * ((binaryKey == null) ? 0 : binaryKey.hashCode());

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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

        SimpleBinaryEntry other = (SimpleBinaryEntry) obj;

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

        if (binaryOriginalValue == null)
        {
            if (other.binaryOriginalValue != null)
            {
                return false;
            }
        }
        else if (!binaryOriginalValue.equals(other.binaryOriginalValue))
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

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("SimpleBinaryEntry{binaryKey=%s (%s), binaryValue=%s (%s), binaryOriginalValue=%s (%s)}",
                             binaryKey,
                             getKey(),
                             binaryValue,
                             getValue(),
                             binaryOriginalValue,
                             getOriginalValue());
    }
}
