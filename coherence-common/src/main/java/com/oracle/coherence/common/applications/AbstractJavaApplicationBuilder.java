/*
 * File: AbstractJavaApplicationBuilder.java
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

import com.oracle.coherence.common.network.AvailablePortIterator;
import com.oracle.coherence.common.network.Constants;
import com.tangosol.util.UUID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link AbstractJavaApplicationBuilder} is the base implementation for {@link JavaApplicationBuilder}s.
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A> The type of {@link JavaApplication} that the {@link AbstractJavaApplicationBuilder} will create.
 * @param <B> The type of the {@link JavaApplicationBuilder} from which default configuration may be retrieved.
 *
 * @author Brian Oliver
 */
@Deprecated
public abstract class AbstractJavaApplicationBuilder<A extends JavaApplication, B extends JavaApplicationBuilder<A, ?>>
    extends AbstractApplicationBuilder<A, B> implements JavaApplicationBuilder<A, B>
{
    /**
     * Field description
     */
    public static final String SUN_MANAGEMENT_JMXREMOTE = "com.sun.management.jmxremote";

    /**
     * Field description
     */
    public static final String SUN_MANAGEMENT_JMXREMOTE_PORT = "com.sun.management.jmxremote.port";

    /**
     * Field description
     */
    public static final String SUN_MANAGEMENT_JMXREMOTE_AUTHENTICATE = "com.sun.management.jmxremote.authenticate";

    /**
     * Field description
     */
    public static final String SUN_MANAGEMENT_JMXREMOTE_SSL = "com.sun.management.jmxremote.ssl";

    /**
     * Field description
     */
    public static final String JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";

    /**
     * The class name for the Java application.
     */
    private String applicationClassName;

    /**
     * The class path for the Java application.
     */
    private String classPath;

    /**
     * The JVM options.
     */
    private ArrayList<String> options;

    /**
     * The system properties for the application.
     */
    private PropertiesBuilder systemPropertiesBuilder;


    /**
     * Standard Constructor.
     *
     * @param applicationClassName The fully qualified class name of the Java application
     */
    public AbstractJavaApplicationBuilder(String applicationClassName)
    {
        this(applicationClassName, System.getProperty("java.class.path"));
    }


    /**
     * Standard Constructor (with class path).
     *
     * @param applicationClassName The fully qualified class name of the Java application
     * @param classPath The class path for the Java application
     */
    public AbstractJavaApplicationBuilder(String applicationClassName,
                                          String classPath)
    {
        this.applicationClassName    = applicationClassName;
        this.classPath               = classPath;
        this.options                 = new ArrayList<String>();
        this.systemPropertiesBuilder = new PropertiesBuilder();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PropertiesBuilder getSystemPropertiesBuilder()
    {
        return systemPropertiesBuilder;
    }


    /**
     * Sets the class path for the Java application.
     *
     * @param classPath
     * @return The resulting {@link AbstractJavaApplicationBuilder}
     */
    @SuppressWarnings("unchecked")
    public B setClassPath(String classPath)
    {
        this.classPath = classPath;

        return (B) this;
    }


    /**
     * Method description
     *
     * @param name
     * @param value
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setSystemProperty(String name,
                               Object value)
    {
        systemPropertiesBuilder.setProperty(name, value);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param name
     * @param value
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setDefaultSystemProperty(String name,
                                      Object value)
    {
        systemPropertiesBuilder.setDefaultProperty(name, value);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param systemProperties
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setSystemProperties(PropertiesBuilder systemProperties)
    {
        systemPropertiesBuilder.addProperties(systemProperties);

        return (B) this;
    }


    /**
     * Adds a JVM Option to use when starting the Java application.
     *
     * @param option The JVM option
     */
    public void addOption(String option)
    {
        // drop the "-" if specified
        options.add(option.startsWith("-") ? option.substring(1) : option);
    }


    /**
     * Adds a JVM Option to use when starting the Java application.
     *
     * @param option The JVM option
     * @return The resulting {@link AbstractJavaApplicationBuilder}
     */
    @SuppressWarnings("unchecked")
    public B setOption(String option)
    {
        addOption(option);

        return (B) this;
    }


    /**
     * Enables/Disables JMX support for the build {@link JavaApplication}s.
     * <p>
     * This method sets/removes numerous properties to enable/disable JMX.  Including:
     * <p>
     * <ol>
     *    <li>The remote JMX port to 9000, the Java default for remote JMX management.
     *        You can override this setting by calling {@link #setJMXPort(int)} or
     *        {@link #setJMXPort(AvailablePortIterator)}.
     *
     *    <li>The java.rmi.server.hostname to be {@link Constants#LOCAL_HOST}
     *        You can override this setting by calling {@link #setRMIServerHostName(String)}.
     *
     *    <li>The sun.management.jmxremote.ssl to false (off)
     *
     *    <li>Disables JMX authentication.
     *        You can override this setting by calling {@link #setJMXAuthentication(boolean)}.
     *
     * @param enabled Should JMX support be enabled
     *
     * @return The resulting {@link AbstractJavaApplicationBuilder}
     */
    @SuppressWarnings("unchecked")
    public B setJMXSupport(boolean enabled)
    {
        if (enabled)
        {
            setDefaultSystemProperty(SUN_MANAGEMENT_JMXREMOTE_PORT, 9000);
            setDefaultSystemProperty(SUN_MANAGEMENT_JMXREMOTE, "");
            setDefaultSystemProperty(SUN_MANAGEMENT_JMXREMOTE_AUTHENTICATE, false);
            setDefaultSystemProperty(SUN_MANAGEMENT_JMXREMOTE_SSL, false);
            setDefaultSystemProperty(JAVA_RMI_SERVER_HOSTNAME, Constants.LOCAL_HOST);

            return (B) this;
        }
        else
        {
            systemPropertiesBuilder.removeProperty(SUN_MANAGEMENT_JMXREMOTE);
            systemPropertiesBuilder.removeProperty(SUN_MANAGEMENT_JMXREMOTE_PORT);
            systemPropertiesBuilder.removeProperty(SUN_MANAGEMENT_JMXREMOTE_AUTHENTICATE);
            systemPropertiesBuilder.removeProperty(SUN_MANAGEMENT_JMXREMOTE_SSL);
            systemPropertiesBuilder.removeProperty(JAVA_RMI_SERVER_HOSTNAME);

            return (B) this;
        }
    }


    /**
     * Specifies if JMX authentication is enabled.
     *
     * @param enabled Is JMX Authentication required.
     *
     * @return The {@link JavaApplication}.
     */
    public B setJMXAuthentication(boolean enabled)
    {
        return setDefaultSystemProperty(SUN_MANAGEMENT_JMXREMOTE_AUTHENTICATE, enabled);
    }


    /**
     * Specifies the JMX remote port.
     *
     * @param port The port on which remote JMX should be enabled.
     *
     * @return The resulting {@link AbstractJavaApplicationBuilder}
     */
    public B setJMXPort(int port)
    {
        return setSystemProperty(SUN_MANAGEMENT_JMXREMOTE_PORT, port);
    }


    /**
     * Specifies the JMX remote port using an AvailablePortIterator.
     *
     * @param portIterator The {@link AvailablePortIterator} that will be used to determine the JMX remote port.
     *
     * @return The resulting {@link AbstractJavaApplicationBuilder}
     */
    public B setJMXPort(AvailablePortIterator portIterator)
    {
        return setSystemProperty(SUN_MANAGEMENT_JMXREMOTE_PORT, portIterator);
    }


    /**
     * Specifies the RMI Server Host Name.  By default this is typically "localhost".
     *
     * @param rmiServerHostName The hostname
     *
     * @return The resulting {@link AbstractJavaApplicationBuilder}
     */
    @SuppressWarnings("unchecked")
    public B setRMIServerHostName(String rmiServerHostName)
    {
        setDefaultSystemProperty(JAVA_RMI_SERVER_HOSTNAME, rmiServerHostName);

        return (B) this;
    }


    /**
     * Starts the Java {@link Process} using the configuration established by the {@link AbstractJavaApplicationBuilder}.
     *
     * @param defaultBuilder An {@link ApplicationBuilder} that should be used to determine default environment
     *                       variable and system property declarations.  This may be <code>null</code> if not required.
     *
     * @param name           The name of the application.
     *
     * @param console        The {@link ApplicationConsole} that will be used for I/O by the
     *                       realized {@link Application}. This may be <code>null</code> if not required.
     *
     * @return A {@link JavaApplication} representing the stated {@link Application}.
     *
     * @throws IOException Thrown if a problem occurs while starting the application.
     */
    @Override
    public A realize(B                  defaultBuilder,
                     String             name,
                     ApplicationConsole console) throws IOException
    {
        // construct the command to start the Java process
        ProcessBuilder processBuilder = new java.lang.ProcessBuilder("java");

        processBuilder.redirectErrorStream(true);

        // add the options
        for (String option : options)
        {
            processBuilder.command().add("-" + option);
        }

        // configure the environment variables for the process (based on the Environment Variables Builder)
        Properties environmentVariables =
            getEnvironmentVariablesBuilder().realize(defaultBuilder == null
                                                     ? null : defaultBuilder.getEnvironmentVariablesBuilder());

        processBuilder.environment().clear();

        for (String variableName : environmentVariables.stringPropertyNames())
        {
            processBuilder.environment().put(variableName, environmentVariables.getProperty(variableName));
        }

        processBuilder.environment().put("CLASSPATH", classPath);

        // realize the system properties for the process
        Properties systemProperties = systemPropertiesBuilder.realize(defaultBuilder == null
                                                                      ? null
                                                                      : defaultBuilder.getSystemPropertiesBuilder());

        for (String propertyName : systemProperties.stringPropertyNames())
        {
            String propertyValue = systemProperties.getProperty(propertyName);

            processBuilder.command().add("-D" + propertyName + (propertyValue.isEmpty() ? "" : "=" + propertyValue));
        }

        // add the applicationClassName to the command for the process
        processBuilder.command().add(applicationClassName);

        // add the arguments to the command for the process
        for (String argument : getArguments())
        {
            processBuilder.command().add(argument);
        }

        // start the process
        A application = createJavaApplication(processBuilder.start(),
                                              name,
                                              console,
                                              environmentVariables,
                                              systemProperties);

        // prepare the application (if we have a preparer)
        if (getApplicationPreparer() != null)
        {
            getApplicationPreparer().prepare(application);
        }

        return application;
    }


    /**
     * Requests the child concrete implementation of this class to construct a suitable {@link JavaApplication}
     * using the provided properties.
     *
     * @param process               The {@link Process} representing the {@link JavaApplication}.
     * @param name                  The name of the {@link JavaApplication}.
     * @param console               The {@link ApplicationConsole} that will be used for I/O by the
     *                              realized {@link Application}. This may be <code>null</code> if not required.
     * @param environmentVariables  The environment variables used when starting the {@link JavaApplication}.
     * @param systemProperties      The system properties provided to the {@link JavaApplication}
     *
     * @return An A.
     */
    protected abstract A createJavaApplication(Process            process,
                                               String             name,
                                               ApplicationConsole console,
                                               Properties         environmentVariables,
                                               Properties         systemProperties);


    /**
     * Starts the Java {@link Process} using the configuration established by the {@link AbstractJavaApplicationBuilder}
     * with no default builder and no console output.
     *
     * @return A {@link JavaApplication} representing the stated {@link Application}.
     *
     * @throws IOException Thrown if a problem occurs while starting the application.
     */
    public A realize() throws IOException
    {
        return realize(null, new UUID().toString(), new NullApplicationConsole());
    }


    /**
     * Starts the Java {@link Process} using the configuration established by the {@link AbstractJavaApplicationBuilder}
     * with no default builder but using the provided {@link ApplicationConsole} for output.
     *
     * @param name      The name of the application.
     *
     * @param console   The {@link ApplicationConsole} that will be used for I/O by the
     *                  realized {@link Application}. This may be <code>null</code> if not required.
     *
     * @return A {@link JavaApplication} representing the stated {@link Application}.
     *
     * @throws IOException Thrown if a problem occurs while starting the application.
     */
    public A realize(String             name,
                     ApplicationConsole console) throws IOException
    {
        return realize(null, name, console);
    }


    /**
     * Starts the Java {@link Process} using the configuration established by the {@link AbstractJavaApplicationBuilder}
     * with no default builder but using a {@link SystemApplicationConsole} with the specified prefix for output.
     *
     * @param name The name of the application.
     *
     * @return A {@link JavaApplication} representing the stated {@link Application}.
     *
     * @throws IOException Thrown if a problem occurs while starting the application.
     */
    public A realize(String name) throws IOException
    {
        return realize(null, name, new SystemApplicationConsole());
    }
}
