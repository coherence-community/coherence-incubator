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

package com.oracle.coherence.patterns.pushreplication.web.examples.utilities;

import com.oracle.tools.runtime.PropertiesBuilder;
import com.oracle.tools.runtime.coherence.Cluster;
import com.oracle.tools.runtime.coherence.ClusterBuilder;
import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;
import com.oracle.tools.runtime.console.SystemApplicationConsole;
import com.oracle.tools.runtime.java.ExternalJavaApplicationBuilder;
import com.oracle.tools.runtime.network.AvailablePortIterator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

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
    private static Cluster cluster = null;


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
        String                propertiesFileName = arguments[0];
        Map<String, String>   config             = parseConfig(propertiesFileName, true);
        AvailablePortIterator iterator           = new AvailablePortIterator();

        try
        {
            if (arguments.length == 1)
            {
                cluster = startCacheServer(config, iterator);
                startJettyServer(config);
            }
            else if (arguments.length == 2)
            {
                System.out.println("For Testing Purposes Only");

                if (arguments[1].equals("cache"))
                {
                    cluster = startCacheServer(config, iterator);
                }
                else if (arguments[1].equals("web"))
                {
                    startJettyServer(config);
                }
            }
        }
        finally
        {
            if (cluster != null)
            {
                cluster.destroy();
                cluster = null;
            }
        }
    }


    /**
     * Method description
     *
     * @param config
     *
     * @return
     *
     * @throws IOException
     */
    public static Cluster startCacheServer(Map<String, String>   config,
                                           AvailablePortIterator portIterator) throws IOException
    {
        PropertiesBuilder cacheProperties = new PropertiesBuilder(PropertiesBuilder.fromCurrentSystemProperties());

        for (Map.Entry<String, String> entry : config.entrySet())
        {
            String key = entry.getKey();

            if (key.startsWith("COHSystem-Property") || key.startsWith("System-Property"))
            {
                setProperty(key, entry.getValue(), cacheProperties);
            }
        }

//        ClusterMemberSchema serverSchema =
//            new ClusterMemberSchema().setEnvironmentVariables(PropertiesBuilder.fromCurrentEnvironmentVariables())
//                .setSystemProperties(cacheProperties).setRoleName("CacheServer").setEnvironmentInherited(false);

        ClusterMemberSchema serverSchema =
            new ClusterMemberSchema().setSystemProperties(cacheProperties).setRoleName("CacheServer").setEnvironmentInherited(false);

        ClusterBuilder builder = new ClusterBuilder();

        builder.addBuilder(new ExternalJavaApplicationBuilder<ClusterMember, ClusterMemberSchema>(),
                           serverSchema,
                           cacheProperties.getProperty("tangosol.coherence.site") + "-CacheServer",
                           1);

        try
        {
            return builder.realize(new SystemApplicationConsole());
        }
        catch (IOException e)
        {
            System.out.printf("Error starting cache server: %s", e.getMessage());
            e.printStackTrace();

            throw e;
        }
    }


    /**
     * Start the webserver that will run the Coherence*Web test application for getting and setting http session
     * variables.
     *
     * @param config  The map of configuration parameters to use
     */
    public static void startJettyServer(Map<String, String> config)
    {
        int webPort = -1;

        for (Map.Entry<String, String> entry : config.entrySet())
        {
            String key = entry.getKey();

            if (key.startsWith("WebSystem-Property"))
            {
                setProperty(key, entry.getValue(), null);
            }
            else if (key.equals("WebServer-Port"))
            {
                webPort = Integer.parseInt(entry.getValue());
            }
        }

        Server           server   = new Server(webPort);
        ProtectionDomain domain   = WebServer.class.getProtectionDomain();
        URL              location = domain.getCodeSource().getLocation();
        WebAppContext    webapp   = new WebAppContext();

        webapp.setContextPath("/");
        webapp.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        webapp.setServer(server);
        webapp.setWar(location.toExternalForm());
        webapp.setTempDirectory(new File(System.getProperty("java.io.tmpdir") + "/"
                                         + System.getProperty("tangosol.coherence.site")));

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
    public static Map<String, String> parseConfig(String  propertiesFileName,
                                                  boolean setSystemProperties) throws Exception
    {
        Map<String, String> result = new TreeMap();

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
                if (propertyName.startsWith("System-Property") && setSystemProperties)
                {
                    setProperty(propertyName, applicationProps.getProperty(propertyName), null);
                }
                else
                {
                    result.put(propertyName, applicationProps.getProperty(propertyName));
                }
            }

        }
        catch (Exception e)
        {
            System.out.printf("Error parsing properties file: %s - %s", propertiesFileName, e.getMessage());
            e.printStackTrace();

            throw e;
        }

        return result;
    }


    /**
     * Parse a property definition formatted as Name=Value and set it on the supplied properties. If the supplied properties
     * is null we'll set use System.setProperty.
     *
     * @param propertyName       The name of the property to set from the configuration file
     * @param propertyDefinition The property to be parsed.
     */
    public static void setProperty(String            propertyName,
                                   String            propertyDefinition,
                                   PropertiesBuilder properties)
    {
        String[] propertyParts = propertyDefinition.split("=");

        if (propertyParts.length == 2)
        {
            if (properties == null)
            {
                System.setProperty(propertyParts[0], propertyParts[1]);
            }
            else
            {
                properties.setProperty(propertyParts[0], propertyParts[1]);
            }
        }
        else
        {
            throw new IllegalArgumentException(String
                .format("ERROR: Invalid Property Value for [%s=%s].  Should be of the form: System-PropertyN=-Dproperty.name=property.value\n",
                        propertyName, propertyDefinition));
        }
    }
}
