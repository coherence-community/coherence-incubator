/*
 * File: WebServer.java
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

package com.oracle.coherence.patterns.pushreplication.cohweb.examples;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * Created with IntelliJ IDEA.
 * User: narliss
 * Date: 3/16/13
 * Time: 10:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebServer
{
    /**
     * Method description
     *
     * @param arguments
     *
     * @throws Exception
     */
    public static void main(String[] arguments)
    {
        try
        {
            String jetty_home = System.getProperty("jetty.home", "..");

            if (arguments.length == 2)
            {
                int              webPort    = Integer.parseInt(arguments[0]);
                String           serverName = arguments[1];
                Server           server     = new Server(webPort);
                ProtectionDomain domain     = WebServer.class.getProtectionDomain();
                URL              location   = domain.getCodeSource().getLocation();
                WebAppContext    webapp     = new WebAppContext();

                webapp.setContextPath("/");
                webapp.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
                webapp.setServer(server);
                webapp.setWar(location.toExternalForm());
                webapp.setTempDirectory(new File(System.getProperty("java.io.tmpdir") + "/" + serverName));

                server.setHandler(webapp);
                server.start();
                server.join();
            }
        }
        catch (Exception e)
        {
            System.out.printf("ERROR: Exception thrown while trying to run webserver!\n");
            e.printStackTrace();
        }
    }
}
