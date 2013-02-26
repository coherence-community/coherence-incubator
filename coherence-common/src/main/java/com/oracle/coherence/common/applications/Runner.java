/*
 * File: Runner.java
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

package com.oracle.coherence.common.applications;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Execute a Java Application based on information defined in a .properties
 * file located on the classpath.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class Runner
{
    /**
     * {@link Runner} application entry point.
     * <p>
     * Usage:  java Runner application.properties
     * <p>
     * Where:  the application.properties file is the only parameter, that
     * of which contains the following:
     * <ol>
     *      <li>Application-Name=My Application</li>
     *      <li>Application-Description=This Application Does Cool Things</li>
     *      <li>Main-Class=fully.qualified.class.Name</li>
     *      <li>System-Property1=-Dsystem.property1=value1</li>
     *      <li>System-Propertyn=-Dsystem.propertyn=valuen</li>
     *      <li>Argument1=value1</li>
     *      <li>Argumentn=valuen</li>
     * </ol>
     *
     * @param arguments  the arguments for the {@link Runner}
     */
    public static void main(String[] arguments)
    {
        System.out.println("-----------------------------------------------------------");
        System.out.println("Oracle Java Application Runner");
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

            try
            {
                // attempt to load the properties (via the classpath)
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(ClassLoader
                        .getSystemResourceAsStream(propertiesFileName)));

                Properties applicationProperties = new Properties();

                applicationProperties.load(reader);

                reader.close();

                // determine the application class name (for main)
                String applicationClassName = applicationProperties.getProperty("Main-Class");

                if (applicationClassName == null || applicationClassName.trim().isEmpty())
                {
                    System.out
                        .println("ERROR: Failed to load the Main-Class for the application as it is either undefined or empty");
                }
                else
                {
                    try
                    {
                        // attempt to load the application class
                        Class<?> applicationClass = Class.forName(applicationClassName);

                        // determine the system properties and application arguments
                        ArrayList<String> applicationArguments = new ArrayList<String>();

                        for (String propertyName : applicationProperties.stringPropertyNames())
                        {
                            if (propertyName.startsWith("System-Property"))
                            {
                                String   propertyDefinition = applicationProperties.getProperty(propertyName);
                                String[] propertyParts      = propertyDefinition.split("=");

                                if (propertyParts.length == 2)
                                {
                                    System.setProperty(propertyParts[0], propertyParts[1]);
                                }
                                else
                                {
                                    throw new IllegalArgumentException(String
                                        .format("ERROR: Invalid Property Value for [%s=%s].  Should be of the form: System-PropertyN=-Dproperty.name=property.value\n",
                                                propertyName, propertyDefinition));
                                }
                            }

                            if (propertyName.startsWith("Argument"))
                            {
                                try
                                {
                                    String  indexNumber = propertyName.substring("Argument".length());
                                    Integer index       = Integer.parseInt(indexNumber);

                                    applicationArguments.set(index, applicationProperties.getProperty(propertyName));
                                }
                                catch (Exception e)
                                {
                                    throw new IllegalArgumentException(String
                                        .format("ERROR: Invalid Argument Index for [%s=%s].  Should be of the form: ArgumentN=value (where N is an integer)\n",
                                                propertyName, applicationProperties.getProperty(propertyName)),
                                                                       e);
                                }
                            }
                        }

                        // determine the Application Name and Description
                        String applicationName = applicationProperties.containsKey("Application-Name")
                                                 ? applicationProperties.getProperty("Application-Name").trim() : null;
                        String applicationDescription =
                            applicationProperties.containsKey("Application-Description")
                            ? applicationProperties.getProperty("Application-Description").trim() : null;

                        // output the application details
                        if (applicationName != null)
                        {
                            System.out.printf("Application Name        : " + applicationName + "\n");
                        }

                        if (applicationDescription != null)
                        {
                            System.out.printf("Application Description : " + applicationDescription + "\n");
                        }

                        System.out.printf("Application Class       : " + applicationClass + "\n");
                        System.out.printf("With Arguments          : "
                                          + (applicationArguments == null || applicationArguments.size() == 0
                                             ? "(no arguments)" : applicationArguments) + "\n");
                        System.out.printf("Using System Properties : " + System.getProperties() + "\n");
                        System.out.println();

                        // copy over the arguments into mainArguments
                        String[] mainArguments = new String[applicationArguments.size()];

                        for (int i = 0; i < mainArguments.length; i++)
                        {
                            String applicationArgument = applicationArguments.get(i);

                            mainArguments[i] = applicationArgument == null ? "" : applicationArgument;
                        }

                        // now run the application
                        Method mainMethod = applicationClass.getMethod("main", String[].class);

                        mainMethod.invoke(null, new Object[] {mainArguments});
                    }
                    catch (Exception e)
                    {
                        System.out.printf("ERROR: Failed to load and execute the Main-Class [%s] due to:\n%s",
                                          applicationClassName,
                                          e);
                    }
                }
            }
            catch (Exception e)
            {
                System.out.printf("ERROR: Unable to loaded the application properties file [%s] due to:\n%s",
                                  propertiesFileName,
                                  e);
            }
        }
        else
        {
            System.out
                .println("Oracle Java Application Runner requires a single parameter, specifying a .properties file on the classpath");
        }
    }
}
