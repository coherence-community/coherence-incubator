/*
 * File: CollectionFieldSerializer.java
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
import java.util.Collection;

/**
 * A {@link CollectionFieldSerializer} is a {@link FieldSerializer} for {@link Collection}s.
 * <p>
 * Importantly all effort is made to ensure type-safety of the collections used, including their
 * generic type arguments by either using the type information declared by the {@link Field} or
 * using a specified preferred type.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
public final class CollectionFieldSerializer extends AbstractGenericFieldSerializer
{
    /**
     * Standard Constructor.
     *
     * @param field         The {@link Field} containing the {@link Collection} to serialize.
     * @param preferredType The preferred type of {@link Collection} to instantiate when deserializing.
     */
    public CollectionFieldSerializer(Field    field,
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
        // determine the existing collection (we'll populate this if there is one)
        Collection<?> collection = (Collection<?>) field.get(object);

        if (collection == null)
        {
            // attempt to create a new collection of the appropriate type
            try
            {
                collection = (Collection<?>) constructor.newInstance();
            }
            catch (Exception e)
            {
                Logger.log(Logger.ERROR, "Failed to create a new instance (%s): %s", collection, e.getMessage());

                throw new IllegalStateException(e);
            }
        }

        field.set(object, reader.readCollection(index, collection));
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
        // when we have concrete type information we can write out a uniform collection (more efficient)
        if (typeArguments == null)
        {
            writer.writeCollection(index, (Collection<?>) field.get(object));
        }
        else
        {
            writer.writeCollection(index, (Collection<?>) field.get(object), (Class<?>) typeArguments[0]);
        }
    }
}
