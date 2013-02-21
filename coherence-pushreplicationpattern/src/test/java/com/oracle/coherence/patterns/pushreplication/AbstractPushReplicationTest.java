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

import com.oracle.coherence.common.resourcing.AbstractDeferredResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceUnavailableException;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelControllerMBean;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.channels.BinaryEntryStoreEventChannel;
import com.oracle.coherence.patterns.eventdistribution.channels.CacheStoreEventChannel;
import com.oracle.coherence.patterns.eventdistribution.filters.ExampleEventFilter;
import com.oracle.tools.junit.AbstractTest;
import com.oracle.tools.runtime.Application;
import com.oracle.tools.runtime.LifecycleEvent;
import com.oracle.tools.runtime.LifecycleEventInterceptor;
import com.oracle.tools.runtime.PropertiesBuilder;
import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema.JMXManagementMode;
import com.oracle.tools.runtime.console.SystemApplicationConsole;
import com.oracle.tools.runtime.java.ExternalJavaApplicationBuilder;
import com.oracle.tools.runtime.java.JavaApplicationBuilder;
import com.oracle.tools.runtime.network.AvailablePortIterator;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Filter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.ObjectName;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

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
public abstract class AbstractPushReplicationTest extends AbstractTest
{
    /**
     * The maximum amount of time we should ever what for a test/assertion to complete.
     */
    private static final long TEST_TIMEOUT_MS = 120000;    // two minutes

    /**
     * The {@link AvailablePortIterator} is use to find available ports.
     */
    private static AvailablePortIterator availablePortIterator;

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
     * Setup the test infrastructure.
     *
     * @throws UnknownHostException When we fail to locate the localhost on which the tests will be running.
     */
    @BeforeClass
    public static void setup() throws UnknownHostException
    {
        availablePortIterator = new AvailablePortIterator(30000);
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
        return availablePortIterator;
    }


    /**
     * Constructs a new {@link ClusterMemberSchema} on which sub-classes of this class should
     * configure new {@link ClusterMemberSchema}s.  ie: This method returns a pre-configured
     * {@link ClusterMemberSchema} that the tests in this class expect to use.
     *
     * @return {@link ClusterMemberSchema}
     */
    protected ClusterMemberSchema newBaseClusterMemberSchema(int iClusterPort)
    {
        ClusterMemberSchema schema =
            new ClusterMemberSchema().setEnvironmentVariables(PropertiesBuilder.fromCurrentEnvironmentVariables())
                .setSingleServerMode()
                .setClusterPort(iClusterPort == 0 ? getAvailablePortIterator().next() : iClusterPort)
                .setPofConfigURI("test-pof-config.xml").setJMXPort(getAvailablePortIterator())
                .setJMXManagementMode(JMXManagementMode.ALL);

        schema.addLifecycleInterceptor(new LifecycleEventInterceptor<ClusterMember>()
        {
            @Override
            public void onEvent(LifecycleEvent<ClusterMember> event)
            {
                if (event.getType() == Application.EventKind.REALIZED)
                {
                    ClusterMember member = event.getObject();

                    // wait for the coherence application cluster MBean to become available
                    member.getClusterMBeanInfo();

                    // wait for the defined extend tcp proxy service to become available
                    // ToDo note there must be a safer way to check for the extend service - this expects to always
                    // be able to see the proxy service on member 1
                    member.getServiceMBeanInfo("ExtendTcpProxyService", 1);
                }
            }
        });

        return schema;
    }


    protected abstract ClusterMemberSchema newPassiveClusterMemberSchema(int iPort);


    protected abstract ClusterMemberSchema newActiveClusterMemberSchema(int iPort);


