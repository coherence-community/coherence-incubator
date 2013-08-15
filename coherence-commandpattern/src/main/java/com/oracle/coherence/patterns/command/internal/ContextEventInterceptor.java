/*
 * File: ContextEventInterceptor.java
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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.identifiers.Identifier;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.PartitionedService;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;

import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link ContextEventInterceptor} is the {@link EventInterceptor}
 * to handle {@link Event}s that occur on Command Contexts (implemented
 * by {@link ContextWrapper}).
 * <p>
 * This implementation is inspired by and based upon the concepts
 * provided by the ContextBackingMapListener implementation of Coherence
 * Incubator releases 8, 9, 10 and 11.  It has been refactored here to adopt
 * the new Unified (Server Side) Event Model introduced in Coherence 12.1.2.
 * <p>
 * Copyright (c) 2012. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Interceptor(
    identifier     = "command-contexts-interceptor",
    entryEvents    = {EntryEvent.Type.INSERTING, EntryEvent.Type.REMOVING},
    transferEvents = {TransferEvent.Type.ARRIVED, TransferEvent.Type.DEPARTING}
)
public class ContextEventInterceptor implements EventInterceptor
{
    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(ContextEventInterceptor.class.getName());

    /**
     * The name of the Cache to which the EventInterceptor has been associated
     */
    private String m_cacheName;


    /**
     * Sets the cache name on which this interceptor has been registered.
     *
     * @param cacheName
     */
    @Injectable("cache-name")
    public void setCacheName(String cacheName)
    {
        m_cacheName = cacheName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(Event event)
    {
        // determine the type of event and entries to process
        EntryEvent.Type  type             = null;
        Set<BinaryEntry> setBinaryEntries = null;

        if (event instanceof EntryEvent)
        {
            EntryEvent entryEvent = (EntryEvent) event;

            setBinaryEntries = entryEvent.getEntrySet();
            type             = (EntryEvent.Type) entryEvent.getType();
        }
        else if (event instanceof TransferEvent)
        {
            TransferEvent transferEvent = (TransferEvent) event;

            setBinaryEntries = transferEvent.getEntries().get(m_cacheName);

            if (transferEvent.getType() == TransferEvent.Type.ARRIVED)
            {
                type = EntryEvent.Type.INSERTING;
            }
            else if (transferEvent.getType() == TransferEvent.Type.DEPARTING)
            {
                type = EntryEvent.Type.REMOVING;
            }
        }

        if (setBinaryEntries != null)
        {
            switch (type)
            {
            case INSERTING :

                for (BinaryEntry binEntry : setBinaryEntries)
                {
                    Identifier contextIdentifier = (Identifier) binEntry.getKey();

                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.log(Level.FINEST, "Context %s is being inserted into this member", contextIdentifier);
                    }

                    CommandExecutor executor = CommandExecutorManager.ensure().ensureCommandExecutor(contextIdentifier,
                                                                                                     (PartitionedService) binEntry
                                                                                                         .getContext()
                                                                                                         .getCacheService());

                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.log(Level.FINEST, "Scheduling ContextExecutor for %s to start", contextIdentifier);
                    }

                    executor.start();
                }

                break;

            case REMOVING :

                for (BinaryEntry binEntry : setBinaryEntries)
                {
                    Identifier contextIdentifier = (Identifier) binEntry.getKey();

                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.log(Level.FINEST, "Context %s is being removed from this member", contextIdentifier);
                    }

                    CommandExecutor executor = CommandExecutorManager.ensure().getCommandExecutor(contextIdentifier);

                    if (executor != null)
                    {
                        executor.stop();
                    }

                }

                break;

            default :

            // do nothing if the event is unknown
            }
        }
    }
}
