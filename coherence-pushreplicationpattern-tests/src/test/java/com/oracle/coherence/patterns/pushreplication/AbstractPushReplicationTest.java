/*
 * File: AbstractPushReplicationTest.java
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

package com.oracle.coherence.patterns.pushreplication;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.deferred.Deferred;
import com.oracle.bedrock.deferred.Eventually;
import com.oracle.bedrock.deferred.Repetitively;
import com.oracle.bedrock.deferred.options.RetryFrequency;
import com.oracle.bedrock.matchers.Equivalence;
import com.oracle.bedrock.matchers.MapMatcher;
import com.oracle.bedrock.matchers.PartitionSetMatcher;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationListener;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.Clustering;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.SiteName;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.EnvironmentVariables;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.patterns.domain.DomainKey;
import com.oracle.coherence.patterns.domain.DomainValue;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerMBean;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.filters.ExampleEventFilter;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Filter;
import org.hamcrest.Matcher;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.valueOf;
import static com.oracle.bedrock.matchers.EntrySetMatcher.sameAs;
import static org.hamcrest.core.Is.is;

/**
 * The {@link AbstractPushReplicationTest} defines a set of standard tests
 * that may be extended and used to test a variety of
 * {@link EventDistributor} implementations.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractPushReplicationTest
{
    /**
     * Determines the {@link AvailablePortIterator} to use for locating free ports on the host.
     *
     * @return {@link AvailablePortIterator}
     */
    protected static AvailablePortIterator getAvailablePortIterator()
    {
        return LocalPlatform.get().getAvailablePorts();
    }


    /**
     * Constructs a new {@link OptionsByType} on which sub-classes of this class can be configured.
     *
     * @return {@link OptionsByType}
     */
    protected OptionsByType newBaseCacheServerOptions(Capture<Integer> clusterPort)
    {
        OptionsByType optionsByType = OptionsByType.empty();

        // establish Bedrock options
        optionsByType.add(Console.system());

        // establish Operating System options
        optionsByType.add(EnvironmentVariables.inherited());

        // establish Java options
        optionsByType.add(IPv4Preferred.yes());
        optionsByType.add(JmxFeature.enabled());

        // establish Coherence options
        optionsByType.add(LocalHost.only());
        optionsByType.add(ClusterPort.of(clusterPort));
        optionsByType.add(Pof.config("test-pof-config.xml"));
        optionsByType.add(JMXManagementMode.ALL);
        optionsByType.add(SystemProperty.of("remote.address",
                                            LocalPlatform.get().getLoopbackAddress().getHostAddress()));
        optionsByType.add(SystemProperty.of("proxy.address",
                                            LocalPlatform.get().getLoopbackAddress().getHostAddress()));

        return optionsByType;
    }


    protected abstract OptionsByType newPassiveCacheServerOptions(Capture<Integer> clusterPort);


    protected abstract OptionsByType newActiveCacheServerOptions(Capture<Integer> clusterPort);


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-active topology are distributed between the active sites.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testActiveActive() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // establish the newyork server options
            OptionsByType newyorkServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            newyorkServerOptions.add(SiteName.of("newyork"));
            newyorkServerOptions.add(DisplayName.of("newyork"));
            newyorkServerOptions.add(SystemProperty.of("proxy.address",
                                                       platform.getLoopbackAddress().getHostAddress()));
            newyorkServerOptions.add(SystemProperty.of("proxy.port", londonServer.getSystemProperty("remote.port")));
            newyorkServerOptions.add(SystemProperty.of("remote.port", londonServer.getSystemProperty("proxy.port")));

            try (CoherenceCacheServer newyorkServer = platform.launch(CoherenceCacheServer.class,
                                                                      newyorkServerOptions.asArray()))
            {
                // populate the newyork site
                NamedCache newyorkCache = newyorkServer.getCache(cacheName);

                randomlyPopulateNamedCache(newyorkCache, 0, 500, "NEWYORK");

                System.out.printf("\nWaiting for %d entries to synchronize between NEWYORK to LONDON.\n",
                                  newyorkCache.size());

                NamedCache londonCache = londonServer.getCache(cacheName);

                Eventually.assertThat(invoking(londonCache).entrySet(), sameAs(newyorkCache.entrySet()));

                // assert that nothing has been queued in London (from New York)
                // (even though this is Active-Active, events from New York to London should not be
                // re-queued in London for distribution back to New York).
                JmxFeature londonJmx = londonServer.get(JmxFeature.class);

                Deferred<Integer> deferredMBeanAttribute =
                    londonJmx.getDeferredMBeanAttribute(new ObjectName("Coherence:type=EventChannels,id=publishing-cache-Remote Cluster Channel,nodeId=1"),
                                                        "EventsDistributedCount",
                                                        Integer.class);

                Eventually.assertThat(valueOf(deferredMBeanAttribute), is(0));

                // update the london site
                randomlyPopulateNamedCache(londonCache, 1000, 500, "LONDON");

                System.out.printf("\nWaiting for %d entries to synchronize between LONDON and NEWYORK.\n",
                                  londonCache.size());

                Eventually.assertThat(invoking(newyorkCache).entrySet(), sameAs(londonCache.entrySet()));
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-passive topology are distributed from the active site to the passive site.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassive() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the passive server options
        OptionsByType passiveCacheServerOptions =
            newPassiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        passiveCacheServerOptions.add(SiteName.of("passive"));
        passiveCacheServerOptions.add(DisplayName.of("passive"));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.address",
                                                        platform.getLoopbackAddress().getHostAddress()));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        passiveCacheServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer passiveServer = platform.launch(CoherenceCacheServer.class,
                                                                  passiveCacheServerOptions.asArray()))
        {
            // establish the active server options
            OptionsByType activeServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            activeServerOptions.add(SiteName.of("active"));
            activeServerOptions.add(DisplayName.of("active"));
            activeServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
            activeServerOptions.add(SystemProperty.of("proxy.port", passiveServer.getSystemProperty("remote.port")));
            activeServerOptions.add(SystemProperty.of("remote.port", passiveServer.getSystemProperty("proxy.port")));

            try (CoherenceCacheServer activeServer = platform.launch(CoherenceCacheServer.class,
                                                                     activeServerOptions.asArray()))
            {
                // populate the active site
                NamedCache activeCache = activeServer.getCache(cacheName);

                randomlyPopulateNamedCache(activeCache, 0, 500, "ACTIVE");

                System.out.printf("\nWaiting for %d entries to synchronize from ACTIVE to PASSIVE.\n",
                                  activeCache.size());

                NamedCache passiveCache = passiveServer.getCache(cacheName);

                Eventually.assertThat(invoking(passiveCache).entrySet(), sameAs(activeCache.entrySet()));
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-passive topology are not distributed from the active site to the passive site
     * when distribution is disabled.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassiveDisabled() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the passive server options
        OptionsByType passiveCacheServerOptions =
            newPassiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        passiveCacheServerOptions.add(SiteName.of("passive"));
        passiveCacheServerOptions.add(DisplayName.of("passive"));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.address",
                                                        platform.getLoopbackAddress().getHostAddress()));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        passiveCacheServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer passiveServer = platform.launch(CoherenceCacheServer.class,
                                                                  passiveCacheServerOptions.asArray()))
        {
            // establish the active server options
            OptionsByType activeServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            activeServerOptions.add(SiteName.of("active"));
            activeServerOptions.add(DisplayName.of("active"));
            activeServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
            activeServerOptions.add(SystemProperty.of("proxy.port", passiveServer.getSystemProperty("remote.port")));
            activeServerOptions.add(SystemProperty.of("remote.port", passiveServer.getSystemProperty("proxy.port")));

            activeServerOptions.add(SystemProperty.of("channel.starting.mode", "disabled"));

            try (CoherenceCacheServer activeServer = platform.launch(CoherenceCacheServer.class,
                                                                     activeServerOptions.asArray()))
            {
                // populate the active site
                NamedCache activeCache = activeServer.getCache(cacheName);

                // put a value in the active site
                activeCache.put("message", "hello world");

                System.out.printf("Ensuring ACTIVE and PASSIVE sites don't become synchronized.\n");

                NamedCache passiveCache = passiveServer.getCache(cacheName);

                Repetitively.assertThat(invoking(passiveCache).size(),
                                        is(0),
                                        Timeout.of(10, TimeUnit.SECONDS),
                                        RetryFrequency.every(250, TimeUnit.MILLISECONDS));
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-passive topology are distributed from the active site to the passive site
     * and in the process, are transformed according to a transformer.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassiveEventTransformation() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the passive server options
        OptionsByType passiveCacheServerOptions =
            newPassiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        passiveCacheServerOptions.add(SiteName.of("passive"));
        passiveCacheServerOptions.add(DisplayName.of("passive"));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.address",
                                                        platform.getLoopbackAddress().getHostAddress()));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        passiveCacheServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer passiveServer = platform.launch(CoherenceCacheServer.class,
                                                                  passiveCacheServerOptions.asArray()))
        {
            // establish the active server options
            OptionsByType activeServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            activeServerOptions.add(SiteName.of("active"));
            activeServerOptions.add(DisplayName.of("active"));
            activeServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
            activeServerOptions.add(SystemProperty.of("proxy.port", passiveServer.getSystemProperty("remote.port")));
            activeServerOptions.add(SystemProperty.of("remote.port", passiveServer.getSystemProperty("proxy.port")));

            activeServerOptions.add(CacheConfig.of("test-remotecluster-eventtransformation-cache-config.xml"));

            try (CoherenceCacheServer activeServer = platform.launch(CoherenceCacheServer.class,
                                                                     activeServerOptions.asArray()))
            {
                // populate the active site
                NamedCache activeCache = activeServer.getCache(cacheName);

                randomlyPopulateNamedCache(activeCache, 0, 500, "ACTIVE");

                System.out.printf("\nWaiting for %d entries to synchronize from ACTIVE to PASSIVE.\n",
                                  activeCache.size());

                NamedCache passiveCache = passiveServer.getCache(cacheName);

                Eventually.assertThat(invoking(passiveCache).entrySet(),
                                      sameAs(activeCache.entrySet(), Equivalence.EQUALS_IGNORE_CASE));
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-passive topology are distributed from the active site to the passive site
     * in accordance with a configured filter.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassiveEventFiltering() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the passive server options
        OptionsByType passiveCacheServerOptions =
            newPassiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        passiveCacheServerOptions.add(SiteName.of("passive"));
        passiveCacheServerOptions.add(DisplayName.of("passive"));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.address",
                                                        platform.getLoopbackAddress().getHostAddress()));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        passiveCacheServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer passiveServer = platform.launch(CoherenceCacheServer.class,
                                                                  passiveCacheServerOptions.asArray()))
        {
            // establish the active server options
            OptionsByType activeServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            activeServerOptions.add(SiteName.of("active"));
            activeServerOptions.add(DisplayName.of("active"));
            activeServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
            activeServerOptions.add(SystemProperty.of("proxy.port", passiveServer.getSystemProperty("remote.port")));
            activeServerOptions.add(SystemProperty.of("remote.port", passiveServer.getSystemProperty("proxy.port")));

            activeServerOptions.add(CacheConfig.of("test-remotecluster-eventfiltering-cache-config.xml"));

            try (CoherenceCacheServer activeServer = platform.launch(CoherenceCacheServer.class,
                                                                     activeServerOptions.asArray()))
            {
                // populate the active site
                NamedCache activeCache = activeServer.getCache(cacheName);

                randomlyPopulateNamedCache(activeCache, 0, 500, "ACTIVE");

                // grab a copy of the active cache entries so we can filter them locally
                // (we'll use this for comparison later)
                final Map activeMap = new HashMap<>(activeCache);

                // remove entries that don't satisfy the filter
                System.out.printf("There are %d entries in the active cache\n", activeCache.size());

                Filter            filter       = new ExampleEventFilter();
                ArrayList<Object> keysToRemove = new ArrayList<Object>();

                for (Object key : activeMap.keySet())
                {
                    String value = (String) activeMap.get(key);

                    if (!filter.evaluate(value))
                    {
                        keysToRemove.add(key);
                    }
                }

                for (Object key : keysToRemove)
                {
                    activeMap.remove(key);
                }

                System.out.printf("There should be %d entries in the passive cache\n", activeMap.size());

                NamedCache passiveCache = passiveServer.getCache(cacheName);

                Eventually.assertThat(invoking(passiveCache).entrySet(),
                                      sameAs(activeMap.entrySet(), Equivalence.EQUALS_IGNORE_CASE));

                System.out.println("The passive cache contains " + passiveCache.size() + " entries");
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-passive topology are not distributed from the active site to the passive site
     * when distribution is disabled, but later is when they are "propagated".
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassivePropagation() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the passive server options
        OptionsByType passiveCacheServerOptions =
            newPassiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        passiveCacheServerOptions.add(SiteName.of("passive"));
        passiveCacheServerOptions.add(DisplayName.of("passive"));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.address",
                                                        platform.getLoopbackAddress().getHostAddress()));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        passiveCacheServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer passiveServer = platform.launch(CoherenceCacheServer.class,
                                                                  passiveCacheServerOptions.asArray()))
        {
            // establish the active server options
            OptionsByType activeServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            activeServerOptions.add(SiteName.of("active"));
            activeServerOptions.add(DisplayName.of("active"));
            activeServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
            activeServerOptions.add(SystemProperty.of("proxy.port", passiveServer.getSystemProperty("remote.port")));
            activeServerOptions.add(SystemProperty.of("remote.port", passiveServer.getSystemProperty("proxy.port")));

            activeServerOptions.add(SystemProperty.of("channel.starting.mode", "disabled"));

            try (CoherenceCacheServer activeServer = platform.launch(CoherenceCacheServer.class,
                                                                     activeServerOptions.asArray()))
            {
                // populate the active site
                NamedCache activeCache = activeServer.getCache(cacheName);

                // populate the active site
                randomlyPopulateNamedCache(activeCache, 0, 500, "ACTIVE");

                System.out.printf("Ensuring ACTIVE and PASSIVE sites don't become synchronized.\n");

                NamedCache passiveCache = passiveServer.getCache(cacheName);

                Repetitively.assertThat(invoking(passiveCache).size(),
                                        is(0),
                                        Timeout.of(10, TimeUnit.SECONDS),
                                        RetryFrequency.every(250, TimeUnit.MILLISECONDS));

                System.out.println("Requesting propagation of ACTIVE site to the PASSIVE site");

                // using JMX, ask the active channel to propagate
                JmxFeature jmxActive = activeServer.get(JmxFeature.class);

                EventChannelControllerMBean controllerMBean =
                    jmxActive.getMBeanProxy(new ObjectName("Coherence:type=EventChannels,id=publishing-cache-Remote Cluster Channel,nodeId=1"),
                                            EventChannelControllerMBean.class);

                controllerMBean.propagate();

                System.out.printf("\nWaiting for %d entries to propagate from the ACTIVE site to the PASSIVE site\n",
                                  activeCache.size());

                Eventually.assertThat(invoking(passiveCache).entrySet(), sameAs(activeCache.entrySet()));
            }
        }
    }


    /**
     * Ensure cache entries created with expiry timeouts in {@link NamedCache} configured with an
     * active-passive topology are distributed from the active site to the passive site and that
     * the expiry timeouts are included.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassiveWithExpiry() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the passive server options
        OptionsByType passiveCacheServerOptions =
            newPassiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        passiveCacheServerOptions.add(SiteName.of("passive"));
        passiveCacheServerOptions.add(DisplayName.of("passive"));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.address",
                                                        platform.getLoopbackAddress().getHostAddress()));
        passiveCacheServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        passiveCacheServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        try (CoherenceCacheServer passiveServer = platform.launch(CoherenceCacheServer.class,
                                                                  passiveCacheServerOptions.asArray()))
        {
            // establish the active server options
            OptionsByType activeServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            activeServerOptions.add(SiteName.of("active"));
            activeServerOptions.add(DisplayName.of("active"));
            activeServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
            activeServerOptions.add(SystemProperty.of("proxy.port", passiveServer.getSystemProperty("remote.port")));
            activeServerOptions.add(SystemProperty.of("remote.port", passiveServer.getSystemProperty("proxy.port")));

            try (CoherenceCacheServer activeServer = platform.launch(CoherenceCacheServer.class,
                                                                     activeServerOptions.asArray()))
            {
                // populate the active site
                NamedCache activeCache = activeServer.getCache(cacheName);

                activeCache.put("5", "5", 5000);
                activeCache.put("10", "10", 10000);
                activeCache.put("15", "15", 15000);

                System.out.printf("\nWaiting for entries to synchronize from ACTIVE to PASSIVE.\n", activeCache.size());

                NamedCache passiveCache = passiveServer.getCache(cacheName);

                Eventually.assertThat(invoking(passiveCache).get("5"), is((Object) "5"));
                Eventually.assertThat(invoking(passiveCache).get("10"), is((Object) "10"));
                Eventually.assertThat(invoking(passiveCache).get("15"), is((Object) "15"));

                // now we must ensure that the entries have expired
                System.out.printf("\nEnsuring entries have expired in the PASSIVE site.\n");

                Eventually.assertThat(invoking(passiveCache).containsKey("5"), is(false));
                Eventually.assertThat(invoking(passiveCache).containsKey("10"), is(false));
                Eventually.assertThat(invoking(passiveCache).containsKey("15"), is(false));
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-active topology are distributed between the active sites and when conflicts
     * occur, they are resolved using a conflict resolver.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testActiveActiveConflictResolution() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        londonServerOptions.add(SystemProperty.of("conflict.resolver.classname",
                                                  "com.oracle.coherence.patterns.pushreplication.MergingConflictResolver"));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // establish the newyork server options
            OptionsByType newyorkServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

            newyorkServerOptions.add(SiteName.of("newyork"));
            newyorkServerOptions.add(DisplayName.of("newyork"));
            newyorkServerOptions.add(SystemProperty.of("proxy.address",
                                                       platform.getLoopbackAddress().getHostAddress()));
            newyorkServerOptions.add(SystemProperty.of("proxy.port", londonServer.getSystemProperty("remote.port")));
            newyorkServerOptions.add(SystemProperty.of("remote.port", londonServer.getSystemProperty("proxy.port")));

            newyorkServerOptions.add(SystemProperty.of("conflict.resolver.classname",
                                                       "com.oracle.coherence.patterns.pushreplication.MergingConflictResolver"));

            try (CoherenceCacheServer newyorkServer = platform.launch(CoherenceCacheServer.class,
                                                                      newyorkServerOptions.asArray()))
            {
                // place a partition set (containing 0) into NEWYORK
                NamedCache   newyorkCache  = newyorkServer.getCache(cacheName);
                PartitionSet setPartitions = new PartitionSet(3);

                setPartitions.add(0);

                newyorkCache.put("partitions", setPartitions);

                // place a partition set (containing 1) into LONDON
                NamedCache londonCache = londonServer.getCache(cacheName);

                setPartitions = new PartitionSet(3);

                setPartitions.add(1);

                londonCache.put("partitions", setPartitions);

                System.out.println("\nWaiting for NEWYORK to LONDON to synchronize\n");

                // ensure that NEWYORK and LONDON contain merged partition sets
                Eventually.assertThat(invoking(newyorkCache).get("partitions"),
                                      (Matcher) PartitionSetMatcher.contains(0, 1));
                Eventually.assertThat(invoking(londonCache).get("partitions"),
                                      (Matcher) PartitionSetMatcher.contains(0, 1));
            }
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * a cache-store event channel are distributed.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testCacheStoreEventChannel() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        londonServerOptions.add(CacheConfig.of("test-cachestore-eventchannel-cache-config.xml"));
        londonServerOptions.add(Pof.config("test-pof-config.xml"));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // populate the london cache
            NamedCache londonCache = londonServer.getCache(cacheName);

            randomlyPopulateNamedCache(londonCache, 0, 500, "LONDON");

            System.out.printf("\nWaiting distribution to complete.\n");

            NamedCache otherCache = londonServer.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(otherCache).entrySet(), sameAs(londonCache.entrySet()));
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * a binary-entry cache-store event channel are distributed.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testBinaryEntryCacheStoreEventChannel() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        londonServerOptions.add(CacheConfig.of("test-binaryentrystore-eventchannel-cache-config.xml"));
        londonServerOptions.add(Pof.config("test-pof-config.xml"));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // populate the london cache
            NamedCache londonCache = londonServer.getCache(cacheName);

            randomlyPopulateNamedCache(londonCache, 0, 500, "LONDON");

            System.out.printf("\nWaiting distribution to complete.\n");

            NamedCache otherCache = londonServer.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(otherCache).entrySet(), sameAs(londonCache.entrySet()));
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * an async cache-store event channel are distributed.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testAsyncCacheStoreEventChannel() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        londonServerOptions.add(CacheConfig.of("test-cachestore-eventchannel-cache-config.xml"));
        londonServerOptions.add(Pof.config("test-pof-config.xml"));
        londonServerOptions.add(SystemProperty.of("write.behind.delay", 1));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // populate the london cache
            NamedCache londonCache = londonServer.getCache(cacheName);

            randomlyPopulateNamedCache(londonCache, 0, 500, "LONDON");

            System.out.printf("\nWaiting distribution to complete.\n");

            NamedCache otherCache = londonServer.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(otherCache).entrySet(), sameAs(londonCache.entrySet()));
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} (using putAll for inserts and updates)
     * configured with an an async cache-store event channel are distributed.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testAsyncPutAllCacheStoreEventChannel() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        londonServerOptions.add(CacheConfig.of("test-cachestore-eventchannel-cache-config.xml"));
        londonServerOptions.add(Pof.config("test-pof-config.xml"));
        londonServerOptions.add(SystemProperty.of("write.behind.delay", 1));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // populate the london cache
            NamedCache londonCache = londonServer.getCache(cacheName);

            randomlyPopulateNamedCache(londonCache, 0, 500, "LONDON", true);

            System.out.printf("\nWaiting distribution to complete.\n");

            NamedCache otherCache = londonServer.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(otherCache).entrySet(), sameAs(londonCache.entrySet()));
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * a custom event channel are distributed.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testCustomEventChannel() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", platform.getLoopbackAddress().getHostAddress()));
        londonServerOptions.add(SystemProperty.of("proxy.port", getAvailablePortIterator()));
        londonServerOptions.add(SystemProperty.of("remote.port", getAvailablePortIterator()));

        londonServerOptions.add(CacheConfig.of("test-custom-eventchannel-cache-config.xml"));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            // populate the london cache
            NamedCache londonCache = londonServer.getCache(cacheName);

            int        changes     = randomlyPopulateNamedCache(londonCache, 0, 1000, "LONDON");

            JmxFeature londonJmx   = londonServer.get(JmxFeature.class);

            // get the MBean for the EventChannel
            final Deferred<EventChannelControllerMBean> mBean =
                londonJmx.getDeferredMBeanProxy(new ObjectName(String.format("Coherence:type=EventChannels,id=%s,nodeId=1",
                                                                             cacheName + "-" + "Custom Event Channel")),
                                                EventChannelControllerMBean.class);

            System.out.printf("Waiting for Event Distribution to complete.\n");

            Eventually.assertThat(invoking(mBean).getEventsDistributedCount(), is(changes));
        }
    }


    /**
     * Ensure Insert/Update/Deletes against a {@link NamedCache} configured with an
     * active-active topology are distributed between the active sites when the keys and values
     * are only POF serializable and the servers don't have a POF configuration, thus
     * ensuring they can't be deserialized server-side.
     * <p>
     * The intent of this test is to ensure that applications purely using .NET or C++
     * (no Java) operate correctly.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testShouldNotDeserializeKeysOrValuesDuringDistribution() throws Exception
    {
        // the name of the cache we'll use for testing
        final String cacheName = "publishing-cache";

        // obtain the platform on which we'll launch the clusters
        LocalPlatform platform = LocalPlatform.get();

        // determine london and newyork proxy ports
        Capture<Integer> londonProxyPort  = new Capture<>(getAvailablePortIterator());
        Capture<Integer> newyorkProxyPort = new Capture<>(getAvailablePortIterator());

        // both london and newyork share the same proxy address
        String proxyAddress = platform.getLoopbackAddress().getHostAddress();

        // establish the london server options
        OptionsByType londonServerOptions = newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

        londonServerOptions.add(SiteName.of("london"));
        londonServerOptions.add(DisplayName.of("london"));
        londonServerOptions.add(SystemProperty.of("proxy.address", proxyAddress));
        londonServerOptions.add(SystemProperty.of("proxy.port", londonProxyPort));
        londonServerOptions.add(SystemProperty.of("remote.port", newyorkProxyPort));

        try (CoherenceCacheServer londonServer = platform.launch(CoherenceCacheServer.class,
                                                                 londonServerOptions.asArray()))
        {
            Eventually.assertThat(invoking(londonServer).getClusterSize(), is(1));

            try (JavaApplication londonClient = platform.launch(JavaApplication.class,
                                                                ClassName.of(KeepAliveApplication.class),
                                                                SiteName.of("london"),
                                                                DisplayName.of("london-client"),
                                                                Clustering.disabled(),
                                                                SystemProperty.of("remote.address", proxyAddress),
                                                                SystemProperty.of("remote.port", londonProxyPort),
                                                                Pof.config("test-client-pof-config.xml"),
                                                                CacheConfig.of("test-client-cache-config.xml"),
                                                                Console.system()))
            {
                londonClient.submit(() -> System.out.println("LONDON Cache [" + cacheName + "] Size:"
                                                             + CacheFactory.getCache(cacheName).size()));

                // establish the newyork server options
                OptionsByType newyorkServerOptions =
                    newActiveCacheServerOptions(new Capture<>(getAvailablePortIterator()));

                newyorkServerOptions.add(SiteName.of("newyork"));
                newyorkServerOptions.add(DisplayName.of("newyork"));
                newyorkServerOptions.add(SystemProperty.of("proxy.address", proxyAddress));
                newyorkServerOptions.add(SystemProperty.of("proxy.port", newyorkProxyPort));
                newyorkServerOptions.add(SystemProperty.of("remote.port", londonProxyPort));

                try (CoherenceClusterMember newyorkServer = platform.launch(CoherenceCacheServer.class,
                                                                            newyorkServerOptions.asArray()))
                {
                    Eventually.assertThat(invoking(newyorkServer).getClusterSize(), is(1));

                    try (JavaApplication newyorkClient = platform.launch(JavaApplication.class,
                                                                         ClassName.of(KeepAliveApplication.class),
                                                                         SiteName.of("newyork"),
                                                                         DisplayName.of("newyork-client"),
                                                                         Clustering.disabled(),
                                                                         SystemProperty.of("remote.address",
                                                                                           proxyAddress),
                                                                         SystemProperty.of("remote.port",
                                                                                           newyorkProxyPort),
                                                                         Pof.config("test-client-pof-config.xml"),
                                                                         CacheConfig.of("test-client-cache-config.xml"),
                                                                         Console.system()))
                    {
                        // a RemoteCallable that will populate a NamedCache and return the number of entries in the populated cache
                        RemoteCallable<Integer> populateCacheCallable = () -> {
                                                                            NamedCache cache =
                                                                                CacheFactory.getCache(cacheName);

                                                                            randomlyPopulateNamedCacheWithDomainValues(cache,
                                                                                                                       0,
                                                                                                                       500,
                                                                                                                       cacheName,
                                                                                                                       false);

                                                                            return cache.size();
                                                                        };

                        // a RemoteCallable that will return a HashMap containing string representations
                        // of the keys and values of a NamedCache (to allow us to compare them without serializing the classes)
                        RemoteCallable<HashMap<String, String>> getCacheCallable = () -> {
                                                                                       NamedCache cache =
                                                                                           CacheFactory.getCache(cacheName);

                                                                                       HashMap<String, String> results =
                                                                                           new HashMap<>();

                                                                                       for (Iterator<Map
                                                                                           .Entry> iterator =
                                                                                               cache.entrySet()
                                                                                               .iterator();
                                                                                           iterator.hasNext(); )
                                                                                       {
                                                                                           Map.Entry entry =
                                                                                               iterator.next();

                                                                                           results.put(entry.getKey()
                                                                                           .toString(),
                                                                                                       entry.getValue()
                                                                                                       .toString());
                                                                                       }

                                                                                       return results;
                                                                                   };

                        // use the newyork client to populate the newyork site
                        CompletableFuture<Integer> newyorkCount = newyorkClient.submit(populateCacheCallable);

                        System.out.println("Waiting for " + newyorkCount.get()
                                           + " entries to distribute from NEWYORK to LONDON");

                        // ensure that the london and newyork caches are the same size
                        Eventually.assertThat(londonClient,
                                              () -> CacheFactory.getCache(cacheName).size(),
                                              is(newyorkCount.get()));

                        // acquire a representation of the newyork cache (via the newyork client)
                        HashMap<String, String> newyorkMap = newyorkClient.submit(getCacheCallable).get();

                        // ensure that the london cache is the same as the newyork cache
                        Eventually.assertThat(londonClient, getCacheCallable, MapMatcher.sameAs(newyorkMap));

                        // use the london client to populate the london site
                        CompletableFuture<Integer> londonCount = londonClient.submit(populateCacheCallable);

                        System.out.println("Waiting for " + londonCount.get()
                                           + " entries to distribute from LONDON to NEWYORK");

                        // ensure that the london and newyork caches are the same size
                        Eventually.assertThat(newyorkClient,
                                              () -> CacheFactory.getCache(cacheName).size(),
                                              is(londonCount.get()));

                        // acquire a representation of the london cache (via the london client)
                        HashMap<String, String> londonMap = londonClient.submit(getCacheCallable).get();

                        // ensure that the newyork cache is the same as the london cache
                        Eventually.assertThat(newyorkClient, getCacheCallable, MapMatcher.sameAs(londonMap));
                    }
                }
            }
        }
    }


    /**
     * Randomly populates the specified {@link NamedCache} by inserting, updating and removing a number of entries
     * (using single operation methods)
     *
     * @param namedCache        The {@link NamedCache} to populate.
     * @param minimumKey        The minimum key value (inclusive)
     * @param nrEntries         The number of entries to generate
     * @param siteName          The name of the site in which the {@link NamedCache} is defined (for logging purposes).
     *
     * @return The number of entries in the {@link NamedCache} that were inserted/updated/removed (ie: changed)
     */
    public int randomlyPopulateNamedCache(NamedCache namedCache,
                                          int        minimumKey,
                                          int        nrEntries,
                                          String     siteName)
    {
        return randomlyPopulateNamedCache(namedCache, minimumKey, nrEntries, siteName, false);
    }


    /**
     * Randomly populates the specified {@link NamedCache} by inserting, updating and removing a number of entries
     * (optionally using bulk operations) where the value of the entry is a String.
     *
     * @param namedCache        The {@link NamedCache} to populate.
     * @param minimumKey        The minimum key value (inclusive)
     * @param nrEntries         The number of entries to generate
     * @param siteName          The name of the site in which the {@link NamedCache} is defined (for logging purposes).
     * @param usePutAll         Should putAll be used to perform inserts
     *
     * @return The number of entries in the {@link NamedCache} that were inserted/updated/removed (ie: changed)
     */
    public int randomlyPopulateNamedCache(NamedCache namedCache,
                                          int        minimumKey,
                                          int        nrEntries,
                                          String     siteName,
                                          boolean    usePutAll)
    {
        Random random = new Random();

        // insert a number of random entries
        System.out.printf("Inserting %d random entries into %s site.\n", nrEntries, siteName);

        @SuppressWarnings("unchecked")
        Map<Integer, String> mapInserts = usePutAll
                                          ? new HashMap<Integer, String>() : (Map<Integer, String>) namedCache;

        for (int i = 0; i < nrEntries; i++)
        {
            int    key   = minimumKey + i;
            String value = randomString(random, "abcdefghijklmnopqrstuvwxyz", 20);

            mapInserts.put(key, value);
        }

        if (usePutAll)
        {
            namedCache.putAll(mapInserts);
        }

        int changeCount = nrEntries;

        // update a random number of existing entries
        int count = random.nextInt(nrEntries / 3);

        System.out.printf("Updating %d random entries in %s site.\n", count, siteName);

        for (int i = 0; i < count; i++)
        {
            int    key          = random.nextInt(nrEntries) + minimumKey;

            String currentValue = (String) namedCache.get(key);

            if (currentValue != null)
            {
                String value = randomString(random, "abcdefghijklmnopqrstuvwxyz", 10);

                namedCache.put(key, currentValue + value);
                changeCount = changeCount + 1;
            }

        }

        // remove a random number of existing entries
        count = random.nextInt(nrEntries / 3);
        System.out.printf("Removing %d random entries from the %s site.\n", count, siteName);

        for (int i = 0; i < count; i++)
        {
            int    key     = random.nextInt(nrEntries) + minimumKey;

            Object current = namedCache.remove(key);

            changeCount = current == null ? changeCount : changeCount + 1;
        }

        System.out.printf("Inserted/Updated/Removed %d actual entries from the %s site.\n", changeCount, siteName);

        return changeCount;
    }


    /**
     * Randomly populates the specified {@link NamedCache} by inserting, updating and removing a number of entries
     * (optionally using bulk operations) where the value of the entry is a {@link DomainValue} instance.
     *
     * @param namedCache        The {@link NamedCache} to populate.
     * @param minimumKey        The minimum key value (inclusive)
     * @param nrEntries         The number of entries to generate
     * @param siteName          The name of the site in which the {@link NamedCache} is defined (for logging purposes).
     * @param usePutAll         Should putAll be used to perform inserts
     *
     * @return The number of entries in the {@link NamedCache} that were inserted/updated/removed (ie: changed)
     */
    public static int randomlyPopulateNamedCacheWithDomainValues(NamedCache namedCache,
                                                                 int        minimumKey,
                                                                 int        nrEntries,
                                                                 String     siteName,
                                                                 boolean    usePutAll)
    {
        Random random = new Random();

        // insert a number of random entries
        System.out.printf("Inserting %d random entries into %s site.\n", nrEntries, siteName);

        @SuppressWarnings("unchecked")
        Map<DomainKey, DomainValue> mapInserts = usePutAll
                                                 ? new HashMap<DomainKey, DomainValue>()
                                                 : (Map<DomainKey, DomainValue>) namedCache;

        for (int i = 0; i < nrEntries; i++)
        {
            DomainKey   key   = new DomainKey(minimumKey + i);
            String      s     = randomString(random, "abcdefghijklmnopqrstuvwxyz", 20);
            DomainValue value = new DomainValue(s);

            mapInserts.put(key, value);
        }

        if (usePutAll)
        {
            namedCache.putAll(mapInserts);
        }

        int changeCount = nrEntries;

        // update a random number of existing entries
        int count = random.nextInt(nrEntries / 3);

        System.out.printf("Updating %d random entries in %s site.\n", count, siteName);

        for (int i = 0; i < count; i++)
        {
            DomainKey   key          = new DomainKey(random.nextInt(nrEntries) + minimumKey);

            DomainValue currentValue = (DomainValue) namedCache.get(key);

            if (currentValue != null)
            {
                String value = randomString(random, "abcdefghijklmnopqrstuvwxyz", 10);

                namedCache.put(key, new DomainValue(currentValue.getValue() + value));
                changeCount = changeCount + 1;
            }

        }

        // remove a random number of existing entries
        count = random.nextInt(nrEntries / 3);
        System.out.printf("Removing %d random entries from the %s site.\n", count, siteName);

        for (int i = 0; i < count; i++)
        {
            DomainKey key     = new DomainKey(random.nextInt(nrEntries) + minimumKey);

            Object    current = namedCache.remove(key);

            changeCount = current == null ? changeCount : changeCount + 1;
        }

        System.out.printf("Inserted/Updated/Removed %d actual entries from the %s site.\n", changeCount, siteName);

        return changeCount;
    }


    /**
     * Generates a pseudo random {@link String} consisting of characters from the specified {@link String} and of
     * the specified length.
     *
     * @param random    The {@link Random} to use for generating random integers
     * @param sFrom     A non-null and non-empty {@link String} of charaters
     * @param cLength   The desired length of the {@link String}
     *
     * @return a pseudo random {@link String}
     */
    public static String randomString(Random random,
                                      String sFrom,
                                      int    cLength)
    {
        assert sFrom != null && sFrom.length() > 0;

        StringBuilder bldrRandomized = new StringBuilder(cLength);

        for (int i = 0; i < cLength; i++)
        {
            bldrRandomized.append(sFrom.charAt(random.nextInt(sFrom.length())));
        }

        return bldrRandomized.toString();
    }


    /**
     * A custom {@link ApplicationListener} that is an {@link Option}.
     * @param <A>
     */
    public static abstract class CustomApplicationListener<A extends Application> implements ApplicationListener<A>,
                                                                                             Option
    {
    }
}
