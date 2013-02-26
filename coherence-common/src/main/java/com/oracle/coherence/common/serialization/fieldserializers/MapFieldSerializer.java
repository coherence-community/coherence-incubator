/*
 * File: MapFieldSerializer.java
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

package com.oracle.coherence.common.serialization.fieldserializers;

import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.common.serialization.FieldSerializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * A {@link MapFieldSerializer} is a {@link FieldSerializer} for {@link Map}s.
 * <p>
 * Importantly all effort is made to ensure type-safety of the {@link Map}s used, including their
 * generic type arguments by either using the type information declared by the {@link Field} or
 * using a specified preferred type.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
public final class MapFieldSerializer extends AbstractGenericFieldSerializer
{
    /**
     * Standard Constructor.
     *
     * @param field         The {@link Field} containing the {@link Map} to serialize.
     * @param preferredType The preferred type of {@link Map} to instantiate when deserializing.
     */
    public MapFieldSerializer(Field    field,
                              Class<?> preferredType)
    {
        super(field, preferredType);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readField(Object    object,
                          Field     field,
                          PofReader reader,
                          int       index) throws IllegalArgumentException, IllegalAccessException, IOException
    {
        // determine the existing map (we'll populate this if there is one)
        Map<?, ?> map = (Map<?, ?>) field.get(object);

        if (map == null)
        {
            // attempt to create a new map of the appropriate type
            try
            {
                map = (Map<?, ?>) constructor.newInstance();
            }
            catch (Exception e)
            {
                Logger.log(Logger.ERROR, "Failed to create new instance of a map for %s", field);

                throw new IllegalStateException(e);
            }
        }

        field.set(object, reader.readMap(index, map));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeField(Object    object,
                           Field     field,
                           PofWriter writer,
                           int       index) throws IllegalArgumentException, IOException, IllegalAccessException
    {
        // when we have concrete type information we can write out a uniform map (more efficient)
        if (typeArguments == null)
        {
            writer.writeMap(index, (Map<?, ?>) field.get(object));
        }
        else
        {
            writer.writeMap(index,
                            (Map<?, ?>) field.get(object),
                            (Class<?>) typeArguments[0],
                            (Class<?>) typeArguments[1]);
        }
    }
}
