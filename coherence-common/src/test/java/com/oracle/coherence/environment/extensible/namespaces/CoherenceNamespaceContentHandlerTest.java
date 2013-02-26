/*
 * File: CoherenceNamespaceContentHandlerTest.java
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

import com.oracle.coherence.common.builders.BuilderRegistry;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.net.NamedCache;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the {@link CoherenceNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Test that we can realize custom-namespace elements in &lt;class-schemes&gt;.
     *
     * @throws URISyntaxException if there is a problem with the URI
     * @throws ConfigurationException if there is a configuration error
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testCustomClassScheme() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("value",
                                              new URI(String.format("class:%s",
                                                                    ValueNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("instance",
                                              new URI(String.format("class:%s",
                                                                    InstanceNamespaceContentHandler.class.getName())));

        String xml = "<class-scheme>" + "<instance:class classname=\"java.awt.Point\">"
                     + "<value:integer>100</value:integer>" + "<value:integer>100</value:integer>"
                     + "</instance:class>" + "</class-scheme>";

        XmlElement element          = XmlHelper.loadXml(xml);

        Object     processedElement = context.processElement(element);

        assertTrue(processedElement instanceof ParameterizedBuilder<?>);

        Point result =
            (Point) ((ParameterizedBuilder<?>) processedElement).realize(SystemPropertyParameterProvider.INSTANCE);

        assertTrue(result.getX() == 100);
        assertTrue(result.getY() == 100);
    }


    /**
     * Test that we can realize a &lt;class-scheme&gt;.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testConfigurableClassScheme() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<class-scheme>" + "<class-name>java.awt.Point</class-name>" + "<init-params>"
                     + "<init-param><param-value>100</param-value></init-param>"
                     + "<init-param><param-value>100</param-value></init-param>" + "</init-params>" + "</class-scheme>";

        XmlElement              element = XmlHelper.loadXml(xml);

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) context.processElement(element);

        assertTrue(builder != null);
        assertTrue(builder.realizesClassOf(Point.class, EmptyParameterProvider.INSTANCE));
        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), new Point(100, 100));
    }


    /**
     * Test static factory use of a &lt;class-scheme&gt;.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testConfigurableStaticFactoryClassScheme() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<class-scheme>" + "<class-factory-name>java.lang.System</class-factory-name>"
                     + "<method-name>getProperty</method-name>" + "<init-params>"
                     + "<init-param><param-value>java.class.path</param-value></init-param>" + "</init-params>"
                     + "</class-scheme>";

        XmlElement              element = XmlHelper.loadXml(xml);

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) context.processElement(element);

        assertTrue(builder != null);
        assertTrue(builder.realizesClassOf(String.class, EmptyParameterProvider.INSTANCE));
        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), System.getProperty("java.class.path"));
    }


    /**
     * Test that invalid strongly typed params in a &lt;class-scheme&gt;s fail.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test(expected = Exception.class)
    public void testInvalidConfigurableClassScheme() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<class-scheme>" + "<class-name>java.awt.Point</class-name>" + "<init-params>"
                     + "<init-param><param-type>double</param-type><param-value>100</param-value></init-param>"
                     + "<init-param><param-value>100</param-value></init-param>" + "</init-params>" + "</class-scheme>";

        XmlElement              element = XmlHelper.loadXml(xml);

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) context.processElement(element);

        assertTrue(builder != null);
        assertTrue(builder.realizesClassOf(Point.class, EmptyParameterProvider.INSTANCE));
        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), new Point(100, 100));
    }


    /**
     * Test nesting of traditional &lt;class-scheme&gt;s.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testNestedConfigurableClassScheme() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<class-scheme>" + "<class-name>java.awt.Rectangle</class-name>" + "<init-params>"
                     + "<init-param><param-value><class-scheme>" + "<class-name>java.awt.Point</class-name>"
                     + "<init-params><init-param><param-value>100</param-value></init-param>"
                     + "<init-param><param-value>100</param-value></init-param>"
                     + "</init-params></class-scheme></param-value></init-param>" + "</init-params></class-scheme>";

        XmlElement              element = XmlHelper.loadXml(xml);

        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) context.processElement(element);

        assertTrue(builder != null);
        assertTrue(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));
        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), new Rectangle(new Point(100, 100)));
    }


    /**
     * Test that an init-param is returned as a {@link Parameter}.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testInitParam() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String     xml = "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>";

        XmlElement element          = XmlHelper.loadXml(xml);

        Object     processedElement = context.processElement(element);

        assertTrue(processedElement instanceof Parameter);
        assertTrue(((Parameter) processedElement).getName().equals("size"));
        assertTrue(((Parameter) processedElement).evaluate(EmptyParameterProvider.INSTANCE).getInt() == 100);
    }


    /**
     * Test that an init-param using a {cache-ref} returns a special {@link Parameter}.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testCacheRefInitParam() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml =
            "<init-param><param-type>{cache-ref}</param-type><param-value>dist-cache</param-value></init-param>";

        XmlElement element   = XmlHelper.loadXml(xml);

        Parameter  parameter = (Parameter) context.processElement(element);

        assertTrue(parameter.isStronglyTyped());
        assertTrue(parameter.getType().equals(NamedCache.class.getName()));

        // TODO: we should use a mock CacheFactory here to ensure we're returning an instance of "dist-cache" here
    }


    /**
     * Test that init-params are returned as a {@link ParameterProvider}s.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testInitParams() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<init-params>"
                     + "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>"
                     + "<init-param><param-name>autostart</param-name><param-value>true</param-value></init-param>"
                     + "<init-param><param-name>name</param-name><param-value>rolf harris</param-value></init-param>"
                     + "</init-params>";

        XmlElement element          = XmlHelper.loadXml(xml);

        Object     processedElement = context.processElement(element);

        assertTrue(processedElement instanceof ParameterProvider);

        ParameterProvider parameterProvider = (ParameterProvider) processedElement;

        assertTrue(parameterProvider.getParameter("size").evaluate(EmptyParameterProvider.INSTANCE).getInt() == 100);
        assertTrue(parameterProvider.getParameter("autostart").evaluate(EmptyParameterProvider.INSTANCE).getBoolean());
        assertTrue(parameterProvider.getParameter("name").evaluate(EmptyParameterProvider.INSTANCE).getString()
            .equals("rolf harris"));
    }


    /**
     * Test that &lt;cache-mapping&gt;'s return a {@link CacheMapping}.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testCacheMapping() throws URISyntaxException, ConfigurationException
    {
        Environment          env                  = mock(Environment.class);

        CacheMappingRegistry cacheMappingRegistry = new CacheMappingRegistry();

        when(env.getResource(CacheMappingRegistry.class)).thenReturn(cacheMappingRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<cache-mapping>" + "<cache-name>dist-*</cache-name>" + "<scheme-name>Distributed</scheme-name>"
                     + "<init-params>"
                     + "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>"
                     + "<init-param><param-name>autostart</param-name><param-value>true</param-value></init-param>"
                     + "<init-param><param-name>name</param-name><param-value>rolf harris</param-value></init-param>"
                     + "</init-params>" + "</cache-mapping>";

        XmlElement element          = XmlHelper.loadXml(xml);

        Object     processedElement = context.processElement(element);

        assertTrue(processedElement instanceof CacheMapping);

        CacheMapping cacheMapping = (CacheMapping) processedElement;

        assertTrue(cacheMappingRegistry.findCacheMapping("dist-*") == processedElement);
        assertTrue(cacheMapping.getCacheName().equals("dist-*"));
        assertTrue(cacheMapping.getSchemeName().equals("Distributed"));
        assertTrue(cacheMapping.getParameterProvider().getParameter("size").evaluate(EmptyParameterProvider.INSTANCE)
            .getInt() == 100);
        assertTrue(cacheMapping.getParameterProvider().getParameter("autostart")
            .evaluate(EmptyParameterProvider.INSTANCE).getBoolean());
        assertTrue(cacheMapping.getParameterProvider().getParameter("name").evaluate(EmptyParameterProvider.INSTANCE)
            .getString().equals("rolf harris"));
    }


    /**
     * Test that &lt;defaults&gt; are captured during parsing and generated by the
     * {@link CoherenceNamespaceContentHandler}.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testDefaults() throws URISyntaxException, ConfigurationException
    {
        Environment          env                  = mock(Environment.class);

        CacheMappingRegistry cacheMappingRegistry = new CacheMappingRegistry();

        when(env.getResource(CacheMappingRegistry.class)).thenReturn(cacheMappingRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);
        CoherenceNamespaceContentHandler handler =
            (CoherenceNamespaceContentHandler) context.ensureNamespaceContentHandler("",
                                                                                     new URI(String
                                                                                         .format("class:%s",
                                                                                             CoherenceNamespaceContentHandler.class
                                                                                                 .getName())));

        String xml = "<defaults>" + "<serializer><instance>"
                     + "<class-name>com.tangosol.io.pof.ConfigurablePofContext</class-name><init-params>"
                     + "<init-param><param-type>String</param-type>" + "<param-value>my-pof-config.xml</param-value>"
                     + "</init-param></init-params></instance>" + "</serializer></defaults>";

        context.processElement(xml);

        // build the coherence config
        StringBuilder builder = new StringBuilder();

        handler.build(builder);

        String coherenceConfig = builder.toString();

        assertTrue(coherenceConfig.contains("<defaults>"));
        assertTrue(coherenceConfig.contains("com.tangosol.io.pof.ConfigurablePofContext"));
    }
}
