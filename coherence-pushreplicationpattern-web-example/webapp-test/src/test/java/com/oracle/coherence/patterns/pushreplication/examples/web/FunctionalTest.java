/*
 * File: FunctionalTest.java
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

package com.oracle.coherence.patterns.pushreplication.examples.web;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import com.oracle.bedrock.deferred.Deferred;
import com.oracle.bedrock.deferred.Eventually;
import com.oracle.bedrock.deferred.PermanentlyUnavailableException;
import com.oracle.bedrock.deferred.TemporarilyUnavailableException;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.PropertiesBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.coherence.patterns.pushreplication.web.examples.utilities.WebServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.ensure;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.valueOf;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test that the CoherenceWeb Examples Work
 * <p>
 * Copyright (c) 2009, 2019. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 */
public class FunctionalTest
{
    private static CoherenceCluster cluster1;
    private static CoherenceCluster cluster2;
    private static JavaApplication  site1;
    private static int              site1Port;
    private static JavaApplication  site2;
    private static int              site2Port;


    /**
     * Setup the test including cache and web servers
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        // Staring a process with oracle bedrock from within an oracle bedrock process appears to be problematic
        // we'll parse the config and start the processes here.
        AvailablePortIterator portIter      = LocalPlatform.get().getAvailablePorts();

        int                   acceptor1Port = portIter.next();
        int                   acceptor2Port = portIter.next();

        // Setup site 1
        PropertiesBuilder globalProps1 = WebServer.parseConfig("System-Property", "site1.properties");
        PropertiesBuilder cohProps1    = WebServer.parseConfig("COHSystem-Property", "site1.properties");

        PropertiesBuilder cache1Props  = new PropertiesBuilder(globalProps1);

        cache1Props.addProperties(cohProps1);

        // Override the system properties for ports for the test environment
        cache1Props.setProperty("client.port", acceptor2Port);
        cache1Props.setProperty("bind.port", acceptor1Port);

        // Setup site 2
        PropertiesBuilder globalProps2 = WebServer.parseConfig("System-Property", "site2.properties");
        PropertiesBuilder cohProps2    = WebServer.parseConfig("COHSystem-Property", "site2.properties");

        PropertiesBuilder cache2Props  = new PropertiesBuilder(globalProps2);

        cache2Props.addProperties(cohProps2);

        // Override the system properties for ports for the test environment
        cache2Props.setProperty("client.port", acceptor1Port);
        cache2Props.setProperty("bind.port", acceptor2Port);

        LocalPlatform platform = LocalPlatform.get();

        // Startup Site1
        site1Port = portIter.next();

        cluster1  = WebServer.startCluster(cache1Props);

        site1 = platform.launch(JavaApplication.class,
                                DisplayName.of("Site1-Web"),
                                ClassName.of("com.oracle.coherence.patterns.pushreplication.web.examples.utilities.WebServer"),
                                Argument.of("site1.properties"),
                                Argument.of(String.valueOf(site1Port)),
                                new SystemProperties(globalProps1.realize()),
                                SystemProperty.of("client.port", acceptor2Port),
                                SystemProperty.of("bind.port", acceptor1Port),
                                SystemProperty.of("proxy.enabled", false),
                                Console.system());

        Eventually.assertThat(invoking(cluster1).getClusterSize(), is(1));

        // Startup Site2
        site2Port = portIter.next();

        cluster2  = WebServer.startCluster(cache2Props);

        site2 = platform.launch(JavaApplication.class,
                                DisplayName.of("Site2-Web"),
                                ClassName.of("com.oracle.coherence.patterns.pushreplication.web.examples.utilities.WebServer"),
                                Argument.of("site2.properties"),
                                Argument.of(String.valueOf(site2Port)),
                                new SystemProperties(globalProps2.realize()),
                                SystemProperty.of("client.port", acceptor1Port),
                                SystemProperty.of("bind.port", acceptor2Port),
                                SystemProperty.of("proxy.enabled", false),
                                Console.system());

        Eventually.assertThat(invoking(cluster2).getClusterSize(), is(1));
    }


    /**
     * Shutdown the cache and web servers
     */
    @AfterClass
    public static void tearDown()
    {
        cluster1.close();
        cluster2.close();
        site1.close();
        site2.close();
    }


