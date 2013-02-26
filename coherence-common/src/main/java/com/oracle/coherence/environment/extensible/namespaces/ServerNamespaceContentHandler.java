/*
 * File: ServerNamespaceContentHandler.java
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
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ServerNamespaceContentHandler} handles XML Configuration processing for the
 * backing map {@link EventProcessor}s for {@link com.tangosol.net.NamedCache}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ServerNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard Constructor.
     */
    public ServerNamespaceContentHandler()
    {
        registerContentHandler("backingmap-event-processor", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // decorate the CacheMapping for the cache with the PublisherDefinition
                String cacheName = getSourceCacheName(xmlElement);
                CacheMapping cacheMapping =
                    context.getEnvironment().getResource(CacheMappingRegistry.class).findCacheMapping(cacheName);
                ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) context.processOnlyElementOf(xmlElement);

                cacheMapping.addEnrichment(ParameterizedBuilder.class, "event-processor", builder);

                return builder;
            }
        });
    }


    /**
     * Determines the name of the cache in which a cache-backingmap-event-processor element is declared.
     *
     * @param cacheEventProcessorElement The {@link XmlElement} for the cache-backingmap-event-processor declaration
     *
     * @return A string containing the name of the cache for the cache-backingmap-event-processor
     *
     * @throws ConfigurationException If the name of the cache could not be determined
     */
    private String getSourceCacheName(XmlElement cacheEventProcessorElement) throws ConfigurationException
    {
        // ensure the publisherElement is inside a cache-mapping
        if (cacheEventProcessorElement.getParent() != null
            && new QualifiedName(cacheEventProcessorElement.getParent()).getLocalName().equals("cache-mapping"))
        {
            // determine the cache name from the cache-mapping
            XmlElement cacheNameElement = cacheEventProcessorElement.getParent().findElement("cache-name");

            if (cacheNameElement == null)
            {
                // the cache-backingmap-event-processor should be in a cache-mapping
                throw new ConfigurationException(String
                    .format("The element %s is missing a <cache-name> element declaration",
                            cacheEventProcessorElement.getParent()),
                                                 "Please consult the documentation on how to use the BackingMapEventProcessor namespace");
            }

            return cacheNameElement.getString();
        }
        else
        {
            // the cache-backingmap-event-processor should be in a cache-mapping
            throw new ConfigurationException(String
                .format("Missing 'cache-name' declaration for the cache-backingmap-event-processor %s",
                        cacheEventProcessorElement),
                                             "The cache-backingmap-event-processor declaration should either inside a <cache-mapping> element or declare a 'cache-name' attribute.");
        }
    }
}
