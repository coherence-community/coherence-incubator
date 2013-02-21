/*
 * File: Cause.java
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

package com.oracle.coherence.common.backingmaplisteners;

/**
 * An enumeration to represent the possible causes of backing map events.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public enum Cause
{
    /**
     * <code>Regular</code> is for regular inserts, updates and delete events.
     */
    Regular,

    /**
     * <code>Eviction</code> is for deletes that are due to cache eviction.
     */
    Eviction,

    /**
     * <code>PartitionManagement</code> is used for inserts and deletes that
     * have occurred due to cache partitions being load-balanced or recovered.
     */
    PartitionManagement,

    /**
     * <code>StoreCompleted</code> is for update events due to a storage decoration
     * change on an entry. Coherence updates a decoration after a successful store
     * operation on a write-behind store. ie: an asynchronous store has completed.
     */
    StoreCompleted;
}
