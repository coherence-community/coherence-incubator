/*
 * File: CacheEventChannel.java
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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.CacheEvent;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.tangosol.net.NamedCache;

/**
 * An {@link CacheEventChannel} is a specialized {@link EventChannel} that is designed to apply
 * {@link CacheEvent}s originating on a "source" {@link NamedCache} into a "target" {@link NamedCache}.
 * <p>
 * During the process of applying {@link CacheEvent}s in the "target" {@link NamedCache}, a configured
 * {@link ConflictResolver} is consulted to resolve any potential conflicts that may occur when the said
 * {@link CacheEvent}s are applied to "target" {@link NamedCache}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Bob Hanckel
 */
public interface CacheEventChannel extends EventChannel
{
    /**
     * Determines the name of the "target" cache to which the {@link CacheEventChannel} will be distributing
     * {@link CacheEvent}s.
     *
     * @return The "target" cache name.
     */
    public String getTargetCacheName();


    /**
     * Determines the {@link ParameterizedBuilder} that can be used to produce {@link ConflictResolver} capable of
     * resolving conflicts when applying {@link CacheEvent}s in the "target" {@link NamedCache}.
     *
     * @return {@link ParameterizedBuilder} for a {@link ConflictResolver}.
     */
    public ParameterizedBuilder<ConflictResolver> getConflictResolverBuilder();


    /**
     * Determines the "target" {@link NamedCache} on which we'll be applying {@link CacheEvent}s.
     * <p>
     * Note: This method may return <code>null</code> or a {@link NamedCache} that is not active
     * (ie: not {@link NamedCache#isActive()}) in which case the {@link CacheEventChannel} is considered off-line
     * and can't be used for distribution.
     */
    public NamedCache getTargetNamedCache();
}
