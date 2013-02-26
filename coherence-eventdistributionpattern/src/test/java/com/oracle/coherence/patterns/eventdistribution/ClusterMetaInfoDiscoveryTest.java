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
import com.tangosol.net.InvocationService;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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


    /**
     * A simple method to wait a specified amount of time, with a message to stdout.
     *
     * @param timeMs The time to wait in ms.
     * @param rationale The rationale (message) for waiting.
     * @throws InterruptedException When interrupted while waiting.
     */
    public void wait(long   time,
                     String rationale) throws InterruptedException
    {
        System.out.printf("%s: Waiting %dms %s\n", new Date(), time, rationale);
        Thread.sleep(time);
    }


    private ClusterMemberSchema newClusterMemberSchema()
    {
        return new ClusterMemberSchema().setEnvironmentVariables(PropertiesBuilder.fromCurrentEnvironmentVariables())
            .setCacheConfigURI("test-cluster-cache-config.xml").setPofConfigURI("test-pof-config.xml")
            .setSingleServerMode().setClusterPort(portIterator).setSystemProperty("proxy.port",
                                                                                  portIterator).setJMXPort(portIterator)
                                                                                  .setJMXManagementMode(JMXManagementMode
                                                                                      .LOCAL_ONLY);
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
        ClusterMember server = null;

        try
        {
            // build the server
            ClusterMemberSchema serverSchema = newClusterMemberSchema();
            JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> serverBuilder =
                new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

            server = serverBuilder.realize(serverSchema, "DCGNRI", new SystemApplicationConsole());

            // wait for the server cluster to start
            server.getClusterMBeanInfo();

            // wait for server extend proxy service to start
            server.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
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

            if (server != null)
            {
                server.destroy();
            }
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
        ClusterMember server = null;

        try
        {
            // build the server
            ClusterMemberSchema serverSchema =
                newClusterMemberSchema().setClusterName("waterloo").setSiteName("london");
            JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> serverBuilder =
                new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

            server = serverBuilder.realize(serverSchema, "SCGNRI", new SystemApplicationConsole());

            // wait for the server cluster to start
            server.getClusterMBeanInfo();

            // wait for server extend proxy service to start
            server.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
            System.setProperty("remote.port", server.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");

            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            InvocationService invocationService =
                (InvocationService) CacheFactory.getService("ExtendTcpInvocationService");

            Map result = invocationService.query(new GetClusterMetaInfoAgent(), null);

            ClusterMetaInfo clusterMetaInfo = (ClusterMetaInfo) (result != null && result.size() == 1
                                                                 ? result.values().iterator().next() : null);

            assertTrue(clusterMetaInfo.getClusterName().equals("waterloo"));
            assertTrue(clusterMetaInfo.getSiteName().equals("london"));
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (server != null)
            {
                server.destroy();
            }
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
        ClusterMember server = null;

        try
        {
            // build the server
            ClusterMemberSchema serverSchema = newClusterMemberSchema();
            JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> serverBuilder =
                new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

            server = serverBuilder.realize(serverSchema, "DCGNRI", new SystemApplicationConsole());

            // wait for the server cluster to start
            server.getClusterMBeanInfo();

            // wait for server extend proxy service to start
            server.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
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

            if (server != null)
            {
                server.destroy();
            }
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
        ClusterMember server = null;

        try
        {
            // build the server
            ClusterMemberSchema serverSchema =
                newClusterMemberSchema().setClusterName("waterloo").setSiteName("london");
            JavaApplicationBuilder<ClusterMember, ClusterMemberSchema> serverBuilder =
                new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>();

            server = serverBuilder.realize(serverSchema, "SCGNRC", new SystemApplicationConsole());

            // wait for the server cluster to start
            server.getClusterMBeanInfo();

            // wait for server extend proxy service to start
            server.getServiceMBeanInfo("ExtendTcpProxyService", 1);

            // turn off local clustering so we don't connect with the process just started
            System.setProperty("tangosol.coherence.tcmp.enabled", "false");
            System.setProperty("remote.port", server.getSystemProperty("proxy.port"));
            System.setProperty("tangosol.pof.config", "test-pof-config.xml");

            CacheFactory
                .setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("test-client-cache-config.xml"));

            ClusterMetaInfo clusterMetaInfo = (ClusterMetaInfo) CacheFactory.getCache("remote").invoke("$dumby$",
                                                                                                       new RemoteCacheEventChannel
                                                                                                           .GetClusterMetaInfoProcessor());

            assertTrue(clusterMetaInfo.getClusterName().equals("waterloo"));
            assertTrue(clusterMetaInfo.getSiteName().equals("london"));
        }
        catch (Exception exception)
        {
            throw exception;
        }
        finally
        {
            // shutdown our local use of coherence
            CacheFactory.shutdown();

            if (server != null)
            {
                server.destroy();
            }
        }
    }
}
