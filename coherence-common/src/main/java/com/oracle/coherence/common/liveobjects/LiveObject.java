/*
 * File: LiveObject.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.common.liveobjects;

import com.tangosol.net.events.EventInterceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link LiveObject} annotation identifies that a class is capable of
 * handling lifecycle events that occur on instances of the said class.
 * <p>
 * To handle the said events, the class will typically either:
 * <ol>
 *      <li>implement the {@link EventInterceptor} interface, and/or</li>
 *      <li>annotate specific event interceptors methods</li>
 * </ol>
 * <p>
 * Copyright (c) 2009-2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @see OnArrived
 * @see OnDeparting
 * @see OnInserted
 * @see OnInserting
 * @see OnRemoved
 * @see OnUpdated
 * @see OnUpdating
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LiveObject
{
    /**
     * This is a marker annotation.
     */
}
