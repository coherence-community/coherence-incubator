/*
 * File: InvocableEntry.java
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

package com.oracle.coherence.common.events;

import com.tangosol.util.InvocableMap.EntryProcessor;

import java.util.Map;

/**
 * An {@link InvocableEntry} is an {@link java.util.Map.Entry} that supports execution of
 * {@link EntryProcessor}s, without explicitly requiring a reference to an
 * underlying {@link com.tangosol.net.NamedCache} and/or {@link com.tangosol.util.InvocableMap}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see com.tangosol.util.InvocableMap.EntryProcessor
 * @see com.tangosol.util.InvocableMap
 *
 * @author Brian Oliver
 */
@SuppressWarnings("rawtypes")
public interface InvocableEntry extends Map.Entry
{
    /**
     * Invokes the specified {@link EntryProcessor} against the
     * {@link InvocableEntry} returning the result of the invocation.
     *
     * @param entryProcessor The {@link EntryProcessor} to invoke.
     *
     * @return the result of invoking the EntryProcessor
     */
    public Object invoke(EntryProcessor entryProcessor);
}
