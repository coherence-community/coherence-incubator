/*
 * File: CohWebFunctionalTest.java
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

package com.oracle.coherence.patterns.pushreplication.cohweb;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

import com.oracle.coherence.patterns.pushreplication.cohweb.examples.utilities.WebServer;

import com.oracle.tools.deferred.Deferred;
import com.oracle.tools.deferred.DeferredAssert;
import com.oracle.tools.deferred.DeferredHelper;
import com.oracle.tools.deferred.ObjectNotAvailableException;

import com.oracle.tools.runtime.coherence.Cluster;

import com.oracle.tools.runtime.console.SystemApplicationConsole;

import com.oracle.tools.runtime.java.ExternalJavaApplicationBuilder;
import com.oracle.tools.runtime.java.SimpleJavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplicationSchema;

import com.oracle.tools.runtime.network.AvailablePortIterator;

import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

import java.util.Map;

import java.util.concurrent.TimeUnit;

/**
 * Test that the CoherenceWeb Examples Work
 */
public class CohWebFunctionalTest
{
    private static Cluster               cluster1;
    private static Cluster               cluster2;
    private static SimpleJavaApplication site1;
    private static int                   site1Port;
    private static SimpleJavaApplication site2;
    private static int                   site2Port;


    /**
     * Method description
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        AvailablePortIterator iterator    = new AvailablePortIterator();

        Map<String, String>   site1Config = WebServer.parseConfig("site1.properties", false);

        site1Port = Integer.parseInt(site1Config.get("WebServer-Port"));
        cluster1  = WebServer.startCacheServer(site1Config, iterator);

        Map<String, String> site2Config = WebServer.parseConfig("site2.properties", false);

        site2Port = Integer.parseInt(site2Config.get("WebServer-Port"));
        cluster2  = WebServer.startCacheServer(site2Config, iterator);

        SimpleJavaApplicationSchema site1schema =
            new SimpleJavaApplicationSchema("com.oracle.coherence.patterns.pushreplication.cohweb.examples.utilities.WebServer");

        site1schema.addArgument("site1.properties");
        site1schema.addArgument("web");

        SimpleJavaApplicationSchema site2schema =
            new SimpleJavaApplicationSchema("com.oracle.coherence.patterns.pushreplication.cohweb.examples.utilities.WebServer");

        site2schema.addArgument("site2.properties");
        site2schema.addArgument("web");

        ExternalJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema> appBuilder =
            new ExternalJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema>();

        SystemApplicationConsole console = new SystemApplicationConsole();

        site1 = appBuilder.realize(site1schema, "Site1-Web", console);
        site2 = appBuilder.realize(site2schema, "Site2-Web", console);
    }


    /**
     * Method description
     */
    @AfterClass
    public static void tearDown()
    {
        cluster1.destroy();
        cluster2.destroy();
        site1.destroy();
        site2.destroy();
    }


    /**
     * Method description
     *
     * @throws Exception
     */
    @Test
    public void testSessionReplication() throws Exception
    {
        try
        {
            final WebConversation wc = new WebConversation();

            // Make initial request to siteA and add a value to the session
            WebResponse response = DeferredHelper.ensure(new DeferredWebResponse(wc,
                                                                                 "http://localhost:8080/sessionAccess.jsp"),
                                                         60,
                                                         TimeUnit.SECONDS);

            WebForm form = response.getFormWithID("HttpSessionAttributesForm");

            form.setParameter("key", "A");
            form.setParameter("value", "B");
            response = form.submit(form.getSubmitButton("action", "add"));

            // Assert that the session variables were set
            WebTable table = response.getTableWithID("HttpSessionAttributes");

            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));

            // Now make a request to the siteB, validate our session data is there and add another value
            DeferredAssert.assertThat("First set of session variables",
                                      new DeferredRowCount(wc,
                                                           "http://localhost:9080/sessionAccess.jsp"),
                                      Matchers.equalTo(2),
                                      60,
                                      TimeUnit.SECONDS);

            response = wc.getResponse("http://localhost:9080/sessionAccess.jsp");
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
            DeferredAssert.assertThat("First set of session variables",
                                      new DeferredRowCount(wc,
                                                           "http://localhost:8080/sessionAccess.jsp"),
                                      Matchers.equalTo(3),
                                      60,
                                      TimeUnit.SECONDS);

            response = wc.getResponse("http://localhost:8080/sessionAccess.jsp");
            table    = response.getTableWithID("HttpSessionAttributes");

            System.out.println("Table is: " + table.toString());
            assertEquals("key", "A", table.getCellAsText(1, 0));
            assertEquals("value", "B", table.getCellAsText(1, 1));
            assertEquals("key2", "C", table.getCellAsText(2, 0));
            assertEquals("value2", "D", table.getCellAsText(2, 1));
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
         * Constructs ...
         *
         *
         * @param wc
         * @param request
         */
        public DeferredRowCount(WebConversation wc,
                                String          request)
        {
            this.wc      = wc;
            this.request = request;
        }


        /**
         * Method description
         *
         * @return
         *
         * @throws ObjectNotAvailableException
         */
        @Override
        public Integer get() throws ObjectNotAvailableException
        {
            try
            {
                WebResponse response = DeferredHelper.ensure(new DeferredWebResponse(wc, request));
                WebTable    table    = response.getTableWithID("HttpSessionAttributes");

                System.out.printf("RowCount: %d - Table: %s", table.getRowCount(), table.toString());

                return table.getRowCount();
            }
            catch (Exception e)
            {
                System.out.println("Error getting rowcount for " + request + " - " + "HttpSessionAttributes");
                e.printStackTrace();

                return null;
            }
        }


        /**
         * Method description
         *
         * @return
         */
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
         * Constructs ...
         *
         *
         * @param wc
         * @param request
         */
        public DeferredWebResponse(WebConversation wc,
                                   String          request)
        {
            this.wc      = wc;
            this.request = request;
        }


        /**
         * Return the WebResponse from the Deffered.
         *
         * @return WebResponse from the Deferred.
         */
        @Override
        public WebResponse get() throws ObjectNotAvailableException
        {
            try
            {
                return wc.getResponse(request);
            }
            catch (Exception e)
            {
                // System.out.printf("!!!!Error getting deffered web response:");
                // e.printStackTrace();
                return null;
            }
        }


        /**
         * Method description
         *
         * @return
         */
        @Override
        public Class<WebResponse> getDeferredClass()
        {
            return WebResponse.class;    // To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
