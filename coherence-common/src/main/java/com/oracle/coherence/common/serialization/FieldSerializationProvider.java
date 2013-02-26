/*
 * File: FieldSerializationProvider.java
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

import java.lang.reflect.Field;

/**
 * A {@link FieldSerializationProvider} provides a mechanism to locate an appropriate {@link FieldSerializer} for
 * a given {@link Field}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
public interface FieldSerializationProvider
{
    /**
     * Determines and returns the {@link FieldSerializer} for the specified {@link Field} or <code>null</code> if
     * an appropriate {@link FieldSerializer} can not be located.
     *
     * @param field         The {@link Field} for which a {@link FieldSerializer} is required.
     * @param preferredType (optional) When specified, this type should be used to determine the
     *                      {@link FieldSerializer}.  Otherwise the type of the {@link Field} itself should be used.
     *
     * @return The {@link FieldSerializer} for the specified {@link Field} or <code>null</code>
     *         if one is not available.
     */
    public FieldSerializer getFieldSerializer(Field    field,
                                              Class<?> preferredType);
}
