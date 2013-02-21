/*
 * File: JavaApplication.java
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

import com.oracle.coherence.common.resourcing.ResourceUnavailableException;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * A {@link JavaApplication} is an {@link Application} for Java-based application processes that use system properties
 * in addition to environment variables as provided by regular {@link Application}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Application
 * @see JavaApplicationBuilder
 *
 * @author Brian Oliver
 */
@Deprecated
public interface JavaApplication extends Application
{
    /**
     * Obtains the system {@link Properties} that were supplied to the {@link JavaApplication} when it was realized.
     *
     * @return A {@link Properties} of name value pairs, each one representing a system property provided to the
     *         {@link JavaApplication} as -Dname=value parameters when it was realized.
     */
    public Properties getSystemProperties();


    /**
     * Obtains an individual system property value for a specified system property name, or <code>null</code>
     * if the property is undefined.
     *
     * @param name The name of the system property
     *
     * @return The value of the defined system property, or <code>null</code> if undefined.
     */
    public String getSystemProperty(String name);


    /**
     * Obtains an {@link MBeanServerConnection} that may be used to access JMX infrastructure in the
     * {@link JavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block and wait
     * for 60 seconds until it becomes available.
     *
     * @return An {@link MBeanServerConnection}.
     *
     * @throws ResourceUnavailableException  If a connection could not be made to the JMX server.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public MBeanServerConnection getMBeanServerConnection();


    /**
     * Obtains an {@link MBeanServerConnection} that may be used to access JMX infrastructure in the
     * {@link JavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block and wait
     * for the specified number of milliseconds until it becomes available.
     *
     * @param waitTimeMS
     *
     * @return An {@link MBeanServerConnection}.
     *
     * @throws ResourceUnavailableException  If a connection could not be made to the JMX server.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public MBeanServerConnection getMBeanServerConnection(long waitTimeMS);


    /**
     * Obtains a local proxy of an MBean registered with the JMX infrastructure in the {@link JavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block and wait for 60
     * seconds until it becomes available.
     *
     * @param <T>
     * @param objectName The name of the MBean.
     * @param clazz      The type of the proxy to create.
     *
     * @return A proxy of type T
     *
     * @throws ResourceUnavailableException  If the specified MBean or JMX server is not available.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public <T> T getMBeanProxy(ObjectName objectName,
                               Class<T>   clazz);


    /**
     * Obtains the {@link MBeanInfo} of the specified MBean using the JMX infrastructure in the {@link JavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block at wait for
     * 60 seconds until it becomes available.
     *
     * @param objectName The name of the MBean.
     *
     * @return A {@link MBeanInfo}.
     *
     * @throws ResourceUnavailableException  If the specified MBean is not available.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public MBeanInfo getMBeanInfo(ObjectName objectName);


    /**
     * Obtains the {@link MBeanInfo} of the specified MBean using the JMX infrastructure in the {@link JavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block and wait for at most
     * the specified number of milliseconds until it becomes available.
     *
     * @param objectName The name of the MBean.
     * @param waitTimeMS The number of milliseconds to wait for the MBean to be available.
     *
     * @return A {@link MBeanInfo}.
     *
     * @throws ResourceUnavailableException  If the specified MBean is not available.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public MBeanInfo getMBeanInfo(ObjectName objectName,
                                  long       waitTimeMS);
}
