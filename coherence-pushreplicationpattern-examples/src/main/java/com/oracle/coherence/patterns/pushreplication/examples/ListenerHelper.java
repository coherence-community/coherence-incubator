/*
 * File: ListenerHelper.java
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

package com.oracle.coherence.patterns.pushreplication.examples;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;

import java.util.HashMap;

/**
 * A helper to register and output cache events.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Makin
 */
public class ListenerHelper
{
    private int                     m_numSenders = 0;
    private HashMap<String, Sender> m_senderMap  = new HashMap<String, Sender>();
    private String                  m_localCacheName;
    private String                  m_localSiteName;
    private boolean                 m_done = false;


    /**
     * Initialize Local Configuration data
     *
     * @param localCacheName this is the name of the "active site" cache (the source of
     * EntryOperations to publish) ie: the source of EntryOperations from which we are publishing
     *
     * @param localSiteName this is the name of the "local site" cache to which EntryOperations
     *  from the "active site" cache will be published.
     *
     */
    public ListenerHelper(String localCacheName,
                          String localSiteName)
    {
        m_localCacheName = localCacheName;
        m_localSiteName  = localSiteName;
    }


    /**
     * Determine if listening has been completed.
     *
     * @return if listening has been completed.
     */
    public synchronized boolean isDoneListening()
    {
        return m_done;
    }


    /**
     * Sets that listening has completed.
     */
    public synchronized void setDoneListening()
    {
        m_done = true;
    }


    /**
     * Add a map listener that will print out any value that is inserted
     * or updated
     */
    public void addListener()
    {
        NamedCache activeCache = CacheFactory.getCache(m_localCacheName);

        activeCache.addMapListener(new MultiplexingMapListener()
        {
            protected void onMapEvent(MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED || mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                {
                    ActiveExampleValue val        = (ActiveExampleValue) mapEvent.getNewValue();
                    String             siteOrigin = val.getSiteOrigin();

                    // Only count events that were replicated from the remote cluster
                    //
                    if (!m_localSiteName.equals(siteOrigin))
                    {
                        Sender sender = m_senderMap.get(siteOrigin);

                        if (sender == null)
                        {
                            m_numSenders++;
                            sender = new Sender();
                            m_senderMap.put(siteOrigin, sender);
                            System.out.println("Object sequence number " + val.getSequenceNumber()
                                               + " received from site [" + val.getSiteOrigin() + "]");
                        }

                        if (val.getValue() == -1)
                        {
                            System.out.println("Done receiving from site [" + siteOrigin + "]");

                            if (--m_numSenders == 0)
                            {
                                setDoneListening();

                                return;
                            }
                        }

                        sender.m_numEvents++;

                        if (val.getSequenceNumber() % 100 == 0)
                        {
                            System.out.println("Object sequence number " + val.getSequenceNumber()
                                               + " received from site [" + val.getSiteOrigin() + "]");
                        }
                    }
                }
            }
        });

    }


    private static class Sender
    {
        long m_numEvents = 0;
    }
}
