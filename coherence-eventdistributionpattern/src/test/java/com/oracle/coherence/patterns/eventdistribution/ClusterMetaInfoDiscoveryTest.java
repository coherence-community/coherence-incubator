/*
 * File: ClusterMetaInfoDiscoveryTest.java
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

package com.oracle.coherence.patterns.eventdistribution;

import com.oracle.coherence.common.cluster.ClusterMetaInfo;

import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannel.GetClusterMetaInfoAgent;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.RemoteCacheEventChannel;

import com.oracle.tools.junit.AbstractTest;

import com.oracle.tools.runtime.LocalPlatform;

import com.oracle.tools.runtime.coherence.CoherenceCacheServer;
import com.oracle.tools.runtime.coherence.CoherenceCacheServerSchema;
import com.oracle.tools.runtime.coherence.JMXManagementMode;

import com.oracle.tools.runtime.console.SystemApplicationConsole;

import com.oracle.tools.runtime.java.JavaApplicationBuilder;
import com.oracle.tools.runtime.java.LocalJavaApplicationBuilder;

import com.oracle.tools.runtime.network.AvailablePortIterator;
import com.oracle.tools.runtime.network.Constants;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.InvocationService;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.tools.deferred.DeferredHelper.invoking;

import static com.oracle.tools.deferred.Eventually.assertThat;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertNotNull;

import java.net.UnknownHostException;

import java.util.Map;
import java.util.Properties;

/**
 * The {@link ClusterMetaInfoDiscoveryTest} is designed to exercise the implementations that {@link EventDistributor}
 * and {@link EventChannel} implementations may use to determine the {@link ClusterMetaInfo} for a remote cluster.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian
 */
public class ClusterMetaInfoDiscoveryTest extends AbstractTest
{
    private static AvailablePortIterator portIterator;

    private Properties                   systemProperties;


    /**
     * Do this before each test
     */
    @Before
    public void beforeEachTest()
    {
        // take a copy of the system properties (as we're going to change them)
        systemProperties = System.getProperties();
    }


    /**
     * Do this after each test
     */
    @After
    public void afterEachTest()
    {
        // reset the system properties
        System.setProperties(systemProperties);
    }


    /**
     * Setup the tests
     *
     * @throws UnknownHostException
     */
    @BeforeClass
    public static void setup() throws UnknownHostException
    {
        portIterator = new AvailablePortIterator(40000);
    }


    private CoherenceCacheServerSchema newCacheServerSchema()
    {
        return new CoherenceCacheServerSchema().setCacheConfigURI("test-cluster-cache-config.xml")
            .setPofConfigURI("test-pof-config.xml").useLocalHostMode().setClusterPort(portIterator)
            .setSystemProperty("proxy.port",
                               portIterator).setJMXPort(portIterator).setJMXManagementMode(JMXManagementMode.LOCAL_ONLY)
                                   .setSystemProperty("proxy.host",
                                                      Constants.getLocalHost());
    }


