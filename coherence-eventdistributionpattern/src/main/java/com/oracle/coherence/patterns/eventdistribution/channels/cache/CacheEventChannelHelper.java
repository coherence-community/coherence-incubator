/*
 * File: CacheEventChannelHelper.java
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
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableCacheInvocationRequestEvent;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.net.NamedCache;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link CacheEventChannelHelper} is a static class that provides helpers for distributing and applying
 * {@link Event}s on {@link NamedCache}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class CacheEventChannelHelper
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(CacheEventChannelHelper.class.getName());


    /**
     * Applies the specified {@link Event}s to a specified {@link NamedCache} using the provided
     * {@link ConflictResolver} to resolve any conflicts.
     *
     * @return The number of {@link Event}s applied.
     */
    public static int apply(NamedCache                             namedCache,
                            Iterator<Event>                        events,
                            ParameterizedBuilder<ConflictResolver> conflictResolverBuilder)
    {
        int distributionCount = 0;

        while (events.hasNext())
        {
            Event event = events.next();

            if (event instanceof DistributableEntryEvent)
            {
                DistributableEntryEvent entryEvent = (DistributableEntryEvent) event;

                try
                {
                    namedCache.invoke(entryEvent.getEntry().getBinaryKey(),
                                      new EntryEventProcessor(entryEvent,
                                                              conflictResolverBuilder,
                                                              namedCache.getCacheName()));

                    distributionCount++;

                }
                catch (IllegalStateException illegalStateException)
                {
                    if (logger.isLoggable(Level.SEVERE))
                    {
                        logger.log(Level.SEVERE, "illegalStateException {0}", illegalStateException);
                    }

                    throw illegalStateException;

                }
                catch (RuntimeException runtimeException)
                {
                    if (logger.isLoggable(Level.SEVERE))
                    {
                        logger.log(Level.SEVERE,
                                   "Failed to apply {0} to cache {1} because of\n{2}",
                                   new Object[] {event, namedCache.getCacheName(), runtimeException});
                    }

                    throw new IllegalStateException(String.format("When attempting to apply an EntryEvent to cache %s",
                                                                  namedCache.getCacheName()),
                                                    runtimeException);
                }
            }
            else if (event instanceof DistributableCacheInvocationRequestEvent)
            {
                DistributableCacheInvocationRequestEvent invocationRequestEvent =
                    (DistributableCacheInvocationRequestEvent) event;

                invocationRequestEvent.invoke(namedCache);

                distributionCount++;
            }
            else
            {
                // we don't know the type of event here, so ignore it
                if (logger.isLoggable(Level.FINEST))
                {
                    logger.log(Level.FINEST, "Unknown Event Type {0}.  Will ignore.", event);
                }
            }
        }

        return distributionCount;
    }
}
