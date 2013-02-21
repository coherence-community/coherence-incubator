/*
 * File: ReflectiveSerializer.java
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

import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.common.serialization.annotations.PofField;
import com.oracle.coherence.common.serialization.annotations.PofIgnore;
import com.oracle.coherence.common.serialization.annotations.PofType;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

/**
 * The {@link ReflectiveSerializer} is an implementation of a {@link PofSerializer} that uses reflection to
 * serialize and deserialize a given {@link Object} at runtime.
 * <p>
 * By declaring that an {@link Object} in a pof configuration (based on a {@link ConfigurablePofContext})
 * is serialized using a {@link ReflectiveSerializer}, allows you to completely avoid implementing the
 * {@link PortableObject} interface on the said {@link Object}.  Instead you simply need to annotate the {@link Object}
 * with the serialization annotations (see below).
 * <p>
 * NOTE 1: This serializer supports the serialization and deserialization of object-hierarchies
 * (ie: object inheritance) through the use of nested-pof streams.  The parent class of an {@link Object} is always
 * written at index 0 in the pof stream.  All other attributes must be written after this index.  As the nested-pof
 * streams feature was only added to Coherence 3.6+, this serializer may only be used with Coherence 3.6+.
 * <p>
 * NOTE 2: This serializer fully supports versioning and thus evolution/evolability of {@link PofType} annotated
 * {@link Object}s.  To enable, simply include a single field of type {@link PofRemainder} in each {@link PofType}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see PofType
 * @see PofField
 * @see PofRemainder
 * @see PofIgnore
 *
 * @author Charlie Helin
 */
public class ReflectiveSerializer implements PofSerializer
{
    /**
     * The {@link FieldSerializationProvider} for the {@link ReflectiveSerializer}.
     */
    static final FieldSerializationProvider serializationProvider;

    /**
     * The {@link ReflectedContext} for the {@link DefaultReflectedSerializer}.
     */
    static final ReflectedContext pofContext;


    /**
     * {@inheritDoc}
     */
    @Override
    public Object deserialize(PofReader reader) throws IOException
    {
        int userTypeId = reader.getUserTypeId();
        ReflectedSerializer serializer =
            getReflectedPofContext().ensurePofSerializer(reader.getPofContext().getClass(userTypeId),
                                                         reader.getPofContext());

        if (serializer == null)
        {
            Logger.log(Logger.ERROR, "Unknown type (%s)", userTypeId);

            throw new IllegalStateException("Unknown type: " + userTypeId);
        }

        try
        {
            return serializer.deserialize(reader);
        }
        catch (Exception e)
        {
            Logger.log(Logger.ERROR,
                       "Failure during serialization of %s: %s",
                       serializer.getType().getName(),
                       e.getMessage());

            throw new IllegalStateException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(PofWriter writer,
                          Object    object) throws IOException
    {
        int userTypeId = writer.getUserTypeId();
        ReflectedSerializer serializer =
            getReflectedPofContext().ensurePofSerializer(writer.getPofContext().getClass(userTypeId),
                                                         writer.getPofContext());

        if (serializer == null)
        {
            Logger.log(Logger.ERROR, "Unknown type (%d)", userTypeId);

            throw new IllegalStateException("Unknown type: " + userTypeId);
        }

        try
        {
            serializer.serialize(writer, object);
        }
        catch (Exception e)
        {
            Logger.log(Logger.ERROR,
                       "Failure during serialization of %s: %s",
                       serializer.getType().getName(),
                       e.getMessage());

            throw new IllegalStateException(e);
        }

    }


    /**
     * Determines the {@link FieldSerializationProvider} configured for the {@link ReflectiveSerializer}.
     *
     * @return A {@link FieldSerializationProvider}
     */
    public FieldSerializationProvider getFieldSerializationProvider()
    {
        return serializationProvider;
    }


    /**
     * Determines the {@link ReflectedContext} for the {@link ReflectiveSerializer}.
     *
     * @return {@link ReflectedContext}
     */
    public ReflectedContext getReflectedPofContext()
    {
        return pofContext;
    }


    static
    {
        serializationProvider = new DefaultFieldSerializationProvider();
        pofContext            = new DefaultReflectedPofSerializer(serializationProvider);
    }
}