    /**
     * Ensure we can detect the default cluster info using a remote invocation service
     *
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testDetermineDefaultClusterMetaInfoWithRemoteInvocationService() throws Exception
    {
        LocalPlatform              platform     = LocalPlatform.getInstance();

        CoherenceCacheServerSchema serverSchema = newCacheServerSchema();

        try (CoherenceCacheServer server = platform.realize("DCGNRI", serverSchema, new SystemApplicationConsole()))
        {
            // wait for the server cluster to start
            assertThat(invoking(server).getClusterSize(), is(1));

            // wait for server extend proxy service to start
            assertThat(invoking(server).isServiceRunning("ExtendTcpProxyService"), is(true));

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
            System.setProperty("remote.address", Constants.getLocalHost());
            System.setProperty("remote.port", server.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");

            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            InvocationService invocationService =
                (InvocationService) CacheFactory.getService("ExtendTcpInvocationService");

            Map result = invocationService.query(new GetClusterMetaInfoAgent(), null);

            ClusterMetaInfo clusterMetaInfo = (ClusterMetaInfo) (result != null && result.size() == 1
                                                                 ? result.values().iterator().next() : null);

            assertNotNull(clusterMetaInfo);
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();
        }
    }


    /**
     * Ensure we can detect a specific cluster info using a remote invocation service
     *
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testDetermineSpecificClusterMetaInfoWithRemoteInvocationService() throws Exception
    {
        LocalPlatform platform = LocalPlatform.getInstance();

        CoherenceCacheServerSchema serverSchema =
            newCacheServerSchema().setClusterName("waterloo").setSiteName("london");

        try (CoherenceCacheServer server = platform.realize("SCGNRI", serverSchema, new SystemApplicationConsole()))
        {
            // wait for the server cluster to start
            assertThat(invoking(server).getClusterSize(), is(1));

            // wait for server extend proxy service to start
            assertThat(invoking(server).isServiceRunning("ExtendTcpProxyService"), is(true));

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
            System.setProperty("remote.address", Constants.getLocalHost());
            System.setProperty("remote.port", server.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");

            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            InvocationService invocationService =
                (InvocationService) CacheFactory.getService("ExtendTcpInvocationService");

            Map result = invocationService.query(new GetClusterMetaInfoAgent(), null);

            ClusterMetaInfo clusterMetaInfo = (ClusterMetaInfo) (result != null && result.size() == 1
                                                                 ? result.values().iterator().next() : null);

            assertThat(clusterMetaInfo.getClusterName(), is("waterloo"));
            assertThat(clusterMetaInfo.getSiteName(), is("london"));
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();
        }
    }


    /**
     * Ensure the can detect default cluster infor with a remote cache service
     *
     * @throws Exception
     */
    @Test
    public void testDetermineDefaultClusterMetaInfoWithRemoteCacheService() throws Exception
    {
        LocalPlatform              platform     = LocalPlatform.getInstance();

        CoherenceCacheServerSchema serverSchema = newCacheServerSchema();

        try (CoherenceCacheServer server = platform.realize("DCGNRI", serverSchema, new SystemApplicationConsole()))
        {
            // wait for the server cluster to start
            assertThat(invoking(server).getClusterSize(), is(1));

            // wait for server extend proxy service to start
            assertThat(invoking(server).isServiceRunning("ExtendTcpProxyService"), is(true));

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
            System.setProperty("remote.address", Constants.getLocalHost());
            System.setProperty("remote.port", server.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");

            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            ClusterMetaInfo clusterMetaInfo = (ClusterMetaInfo) CacheFactory.getCache("remote").invoke("$dumby$",
                                                                                                       new RemoteCacheEventChannel
                                                                                                           .GetClusterMetaInfoProcessor());

            assertNotNull(clusterMetaInfo);
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();
        }
    }


    /**
     * Ensure we can detect specific cluster info with a remote cache service
     *
     * @throws Exception
     */
    @Test
    public void testDetermineSpecificClusterMetaInfoWithRemoteCacheService() throws Exception
    {
        LocalPlatform platform = LocalPlatform.getInstance();

        CoherenceCacheServerSchema serverSchema =
            newCacheServerSchema().setClusterName("waterloo").setSiteName("london");

        try (CoherenceCacheServer server = platform.realize("SCGNRC", serverSchema, new SystemApplicationConsole()))
        {
            // wait for the server cluster to start
            assertThat(invoking(server).getClusterSize(), is(1));

            // wait for server extend proxy service to start
            assertThat(invoking(server).isServiceRunning("ExtendTcpProxyService"), is(true));

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
            System.setProperty("remote.address", Constants.getLocalHost());
            System.setProperty("remote.port", server.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");

            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            ClusterMetaInfo clusterMetaInfo = (ClusterMetaInfo) CacheFactory.getCache("remote").invoke("$dumby$",
                                                                                                       new RemoteCacheEventChannel
                                                                                                           .GetClusterMetaInfoProcessor());

            assertThat(clusterMetaInfo.getClusterName(), is("waterloo"));
            assertThat(clusterMetaInfo.getSiteName(), is("london"));
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();
        }
    }
}
