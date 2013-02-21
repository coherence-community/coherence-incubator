/*
 * File: ObjectChangeCallback.java
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

/**
 * An {@link ObjectChangeCallback} is a callback interface to be used to be informed about changes
 * to remote objects represented by a {@link NamedCacheObjectProxy}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <T> the type of the object that we are calling back for
 *
 * @author Christer Fahlgren
 */
public interface ObjectChangeCallback<T>
{
    /**
     * Callback when the remote object has changed. A local copy of the changed object is passed as the parameter.
     *
     * @param object the changed object
     */
    public void objectChanged(T object);


    /**
     * Callback when the remote object has been created. A local copy of the created object is passed as the parameter.
     *
     * @param object the created object
     */
    public void objectCreated(T object);


    /**
     * Callback when the remote object has been deleted. The key to the remote object is passed as a parameter.
     *
     * @param key the key to the remote object
     */
    public void objectDeleted(Object key);
}
