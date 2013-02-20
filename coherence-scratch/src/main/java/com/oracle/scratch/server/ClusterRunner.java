/*
 * File: ClusterRunner.java
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

package com.oracle.scratch.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: narliss
 * Date: 2/18/13
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClusterRunner
{
    /**
     * Method description
     *
     * @param arguments
     *
     * @throws IOException
     */
    public static void main(String[] arguments) throws Exception
    {
        System.out.println("-----------------------------------------------------------");
        System.out.println("Oracle Simple Cluster Runner");
        System.out.println("Copyright (c) 2013. Oracle Corporation");
        System.out.println("-----------------------------------------------------------");
        System.out.println();

        // ensure that there's only a single argument
        if (arguments.length == 1)
        {
            // determine the application properties file name
            String propertiesFileName = arguments[0].trim();

            // warn if the file name doesn't end in properties
            if (!propertiesFileName.endsWith(".properties"))
            {
                System.out
                    .printf("WARNING: The specified application properties file [%s] doesn't end in .properties\n",
                            propertiesFileName);
            }

            ClusterManager manager = null;
            try
            {
                // attempt to load the properties (via the classpath)
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(ClassLoader
                        .getSystemResourceAsStream(propertiesFileName)));

                Properties applicationProperties = new Properties();

                applicationProperties.load(reader);

                reader.close();

                int serverCount     = Integer.parseInt(loadProperty("servercount", applicationProperties, propertiesFileName));
                int proxyCount      = Integer.parseInt(loadProperty("proxycount", applicationProperties, propertiesFileName));
                int managementCount = Integer.parseInt(loadProperty("managementcount", applicationProperties, propertiesFileName));
                int clusterPort     = Integer.parseInt(loadProperty("clusterport", applicationProperties, propertiesFileName));
                int proxyPort       = Integer.parseInt(loadProperty("proxyport", applicationProperties, propertiesFileName));
                int remoteProxyPort = Integer.parseInt(loadProperty("remoteproxyport", applicationProperties, propertiesFileName));
                int JMXPort         = Integer.parseInt(loadProperty("jmxport", applicationProperties, propertiesFileName));

                String serverConfig     = loadProperty("serverconfig", applicationProperties, propertiesFileName);
                String proxyConfig      = loadProperty("proxyconfig", applicationProperties, propertiesFileName);
                String managementConfig = loadProperty("managementconfig", applicationProperties, propertiesFileName);
                String pofConfig        = loadProperty("pofconfig", applicationProperties, propertiesFileName);
                String siteName         = loadProperty("sitename", applicationProperties, propertiesFileName);

                // determine the system properties and application arguments
                ArrayList<String> jvmArguments = new ArrayList<String>();

                for (String propertyName : applicationProperties.stringPropertyNames())
                    {
                    if (propertyName.startsWith("jvmarg"))
                        {
                            try
                            {
                                jvmArguments.add(applicationProperties.getProperty(propertyName));
                            }
                            catch (Exception e)
                            {
                                throw new IllegalArgumentException(String
                                    .format("ERROR: Invalid Argument Index for [%s=%s].  Should be of the form: jvmargN=value (where N is an integer)\n",
                                            propertyName, applicationProperties.getProperty(propertyName)),
                                                                   e);
                            }
                        }
                    }

                manager = new ClusterManager(serverCount, proxyCount, managementCount, clusterPort, proxyPort, remoteProxyPort,
                                    JMXPort, serverConfig, proxyConfig, managementConfig, pofConfig, siteName, jvmArguments);

            }
            catch (Exception e)
            {
                System.out.printf("WARNING: Unable to loaded the application properties file [%s] due to:\n%s",
                                  propertiesFileName,
                                  e);
            }

        try
            {
            manager.startCluster();

            InputStreamReader converter = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(converter);

            in.readLine();
            }
        catch(Exception e)
            {
            e.printStackTrace();
            throw e;
            }
        finally
            {
            if (manager != null)
                {
                manager.stopCluster();
                }
            }
        }
        else
            {
            System.out.println("Invalid usage: please provide the properties file you wish to run.");
            }
    }

    public static String loadProperty(String propertyName, Properties properties, String fileName)
        {
        String value = properties.getProperty(propertyName);

        if (value == null)
            {
            //throw new IllegalArgumentException(propertyName + " missing from " + fileName);
            return "0";
            }

        return value;
        }
}
