/*
 * File: ServerNamespaceContentHandlerTest.java
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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A {@link ServerNamespaceContentHandlerTest} tests the BackingMapEventProcessor configuration.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ServerNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Test creation of a BackingMapEventProcessor.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration issue.
     */
    @Test
    public void testBackingMapEventProcessor() throws URISyntaxException, ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        CacheMappingRegistry mockRegistry     = mock(CacheMappingRegistry.class);
        CacheMapping         mockCacheMapping = mock(CacheMapping.class);

        when(mockRegistry.findCacheMapping("coherence.patterns.actor.actors")).thenReturn(mockCacheMapping);
        when(env.getResource(CacheMappingRegistry.class)).thenReturn(mockRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));
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
        context.ensureNamespaceContentHandler("bm",
                                              new URI(String.format("class:%s",
                                                                    ServerNamespaceContentHandler.class.getName())));

        XmlElement elem =
            XmlHelper
                .loadXml("  <cache-mapping><cache-name>coherence.patterns.actor.actors</cache-name><scheme-name>ActorCacheScheme</scheme-name> <bm:backingmap-event-processor><instance:class classname=\"com.oracle.coherence.environment.extensible.namespaces.BMProcessor\" /></bm:backingmap-event-processor></cache-mapping>");
        XmlElement bmelem = elem.findElement("bm:backingmap-event-processor");
        ServerNamespaceContentHandler bmNamespaceContentHandler =
            (ServerNamespaceContentHandler) context.getNamespaceContentHandler("bm");
        ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) bmNamespaceContentHandler.onElement(context,
                                                                                                        new QualifiedName(bmelem),
                                                                                                        bmelem);

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE).getClass(), BMProcessor.class);
    }


    /**
     * Test creation of a BackingMapEventProcessor but no cache-name in the enclosing element.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a configuration issue.
     */
    @Test(expected = ConfigurationException.class)
    public void testNoCacheName() throws URISyntaxException, ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        CacheMappingRegistry mockRegistry     = mock(CacheMappingRegistry.class);
        CacheMapping         mockCacheMapping = mock(CacheMapping.class);

        when(mockRegistry.findCacheMapping("coherence.patterns.actor.actors")).thenReturn(mockCacheMapping);
        when(env.getResource(CacheMappingRegistry.class)).thenReturn(mockRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));
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
        context.ensureNamespaceContentHandler("bm",
                                              new URI(String.format("class:%s",
                                                                    ServerNamespaceContentHandler.class.getName())));

        XmlElement elem =
            XmlHelper
                .loadXml("  <cache-mapping><scheme-name>ActorCacheScheme</scheme-name> <bm:backingmap-event-processor><instance:class classname=\"com.oracle.coherence.environment.extensible.namespaces.BMProcessor\" /></bm:backingmap-event-processor></cache-mapping>");
        XmlElement bmelem = elem.findElement("bm:backingmap-event-processor");
        ServerNamespaceContentHandler bmNamespaceContentHandler =
            (ServerNamespaceContentHandler) context.getNamespaceContentHandler("bm");
        ParameterizedBuilder<?> result = (ParameterizedBuilder<?>) bmNamespaceContentHandler.onElement(context,
                                                                                                       new QualifiedName(bmelem),
                                                                                                       bmelem);

        assertEquals(result.realize(EmptyParameterProvider.INSTANCE).getClass(), BMProcessor.class);
    }
}
