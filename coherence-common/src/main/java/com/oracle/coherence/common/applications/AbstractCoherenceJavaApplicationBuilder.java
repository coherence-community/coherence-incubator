/*
 * File: AbstractCoherenceJavaApplicationBuilder.java
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

import com.tangosol.coherence.component.net.Management;

import java.util.Iterator;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link AbstractCoherenceJavaApplicationBuilder} is a specialized {@link AbstractJavaApplicationBuilder} that's
 * designed to build {@link CoherenceJavaApplication}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public abstract class AbstractCoherenceJavaApplicationBuilder<B extends AbstractCoherenceJavaApplicationBuilder<?>>
    extends AbstractJavaApplicationBuilder<CoherenceJavaApplication, B>
{
    /**
     * The {@link Management} enumeration specifies the valid JMX Management Modes for a cluster node.
     */
    public enum JMXManagementMode
    {
        ALL,
        NONE,
        REMOTE_ONLY,
        LOCAL_ONLY;

        /**
         * Determines the system property representation of the {@link JMXManagementMode}
         *
         * @return A {@link String}
         */
        public String getSystemProperty()
        {
            // default to all
            String result = "all";

            if (this == NONE)
            {
                result = "none";
            }
            else if (this == REMOTE_ONLY)
            {
                result = "remote-only";
            }
            else if (this == LOCAL_ONLY)
            {
                result = "local-only";
            }

            return result;
        }
    }


    /**
     * Field description
     */
    public static final String PROPERTY_CACHECONFIG = "tangosol.coherence.cacheconfig";

    /**
     * Field description
     */
    public static final String PROPERTY_CLUSTER_NAME = "tangosol.coherence.cluster";

    /**
     * Field description
     */
    public static final String PROPERTY_CLUSTER_PORT = "tangosol.coherence.clusterport";

    /**
     * Field description
     */
    public static final String PROPERTY_DISTRIBUTED_LOCALSTORAGE = "tangosol.coherence.distributed.localstorage";

    /**
     * Field description
     */
    public static final String PROPERTY_LOCALHOST_ADDRESS = "tangosol.coherence.localhost";

    /**
     * Field description
     */
    public static final String PROPERTY_LOG_LEVEL = "tangosol.coherence.log.level";

    /**
     * Field description
     */
    public static final String PROPERTY_SITE_NAME = "tangosol.coherence.site";

    /**
     * Field description
     */
    public static final String PROPERTY_TCMP_ENABLED = "tangosol.coherence.tcmp.enabled";

    /**
     * Field description
     */
    public static final String PROPERTY_MANAGEMENT_MODE = "tangosol.coherence.management";

    /**
     * Field description
     */
    public static final String PROPERTY_MANAGEMENT_REMOTE = "tangosol.coherence.management.remote";

    /**
     * Field description
     */
    public static final String PROPERTY_MULTICAST_TTL = "tangosol.coherence.ttl";

    /**
     * Field description
     */
    public static final String PROPERTY_POF_CONFIG = "tangosol.pof.config";

    /**
     * Field description
     */
    public static final String PROPERTY_POF_ENABLED = "tangosol.pof.enabled";


    /**
     * Constructs ...
     *
     *
     * @param applicationClassName
     */
    public AbstractCoherenceJavaApplicationBuilder(String applicationClassName)
    {
        super(applicationClassName);
    }


    /**
     * Constructs ...
     *
     *
     * @param applicationClassName
     * @param classPath
     */
    public AbstractCoherenceJavaApplicationBuilder(String applicationClassName,
                                                   String classPath)
    {
        super(applicationClassName, classPath);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected CoherenceJavaApplication createJavaApplication(Process            process,
                                                             String             name,
                                                             ApplicationConsole console,
                                                             Properties         environmentVariables,
                                                             Properties         systemProperties)
    {
        return new CoherenceJavaApplication(process, name, console, environmentVariables, systemProperties);
    }


    /**
     * Method description
     *
     * @param cacheConfigURI
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setCacheConfigURI(String cacheConfigURI)
    {
        setSystemProperty(PROPERTY_CACHECONFIG, cacheConfigURI);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param isStorageEnabled
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setStorageEnabled(boolean isStorageEnabled)
    {
        setSystemProperty(PROPERTY_DISTRIBUTED_LOCALSTORAGE, isStorageEnabled);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param isTCMPEnabled
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setTCMPEnabled(boolean isTCMPEnabled)
    {
        setSystemProperty(PROPERTY_TCMP_ENABLED, isTCMPEnabled);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param port
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setClusterPort(int port)
    {
        setSystemProperty(PROPERTY_CLUSTER_PORT, port);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param portIterator
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setClusterPort(Iterator<Integer> portIterator)
    {
        setSystemProperty(PROPERTY_CLUSTER_PORT, portIterator);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param name
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setClusterName(String name)
    {
        setSystemProperty(PROPERTY_CLUSTER_NAME, name);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param name
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setSiteName(String name)
    {
        setSystemProperty(PROPERTY_SITE_NAME, name);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param ttl
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setMulticastTTL(int ttl)
    {
        setSystemProperty(PROPERTY_MULTICAST_TTL, ttl);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param mode
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setJMXManagementMode(JMXManagementMode mode)
    {
        setJMXSupport(mode != JMXManagementMode.NONE);
        setSystemProperty(PROPERTY_MANAGEMENT_MODE, mode.getSystemProperty());

        return (B) this;
    }


    /**
     * Method description
     *
     * @param enabled
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setRemoteJMXManagement(boolean enabled)
    {
        setJMXSupport(enabled);
        setSystemProperty(PROPERTY_MANAGEMENT_REMOTE, enabled);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param localHostAddress
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setLocalHostAddress(String localHostAddress)
    {
        setSystemProperty(PROPERTY_LOCALHOST_ADDRESS, localHostAddress);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param level
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setLogLevel(int level)
    {
        setSystemProperty(PROPERTY_LOG_LEVEL, level);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param pofConfigURI
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setPofConfigURI(String pofConfigURI)
    {
        setSystemProperty(PROPERTY_POF_CONFIG, pofConfigURI);
        setPofEnabled(true);

        return (B) this;
    }


    /**
     * Method description
     *
     * @param isEnabled
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public B setPofEnabled(boolean isEnabled)
    {
        setSystemProperty(PROPERTY_POF_ENABLED, isEnabled);

        return (B) this;
    }
}
