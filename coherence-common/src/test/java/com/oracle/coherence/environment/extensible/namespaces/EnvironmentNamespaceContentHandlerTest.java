/*
 * File: EnvironmentNamespaceContentHandlerTest.java
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

package com.oracle.coherence.environment.extensible.namespaces;

import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

/**
 * The unit tests for the {@link EnvironmentNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
public class EnvironmentNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Test environment:instance.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testEnvironmentInstance() throws ConfigurationException
    {
        Environment                        env     = mock(Environment.class);

        ConfigurationContext               context = new DefaultConfigurationContext(env);

        StringBuilder                      sb      = new StringBuilder("<environment:instance/>");
        XmlElement                         elem    = XmlHelper.loadXml(sb.toString());

        EnvironmentNamespaceContentHandler handler = new EnvironmentNamespaceContentHandler();
        Object                             result  = handler.onElement(context, new QualifiedName(elem), elem);

        assertSame(result, env);
    }


    /**
     * Test environment:resource.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testEnvironmentResource() throws URISyntaxException, ConfigurationException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("value",
                                              new URI(String.format("class:%s",
                                                                    ValueNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("environment",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        StringBuilder sb = new StringBuilder("<environment:resource id=\"java.awt.Point\">");

        sb.append("  <instance:class classname=\"java.awt.Point\">");
        sb.append("    <value:integer>100</value:integer>");
        sb.append("    <value:integer>100</value:integer>");
        sb.append("  </instance:class>");
        sb.append("</environment:resource>");

        XmlElement elem = XmlHelper.loadXml(sb.toString());

        EnvironmentNamespaceContentHandler handler =
            (EnvironmentNamespaceContentHandler) context.getNamespaceContentHandler("environment");
        Object result = handler.onElement(context, new QualifiedName(elem), elem);

        verify(env).registerResource(Point.class, (Point) result);
    }


    /**
     * Test environment:ref.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testEnvironmentReference() throws URISyntaxException, ConfigurationException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("environment",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        when(env.getResource(Point.class)).thenReturn(new Point(100, 100));

        StringBuilder sb   = new StringBuilder("<environment:ref id=\"java.awt.Point\"/>");
        XmlElement    elem = XmlHelper.loadXml(sb.toString());

        EnvironmentNamespaceContentHandler handler =
            (EnvironmentNamespaceContentHandler) context.getNamespaceContentHandler("environment");
        Object result = handler.onElement(context, new QualifiedName(elem), elem);

        assertEquals(result.getClass(), Point.class);
    }
}
