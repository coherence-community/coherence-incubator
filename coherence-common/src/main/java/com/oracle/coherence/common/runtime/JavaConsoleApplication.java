/*
 * File: JavaConsoleApplication.java
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

package com.oracle.coherence.common.runtime;

import com.oracle.coherence.common.resourcing.AbstractDeferredResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceProvider;
import com.oracle.coherence.common.resourcing.ResourceUnavailableException;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * A {@link JavaConsoleApplication} is an implementation of a {@link JavaApplication} that has
 * a Console (with standard output, standard error and standard input streams).
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public class JavaConsoleApplication extends AbstractApplication implements JavaApplication
{
    /**
     * The System Properties used to create the underlying {@link Process} represented by
     * the {@link JavaConsoleApplication}.
     */
    private Properties systemProperties;

    /**
     * The {@link JMXConnector} that can be used to connect to the JMX Server for the {@link JavaApplication}.
     * <p>
     * Note: <code>null</code> if JMX is not available and/or we haven't connected as yet.
     */
    private JMXConnector jmxConnector;

    /**
     * The {@link MBeanServerConnection} for the {@link JavaApplication}.
     * <p>
     * Note: <code>null</code> when not connected.
     */
    private MBeanServerConnection mBeanServerConnection;


    /**
     * Standard Constructor.
     *
     * @param process               The {@link Process} representing the {@link JavaApplication}.
     * @param name                  The name of the {@link JavaApplication}.
     * @param console               The {@link ApplicationConsole} that will be used for I/O by the
     *                              realized {@link Application}. This may be <code>null</code> if not required.
     * @param environmentVariables  The environment variables used when starting the {@link JavaApplication}.
     * @param systemProperties      The system properties provided to the {@link JavaApplication}
     */
    public JavaConsoleApplication(Process            process,
                                  String             name,
                                  ApplicationConsole console,
                                  Properties         environmentVariables,
                                  Properties         systemProperties)
    {
        super(process, name, console, environmentVariables);

        this.systemProperties      = systemProperties;
        this.jmxConnector          = null;
        this.mBeanServerConnection = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getSystemProperties()
    {
        return systemProperties;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemProperty(String name)
    {
        return systemProperties.getProperty(name);
    }


    /**
     * Obtains the configured JMX port of the application (which by definition is remote)
     *
     * @return Port Number
     *
     * @throws UnsupportedOperationException if JMX is not enabled on the {@link JavaApplication}.
     */
    public int getRemoteJMXPort()
    {
        if (systemProperties.containsKey(AbstractJavaApplicationSchema.SUN_MANAGEMENT_JMXREMOTE_PORT))
        {
            return Integer
                .parseInt(systemProperties.getProperty(AbstractJavaApplicationSchema.SUN_MANAGEMENT_JMXREMOTE_PORT));
        }
        else
        {
            throw new UnsupportedOperationException("Application is not enabled for remote JMX management");
        }
    }


    /**
     * Obtains the configured RMI Server Host Name for the RMI server possibly running in the
     * {@link JavaConsoleApplication}.
     *
     * @return {@link String} (<code>null</code> if not defined).
     */
    public String getRMIServerHostName()
    {
        return systemProperties.getProperty(AbstractJavaApplicationSchema.JAVA_RMI_SERVER_HOSTNAME);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MBeanServerConnection getMBeanServerConnection(long waitTimeMS)
    {
        if (mBeanServerConnection == null)
        {
            // attempt to connect to the configured JMX MBeanServer
            try
            {
                // construct an appropriate URL
                final String remoteJMXConnectionUrl = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi",
                                                                    getRMIServerHostName(),
                                                                    getRemoteJMXPort());

                // build a ResourceProvider for the JMXConnector
                // (we need to do this as we might not be able to connect immediately to the remote JMX server
                // when it's in the process of coming up)
                ResourceProvider<JMXConnector> deferredJMXConnector =
                    new AbstractDeferredResourceProvider<JMXConnector>("JMX Connector " + remoteJMXConnectionUrl,
                                                                       1000,
                                                                       waitTimeMS)
                {
                    @Override
                    protected JMXConnector ensureResource() throws ResourceUnavailableException
                    {
                        // establish the environment for the connector
                        String              username    = "";
                        String              password    = "";

                        Map<String, Object> env         = new HashMap<String, Object>();
                        String[]            credentials = new String[] {username, password};

                        env.put(JMXConnector.CREDENTIALS, credentials);

                        JMXConnector connector;

                        try
                        {
                            connector = JMXConnectorFactory.newJMXConnector(new JMXServiceURL(remoteJMXConnectionUrl),
                                                                            env);
                            connector.connect();

                            return connector;
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                };

                // ensure the JMXConnector is available
                jmxConnector = deferredJMXConnector.getResource();

                // now grab the MBeanServerConnection implementation
                mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            }
            catch (MalformedURLException e)
            {
                throw new UnsupportedOperationException("Failed to connect to the JMX infrastructure for the application",
                                                        e);
            }
            catch (IOException e)
            {
                throw new UnsupportedOperationException("Failed to communicate with the JMX infrastructure for the application",
                                                        e);
            }
            catch (ResourceUnavailableException e)
            {
                throw new UnsupportedOperationException("Timed out trying to connect to the JMX infrastructure for the application",
                                                        e);
            }

        }

        return mBeanServerConnection;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MBeanServerConnection getMBeanServerConnection()
    {
        return getMBeanServerConnection(60000);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getMBeanProxy(ObjectName objectName,
                               Class<T>   clazz)
    {
        return JMX.newMBeanProxy(getMBeanServerConnection(), objectName, clazz);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MBeanInfo getMBeanInfo(final ObjectName objectName,
                                  final long       waitTimeMS)
    {
        try
        {
            ResourceProvider<MBeanInfo> mBeanInfoProvider =
                new AbstractDeferredResourceProvider<MBeanInfo>(objectName.toString(),
                                                                200,
                                                                waitTimeMS)
            {
                @Override
                protected MBeanInfo ensureResource() throws ResourceUnavailableException
                {
                    try
                    {
                        return getMBeanServerConnection().getMBeanInfo(objectName);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };

            return mBeanInfoProvider.getResource();
        }
        catch (ResourceUnavailableException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException("Failed to retrieve specified MBean [" + objectName + "]", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MBeanInfo getMBeanInfo(ObjectName objectName)
    {
        return getMBeanInfo(objectName, 60000);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy()
    {
        // close the JMXConnector (if we've got a connector)
        if (jmxConnector != null)
        {
            try
            {
                mBeanServerConnection = null;
                jmxConnector.close();
            }
            catch (IOException e)
            {
                // nothing to do here as we don't care
            }
        }

        super.destroy();
    }
}
