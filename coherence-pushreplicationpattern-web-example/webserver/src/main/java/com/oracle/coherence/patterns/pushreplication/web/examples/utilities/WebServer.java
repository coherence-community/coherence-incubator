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

package com.oracle.coherence.patterns.pushreplication.web.examples.utilities;

import com.oracle.bedrock.predicate.Predicates;
import com.oracle.bedrock.runtime.PropertiesBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Properties;

/**
 * The WebServer class is responsible for starting up the Coherence*Web Example for runtime. Using the functionality
 * from oracle-tools runtime it will start a Cache Server and then start a storage disabled instance of JETTY that will
 * be used to host the web application for the Coherence*Web Example.
 * <p>
 *
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class WebServer
{
    private static CoherenceCluster cluster = null;


    /**
     * {@link WebServer} application entry point.
     * <p>
     * Usage:  java Runner application.properties
     * <p>
     * Where:  the application.properties file is the only parameter, that
     * of which contains the following:
     * <ol>
     *      <li>Server-port=<port value for the http server to listen on></li>
     *      <li>System-Property1=-Dsystem.property1=value</li>
     *      <li>System-Propertyn=-Dsystempropertyn=value</li>
     *      <li>WebSystem-Property1=-Dsystem.property1=value1</li>
     *      <li>WebSystem-Propertyn=-Dsystem.propertyn=valuen</li>
     *      <li>COHSystem-Property1=-Dsystem.property1=value</li>
     *      <li>COHSystem-Propertyn=-Dsystem.propertyn=value</li>
     * </ol>
     */
    public static void main(String[] arguments) throws Exception
    {
        System.out.println("-----------------------------------------------------------");
        System.out.println("Oracle Java Coherence*Web Example");
        System.out.println("Copyright (c) 2013. Oracle Corporation");
        System.out.println("-----------------------------------------------------------");
        System.out.println();

        try
        {
            if (arguments.length == 1)
            {
                String            propertiesFileName = arguments[0];
                PropertiesBuilder globalProps        = parseConfig("System-Property", propertiesFileName);
                PropertiesBuilder jettyProps         = parseConfig("WebSystem-Property", propertiesFileName);
                PropertiesBuilder cohProps           = parseConfig("COHSystem-Property", propertiesFileName);

                PropertiesBuilder cacheProps         = new PropertiesBuilder(globalProps);
                PropertiesBuilder webserverProps     = new PropertiesBuilder(globalProps);

                cacheProps.addProperties(cohProps);
                webserverProps.addProperties(jettyProps);

                cluster = startCluster(cacheProps);
                startJettyServer(webserverProps);
            }
            else if (arguments.length == 2)
            {
                System.out.println("For functional testing purposes only.");

                if (arguments.length != 2)
                {
                    System.out.println("Tests should pass in 2 parameters (properties file and webserver port."
                                       + " We only received: " + arguments.length + " parameters");
                }

                String            propertiesFileName = arguments[0];
                PropertiesBuilder globalProps        = parseConfig("System-Property", propertiesFileName);
                PropertiesBuilder jettyProps         = parseConfig("WebSystem-Property", propertiesFileName);
                PropertiesBuilder webserverProps     = new PropertiesBuilder(globalProps);

                webserverProps.addProperties(jettyProps);
                webserverProps.setProperty("WebServer-Port", arguments[1]);

                startJettyServer(webserverProps);
            }
            else
            {
                System.out.println("Oracle Java Coherence*Web Example requires a single parameter, specifying a "
                                   + ".properties file on the classpath");
            }
        }
        finally
        {
            if (cluster != null)
            {
                cluster.close();
                cluster = null;
            }
        }
    }


    /**
     * Method description
     *
     * @param propertiesBuilder  The properties to use to start this server.
     *
     * @return The Cluster that was realized.
     *
     * @throws IOException
     */
    public static CoherenceCluster startCluster(PropertiesBuilder propertiesBuilder)
    {
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(1,
                        CoherenceCacheServer.class,
                        new SystemProperties(propertiesBuilder.realize()),
                        RoleName.of("CacheServer"),
                        DisplayName.of(propertiesBuilder.getProperty("tangosol.coherence.site") + "-CacheServer"),
                        StabilityPredicate.of(null));

        return builder.build(Console.system());
    }


    /**
     * Start the webserver that will run the Coherence*Web test application for getting and setting http session
     * variables.
     *
     * @param propertiesBuilder  The propertiesBuilder containing the configuration for this server
     */
    public static void startJettyServer(PropertiesBuilder propertiesBuilder)
    {
        int        webPort = Integer.parseInt((String) propertiesBuilder.getProperty("WebServer-Port"));
        Properties props   = propertiesBuilder.realize();

        for (String prop : props.stringPropertyNames())
        {
            System.setProperty(prop, props.getProperty(prop));
        }

        Server           server   = new Server(webPort);
        ProtectionDomain domain   = WebServer.class.getProtectionDomain();
        URL              location = domain.getCodeSource().getLocation();
        WebAppContext    webapp   = new WebAppContext();

        webapp.setContextPath("/");
        webapp.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        webapp.setServer(server);
        webapp.setWar(location.toExternalForm());

        String sTemp = System.getProperty("java.io.tmpdir");

        sTemp += "/";
        sTemp += System.getProperty("tangosol.coherence.site");
        webapp.setTempDirectory(new File(sTemp));

        server.setHandler(webapp);

        try
        {
            server.start();
            server.join();
        }
        catch (Exception e)
        {
            System.out.printf("Error starting or joining web server: %s", e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Parse the properties file into a sorted Map<String,String> of property values. Generic System-Properties will be set in the
     * environment within this method as well.
     *
     * @param propertiesFileName The properties file to parse
     *
     * @return a
     *
     * @throws Exception
     */
    public static PropertiesBuilder parseConfig(String propPrefix,
                                                String propertiesFileName) throws Exception
    {
        PropertiesBuilder builder = new PropertiesBuilder();

        // warn if the file name doesn't end in properties
        if (!propertiesFileName.endsWith(".properties"))
        {
            System.out.printf("WARNING: The specified application properties file [%s] doesn't end in .properties\n",
                              propertiesFileName);
        }

        try
        {
            // attempt to load the properties (via the classpath)
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(propertiesFileName)));

            Properties applicationProps = new Properties();

            applicationProps.load(reader);
            reader.close();

            for (String propertyName : applicationProps.stringPropertyNames())
            {
                if (propertyName.startsWith(propPrefix))
                {
                    String   propertyDefinition = applicationProps.getProperty(propertyName);
                    String[] propertyParts      = propertyDefinition.split("=");

                    builder.setProperty(propertyParts[0], propertyParts[1]);
                }
            }

        }
        catch (Exception e)
        {
            System.out.printf("Error parsing properties file: %s - %s", propertiesFileName, e.getMessage());
            e.printStackTrace();

            throw e;
        }

        return builder;
    }
}
