/*
 * File: ConflictResolver.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.tangosol.util.BinaryEntry;

/**
 * An {@link ConflictResolver} is responsible what {@link ConflictResolution} must occur as the
 * result of attempting to apply an {@link EntryEvent} on a specified {@link BinaryEntry}.
 * <p>
 * By providing a custom {@link ConflictResolver} implementation, it is possible to detect and perform all kinds of
 * conflict resolution on cache entries during the process of applying an {@link EntryEvent}, potentially originating
 * from another cluster.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ConflictResolver
{
    /**
     * Determines and returns the appropriate {@link ConflictResolution} that is the result of applying the
     * {@link EntryEvent} on the specified {@link BinaryEntry}.
     * <p>
     * NOTE: Any exception thrown by this method will cause the distribution and application of {@link EntryEvent}s to
     * stop. ie: fail-fast.  Hence you should be careful to ensure that internal exceptions are dealt with appropriate
     * and only escape this method if you <strong>want</strong> the associated {@link CacheEventChannel} to become
     * suspended.
     *
     * @param entryEvent The {@link EntryEvent}.
     * @param binaryEntry The entry on which the {@link EntryEvent} should be replicated.
     *
     * @return A {@link ConflictResolution} indicating the required processing to apply to the {@link BinaryEntry}.
     */
    public ConflictResolution resolve(EntryEvent<DistributableEntry> entryEvent,
                                      BinaryEntry                    binaryEntry);
}
