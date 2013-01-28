/*
 * File: PublisherHelper.java
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

import com.tangosol.util.Base;

/**
 * The PublisherHelper provides the facilities to register and
 * populate caches used as a part of the push replication examples.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Makin
 */
public class PublisherHelper
{
    static int             suffix           = 1;

    private ListenerHelper m_listenerHelper = null;
    private String         m_localCacheName;
    private String         m_localSiteName;


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
    public PublisherHelper(String localCacheName,
                           String localSiteName)
    {
        m_localCacheName = localCacheName;
        m_localSiteName  = localSiteName;
        m_listenerHelper = new ListenerHelper(localCacheName, localSiteName);
    }


    /**
     * Determines if the listener is done listening.
     *
     * @return
     */
    public boolean isDoneListening()
    {
        return m_listenerHelper.isDoneListening();
    }


    /**
     * Adds a listener.
     */
    public void addListener()
    {
        m_listenerHelper.addListener();
    }


    /**
     * Load the cache with test data.
     */
    public void loadCache()
    {
        // let's do some work with the active site cache to insert/update/delete
        // (ie: EntryOperations) can be "pushed" to the passive site using
        // the above registered publisher.
        NamedCache namedCache = CacheFactory.getCache(m_localCacheName);

        long       MAX        = 10000;

        System.out.printf("Commenced: putting %d entries into cache '%s'\n", MAX, m_localCacheName);

        namedCache.put(m_localSiteName + "Key", new ActiveExampleValue(m_localSiteName, 1));

        long ldtStart = Base.getSafeTimeMillis();

        for (long i = 1; i <= MAX; i++)
        {
            namedCache.put("" + (i % 10), new ActiveExampleValue(m_localSiteName, i));

            try
            {
                Thread.sleep(10);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        long ldtEnd = Base.getSafeTimeMillis();

        // Load done marker
        namedCache.put(m_localSiteName + "Key", new ActiveExampleValue(m_localSiteName, -1));

        System.out.printf("Completed: putting %d entries into cache '%s'\n", MAX, m_localCacheName);
        System.out.printf("Total time to put %d entries into cache '%s': %d (ms)  (includes delibrate delays)\n",
                          MAX,
                          m_localCacheName,
                          (ldtEnd - ldtStart));

    }
}