    protected JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> newClusterMemberBuilder()
    {
        return new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();
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

            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newActiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setSystemProperty("proxy.port",
                            getAvailablePortIterator())
                                           .setSystemProperty("remote.port", getAvailablePortIterator());

            londonServerSchema.setSystemProperty("tangosol.coherence.log",
                                                 "/Users/narliss/dev/git/coherence-incubator/coherence-pushreplicationpattern/testActiveActive-london.log");
            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            // establish the newyork server
            ClusterMemberSchema newyorkServerSchema =
                newActiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("newyork")
                    .setSystemProperty("proxy.port",
                            londonServer.getSystemProperty("remote.port")).setSystemProperty("remote.port",
                        londonServer
                                .getSystemProperty("proxy.port"));

            newyorkServerSchema.setSystemProperty("tangosol.coherence.log",
                                                  "/Users/narliss/dev/git/coherence-incubator/coherence-pushreplicationpattern/testActiveActive-ny.log");
            newyorkServer = builder.realize(newyorkServerSchema, "NEWYORK", console);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

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
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // construct a deferred NamedCache provider that is only available when the caches are identical
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>("LONDON publishing-cache",
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache namedCache = CacheFactory.getCache(cacheName);

                    return sameMaps(newyorkCache, namedCache) ? namedCache : null;
                }
            };

            System.out.printf("\nWaiting for %d entries to synchronize between NEWYORK to LONDON.\n",
                              newyorkCache.size());

            NamedCache namedCache = deferredNamedCache.getResource();

            Assert.assertNotNull(namedCache);

            // update the london site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 1000, 500, "LONDON");

            // grab a copy of the london site entries (we'll use this for comparison later)
            final Map<Object, Object> londonCache = new HashMap<Object, Object>(CacheFactory.getCache(cacheName));

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();

            // assert that the value is in the newyork cluster
            System.setProperty("remote.port", newyorkServer.getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // construct a deferred NamedCache provider that is only available the marker is available
            deferredNamedCache = new AbstractDeferredResourceProvider<NamedCache>("NEWYORK publishing-cache",
                                                                                  200,
                                                                                  TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache namedCache = CacheFactory.getCache(cacheName);

                    return sameMaps(londonCache, namedCache) ? namedCache : null;
                }
            };

            System.out.printf("\nWaiting for %d entries to synchronize between LONDON and NEWYORK.\n",
                              londonCache.size());
            namedCache = deferredNamedCache.getResource();
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

            if (londonServer != null)
            {
                londonServer.destroy();
            }

