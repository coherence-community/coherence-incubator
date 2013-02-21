/*
 * File: FieldSerializer.java
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

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * A {@link FieldSerializer} provides the ability to read and write a specific {@link Field} of an
 * {@link Object} to and from a {@link PortableObject} stream using a {@link PofReader} and {@link PofWriter}
 * respectively.
 * <p>
 * Classes implementing this interface are designed to provide low-level strongly-typed {@link Field}-based
 * serialization to support higher-level serialization like that provided by the {@link DefaultReflectedSerializer}s.
 * <p>
 * In general, the only time you will ever need to implement this interface is when you wish to provide
 * serialization support for a new primitive type of serializer.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
public interface FieldSerializer
{
    /**
     * Writes the specified {@link Field} of the specified {@link Object} to the provided {@link PofWriter} at the
     * specified index.
     *
     * @param object    The {@link Object} containing the {@link Field} to write
     * @param field     The {@link Field} to write
     * @param writer    The {@link PofWriter} in which to write the {@link Field} value
     * @param index     The index in the {@link PofWriter} to write the value
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public void writeField(Object    object,
                           Field     field,
                           PofWriter writer,
                           int       index) throws IllegalArgumentException, IOException, IllegalAccessException;


    /**
     * Reads and assigns the specified {@link Field} of the specified {@link Object} from the provided {@link PofReader}
     * from the specified index.
     *
     * @param object    The {@link Object} in which to assign the read {@link Field}
     * @param field     The {@link Field} to assign in the {@link Object}
     * @param reader    The {@link PofReader} from which to read the {@link Field} value
     * @param index     The index in the {@link PofReader} from which to read the value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    void readField(Object    object,
                   Field     field,
                   PofReader reader,
                   int       index) throws IllegalArgumentException, IllegalAccessException, IOException;
}