    /**
     * Test Session Replication by connecting to each of the web servers and validating that session data stored in
     * one cluster is visible to the other cluster.
     *
     * @throws Exception
     */
    @Test
    public void testSessionReplication() throws Exception
    {
        try
        {
            final WebConversation wc       = new WebConversation();

            final String          site1url = "http://127.0.0.1:" + site1Port + "/sessionAccess.jsp";
            final String          site2url = "http://127.0.0.1:" + site2Port + "/sessionAccess.jsp";

            // Make initial request to siteA and add a value to the session
            WebResponse response = ensure(new DeferredWebResponse(wc, site1url));

            WebForm     form     = response.getFormWithID("HttpSessionAttributesForm");

            form.setParameter("key", "A");
            form.setParameter("value", "B");
            response = form.submit(form.getSubmitButton("action", "add"));

            // Assert that the session variables were set
            WebTable table = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));

            // Now make a request to the siteB, validate our session data is there and add another value
            Eventually.assertThat(valueOf(new DeferredRowCount(wc, site2url)), is(2));

            response = wc.getResponse(site2url);
            table    = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));

            // Add another set of variables from this side
            form = response.getFormWithID("HttpSessionAttributesForm");
            form.setParameter("key", "C");
            form.setParameter("value", "D");

            response = form.submit(form.getSubmitButton("action", "add"));
            table    = response.getTableWithID("HttpSessionAttributes");
            assertEquals("rows", 3, table.getRowCount());

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));
            assertEquals("key2", "C", table.getCellAsText(2, 0));
            assertEquals("value2", "D", table.getCellAsText(2, 1));

            // Now go back to the original site and make sure everything replicated properly
            Eventually.assertThat(valueOf(new DeferredRowCount(wc, site1url)), is(3));

            response = wc.getResponse(site1url);
            table    = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));
            assertEquals("key2", "C", table.getCellAsText(2, 0));
            assertEquals("value2", "D", table.getCellAsText(2, 1));

            System.out.println("*** Asserted both sites have the same session data shutting down the cluster!");
        }
        catch (Exception e)
        {
            e.printStackTrace();

            throw e;
        }
    }


    /**
     * Note that the DeferredRowCount is necessary to take into account that there will be some lag required for the
     * session data to replicate between sites
     */
    private class DeferredRowCount implements Deferred<Integer>
    {
        private WebConversation wc;
        private String          request;


        /**
         * Constructs a Deferred RowCount
         *
         *
         * @param wc      Handle to the web conversation
         * @param request request to make over the conversation
         */
        public DeferredRowCount(WebConversation wc,
                                String          request)
        {
            this.wc      = wc;
            this.request = request;
        }


        @Override
        public Integer get() throws PermanentlyUnavailableException, TemporarilyUnavailableException
        {
            try
            {
                WebResponse response = ensure(new DeferredWebResponse(wc, request));
                WebTable    table    = response.getTableWithID("HttpSessionAttributes");

                return table.getRowCount();
            }
            catch (Exception e)
            {
                System.out.println("Error getting rowcount for " + request + " - " + "HttpSessionAttributes");
                e.printStackTrace();

                throw new TemporarilyUnavailableException(this, e);
            }
        }


        @Override
        public Class<Integer> getDeferredClass()
        {
            return Integer.class;
        }
    }


    /**
     * Deferred WebResponse is useful for getting resources from a test environment where it may take time for the
     * resources to be available
     */
    private class DeferredWebResponse implements Deferred<WebResponse>
    {
        private WebConversation wc;
        private String          request;


        /**
         * Construct a DeferredWebResponse
         *
         *
         * @param wc      Handle to the web conversation
         * @param request Request to make over the web conversation
         */
        public DeferredWebResponse(WebConversation wc,
                                   String          request)
        {
            this.wc      = wc;
            this.request = request;
        }


        /**
         * Return the WebResponse from the Deferred.
         *
         * @return WebResponse from the Deferred.
         */
        @Override
        public WebResponse get() throws PermanentlyUnavailableException, TemporarilyUnavailableException
        {
            try
            {
                return wc.getResponse(request);
            }
            catch (Exception e)
            {
                throw new TemporarilyUnavailableException(this, e);
            }
        }


        @Override
        public Class<WebResponse> getDeferredClass()
        {
            return WebResponse.class;
        }
    }
}