            if (newyorkServer != null)
            {
                newyorkServer.destroy();
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
        String       siteName         = "testActivePassive";
        final String cacheName        = "publishing-cache";

        int          nrPassiveServers = 2;
        int          nrActiveServers  = 2;

        ArrayList<ClusterMember> passiveServers = new ArrayList<ClusterMember>(nrPassiveServers);
        ArrayList<ClusterMember> activeServers = new ArrayList<ClusterMember>(nrActiveServers);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder =
            new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

        try
        {
            // establish the passive cluster
            int                      iPort   = getAvailablePortIterator().next();
            SystemApplicationConsole console = new SystemApplicationConsole();

            for (int i = 0; i < nrPassiveServers; i++)
            {
                ClusterMemberSchema passiveServerSchema =
                    newPassiveClusterMemberSchema(iPort).setSiteName(siteName)
                        .setSystemProperty("tangosol.coherence.log",
                                "/Users/narliss/dev/git/coherence-incubator/coherence-pushreplicationpattern/testActivePassive-passive-"
                                        + i + ".log");

                passiveServers.add(builder.realize(passiveServerSchema, String.format("PASSIVE %s", i), console));
            }

            // establish the active cluster
            iPort = getAvailablePortIterator().next();

            for (int i = 0; i < nrActiveServers; i++)
            {
                ClusterMemberSchema activeServerSchema =
                    newActiveClusterMemberSchema(iPort).setSiteName(siteName).setSystemProperty("remote.port",
                            passiveServers.get(0)
                                    .getSystemProperty("proxy.port"))
                                                                                                        .setSystemProperty("tangosol.coherence.log",
                                                                                                                "/Users/narliss/dev/git/coherence-incubator/coherence-pushreplicationpattern/testActivePassive-active-"
                                                                                                                        + i + ".log");

                activeServers.add(builder.realize(activeServerSchema, String.format("ACTIVE  %d", i), console));
            }

            // wait for passive cluster to start
            for (ClusterMember server : passiveServers)
            {
                server.getClusterMBeanInfo();
                server.getServiceMBeanInfo("ExtendTcpProxyService", 1);
            }

            // wait for active server cluster to start
            for (ClusterMember server : activeServers)
            {
                server.getClusterMBeanInfo();
                server.getServiceMBeanInfo("ExtendTcpProxyService", 1);
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

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
            System.setProperty("remote.port", passiveServers.get(0).getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // construct a deferred NamedCache provider that is only available when the caches are identical
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>("PASSIVE publishing-cache",
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
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
                server.destroy();
            }

            for (ClusterMember server : activeServers)
            {
                server.destroy();
            }
        }
    }


    /**
     * Performs Active-Passive topology tests (but with the channels disabled)
     *
     * @throws Exception
     */
    @Test(expected = ResourceUnavailableException.class)
    public final void testActivePassiveDisabled() throws Exception
    {
        ClusterMember                                              passiveServer = null;
        ClusterMember                                              activeServer  = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder       = newClusterMemberBuilder();
        SystemApplicationConsole                                   console       = new SystemApplicationConsole();

        try
        {
            // establish the passive cluster
            ClusterMemberSchema passiveServerSchema =
                newPassiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("sydney");

            passiveServer = builder.realize(passiveServerSchema, "PASSIVE", console);

            // establish the active cluster
            ClusterMemberSchema activeServerSchema =
                newActiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("sydney")
                    .setSystemProperty("remote.port",
                            passiveServer.getSystemProperty("proxy.port"))
                                           .setSystemProperty("channel.starting.mode", "disabled");

            activeServer = builder.realize(activeServerSchema, "ACTIVE", console);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.port", activeServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // put a value in the active site
            CacheFactory.getCache("publishing-cache").put("message", "hello world");

            // shutdown our connection to the active site
            CacheFactory.shutdown();

            // assert that the value is in the passive site
            System.setProperty("remote.port", passiveServer.getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // construct a deferred NamedCache provider that is only available when it's of a certain size.
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>(String
                    .format("PASSIVE publishing-cache with %d entries", 1),
                                                                 200,
                                                                 TEST_TIMEOUT_MS / 40)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache namedCache = CacheFactory.getCache("publishing-cache");

                    return namedCache.size() == 1 ? namedCache : null;
                }
            };

            System.out.printf("Waiting for ACTIVE and PASSIVE sites to synchronize.\n");
            deferredNamedCache.getResource();
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
                passiveServer.destroy();
            }

            if (activeServer != null)
            {
                activeServer.destroy();
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
        String       siteName         = "testActivePassive";
        final String cacheName        = "publishing-cache";

        int          nrPassiveServers = 2;
        int          nrActiveServers  = 2;

        ArrayList<ClusterMember> passiveServers = new ArrayList<ClusterMember>(nrPassiveServers);
        ArrayList<ClusterMember> activeServers = new ArrayList<ClusterMember>(nrActiveServers);

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder =
            new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

        try
        {
            // establish the passive cluster
            int                      iPort   = getAvailablePortIterator().next();
            SystemApplicationConsole console = new SystemApplicationConsole();

            for (int i = 0; i < nrPassiveServers; i++)
            {
                ClusterMemberSchema passiveServerSchema = newPassiveClusterMemberSchema(iPort).setSiteName(siteName);

                passiveServers.add(builder.realize(passiveServerSchema, String.format("PASSIVE %s", i), console));
            }

            // establish the active cluster
            iPort = getAvailablePortIterator().next();

            for (int i = 0; i < nrActiveServers; i++)
            {
                ClusterMemberSchema activeServerSchema =
                    newActiveClusterMemberSchema(iPort)
                        .setCacheConfigURI("test-remotecluster-eventtransformation-cache-config.xml")
                        .setSiteName(siteName).setSystemProperty("remote.port",
                                                                 passiveServers.get(0).getSystemProperty("proxy.port"));

                activeServers.add(builder.realize(activeServerSchema, String.format("ACTIVE  %d", i), console));
            }

            // wait for passive cluster to start
            for (ClusterMember server : passiveServers)
            {
                server.getClusterMBeanInfo();
                server.getServiceMBeanInfo("ExtendTcpProxyService", 1);
            }

            // wait for active server cluster to start
            for (ClusterMember server : activeServers)
            {
                server.getClusterMBeanInfo();
                server.getServiceMBeanInfo("ExtendTcpProxyService", 1);
            }

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

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
            System.setProperty("remote.port", passiveServers.get(0).getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // construct a deferred NamedCache provider that is only available when the caches are identical
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>("PASSIVE publishing-cache",
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache          namedCache = CacheFactory.getCache(cacheName);

                    Equivalence<String> eq         = new Equivalence<String>()
                    {
                        @Override
                        public boolean equals(String x,
                                              String y)
                        {
                            return x.equalsIgnoreCase(y);
                        }
                    };

                    return sameMaps(activeCache, namedCache, eq) ? namedCache : null;
                }
            };

            System.out.printf("\nWaiting for %d entries to synchronize between ACTIVE and PASSIVE site.\n",
                              activeCache.size());

            NamedCache namedCache = deferredNamedCache.getResource();

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
                server.destroy();
            }

            for (ClusterMember server : activeServers)
            {
                server.destroy();
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
        String        siteName      = "testActivePassiveWithFiltering";
        final String  cacheName     = "publishing-cache";

        ClusterMember passiveServer = null;
        ClusterMember activeServer  = null;

        JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> builder =
            new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

        SystemApplicationConsole console = new SystemApplicationConsole();

        try
        {
            // establish the passive cluster
            ClusterMemberSchema passiveServerSchema =
                newPassiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName(siteName);

            passiveServer = builder.realize(passiveServerSchema, "PASSIVE", console);

            passiveServer.getClusterMBeanInfo();
            passiveServer.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // establish the active cluster
            ClusterMemberSchema activeServerSchema =
                newActiveClusterMemberSchema(getAvailablePortIterator().next())
                    .setCacheConfigURI("test-remotecluster-eventfiltering-cache-config.xml").setSiteName(siteName)
                    .setSystemProperty("remote.port",
                            passiveServer.getSystemProperty("proxy.port"));

            activeServer = builder.realize(activeServerSchema, "ACTIVE", console);

            activeServer.getClusterMBeanInfo();
            activeServer.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

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
            System.setProperty("remote.port", passiveServer.getSystemProperty("proxy.port"));
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // construct a deferred NamedCache provider that is only available when the caches are identical
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>("PASSIVE publishing-cache",
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache          namedCache = CacheFactory.getCache(cacheName);

                    Equivalence<String> eq         = new Equivalence<String>()
                    {
                        @Override
                        public boolean equals(String x,
                                              String y)
                        {
                            return x.equalsIgnoreCase(y);
                        }
                    };

                    return sameMaps(activeCache, namedCache, eq) ? namedCache : null;
                }
            };

            System.out.printf("\nWaiting for %d entries to synchronize between ACTIVE and PASSIVE site.\n",
                              activeCache.size());

            NamedCache namedCache = deferredNamedCache.getResource();

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
                passiveServer.destroy();
            }

            if (activeServer != null)
            {
                activeServer.destroy();
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
            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setCacheConfigURI("test-cachestore-eventchannel-cache-config.xml").setClusterName("passive");

            londonServer = builder.realize(londonServerSchema, "LONDON");

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "LONDON");

            // construct a deferred NamedCache provider that is only available the test cache and copy of the test cache
            // are the same
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>(String.format("replication with %d updates", 500),
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache testCache  = CacheFactory.getCache(cacheName);
                    NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

                    return sameMaps(testCache, otherCache) ? testCache : null;
                }
            };

            System.out.printf("Waiting for Event Distribution to complete.\n");
            deferredNamedCache.getResource();

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
                londonServer.destroy();
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
            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setCacheConfigURI("test-binaryentrystore-eventchannel-cache-config.xml").setClusterName("passive");

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "LONDON");

            // construct a deferred NamedCache provider that is only available the test cache and copy of the test cache
            // are the same
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>(String.format("replication with %d updates", 500),
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache testCache  = CacheFactory.getCache(cacheName);
                    NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

                    return sameMaps(testCache, otherCache) ? testCache : null;
                }
            };

