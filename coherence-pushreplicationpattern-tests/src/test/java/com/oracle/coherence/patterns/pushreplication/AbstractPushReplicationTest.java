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

import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerMBean;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.channels.BinaryEntryStoreEventChannel;
import com.oracle.coherence.patterns.eventdistribution.channels.CacheStoreEventChannel;
import com.oracle.coherence.patterns.eventdistribution.filters.ExampleEventFilter;
import com.oracle.tools.deferred.Deferred;
import com.oracle.tools.deferred.Eventually;
import com.oracle.tools.deferred.UnresolvableInstanceException;
import com.oracle.tools.junit.AbstractCoherenceTest;
import com.oracle.tools.matchers.Equivalence;
import com.oracle.tools.matchers.PartitionSetMatcher;
import com.oracle.tools.runtime.Application;
import com.oracle.tools.runtime.LifecycleEvent;
import com.oracle.tools.runtime.LifecycleEventInterceptor;
import com.oracle.tools.runtime.PropertiesBuilder;
import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema.JMXManagementMode;
import com.oracle.tools.runtime.console.SystemApplicationConsole;
import com.oracle.tools.runtime.java.JavaApplicationBuilder;
import com.oracle.tools.runtime.java.NativeJavaApplicationBuilder;
import com.oracle.tools.runtime.java.container.Container;
import com.oracle.tools.runtime.network.AvailablePortIterator;
import com.oracle.tools.runtime.network.Constants;
import com.oracle.tools.util.Capture;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Filter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.oracle.tools.deferred.DeferredAssert.assertThat;
import static com.oracle.tools.deferred.DeferredHelper.eventually;
import static com.oracle.tools.deferred.DeferredHelper.invoking;
import static com.oracle.tools.matchers.MapMatcher.sameAs;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

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
public abstract class AbstractPushReplicationTest extends AbstractCoherenceTest
{
    /**
     * The maximum amount of time we should ever what for a test/assertion to complete.
     */
    private static final long TEST_TIMEOUT_MS = 120000;    // two minutes

    /**
     * The System {@link Properties} used to start the test.
     */
    private Properties systemProperties;


    /**
     * Before each test run we take a copy of the System {@link Properties} so we can restore
     * the prior to running other tests.
     */
    @Before
    public void beforeEachTest()
    {
        // take a copy of the system properties (as we're going to change them)
        systemProperties = System.getProperties();
    }


    /**
     * After each test we restore the System {@link Properties}.
     */
    @After
    public void afterEachTest()
    {
        // reset the system properties
        System.setProperties(systemProperties);
    }


    /**
     * A simple method to wait a specified amount of time, with a message to stdout.
     *
     * @param time The time to wait in ms.
     * @param rationale The rationale (message) for waiting.
     * @throws InterruptedException When interrupted while waiting.
     */
    public void wait(long   time,
                     String rationale) throws InterruptedException
    {
        System.out.printf("%s: Waiting %dms %s\n", new Date(), time, rationale);
        Thread.sleep(time);
    }


    /**
     * Determines the {@link AvailablePortIterator} to use for locating free ports on the host.
     *
     * @return {@link AvailablePortIterator}
     */
    protected static AvailablePortIterator getAvailablePortIterator()
    {
        return Container.getAvailablePorts();
    }


    /**
     * Constructs a new {@link ClusterMemberSchema} on which sub-classes of this class should
     * configure new {@link ClusterMemberSchema}s.  ie: This method returns a pre-configured
     * {@link ClusterMemberSchema} that the tests in this class expect to use.
     *
     * @return {@link ClusterMemberSchema}
     */
    protected ClusterMemberSchema newBaseClusterMemberSchema(Capture<Integer> clusterPort)
    {
        ClusterMemberSchema schema =
            new ClusterMemberSchema().setEnvironmentVariables(PropertiesBuilder.fromCurrentEnvironmentVariables())
                .useLocalHostMode().setClusterPort(clusterPort).setPofConfigURI("test-pof-config.xml")
                .setJMXManagementMode(JMXManagementMode.ALL).setJMXPort(getAvailablePortIterator())
                .setSystemProperty("remote.address",
                                   Constants.getLocalHost())
                                       .setSystemProperty("proxy.address", Constants.getLocalHost());

        schema.addLifecycleInterceptor(new LifecycleEventInterceptor<ClusterMember>()
        {
            @Override
            public void onEvent(LifecycleEvent<ClusterMember> event)
            {
                if (event.getType() == Application.EventKind.REALIZED)
                {
                    // ensure that we wait until the extend tcp proxy service is running
                    ClusterMember member = event.getObject();

                    Eventually.assertThat(invoking(member).isServiceRunning("ExtendTcpProxyService"), is(true));
                }
            }
        });

        return schema;
    }


