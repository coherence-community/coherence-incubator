/*
 * File: ChangeIndication.java
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
 * The {@link ChangeIndication} interface is used to indicate whether an object has a change
 * that needs persistence. {@code beforeChange} is called before the potentially state changing operation
 * is called on the object. After the state changing operation, changed() is called to determine
 * whether a change actually happened.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface ChangeIndication
{
    /**
     * beforeChange is called on the object before the state changing operation was called.
     */
    void beforeChange();


    /**
     * changed is called to determine whether the object changed since the call to beforeChange.
     *
     * @return true if the object changed
     */
    boolean changed();
}
