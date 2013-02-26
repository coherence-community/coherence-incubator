/*
 * File: InstanceNamespaceContentHandlerTest.java
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

import com.oracle.coherence.common.builders.Builder;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.apache.http.conn.scheme.Scheme;
import org.junit.Test;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the {@link InstanceNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
@Deprecated
public class InstanceNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Test instance:class.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testOnElementInstance() throws URISyntaxException, ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("environment",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("value",
                                              new URI(String.format("class:%s",
                                                                    ValueNamespaceContentHandler.class.getName())));

        XmlElement elem =
            XmlHelper
                .loadXml("<instance:class classname=\"java.awt.Point\"><value:integer>100</value:integer><value:integer>200</value:integer></instance:class>");

        InstanceNamespaceContentHandler instanceNamespaceContentHandler =
            (InstanceNamespaceContentHandler) context.getNamespaceContentHandler("instance");

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) instanceNamespaceContentHandler.onElement(context,
            new QualifiedName(elem),
            elem);

        assertEquals(builder.realize(SystemPropertyParameterProvider.INSTANCE), new Point(100, 200));
    }


    /**
     * Test instance:class.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testOnElementInstanceNotEquals() throws URISyntaxException, ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("environment",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("value",
                                              new URI(String.format("class:%s",
                                                                    ValueNamespaceContentHandler.class.getName())));

        XmlElement elem =
            XmlHelper
                .loadXml("<instance:class classname=\"java.awt.Point\"><value:integer>100</value:integer><value:integer>200</value:integer></instance:class>");

        InstanceNamespaceContentHandler instanceNamespaceContentHandler =
            (InstanceNamespaceContentHandler) context.getNamespaceContentHandler("instance");

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) instanceNamespaceContentHandler.onElement(context,
            new QualifiedName(elem),
            elem);

        assertFalse(builder.realize(SystemPropertyParameterProvider.INSTANCE).equals(new Point(100, 100)));
    }


    /**
     * Test instance:class.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testOnElementInstanceRectangle() throws URISyntaxException, ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("environment",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("value",
                                              new URI(String.format("class:%s",
                                                                    ValueNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        InstanceNamespaceContentHandler instanceNamespaceContentHandler =
            (InstanceNamespaceContentHandler) context.getNamespaceContentHandler("instance");

        StringBuilder sb = new StringBuilder("<instance:class classname=\"java.awt.Rectangle\">");

        sb.append("  <instance:class classname=\"java.awt.Point\">");
        sb.append("    <value:integer>100</value:integer>");
        sb.append("    <value:integer>100</value:integer>");
        sb.append("  </instance:class>");
        sb.append("  <instance:class classname=\"java.awt.Dimension\">");
        sb.append("    <value:integer>200</value:integer>");
        sb.append("    <value:integer>200</value:integer>");
        sb.append("  </instance:class>");
        sb.append("</instance:class>");

        XmlElement elem = XmlHelper.loadXml(sb.toString());

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) instanceNamespaceContentHandler.onElement(context,
            new QualifiedName(elem),
            elem);

        assertEquals(builder.realize(SystemPropertyParameterProvider.INSTANCE),
                     new Rectangle(new Point(100, 100), new Dimension(200, 200)));
    }


    /**
     * Test instance:class with a scheme.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testOnElementInstanceWithScheme() throws URISyntaxException, ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("environment",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("value",
                                              new URI(String.format("class:%s",
                                                                    ValueNamespaceContentHandler.class.getName())));

        XmlElement elem =
            XmlHelper
                .loadXml("<instance:class classname=\"com.oracle.coherence.environment.extensible.namespaces.InstanceNamespaceContentHandlerTest$HelloWorldScheme\"/>");

        InstanceNamespaceContentHandler instanceNamespaceContentHandler =
            (InstanceNamespaceContentHandler) context.getNamespaceContentHandler("instance");

        Object result = instanceNamespaceContentHandler.onElement(context, new QualifiedName(elem), elem);

        assertTrue(result instanceof HelloWorldScheme);
    }


    /**
     * A {@link HelloWorldScheme} is a simple {@link Builder} that when realized will return a greeting.
     */
    public static class HelloWorldScheme implements Builder<String>
    {
        /**
         * A simple test {@link Scheme}.
         *
         * @return a String
         */
        public String realize()
        {
            return "Hello World";
        }
    }
}
