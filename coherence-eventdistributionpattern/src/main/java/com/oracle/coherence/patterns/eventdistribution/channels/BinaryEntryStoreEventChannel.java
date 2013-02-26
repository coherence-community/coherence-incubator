/*
 * File: BinaryEntryStoreEventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.EntryInsertedEvent;
import com.oracle.coherence.common.events.EntryRemovedEvent;
import com.oracle.coherence.common.events.EntryUpdatedEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A {@link BinaryEntryStoreEventChannel} is an {@link EventChannel} implementation that allows
 * regular Coherence {@link BinaryEntryStore}s to be used as {@link EventChannel}s.
 * <p>
 * This allows pre-existing {@link BinaryEntryStore}s to be configured and used with the
 * Event Distribution Pattern.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 */
public class BinaryEntryStoreEventChannel implements EventChannel
{
    /**
     * The name of the {@link NamedCache} that can be used to provide a {@link BackingMapManagerContext}
     * for deserializing {@link EntryEvent}s.
     */
    private String contextCacheName;

    /**
     * The {@link BinaryEntryStore} to adapt.
     */
    private BinaryEntryStore binaryEntryStore;


    /**
     * Standard Constructor.
     *
     * @param contextCacheName The name of the {@link NamedCache} that can be used to provide
     *                         a {@link BackingMapManagerContext}.
     * @param binaryEntryStore The {@link BinaryEntryStore} to adapt.
     */
    public BinaryEntryStoreEventChannel(String           contextCacheName,
                                        BinaryEntryStore binaryEntryStore)
    {
        this.contextCacheName = contextCacheName;
        this.binaryEntryStore = binaryEntryStore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(EventDistributor.Identifier       distributorIdentifer,
                        EventChannelController.Identifier controllerIdentifier) throws EventChannelNotReadyException
    {
        // deliberately empty - no connection setup is required.
        // we assume the cache store will do this.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect()
    {
        // deliberately empty - no  disconnection is required.
        // we assume the cache store will do this.
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public int send(Iterator<Event> events)
    {
        int                          distributionCount = 0;

        HashMap<Binary, BinaryEntry> modifiedEntries   = new HashMap<Binary, BinaryEntry>();
        HashMap<Binary, BinaryEntry> removedEntries    = new HashMap<Binary, BinaryEntry>();

        // determine a BackingMapManagerContext so we can deserialize EventEntries if required
        NamedCache               contextNamedCache   = CacheFactory.getCache(contextCacheName);
        CacheService             contextCacheService = (CacheService) contextNamedCache.getCacheService();
        BackingMapManagerContext context             = contextCacheService.getBackingMapManager().getContext();

        while (events.hasNext())
        {
            Event event = events.next();

            if (event instanceof DistributableEntryEvent)
            {
                // set the context in case (de)serialization needs to occur
                DistributableEntryEvent entryEvent = (DistributableEntryEvent) event;

                entryEvent.getEntry().setContext(context);

                Binary      binKey   = entryEvent.getEntry().getBinaryKey();
                BinaryEntry binEntry = entryEvent.getEntry();

                if (entryEvent instanceof EntryRemovedEvent)
                {
                    removedEntries.put(binKey, binEntry);
                    modifiedEntries.remove(binKey);
                }
                else if (entryEvent instanceof EntryInsertedEvent || entryEvent instanceof EntryUpdatedEvent)
                {
                    modifiedEntries.put(binKey, binEntry);
                    removedEntries.remove(binKey);
                }

                distributionCount++;
            }
        }

        if (removedEntries.size() > 0)
        {
            binaryEntryStore.eraseAll(new HashSet(removedEntries.values()));
        }

        if (modifiedEntries.size() > 0)
        {
            binaryEntryStore.storeAll(new HashSet(modifiedEntries.values()));
        }

        return distributionCount;
    }
}
