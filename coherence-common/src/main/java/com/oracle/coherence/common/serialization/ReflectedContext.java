/*
 * File: ReflectedContext.java
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

import com.oracle.coherence.common.serialization.annotations.PofType;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;

import java.lang.reflect.Field;

/**
 * A {@link ReflectedContext} is a context of {@link DefaultReflectedSerializer}s that is built-up
 * through the runtime reflection of {@link PofType}s.
 * <p>
 * Note 1: Unlike the {@link ConfigurablePofContext}, you should never directly configure an instance of this class as a
 * Pof Serializer. Instead you should use the {@link ReflectiveSerializer} that will manage instances of this class for
 * you.
 * <p>
 * Note 2: Although like a {@link PofContext}, a {@link ReflectedContext} does not implement Serializer.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 * @author Brian Oliver
 */
public interface ReflectedContext
{
    /**
     * Retrieves the {@link ReflectedSerializer} from the
     * {@link ReflectedContext} based on the specified userTypeId.
     *
     * @param userTypeId The User Type Identifier
     *
     * @return The {@link ReflectedSerializer} for the userTypeId
     *
     * @throws IllegalArgumentException If the specified userTypeId is unknown
     *             to the {@link ReflectedContext}.
     */
    public ReflectedSerializer getPofSerializer(int userTypeId);


    /**
     * Retrieves the User Type Identifier for the {@link DefaultReflectedSerializer} associated with the
     * specified {@link Object}.
     *
     * @param object The {@link Object}
     *
     * @return The User Type Identifier
     *
     * @throws IllegalArgumentException If the specified {@link Object} class is unknown to the {@link ReflectedContext}.
     */
    public int getUserTypeIdentifier(Object object);


    /**
     * Retrieves the User Type Identifier for the {@link DefaultReflectedSerializer} associated with the
     * specified {@link Class}.
     *
     * @param clazz The {@link Class}
     *
     * @return The User Type Identifier
     *
     * @throws IllegalArgumentException If the specified {@link Class} is unknown to the {@link ReflectedContext}.
     */
    public int getUserTypeIdentifier(Class<?> clazz);


    /**
     * Retrieves the User Type Identifier for the {@link DefaultReflectedSerializer} associated with the
     * specified class name
     *
     * @param className The name of the {@link Class}
     *
     * @return The User Type Identifier
     *
     * @throws IllegalArgumentException If the specified class name is unknown to the {@link ReflectedContext}.
     */
    public int getUserTypeIdentifier(String className);


    /**
     * Retrieves the name of the {@link Class} associated with the specified User Type Identifier.
     *
     * @param userTypeId The User Type Identifier
     *
     * @return The class name associated with the userTypeId
     *
     * @throws IllegalArgumentException If the specified userTypeId is unknown to the {@link ReflectedContext}.
     */
    public String getClassName(int userTypeId);


    /**
     * Retrieves the {@link Class} associated with the specified User Type Identifier.
     *
     * @param userTypeId The User Type Identifier
     *
     * @return The {@link Class} associated with the userTypeId
     *
     * @throws IllegalArgumentException If the specified userTypeId is unknown to the {@link ReflectedContext}.
     */
    public Class<?> getClass(int userTypeId);


    /**
     * Retrieves the {@link ReflectedSerializer} associated with the specified
     * {@link Class}.
     *
     * @param clazz The {@link Class}
     *
     * @return the {@link ReflectedSerializer} used to serialize the type or
     *         null if no serializer could be found
     */
    public ReflectedSerializer getTypeSerializer(Class<?> clazz);


    /**
     * Determines if a {@link DefaultReflectedSerializer} can be associated to specified {@link Object} in
     * the {@link ReflectedContext}.
     *
     * @param object The {@link Object}
     *
     * @return <code>true</code> if an associated {@link DefaultReflectedSerializer} is known to the
     *         {@link DefaultReflectedSerializer}, <code>false</code> otherwise.
     */
    public boolean isUserType(Object object);


    /**
     * Determines if a {@link DefaultReflectedSerializer} is associated with specified {@link Class} in
     * the {@link ReflectedContext}.
     *
     * @param clazz The {@link Class}
     *
     * @return <code>true</code> if an associated {@link DefaultReflectedSerializer} is known to the
     *         {@link DefaultReflectedSerializer}, <code>false</code> otherwise.
     */
    public boolean isUserType(Class<?> clazz);


    /**
     * Determines if a {@link DefaultReflectedSerializer} is associated with specified class name in
     * the {@link ReflectedContext}.
     *
     * @param className The name of the {@link Class}.
     *
     * @return <code>true</code> if an associated {@link DefaultReflectedSerializer} is known to the
     *         {@link DefaultReflectedSerializer}, <code>false</code> otherwise.
     */
    public boolean isUserType(String className);


    /**
     * Ensures that a {@link ReflectedSerializer} for the specified type is
     * available in the {@link ReflectedContext}. If one is not (as the type has
     * not been previously seen by the {@link ReflectedContext}), reflection is
     * used to generate an appropriate {@link ReflectedSerializer}. The newly
     * generated {@link ReflectedContext} is then remembered for later retrieval
     * via the {@link ReflectedContext}.
     *
     * @param type The type for which to determine the
     *            {@link ReflectedSerializer}
     * @param pofContext a Base context used to locate information about an
     *            unknown type
     * @return the TypeMetaInfo representing the type
     */
    public ReflectedSerializer ensurePofSerializer(Class<?>   type,
                                                   PofContext pofContext);


    /**
     * The {@link FieldSerializationProvider} associated with this context to serialize {@link Field}s.
     *
     * @return the {@link FieldSerializationProvider}
     */
    public FieldSerializationProvider getFieldSerializationProvider();
}
