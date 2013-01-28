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

package com.oracle.coherence.patterns.pushreplication.examples.activeactive;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.processor.ExtractorProcessor;

import java.util.Random;

/**
 * The {@link ActiveActivePublisher1} acts as an application
 * issuing updates which compute a running sum.  Running sums
 * are being maintained on ten entries keyed as strings from "0"
 * to "9".
 * <p>
 * All updates are computed in the local cluster ("Site1") and are
 * propagated to another cluster ("Site2") which is has its own
 * publisher issuing similar updates. When both sets of publishers finish,
 * the running totals on both clusters should converge and reflect
 * the same numbers.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Robert Hanckel
 */

public class ActiveActivePublisher1
{
    static Random m_generator = new Random(System.currentTimeMillis());


    static private long getRandomLong(long upperBound)
    {
        long randomLong = m_generator.nextLong();

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

        // Seed the cache with 10 running sums

        for (long i = 0; i < 10; i++)
        {
            System.out.println("Seeding the cache with running sum object key = [" + i + "]");
            namedCache.put("" + i, new ActiveActiveRunningSum());
        }

        long totalUpdates = 0;

        do
        {
            long itemKey = totalUpdates % 10;

            Long currentUpdateValue = (Long) namedCache.invoke("" + itemKey,
                                                               new ExtractorProcessor("getLatestUpdateValue"));

            if (currentUpdateValue != null)
            {
                long randomLong = getRandomLong(1000);

                System.out.println("Updating the cache with running sum object key = [" + itemKey + "] and value ["
                                   + randomLong + "]");
                namedCache.invoke("" + itemKey, new ActiveActiveComputeSum(randomLong));
                totalUpdates++;
                System.out.println("Total updates = [" + totalUpdates + "]");
            }
            else
            {
                System.out.println("Null item for = [" + itemKey + "]. Skipping update");
            }

            try
            {
                Thread.sleep(10);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        while (totalUpdates < 10000);

        String doneKey = "Site1Done";

        namedCache.put(doneKey, new ActiveActiveRunningSum());
        namedCache.invoke(doneKey, new ActiveActiveComputeSum(0));

        System.out.println("All local updates have been issued");

        boolean isDone;

        do
        {
            isDone = true;

            for (long i = 0; i < 10; i++)
            {
                System.out.println("Getting the running sum object key = [" + i + "]");

                ActiveActiveRunningSum runningSum = (ActiveActiveRunningSum) namedCache.get("" + i);

                System.out.println("...Running sum  = [" + runningSum.m_runningSum + "]");
                System.out.println("...Update count = [" + runningSum.m_updateCount + "]");

                if (runningSum.m_updateCount != 2000)
                {
                    isDone = false;
                }
            }

            Thread.sleep(10000);

        }
        while (!isDone);

        // Load done marker
        namedCache.invoke(doneKey, new ActiveActiveComputeSum(-1));

        /*
         * ActiveActiveRunningSum site1done = (ActiveActiveRunningSum) namedCache.get(doneKey);
         * System.out.println("...Site1Done sum   = [" + site1done.m_runningSum + "]");
         * System.out.println("...Site1Done count = [" + site1done.m_updateCount + "]");
         */

        System.out.println("All updates have been received.");
    }
}