            System.out.printf("Waiting for Event Distribution to complete.\n");
            deferredNamedCache.getResource();

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
                londonServer.destroy();
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
            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setCacheConfigURI("test-custom-eventchannel-cache-config.xml").setClusterName("passive");

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            final int changes = randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, 500, "LONDON");

            // get the MBean for the EventChannel
            final EventChannelControllerMBean mBean =
                londonServer.getMBeanProxy(new ObjectName(String.format("Coherence:type=EventChannels,id=%s,nodeId=1",
                                                                        cacheName + "-" + "Custom Event Channel")),
                                           EventChannelControllerMBean.class);

            ResourceProvider<Integer> eventsDistributed =
                new AbstractDeferredResourceProvider<Integer>("Event Distribution Count",
                                                              200,
                                                              TEST_TIMEOUT_MS)
            {
                @Override
                protected Integer ensureResource() throws ResourceUnavailableException
                {
                    return mBean.getEventsDistributedCount() == changes ? changes : null;
                }
            };

            System.out.printf("Waiting for Event Distribution to complete.\n");
            eventsDistributed.getResource();

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
                londonServer.destroy();
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
            ClusterMemberSchema londonServerSchema =
                newActiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setSystemProperty("proxy.port",
                            getAvailablePortIterator()).setSystemProperty("remote.port",
                        getAvailablePortIterator())
                                                                                         .setSystemProperty("conflict.resolver.classname",
                                                                                                 "com.oracle.coherence.patterns.pushreplication.MergingConflictResolver")
                                                                                                                .setSystemProperty("tangosol.coherence.log",
                                                                                                                        "/Users/narliss/dev/git/coherence-incubator/coherence-pushreplicationpattern/testActiveActiveCR-london.log");

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            // establish the newyork server
            ClusterMemberSchema newyorkServerSchema =
                newActiveClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("newyork")
                    .setSystemProperty("proxy.port",
                            londonServer.getSystemProperty("remote.port")).setSystemProperty("remote.port",
                        londonServer
                                .getSystemProperty("proxy.port"))
                                                                                                                .setSystemProperty("conflict.resolver.classname",
                                                                                                                        "com.oracle.coherence.patterns.pushreplication.MergingConflictResolver")
                    .setSystemProperty("tangosol.coherence.log",
                            "/Users/narliss/dev/git/coherence-incubator/coherence-pushreplicationpattern/testActiveActiveCR-NY.log");
            ;

            newyorkServer = builder.realize(newyorkServerSchema, "NEWYORK", console);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

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
            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // place a partition set into LONDON
            setPartitions = new PartitionSet(3);
            setPartitions.add(1);
            CacheFactory.getCache(cacheName).put("partitions", setPartitions);

            // construct a deferred NamedCache provider that is only available the the merged partition set is available
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>("NEWYORK publishing-cache",
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache   namedCache    = CacheFactory.getCache(cacheName);
                    PartitionSet setPartitions = (PartitionSet) namedCache.get("partitions");

                    return setPartitions.contains(0) && setPartitions.contains(1) ? namedCache : null;
                }
            };

            Assert.assertNotNull(deferredNamedCache.getResource());

            // shutdown our connection to the london cluster
            CacheFactory.shutdown();

            // TODO: assert that the newyork cluster has the same merged partition
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
                londonServer.destroy();
            }

            if (newyorkServer != null)
            {
                newyorkServer.destroy();
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
            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setCacheConfigURI("test-cachestore-eventchannel-cache-config.xml").setClusterName("passive")
                    .setSystemProperty("write.behind.delay",
                                       1);

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            final int ENTRY_COUNT = 500;

            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, ENTRY_COUNT, "LONDON");

            // construct a deferred NamedCache provider that is only available when the test cache and copy of the test cache
            // are the same
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>(String.format("replication with %d updates",
                                                                               ENTRY_COUNT),
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache testCache  = CacheFactory.getCache(cacheName);
                    NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

                    return sameMaps(testCache, otherCache) ? testCache : null;
                }
            };

            System.out.printf("Waiting for Event Distribution to complete.\n");
            deferredNamedCache.getResource();

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
                londonServer.destroy();
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
            // establish the london server
            ClusterMemberSchema londonServerSchema =
                newBaseClusterMemberSchema(getAvailablePortIterator().next()).setSiteName("london")
                    .setCacheConfigURI("test-cachestore-eventchannel-cache-config.xml").setClusterName("passive")
                    .setSystemProperty("write.behind.delay",
                                       1);

            londonServer = builder.realize(londonServerSchema, "LONDON", console);

            final String cacheName = "publishing-cache";

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");

            System.setProperty("remote.port", londonServer.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");
            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            // populate the london site
            final int ENTRY_COUNT = 500;

            randomlyPopulateNamedCache(CacheFactory.getCache(cacheName), 0, ENTRY_COUNT, "LONDON", true);

            // construct a deferred NamedCache provider that is only available when the test cache and copy of the test cache
            // are the same
            ResourceProvider<NamedCache> deferredNamedCache =
                new AbstractDeferredResourceProvider<NamedCache>(String.format("replication with %d updates",
                                                                               ENTRY_COUNT),
                                                                 200,
                                                                 TEST_TIMEOUT_MS)
            {
                @Override
                protected NamedCache ensureResource() throws ResourceUnavailableException
                {
                    NamedCache testCache  = CacheFactory.getCache(cacheName);
                    NamedCache otherCache = CacheFactory.getCache("test-" + cacheName);

                    return sameMaps(testCache, otherCache) ? testCache : null;
                }
            };

            System.out.printf("Waiting for Event Distribution to complete.\n");
            deferredNamedCache.getResource();

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
                londonServer.destroy();
            }
        }
    }


    /**
     * Randomly populates the specified {@link NamedCache} by inserting, updating and removing a number of entries
     * (using single operation methods)
     *
     * @param namedCache        The {@link NamedCache} to populate.
     * @param minumumKey        The minimum key value (inclusive)
     * @param nrEntries         The number of entries to generate
     * @param siteName          The name of the site in which the {@link NamedCache} is defined (for logging purposes).
     * @param usePutAll         Should putAll be used to perform inserts
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
     * @param minumumKey        The minimum key value (inclusive)
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
                                          boolean    fUsePutAll)
    {
        Random random = new Random();

        // insert a number of random entries
        System.out.printf("Inserting %d random entries into %s site.\n", nrEntries, siteName);

        @SuppressWarnings("unchecked") Map<Integer, String> mapInserts = fUsePutAll
                                                                         ? new HashMap<Integer, String>()
                                                                         : (Map<Integer, String>) namedCache;

        for (int i = 0; i < nrEntries; i++)
        {
            int    key   = minimumKey + i;
            String value = randomString(random, "abcdefghijklmnopqrstuvwxyz", 20);

            mapInserts.put(key, value);
        }

        if (fUsePutAll)
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


    /**
     * Compares two {@link Map}s for deep equality.
     *
     * @param map1 The first {@link Map} to compare
     * @param map2 The second {@link Map} to compare
     *
     * @return <code>true</code> if both {@link Map}s contain exactly the same values.
     */
    @SuppressWarnings("rawtypes")
    public boolean sameMaps(Map map1,
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


    /**
     * Compares two {@link Map}s for equality based on a specified implementation of {@link Equivalence}.
     *
     * @param map1          The first {@link Map} to compare
     * @param map2          The second {@link Map} to compare
     * @param equivalence   The {@link Equivalence} implementation for comparison
     *
     * @return <code>true</code> if both {@link Map}s contain exactly the same values based on the {@link Equivalence} implementation
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> boolean sameMaps(Map            map1,
                                Map            map2,
                                Equivalence<T> equivalence)
    {
        int cSize1 = map1.size();
        int cSize2 = map2.size();

        if (cSize1 == cSize2)
        {
            boolean same = true;

            for (Iterator i = map1.keySet().iterator(); same && i.hasNext(); )
            {
                Object key    = i.next();
                T      value1 = (T) map1.get(key);
                T      value2 = (T) map2.get(key);

                same = (value1 == value2) || (value1 != null && equivalence.equals(value1, value2));
            }

            for (Iterator i = map2.keySet().iterator(); same && i.hasNext(); )
            {
                Object key    = i.next();
                T      value1 = (T) map1.get(key);
                T      value2 = (T) map2.get(key);

                same = (value1 == value2) || (value2 != null && equivalence.equals(value2, value1));
            }

            return same;
        }
        else
        {
            return false;
        }
    }


    /**
     * An abstraction of the equals method.
     */
    public static interface Equivalence<T>
    {
        /**
         * Method description
         *
         * @param x
         * @param y
         *
         * @return
         */
        public boolean equals(T x,
                              T y);
    }
}