    protected abstract ClusterMemberSchema newPassiveClusterMemberSchema(Capture<Integer> clusterPort);


    protected abstract ClusterMemberSchema newActiveClusterMemberSchema(Capture<Integer> clusterPort);


    protected JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> newClusterMemberBuilder()
    {
        return new NativeJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();
    }


    /**
     * Performs Active-Active topology tests.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testActiveActive() throws Exception
    {
        final String                                               cacheName     = "publishing-cache";

        ClusterMember                                              londonServer  = null;
        ClusterMember                                              newyorkServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder       = newClusterMemberBuilder();

        try
        {
            SystemApplicationConsole console = new SystemApplicationConsole();

            // define the london cluster port
            Capture<Integer> londonClusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newActiveClusterMemberSchema(londonClusterPort).setSiteName("london").setSystemProperty("proxy.address",
                                                                                                        Constants
                                                                                                            .getLocalHost())
                                                                                                                .setSystemProperty("proxy.port",
                getAvailablePortIterator()).setSystemProperty("remote.port",
                                                              getAvailablePortIterator());

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            // define the london cluster port
            Capture<Integer> newyorkClusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the newyork server
            ClusterMemberSchema newyorkServerSchema =
                newActiveClusterMemberSchema(newyorkClusterPort).setSiteName("newyork").setSystemProperty("proxy.port",
                                                                                                          londonServer
                                                                                                              .getSystemProperty("remote.port"))
                                                                                                                  .setSystemProperty("remote.port",
                londonServer.getSystemProperty("proxy.port"));

            newyorkServer = builder.realize(newyorkServerSchema, "NEWYORK", console);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", newyorkServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", newyorkServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the newyork site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "NEWYORK");

            // grab a copy of the newyork site entries (we'll use this for comparison later)
            final Map<Object, Object> newyorkCache = new HashMap<Object, Object>(CacheFactory.getCache(cacheName));

            // shutdown our connection to the newyork cluster
            CacheFactory.shutdown();

            // connect to the london cluster
            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            System.out.printf("\nWaiting for %d entries to synchronize between NEWYORK to LONDON.\n",
                              newyorkCache.size());

            NamedCache namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache), sameAs(newyorkCache));

            // assert that no been queued in London (from New York)
            // (even though this is Active-Active, events from New York to London should not be
            // re-queued in London for distribution back to New York).
            Deferred<Integer> deferred =
                londonServer
                    .getDeferredMBeanAttribute(new ObjectName("Coherence:type=EventChannels,id=publishing-cache-Remote Cluster Channel,nodeId=1"),
                                               "EventsDistributedCount",
                                               Integer.class);

            assertThat(deferred, is(0));

            // update the london site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 1000, 500, "LONDON");

            // grab a copy of the london site entries (we'll use this for comparison later)
            final Map<Object, Object> londonCache = new HashMap<Object, Object>(CacheFactory.getCache(cacheName));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();

            // assert that the value is in the newyork cluster
            System.setProperty("remote.address", newyorkServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", newyorkServer.getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            System.out.printf("\nWaiting for %d entries to synchronize between LONDON and NEWYORK.\n",
                              londonCache.size());

            namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache), sameAs(londonCache));
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
            }

            if (newyorkServer != null)
            {
                newyorkServer.close();
            }
        }
    }


    /**
     * Performs Active-Passive topology tests
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassive() throws Exception
    {
        String                                                     siteName         = "testActivePassive";
        final String                                               cacheName        = "publishing-cache";

        int                                                        nrPassiveServers = 2;
        int                                                        nrActiveServers  = 2;

        ArrayList<ClusterMember> passiveServers = new ArrayList<ClusterMember>(nrPassiveServers);
        ArrayList<ClusterMember> activeServers = new ArrayList<ClusterMember>(nrActiveServers);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder          = newClusterMemberBuilder();

        try
        {
            SystemApplicationConsole console = new SystemApplicationConsole();

            // establish the passive cluster
            Capture<Integer> passiveClusterPort = new Capture<Integer>(getAvailablePortIterator());

            for (int i = 0; i < nrPassiveServers; i++)
            {
                ClusterMemberSchema passiveServerSchema =
                    newPassiveClusterMemberSchema(passiveClusterPort).setSiteName(siteName);

                passiveServers.add(builder.realize(passiveServerSchema, String.format("PASSIVE %s", i), console));
            }

            // establish the active cluster
            Capture<Integer> activeClusterPort = new Capture<Integer>(getAvailablePortIterator());

            for (int i = 0; i < nrActiveServers; i++)
            {
                ClusterMemberSchema activeServerSchema =
                    newActiveClusterMemberSchema(activeClusterPort).setSiteName(siteName)
                        .setSystemProperty("remote.port",
                                           passiveServers.get(0).getSystemProperty("proxy.port"));

                activeServers.add(builder.realize(activeServerSchema, String.format("ACTIVE  %d", i), console));
            }

            // wait for passive cluster to start
            for (ClusterMember server : passiveServers)
            {
                assertThat(eventually(invoking(server).getClusterSize()), is(nrPassiveServers));
            }

            // wait for active server cluster to start
            for (ClusterMember server : activeServers)
            {
                assertThat(eventually(invoking(server).getClusterSize()), is(nrActiveServers));
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", activeServers.get(0).getSystemProperty("proxy.address"));
            System.setProperty("remote.port", activeServers.get(0).getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the active site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "ACTIVE");

            // grab a copy of the active site entries (we'll use this for comparison later)
            final Map<Object, Object> activeCache = new HashMap<Object, Object>(CacheFactory.getCache(cacheName));

            // shutdown our connection to the active site
            CacheFactory.shutdown();

            // assert that the values have arrived in the remote site
            System.out.printf("Connecting to PASSIVE site.\n");
            System.setProperty("remote.address", passiveServers.get(0).getSystemProperty("proxy.address"));
            System.setProperty("remote.port", passiveServers.get(0).getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            System.out.printf("\nWaiting for %d entries to synchronize between ACTIVE and PASSIVE site.\n",
                              activeCache.size());

            NamedCache namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache), sameAs(activeCache));

            Assert.assertNotNull(namedCache);
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            for (ClusterMember server : passiveServers)
            {
                server.close();
            }

            for (ClusterMember server : activeServers)
            {
                server.close();
            }
        }
    }


    /**
     * Performs Active-Passive topology tests (but with the channels disabled)
     *
     * @throws Exception
     */
    @Test
    public final void testActivePassiveDisabled() throws Exception
    {
        ClusterMember                                              passiveServer = null;
        ClusterMember                                              activeServer  = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder       = newClusterMemberBuilder();
        SystemApplicationConsole                                   console       = new SystemApplicationConsole();

        try
        {
            // establish the passive cluster
            Capture<Integer> passiveClusterPort = new Capture<Integer>(getAvailablePortIterator());

            ClusterMemberSchema passiveServerSchema =
                newPassiveClusterMemberSchema(passiveClusterPort).setSiteName("sydney");

            passiveServer = builder.realize(passiveServerSchema, "PASSIVE", console);

            // establish the active cluster
            Capture<Integer> activeClusterPort = new Capture<Integer>(getAvailablePortIterator());

            ClusterMemberSchema activeServerSchema =
                newActiveClusterMemberSchema(activeClusterPort).setSiteName("sydney").setSystemProperty("remote.port",
                                                                                                        passiveServer
                                                                                                            .getSystemProperty("proxy.port"))
                                                                                                                .setSystemProperty("channel.starting.mode", "disabled");

            activeServer = builder.realize(activeServerSchema, "ACTIVE", console);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", activeServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", activeServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // put a value in the active site
            CacheFactory.getCache("publishing-cache").put("message", "hello world");

            // shutdown our connection to the active site
            CacheFactory.shutdown();

            // assert that the value is in the passive site
            System.setProperty("remote.address", passiveServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", passiveServer.getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            System.out.printf("Ensuring ACTIVE and PASSIVE sites don't become synchronized.\n");

            NamedCache namedCache = CacheFactory.getCache("publishing-cache");

            // we only wait a little bit for this
            Eventually.assertThat(eventually(invoking(namedCache).size()), is(1), 5, TimeUnit.SECONDS);

            fail("The disabled site should not have received any values");
        }
        catch (AssertionError error)
        {
            if (error.getCause() instanceof UnresolvableInstanceException)
            {
                // this is actually ok as we should never assert that the disabled site receives information
            }
            else
            {
                throw error;
            }
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (passiveServer != null)
            {
                passiveServer.close();
            }

            if (activeServer != null)
            {
                activeServer.close();
            }
        }
    }


    /**
     * Performs Active-Passive topology tests with Event transformation
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassiveEventTranformation() throws Exception
    {
        String                                                     siteName         = "testActivePassive";
        final String                                               cacheName        = "publishing-cache";

        int                                                        nrPassiveServers = 2;
        int                                                        nrActiveServers  = 2;

        ArrayList<ClusterMember> passiveServers = new ArrayList<ClusterMember>(nrPassiveServers);
        ArrayList<ClusterMember> activeServers = new ArrayList<ClusterMember>(nrActiveServers);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder          = newClusterMemberBuilder();

        try
        {
            SystemApplicationConsole console = new SystemApplicationConsole();

            // establish the passive cluster
            Capture<Integer> passiveClusterPort = new Capture<Integer>(getAvailablePortIterator());

            for (int i = 0; i < nrPassiveServers; i++)
            {
                ClusterMemberSchema passiveServerSchema =
                    newPassiveClusterMemberSchema(passiveClusterPort).setSiteName(siteName);

                passiveServers.add(builder.realize(passiveServerSchema, String.format("PASSIVE %s", i), console));
            }

            // establish the active cluster
            Capture<Integer> activeClusterPort = new Capture<Integer>(getAvailablePortIterator());

            for (int i = 0; i < nrActiveServers; i++)
            {
                ClusterMemberSchema activeServerSchema =
                    newActiveClusterMemberSchema(activeClusterPort)
                        .setCacheConfigURI("test-remotecluster-eventtransformation-cache-config.xml")
                        .setSiteName(siteName).setSystemProperty("remote.port",
                                                                 passiveServers.get(0).getSystemProperty("proxy.port"));

                activeServers.add(builder.realize(activeServerSchema, String.format("ACTIVE  %d", i), console));
            }

            // wait for passive cluster to start
            for (ClusterMember server : passiveServers)
            {
                assertThat(eventually(invoking(server).getClusterSize()), is(nrPassiveServers));
            }

            // wait for active server cluster to start
            for (ClusterMember server : activeServers)
            {
                assertThat(eventually(invoking(server).getClusterSize()), is(nrActiveServers));
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", activeServers.get(0).getSystemProperty("proxy.address"));
            System.setProperty("remote.port", activeServers.get(0).getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the active site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "ACTIVE");

            // grab a copy of the active site entries (we'll use this for comparison later)
            final Map<Object, String> activeCache = new HashMap<Object, String>(CacheFactory.getCache(cacheName));

            // shutdown our connection to the active site
            CacheFactory.shutdown();

            // assert that the values have arrived in the remote site
            System.out.printf("Connecting to PASSIVE site.\n");
            System.setProperty("remote.address", passiveServers.get(0).getSystemProperty("proxy.address"));
            System.setProperty("remote.port", passiveServers.get(0).getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            System.out.printf("\nWaiting for %d entries to synchronize between ACTIVE and PASSIVE site.\n",
                              activeCache.size());

            NamedCache namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache), is(sameAs(activeCache, Equivalence.EQUALS_IGNORE_CASE)));
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            for (ClusterMember server : passiveServers)
            {
                server.close();
            }

            for (ClusterMember server : activeServers)
            {
                server.close();
            }
        }
    }


    /**
     * Performs Active-Passive topology tests with Event Filtering
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivePassiveEventFiltering() throws Exception
    {
        String                                                     siteName      = "testActivePassiveWithFiltering";
        final String                                               cacheName     = "publishing-cache";

        ClusterMember                                              passiveServer = null;
        ClusterMember                                              activeServer  = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder       = newClusterMemberBuilder();
        SystemApplicationConsole                                   console       = new SystemApplicationConsole();

        try
        {
            // establish the passive cluster
            Capture<Integer> passiveClusterPort = new Capture<Integer>(getAvailablePortIterator());

            ClusterMemberSchema passiveServerSchema =
                newPassiveClusterMemberSchema(passiveClusterPort).setSiteName(siteName);

            passiveServer = builder.realize(passiveServerSchema, "PASSIVE", console);

            passiveServer.getClusterMBeanInfo();
            passiveServer.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // establish the active cluster
            Capture<Integer> activeClusterPort = new Capture<Integer>(getAvailablePortIterator());

            ClusterMemberSchema activeServerSchema =
                newActiveClusterMemberSchema(activeClusterPort)
                    .setCacheConfigURI("test-remotecluster-eventfiltering-cache-config.xml").setSiteName(siteName)
                    .setSystemProperty("remote.port",
                                       passiveServer.getSystemProperty("proxy.port"));

            activeServer = builder.realize(activeServerSchema, "ACTIVE", console);

            activeServer.getClusterMBeanInfo();
            activeServer.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", activeServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", activeServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the active site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "ACTIVE");

            // grab a copy of the active site entries (we'll use this for comparison later)
            final Map<Object, String> activeCache = new HashMap<Object, String>(CacheFactory.getCache(cacheName));

            // remove entries that don't satisfy the filter
            System.out.printf("There are %d entries in the active cache\n", activeCache.size());

            Filter            filter       = new ExampleEventFilter();
            ArrayList<Object> keysToRemove = new ArrayList<Object>();

            for (Object key : activeCache.keySet())
            {
                String value = activeCache.get(key);

                if (!filter.evaluate(value))
                {
                    keysToRemove.add(key);
                }
            }

            for (Object key : keysToRemove)
            {
                activeCache.remove(key);
            }

            System.out.printf("There should be %d entries in the passive cache\n", activeCache.size());

            // shutdown our connection to the active site
            CacheFactory.shutdown();

            // assert that the values have arrived in the remote site
            System.out.printf("Connecting to PASSIVE site.\n");
            System.setProperty("remote.address", passiveServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", passiveServer.getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            System.out.printf("\nWaiting for %d entries to synchronize between ACTIVE and PASSIVE site.\n",
                              activeCache.size());

            NamedCache namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache), is(sameAs(activeCache, Equivalence.EQUALS_IGNORE_CASE)));

            Assert.assertNotNull(namedCache);
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (passiveServer != null)
            {
                passiveServer.close();
            }

            if (activeServer != null)
            {
                activeServer.close();
            }
        }
    }


    /**
     * Test that the {@link CacheStoreEventChannel} publishes entries.
     *
     * @throws Exception
     */
    @Test
    public void testCacheStoreEventChannel() throws Exception
    {
        ClusterMember                                              londonServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder      = newClusterMemberBuilder();

        try
        {
            // define the cluster port
            Capture<Integer> clusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(clusterPort).setSiteName("london")
                    .setCacheConfigURI("test-cachestore-eventchannel-cache-config.xml").setClusterName("passive");

            londonServer = builder.realize(londonServerSchema, "LONDON");

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "LONDON");

            System.out.printf("Waiting for Event Distribution to complete.\n");

            NamedCache testCache  = CacheFactory.getCache(cacheName);
            NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(testCache), is(sameAs(otherCache)));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
            }
        }
    }


    /**
     * Test that the {@link BinaryEntryStoreEventChannel} publishes entries.
     *
     * @throws Exception
     */
    @Test
    public void testBinaryEntryStoreEventChannel() throws Exception
    {
        ClusterMember                                              londonServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder      = newClusterMemberBuilder();
        SystemApplicationConsole                                   console      = new SystemApplicationConsole();

        try
        {
            // define the cluster port
            Capture<Integer> clusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(clusterPort).setSiteName("london")
                    .setCacheConfigURI("test-binaryentrystore-eventchannel-cache-config.xml").setClusterName("passive");

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "LONDON");

            System.out.printf("Waiting for Event Distribution to complete.\n");

            NamedCache testCache  = CacheFactory.getCache(cacheName);
            NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(testCache), is(sameAs(otherCache)));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
            }
        }
    }


    /**
     * Test that a Custom {@link EventChannel} dispatches entries.
     *
     * @throws Exception
     */
    @Test
    public void testCustomEventChannel() throws Exception
    {
        ClusterMember                                              londonServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder      = newClusterMemberBuilder();
        SystemApplicationConsole                                   console      = new SystemApplicationConsole();

        try
        {
            // define the cluster port
            Capture<Integer> clusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(clusterPort).setSiteName("london")
                    .setCacheConfigURI("test-custom-eventchannel-cache-config.xml").setClusterName("passive");

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            final int changes = randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "LONDON");

            // get the MBean for the EventChannel
            final Deferred<EventChannelControllerMBean> mBean =
                londonServer
                    .getDeferredMBeanProxy(new ObjectName(String
                        .format("Coherence:type=EventChannels,id=%s,nodeId=1",
                                cacheName + "-" + "Custom Event Channel")),
                                           EventChannelControllerMBean.class);

            System.out.printf("Waiting for Event Distribution to complete.\n");

            Eventually.assertThat(invoking(mBean).getEventsDistributedCount(), is(changes));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
            }
        }
    }


    /**
     * Performs Active-Active topology testing using a custom Conflict Resolver
     *
     * @throws Exception
     */
    @Test
    public void testActiveActiveConflictResolution() throws Exception
    {
        final String                                               cacheName     = "publishing-cache";

        ClusterMember                                              londonServer  = null;
        ClusterMember                                              newyorkServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder       = newClusterMemberBuilder();
        SystemApplicationConsole                                   console       = new SystemApplicationConsole();

        try
        {
            // establish the london server
            Capture<Integer> londonClusterPort = new Capture<Integer>(getAvailablePortIterator());

            ClusterMemberSchema londonServerSchema =
                newActiveClusterMemberSchema(londonClusterPort).setSiteName("london").setSystemProperty("proxy.port",
                                                                                                        getAvailablePortIterator())
                                                                                                            .setSystemProperty("remote.port",
                getAvailablePortIterator()).setSystemProperty("conflict.resolver.classname",
                                                              "com.oracle.coherence.patterns.pushreplication.MergingConflictResolver");

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            // establish the newyork server
            Capture<Integer> newyorkClusterPort = new Capture<Integer>(getAvailablePortIterator());

            ClusterMemberSchema newyorkServerSchema =
                newActiveClusterMemberSchema(newyorkClusterPort).setSiteName("newyork").setSystemProperty("proxy.port",
                                                                                                          londonServer
                                                                                                              .getSystemProperty("remote.port"))
                                                                                                                  .setSystemProperty("remote.port",
                londonServer.getSystemProperty("proxy.port")).setSystemProperty("conflict.resolver.classname",
                                                                                "com.oracle.coherence.patterns.pushreplication.MergingConflictResolver");

            newyorkServer = builder.realize(newyorkServerSchema, "NEWYORK", console);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            // connect to the newyork cluster
            System.setProperty("remote.address", newyorkServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", newyorkServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // place a partition set into NEWYORK
            PartitionSet setPartitions = new PartitionSet(3);

            setPartitions.add(0);
            CacheFactory.getCache(cacheName).put("partitions", setPartitions);

            // shutdown our connection to the newyork cluster
            CacheFactory.shutdown();

            // connect to the london cluster
            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // place a partition set into LONDON
            setPartitions = new PartitionSet(3);
            setPartitions.add(1);
            CacheFactory.getCache(cacheName).put("partitions", setPartitions);

            NamedCache namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache).get("partitions"), PartitionSetMatcher.contains(0, 1));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();

            // connect to the newyork cluster
            System.setProperty("remote.address", newyorkServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", newyorkServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                    .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            namedCache = CacheFactory.getCache(cacheName);

            Eventually.assertThat(invoking(namedCache).get("partitions"), PartitionSetMatcher.contains(0, 1));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
            }

            if (newyorkServer != null)
            {
                newyorkServer.close();
            }
        }
    }


    /**
     * Test asynchronous {@link PublishingCacheStore} configuration with a {@link CacheStoreEventChannel}.
     *
     * @throws Exception
     */
    @Test
    public void testAsyncCacheStoreEventChannel() throws Exception
    {
        ClusterMember                                              londonServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder      = newClusterMemberBuilder();
        SystemApplicationConsole                                   console      = new SystemApplicationConsole();

        try
        {
            // define the cluster port
            Capture<Integer> clusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(clusterPort).setSiteName("london")
                    .setCacheConfigURI("test-cachestore-eventchannel-cache-config.xml").setClusterName("passive")
                    .setSystemProperty("write.behind.delay",
                                       1);

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            final int ENTRY_COUNT = 500;

            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, ENTRY_COUNT, "LONDON");

            System.out.printf("Waiting for Event Distribution to complete.\n");

            NamedCache testCache  = CacheFactory.getCache(cacheName);
            NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(testCache), is(sameAs(otherCache)));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
            }
        }
    }


    /**
     * Test performing an asynchronous putAll using a CacheStoreChannel
     *
     * @throws Exception
     */
    @Test
    public void testAsyncPutAllWithCacheStoreChannel() throws Exception
    {
        ClusterMember                                              londonServer = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder      = newClusterMemberBuilder();
        SystemApplicationConsole                                   console      = new SystemApplicationConsole();

        try
        {
            // define the cluster port
            Capture<Integer> clusterPort = new Capture<Integer>(getAvailablePortIterator());

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(clusterPort).setSiteName("london")
                    .setCacheConfigURI("test-cachestore-eventchannel-cache-config.xml").setClusterName("passive")
                    .setSystemProperty("write.behind.delay",
                                       1);

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.address", londonServer.getSystemProperty("proxy.address"));
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            final int ENTRY_COUNT = 500;

            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, ENTRY_COUNT, "LONDON", true);

            System.out.printf("Waiting for Event Distribution to complete.\n");

            NamedCache testCache  = CacheFactory.getCache(cacheName);
            NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

            Eventually.assertThat(invoking(testCache), is(sameAs(otherCache)));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (londonServer != null)
            {
                londonServer.close();
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
     * (optionally using bulk operations).
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

        @SuppressWarnings("unchecked") Map<Integer, String> mapInserts = usePutAll
                                                                         ? new HashMap<Integer, String>()
                                                                         : (Map<Integer, String>) namedCache;

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
     * Generates a pseudo random {@link String} consisting of characters from the specified {@link String} and of
     * the specified length.
     *
     * @param random    The {@link Random} to use for generating random integers
     * @param sFrom     A non-null and non-empty {@link String} of charaters
     * @param cLength   The desired length of the {@link String}
     *
     * @return a pseudo random {@link String}
     */
    public String randomString(Random random,
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
}
