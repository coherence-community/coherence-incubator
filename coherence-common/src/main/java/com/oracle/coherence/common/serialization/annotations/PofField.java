/*
 * File: PofField.java
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

package com.oracle.coherence.common.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * A {@link PofField} annotation is used to specify serialization information for a {@link Field} that is declared
 * on a {@link PofType}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see PofType
 *
 * @author Charlie Helin
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PofField
{
    /**
     * (Optional) The name of the {@link Field}.  When specified will be used to identify the {@link Field}
     * instead of the declared {@link Field#getName()}.
     */
    public String name()   default "";

    /**
     * (Optional) Specifies the version of the enclosing {@link PofType} when the {@link Field} was added.  If
     * not specified, the default value of 0 is used as the initial version.
     */
    public int since()     default 0;

    /**
     *  (Optional) Specifies the required concrete type of the {@link Field} to instantiate when deserializing
     *  the {@link Field}. If not specified, the declared type of the {@link Field}, ie: {@link Field#getType()} will
     *  be used.
     *  <p>
     *  For example:  If a {@link Field} is declared as a generic Map<?, ?>, by specifying the {@link #type()} of this
     *  annotation to be a ConcurrentHashMap.class a serializer must instantiate and deserialize values into a new
     *  ConcurrentHashMap instance instead of using some other, possibly internal, Map implementation and/or the
     *  declared {@link Field} type.
     *  <p>
     *  Note: That when a {@link #type()} is specified, it must be assignable to the declared type of the
     *  annonated {@link Field}.
     */
    public Class<?> type() default Object.class;
}
