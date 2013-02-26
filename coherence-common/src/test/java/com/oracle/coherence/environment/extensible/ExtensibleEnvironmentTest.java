/*
 * File: ExtensibleEnvironmentTest.java
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

package com.oracle.coherence.environment.extensible;

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.environment.extensible.namespaces.EnvironmentNamespaceContentHandler;
import com.oracle.coherence.environment.extensible.namespaces.InstanceNamespaceContentHandler;
import com.oracle.coherence.environment.extensible.namespaces.ValueNamespaceContentHandler;
import com.oracle.tools.junit.AbstractTest;
import com.oracle.tools.runtime.java.virtualization.Virtualization;
import com.tangosol.net.CacheFactory;
import com.tangosol.run.xml.XmlHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertTrue;

/**
 * The unit tests for the {@link ExtensibleEnvironment}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ExtensibleEnvironmentTest extends AbstractTest
{
    /**
     * Setup for each test.
     */
    @Before
    public void setup()
    {
        System.setProperty("tangosol.coherence.mode", "dev");
        System.setProperty("tangosol.coherence.ttl", "0");
        System.setProperty("tangosol.coherence.localhost", "127.0.0.1");
        System.setProperty("tangosol.coherence.clusterport",
                           Integer.toString(Virtualization.getAvailablePorts().next()));
    }


    /**
     * Cleanup after each test.
     */
    @After
    public void cleanup()
    {
        CacheFactory.shutdown();
    }


    /**
     * Test that an empty configuration file produces an {@link ExtensibleEnvironment}
     * with a {@link EventDispatcher}.
     */
    @Test
    public void testExtensibleEnvironmentInitialization()
    {
        String                xml = "<cache-config>" + "</cache-config>";

        ExtensibleEnvironment env = new ExtensibleEnvironment(XmlHelper.loadXml(xml));

        assertTrue(env.getResource(EventDispatcher.class) != null);
        assertTrue(env.getResource(EventDispatcher.class) instanceof EventDispatcher);
    }


    /**
     * Test that resources are registered and returned as expected from an {@link ExtensibleEnvironment}.
     */
    @Test
    public void testResourceRegistration()
    {
        String xml =
            "<cache-config xmlns:env=\"class://" + EnvironmentNamespaceContentHandler.class.getName() + "\" "
            + "              xmlns:instance=\"class://" + InstanceNamespaceContentHandler.class.getName() + "\" "
            + "              xmlns:value=\"class://" + ValueNamespaceContentHandler.class.getName() + "\" " + ">"
            + "<env:resource id=\"java.awt.Point\">"
            + "<instance:class classname=\"java.awt.Point\"><value:integer>100</value:integer><value:integer>200</value:integer></instance:class>"
            + "</env:resource>" + "</cache-config>";

        ExtensibleEnvironment env = new ExtensibleEnvironment(XmlHelper.loadXml(xml));

        assertTrue(env.getResource(Point.class) != null);
        assertTrue(env.getResource(Point.class) instanceof Point);
        assertTrue(env.getResource(Point.class).equals(new Point(100, 200)));
    }
}
