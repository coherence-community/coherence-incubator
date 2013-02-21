/*
 * File: CoherenceJavaApplication.java
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

import com.oracle.coherence.common.resourcing.ResourceUnavailableException;

import javax.management.MBeanInfo;
import javax.management.ObjectName;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * A {@link CoherenceJavaApplication} is a {@link JavaConsoleApplication} that provides special methods
 * to manage Coherence-based Java Applications.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public class CoherenceJavaApplication extends JavaConsoleApplication
{
    /**
     * Standard Constructor.
     *
     * @param process               The {@link Process} representing the {@link CoherenceJavaApplication}.
     *
     * @param name                  The name of the {@link CoherenceJavaApplication}.
     *
     * @param console               The {@link ApplicationConsole} that will be used for I/O by the
     *                              realized {@link Application}. This may be <code>null</code> if not required.
     *
     * @param environmentVariables  The environment variables used when starting the {@link CoherenceJavaApplication}.
     *
     * @param systemProperties      The system properties provided to the {@link CoherenceJavaApplication}
     */
    public CoherenceJavaApplication(Process            process,
                                    String             name,
                                    ApplicationConsole console,
                                    Properties         environmentVariables,
                                    Properties         systemProperties)
    {
        super(process, name, console, environmentVariables, systemProperties);
    }


    /**
     * Returns the Coherence Cluster {@link MBeanInfo} for the {@link CoherenceJavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block at wait for
     * 60 seconds until it becomes available.
     *
     * @return A {@link MBeanInfo}.
     *
     * @throws ResourceUnavailableException  If the Cluster MBean is not available.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public MBeanInfo getClusterMBeanInfo()
    {
        try
        {
            return super.getMBeanInfo(new ObjectName("Coherence:type=Cluster"));
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException("Could not retrieve the Coherence Cluster MBean", e);
        }
    }


    /**
     * Returns the Coherence Service {@link MBeanInfo} for the {@link CoherenceJavaApplication}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet available, it will block at wait for
     * 60 seconds until it becomes available.
     *
     * @param serviceName The name of the service
     * @param nodeId      The nodeId on which the service is defined
     *
     * @return A {@link MBeanInfo}.
     *
     * @throws ResourceUnavailableException  If the Cluster MBean is not available.
     * @throws UnsupportedOperationException If JMX is not enabled for the {@link JavaApplication}.
     */
    public MBeanInfo getServiceMBeanInfo(String serviceName,
                                         int    nodeId)
    {
        try
        {
            return super.getMBeanInfo(new ObjectName(String.format("Coherence:type=Service,name=%s,nodeId=%d",
                                                                   serviceName, nodeId)));
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(String
                .format("Could not retrieve the Coherence Service MBean [%s]", serviceName),
                                                    e);
        }
    }
}
