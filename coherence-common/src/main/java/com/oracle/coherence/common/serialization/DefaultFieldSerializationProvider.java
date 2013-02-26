/*
 * File: DefaultFieldSerializationProvider.java
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

package com.oracle.coherence.common.serialization;

import com.oracle.coherence.common.serialization.fieldserializers.BigDecimalFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.BigIntegerFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.BinaryFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.BooleanArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.BooleanFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.ByteArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.ByteFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.CharacterArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.CharacterFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.CollectionFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.DateFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.DoubleArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.DoubleFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.FloatArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.FloatFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.IntegerArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.IntegerFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.LongArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.LongFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.MapFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.ObjectArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.ObjectFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.RawQuadFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.ShortArrayFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.ShortFieldSerializer;
import com.oracle.coherence.common.serialization.fieldserializers.StringFieldSerializer;
import com.tangosol.io.pof.RawQuad;
import com.tangosol.util.Binary;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.util.Collection;
import java.util.Map;

/**
 * A {@link DefaultFieldSerializationProvider} in an implementation of a {@link FieldSerializationProvider}
 * that provides {@link FieldSerializer} for each of the Java primitive types and several of the Coherence
 * Portable Object types.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
public class DefaultFieldSerializationProvider implements FieldSerializationProvider
{
    /**
     * The {@link BigDecimalFieldSerializer}.
     */
    private final BigDecimalFieldSerializer bigDecimalFieldSerializer;

    /**
     * The {@link BigIntegerFieldSerializer}.
     */
    private final BigIntegerFieldSerializer bigIntFieldSerializer;

    /**
     * The {@link BinaryFieldSerializer}.
     */
    private final BinaryFieldSerializer binaryFieldSerializer;

    /**
     * The {@link BooleanFieldSerializer}.
     */
    private final BooleanArrayFieldSerializer booleanArrayFieldSerializer;

    /**
     * The {@link BooleanFieldSerializer}.
     */
    private final BooleanFieldSerializer booleanFieldSerializer;

    /**
     * The {@link ByteArrayFieldSerializer}.
     */
    private final ByteArrayFieldSerializer byteArrayFieldSerializer;

    /**
     * The {@link ByteFieldSerializer}.
     */
    private final ByteFieldSerializer byteFieldSerializer;

    /**
     * The {@link CharacterArrayFieldSerializer}.
     */
    private final CharacterArrayFieldSerializer charArrayFieldSerializer;

    /**
     * The {@link CharacterFieldSerializer}.
     */
    private final CharacterFieldSerializer charFieldSerializer;

    /**
     * The {@link DateFieldSerializer}.
     */
    private final DateFieldSerializer dateFieldSerializer;

    /**
     * The {@link DoubleArrayFieldSerializer}.
     */
    private final DoubleArrayFieldSerializer doubleArrayFieldSerializer;

    /**
     * The {@link DoubleFieldSerializer}.
     */
    private final DoubleFieldSerializer doubleFieldSerializer;

    /**
     * The {@link FloatArrayFieldSerializer}.
     */
    private final FloatArrayFieldSerializer floatArrayFieldSerializer;

    /**
     * The {@link FloatFieldSerializer}.
     */
    private final FloatFieldSerializer floatFieldSerializer;

    /**
     * The {@link IntegerArrayFieldSerializer}.
     */
    private final IntegerArrayFieldSerializer intArrayFieldSerializer;

    /**
     * The {@link IntegerFieldSerializer}.
     */
    private final IntegerFieldSerializer intFieldSerializer;

    /**
     * The {@link LongArrayFieldSerializer}.
     */
    private final LongArrayFieldSerializer longArrayFieldSerializer;

    /**
     * The {@link LongFieldSerializer}.
     */
    private final LongFieldSerializer longFieldSerializer;

    /**
     * The {@link ObjectFieldSerializer}.
     */
    private final ObjectFieldSerializer objectFieldSerializer;

    /**
     * The {@link RawQuadFieldSerializer}.
     */
    private final RawQuadFieldSerializer rawQuadFieldSerializer;

    /**
     * The {@link ShortArrayFieldSerializer}.
     */
    private final ShortArrayFieldSerializer shortArrayFieldSerializer;

    /**
     * The {@link ShortFieldSerializer}.
     */
    private final ShortFieldSerializer shortFieldSerializer;

    /**
     * The {@link StringFieldSerializer}.
     */
    private final StringFieldSerializer stringFieldSerializer;


    /**
     * Standard Constructor.
     */
    public DefaultFieldSerializationProvider()
    {
        intFieldSerializer          = new IntegerFieldSerializer();
        stringFieldSerializer       = new StringFieldSerializer();
        longFieldSerializer         = new LongFieldSerializer();
        byteFieldSerializer         = new ByteFieldSerializer();
        charFieldSerializer         = new CharacterFieldSerializer();
        shortFieldSerializer        = new ShortFieldSerializer();
        booleanFieldSerializer      = new BooleanFieldSerializer();
        doubleFieldSerializer       = new DoubleFieldSerializer();
        floatFieldSerializer        = new FloatFieldSerializer();

        intArrayFieldSerializer     = new IntegerArrayFieldSerializer();
        longArrayFieldSerializer    = new LongArrayFieldSerializer();
        byteArrayFieldSerializer    = new ByteArrayFieldSerializer();
        charArrayFieldSerializer    = new CharacterArrayFieldSerializer();
        shortArrayFieldSerializer   = new ShortArrayFieldSerializer();
        booleanArrayFieldSerializer = new BooleanArrayFieldSerializer();
        doubleArrayFieldSerializer  = new DoubleArrayFieldSerializer();
        floatArrayFieldSerializer   = new FloatArrayFieldSerializer();

        bigIntFieldSerializer       = new BigIntegerFieldSerializer();
        bigDecimalFieldSerializer   = new BigDecimalFieldSerializer();
        rawQuadFieldSerializer      = new RawQuadFieldSerializer();
        dateFieldSerializer         = new DateFieldSerializer();
        binaryFieldSerializer       = new BinaryFieldSerializer();

        objectFieldSerializer       = new ObjectFieldSerializer();
    }


    /**
     * {@inheritDoc}
     */
    public FieldSerializer getFieldSerializer(Field              field,
                                              java.lang.Class<?> preferredType)
    {
        Class<?> fieldType = field.getType();

        if (int.class.isAssignableFrom(fieldType))
        {
            return intFieldSerializer;
        }
        else if (String.class.isAssignableFrom(fieldType))
        {
            return stringFieldSerializer;
        }
        else if (long.class.isAssignableFrom(fieldType))
        {
            return longFieldSerializer;
        }
        else if (byte.class.isAssignableFrom(fieldType))
        {
            return byteFieldSerializer;
        }
        else if (char.class.isAssignableFrom(fieldType))
        {
            return charFieldSerializer;
        }
        else if (short.class.isAssignableFrom(fieldType))
        {
            return shortFieldSerializer;
        }
        else if (boolean.class.isAssignableFrom(fieldType))
        {
            return booleanFieldSerializer;
        }
        else if (double.class.isAssignableFrom(fieldType))
        {
            return doubleFieldSerializer;
        }
        else if (float.class.isAssignableFrom(fieldType))
        {
            return floatFieldSerializer;
        }
        else if (fieldType.isArray())
        {
            Class<?> componentType = fieldType.getComponentType();

            if (int.class.isAssignableFrom(componentType))
            {
                return intArrayFieldSerializer;
            }
            else if (long.class.isAssignableFrom(componentType))
            {
                return longArrayFieldSerializer;
            }
            else if (byte.class.isAssignableFrom(componentType))
            {
                return byteArrayFieldSerializer;
            }
            else if (char.class.isAssignableFrom(componentType))
            {
                return charArrayFieldSerializer;
            }
            else if (short.class.isAssignableFrom(componentType))
            {
                return shortArrayFieldSerializer;
            }
            else if (boolean.class.isAssignableFrom(componentType))
            {
                return booleanArrayFieldSerializer;
            }
            else if (double.class.isAssignableFrom(componentType))
            {
                return doubleArrayFieldSerializer;
            }
            else if (float.class.isAssignableFrom(componentType))
            {
                return floatArrayFieldSerializer;
            }
            else
            {
                return new ObjectArrayFieldSerializer(componentType);
            }
        }
        else if (Map.class.isAssignableFrom(fieldType))
        {
            return new MapFieldSerializer(field, preferredType);
        }
        else if (Collection.class.isAssignableFrom(fieldType))
        {
            return new CollectionFieldSerializer(field, preferredType);
        }
        else if (Binary.class.isAssignableFrom(fieldType))
        {
            return binaryFieldSerializer;
        }
        else if (BigInteger.class.isAssignableFrom(fieldType))
        {
            return bigIntFieldSerializer;
        }
        else if (BigDecimal.class.isAssignableFrom(fieldType))
        {
            return bigDecimalFieldSerializer;
        }
        else if (RawQuad.class.isAssignableFrom(fieldType))
        {
            return rawQuadFieldSerializer;
        }
        else if (Date.class.isAssignableFrom(fieldType))
        {
            return dateFieldSerializer;
        }
        else
        {
            return objectFieldSerializer;
        }
    }
}
