/*
 * File: ActiveActivePublisher1.java
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

package com.oracle.coherence.patterns.pushreplication.examples.activeactive;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Random;

/**
 * The {@link ActiveActiveUpdater} is an application that updates values for
 * a particular site, the values of which are summed.
 * <p>
 * All updates are computed in the local cluster ("Site1") and are
 * propagated to another cluster ("Site2") which is has its own
 * updater issuing similar updates. When both all updates are completed, on all
 * sites, the running totals will eventually become consistent.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */

public class ActiveActiveUpdater
{
    static Random m_generator = new Random(System.currentTimeMillis());


    static private long getRandomLong(long upperBound)
    {
        long randomLong = Math.abs(m_generator.nextLong());

        return (randomLong % upperBound);
    }


    /**
     * Entrance point for the example
     *
     * @param args
     *
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {
        NamedCache namedCache = CacheFactory.getCache("publishing-cache");

        long totalUpdates = 0;

        do
        {
            long itemKey    = totalUpdates % 10;

            long randomLong = getRandomLong(1000);

            System.out.println("Updating the cache with running sum object key = [" + itemKey + "] and value ["
                               + randomLong + "]");
            namedCache.invoke("" + itemKey, new ActiveActiveUpdaterProcessor(randomLong));

            totalUpdates++;

            System.out.println("Total updates = [" + totalUpdates + "]");

            try
            {
                Thread.sleep(getRandomLong(100));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        while (totalUpdates < 100);

        System.out.println("All local updates have been issued");
    }
}
