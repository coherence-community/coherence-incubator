/*
 * File: SimpleClient.java
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

package com.oracle.scratch.client;

import com.oracle.coherence.common.resourcing.AbstractDeferredResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceUnavailableException;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: narliss
 * Date: 2/16/13
 * Time: 6:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleClient
{
    private static final long TEST_TIMEOUT_MS = 120000;    // two minutes


    /**
     * Method description
     *
     * @param arguments
     */
    public static void main(String[] arguments) throws IOException
        {
        System.out.println("-----------------------------------------------------------");
        System.out.println("Oracle Simple Client Runner");
        System.out.println("Copyright (c) 2013. Oracle Corporation");
        System.out.println("-----------------------------------------------------------");
        System.out.println();

        // ensure that there's only a single argument
        if (arguments.length == 1)
        {
            // determine the application properties file name
            String propertiesFileName = arguments[0].trim();

            // warn if the file name doesn't end in properties
            if (!propertiesFileName.endsWith(".properties"))
            {
                System.out
                    .printf("WARNING: The specified application properties file [%s] doesn't end in .properties\n",
                            propertiesFileName);
            }

            try
            {
                // attempt to load the properties (via the classpath)
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(ClassLoader
                        .getSystemResourceAsStream(propertiesFileName)));

                Properties applicationProperties = new Properties();

                applicationProperties.load(reader);

                reader.close();

                // determine the application class name (for main)
                String testMethodName = applicationProperties.getProperty("testname");

                if (testMethodName == "runPutClient")
                    {
                    String sourcePort = applicationProperties.getProperty("sourcePort");

                    if (sourcePort == null)
                        {
                        throw new IllegalArgumentException("sourcePort missing from properties file");
                        }

                    String destinationPort = applicationProperties.getProperty("destinationPort");

                    if (destinationPort == null)
                        {
                        throw new IllegalArgumentException("destinationPort missing from properties file");
                        }

                    String cacheConfig = applicationProperties.getProperty("cacheConfig");

                    if (cacheConfig == null)
                        {
                        throw new IllegalArgumentException("cacheConfig missing from properties file");
                        }

                    String cacheName = applicationProperties.getProperty("cacheName");

                    if (cacheName == null)
                        {
                        throw new IllegalArgumentException("cacheName missing from properties file");
                        }

                    String entriesPerIterationString = applicationProperties.getProperty("entriesPerIteration");

                    if (entriesPerIterationString == null)
                        {
                        throw new IllegalArgumentException("entriesPerIteration missing from properties file");
                        }

                    int entriesPerIteration = Integer.parseInt(entriesPerIterationString);

                    String publishIterationsString = applicationProperties.getProperty("publishIterations");

                    if (publishIterationsString == null)
                        {
                        throw new IllegalArgumentException("publishIterations missing from properties file");
                        }

                    int publishIterations = Integer.parseInt(publishIterationsString);

                    String usePutAllString = applicationProperties.getProperty("usePutAll");

                    if (usePutAllString == null)
                        {
                        throw new IllegalArgumentException("usePutAll missing from properties file");
                        }

                    boolean usePutAll = Boolean.parseBoolean(usePutAllString);

                    String timeoutMillisString = applicationProperties.getProperty("timeoutMillis");

                    if (timeoutMillisString == null)
                        {
                        throw new IllegalArgumentException("timeoutMillis missing from properties file");
                        }

                    long lTimeoutMillis = Long.parseLong(timeoutMillisString);

                    runPutClient(sourcePort, destinationPort, cacheConfig, cacheName, entriesPerIteration, publishIterations,
                            usePutAll, lTimeoutMillis);
                    }
            }
            catch (Exception e)
            {
                System.out.printf("ERROR: Unable to loaded the application properties file [%s] due to:\n%s",
                                  propertiesFileName,
                                  e);
            }
        }
        else
        {
            System.out
                .println("Oracle Java Application Runner requires a single parameter, specifying a .properties file on the classpath");
        }

    InputStreamReader converter = new InputStreamReader(System.in);
    BufferedReader in = new BufferedReader(converter);

    in.readLine();
    }


    /**
     * Method description
     *
     * @param sourcePort
     * @param destinationPort
     * @param cacheConfig
     * @param cacheName
     * @param entriesPerIteration
     * @param publishIterations
     * @param usePutAll
     * @param timeoutMillis
     */
    public static void runPutClient(String sourcePort,
                             String       destinationPort,
                             String       cacheConfig,
                             final String cacheName,
                             int          entriesPerIteration,
                             int          publishIterations,
                             boolean      usePutAll,
                             long         timeoutMillis)
    {
        // turn off local clustering
        System.setProperty("tangosol.coherence.tcmp.enabled", "false");
        System.setProperty("remote.port", sourcePort);
        System.setProperty("tangosol.pof.config", "test-pof-config.xml");

        // Connect to source site
        System.out.println("Connecting to Primary Site");
        CacheFactory.setConfigurableCacheFactory(new DefaultConfigurableCacheFactory(cacheConfig));

        long lStartTime = System.currentTimeMillis();

        for (int x = 0; x < publishIterations; ++x)
        {
            populateNamedCache(CacheFactory.getCache(cacheName),
                               entriesPerIteration,
                               (x == 0 ? 0 : x * entriesPerIteration),
                               usePutAll);
        }

        // grab a copy of the active site entries (we'll use this for comparison later)
        final Map<Object, Object> activeCache = new HashMap<Object, Object>(CacheFactory.getCache(cacheName));

        // shutdown our connection to the active site
        CacheFactory.shutdown();

        // assert that the values have arrived in the remote site
        System.out.printf("Connecting to PASSIVE site.\n");
        System.setProperty("remote.port", destinationPort);
        CacheFactory.setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

        // construct a deferred NamedCache provider that is only available when the caches are identical
        ResourceProvider<NamedCache> deferredNamedCache =
            new AbstractDeferredResourceProvider<NamedCache>("PASSIVE publishing-cache",
                                                             200,
                                                             timeoutMillis)
        {
            @Override
            protected NamedCache ensureResource() throws ResourceUnavailableException
            {
                NamedCache namedCache = CacheFactory.getCache(cacheName);

                return sameMaps(activeCache, namedCache) ? namedCache : null;
            }
        };

        System.out.printf("\nWaiting for %d entries to synchronize between ACTIVE and PASSIVE site.\n",
                          activeCache.size());

        NamedCache namedCache = deferredNamedCache.getResource();

        if (namedCache == null)
        {
            System.out.println("**Error - objects didn't synchronize between sites in timeout");
        }
        else
        {
            long lFinishTime = System.currentTimeMillis();
            long lTestTime   = lFinishTime - lStartTime;

            System.out.printf("Test completed replicating %d entries in %d millis\n",
                              (entriesPerIteration * publishIterations),
                              lFinishTime);
        }
    }


    /**
     * Method description
     *
     * @param namedCache
     * @param nrEntries
     * @param nMinKey
     * @param fUsePutAll
     *
     * @return
     */
    public static int populateNamedCache(NamedCache namedCache,
                                  int        nrEntries,
                                  int        nMinKey,
                                  boolean    fUsePutAll)
    {
        // insert a number of random entries
        System.out.printf("Inserting %d entries.\n", nrEntries);

        @SuppressWarnings("unchecked") Map<Integer, String> mapInserts = fUsePutAll
                                                                         ? new HashMap<Integer, String>()
                                                                         : (Map<Integer, String>) namedCache;

        String value =
            "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

        for (int i = nMinKey; i < (nMinKey + nrEntries); i++)
        {
            int key = i;

            // String value = randomString(random, "abcdefghijklmnopqrstuvwxyz", 20);

            mapInserts.put(key, value);
        }

        if (fUsePutAll)
        {
            namedCache.putAll(mapInserts);
        }

        System.out.printf("Populated %d actual entries.\n", nrEntries);

        return nrEntries;
    }


    /**
     * Method description
     *
     * @param map1
     * @param map2
     *
     * @return
     */
    public static boolean sameMaps(Map map1,
                            Map map2)
    {
        if (map1.size() == map2.size())
        {
            boolean same = true;

            for (Iterator i = map1.keySet().iterator(); same && i.hasNext(); )
            {
                Object key    = i.next();
                Object value1 = map1.get(key);
                Object value2 = map2.get(key);

                same = (value1 == value2) || (value1 != null && value1.equals(value2));
            }

            for (Iterator i = map2.keySet().iterator(); same && i.hasNext(); )
            {
                Object key    = i.next();
                Object value1 = map1.get(key);
                Object value2 = map2.get(key);

                same = (value1 == value2) || (value2 != null && value2.equals(value1));
            }

            return same;
        }
        else
        {
            return false;
        }
    }
}
