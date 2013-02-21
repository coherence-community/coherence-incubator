/*
 * File: EventDistributionNamespaceContentHandler.java
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

package com.oracle.coherence.patterns.eventdistribution.configuration;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.resourcing.ResourceProviderManager;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.oracle.coherence.environment.extensible.namespaces.AbstractNamespaceContentHandler;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.channels.BinaryEntryStoreEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.CacheStoreEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.FileEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.StdErrEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.LocalCacheEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ParallelLocalCacheEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.RemoteCacheEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.distributors.EventChannelControllerManager;
import com.oracle.coherence.patterns.eventdistribution.distributors.coherence.CoherenceEventDistributorBuilder;
import com.oracle.coherence.patterns.eventdistribution.distributors.jms.JMSEventDistributorBuilder;
import com.oracle.coherence.patterns.eventdistribution.filters.EventEntryFilter;
import com.oracle.coherence.patterns.eventdistribution.filters.EventOriginalEntryFilter;
import com.oracle.coherence.patterns.eventdistribution.filters.EventTypeFilter;
import com.oracle.coherence.patterns.eventdistribution.filters.EventTypeFilter.EventType;
import com.oracle.coherence.patterns.eventdistribution.transformers.ChainedEventIteratorTransformerBuilder;
import com.oracle.coherence.patterns.eventdistribution.transformers.CoalescingEventIteratorTransformerBuilder;
import com.oracle.coherence.patterns.eventdistribution.transformers.FilteringEventIteratorTransformerBuilder;
import com.oracle.coherence.patterns.eventdistribution.transformers.MutatingEventIteratorTransformerBuilder;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.EntryFilter;

import javax.jms.JMSException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A {@link EventDistributionNamespaceContentHandler} handles XML Configuration processing for the
 * Event Distribution Pattern
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventDistributionNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard Constructor.
     */
    public EventDistributionNamespaceContentHandler()
    {
        registerContentHandler("distributor", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // configure the event distributor template
                EventDistributorTemplate template = new EventDistributorTemplate();

                context.configure(template, qualifiedName, xmlElement);

                // register the template with the environment
                context.getEnvironment().registerResource(EventDistributorTemplate.class,
                                                          template.getTemplateId(),
                                                          template);

                // when we're inside a cache mapping, enrich the CacheMapping with the distributor template id
                if (xmlElement.getParent() != null
                    && new QualifiedName(xmlElement.getParent()).getLocalName().equals("cache-mapping"))
                {
                    // determine the cache name from the cache-mapping
                    XmlElement cacheNameElement = xmlElement.getParent().findElement("cache-name");

                    if (cacheNameElement == null)
                    {
                        // the xmlElement should be in a cache-mapping
                        throw new ConfigurationException(String
                            .format("The EventDistributor element in %s can't determine the <cache-name> from it's parent.",
                                    xmlElement.getParent()),
                                                         "Please consult the documentation on how to use the Event Distribution namespace");
                    }

                    String cacheName = cacheNameElement.getString();
                    CacheMapping cacheMapping =
                        context.getEnvironment().getResource(CacheMappingRegistry.class).findCacheMapping(cacheName);

                    cacheMapping.addEnrichment(EventDistributorTemplate.class, template.getTemplateId(), template);
                }
                else
                {
                    // when outside a cache-mapping register the distributor template with the environment
                    context.getEnvironment().registerResource(EventDistributorTemplate.class,
                                                              template.getTemplateId(),
                                                              template);
                }

                return template;
            }
        });

        registerContentHandler("coherence-based-distributor-scheme", new ElementContentHandler()
        {
            public Object onElement(final ConfigurationContext context,
                                    QualifiedName              qualifiedName,
                                    XmlElement                 xmlElement) throws ConfigurationException
            {
                CoherenceEventDistributorBuilder builder = new CoherenceEventDistributorBuilder();

                context.configure(builder, qualifiedName, xmlElement);

                context.processDocumentAt("coherence-messagingpattern-cache-config.xml");

                return builder;
            }
        });

        registerContentHandler("distributor-scheme", new ElementContentHandler()
        {
            public Object onElement(final ConfigurationContext context,
                                    QualifiedName              qualifiedName,
                                    XmlElement                 xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("jms-based-distributor-scheme", new ElementContentHandler()
        {
            public Object onElement(final ConfigurationContext context,
                                    QualifiedName              qualifiedName,
                                    XmlElement                 xmlElement) throws ConfigurationException
            {
                try
                {
                    // load the coherence-common cache configuration (as we use live objects)
                    context.processDocumentAt("coherence-common-cache-config.xml");

                    JMSEventDistributorBuilder builder = new JMSEventDistributorBuilder();

                    context.configure(builder, qualifiedName, xmlElement);

                    return builder;
                }
                catch (JMSException e)
                {
                    throw new ConfigurationException(String.format("Failed to establish JMS infrastructure for %s",
                                                                   xmlElement),
                                                     "Please ensure the JMS infrastructure is available and correctly configured for access",
                                                     e);
                }
            }
        });

        registerContentHandler("connection-factory-scheme", new ElementContentHandler()
        {
            public Object onElement(final ConfigurationContext context,
                                    QualifiedName              qualifiedName,
                                    XmlElement                 xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("distribution-channels", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // process each of the children (each should be an EventChannelControllerDependenciesTemplate)
                Map<String, EventChannelControllerDependenciesTemplate> children = context.processElementsOf(xmlElement,
                                                                                                             EventChannelControllerDependenciesTemplate.class);

                return children.values();
            }
        });

        registerContentHandler("distribution-channel", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                EventChannelControllerDependenciesTemplate template = new EventChannelControllerDependenciesTemplate();

                context.configure(template, qualifiedName, xmlElement);

                return template;
            }
        });

        registerContentHandler("channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("stderr-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable builder for the StdErrEventChannelBuilder
                EventChannelBuilder builder = new StdErrEventChannelBuilder();

                // configure the builder from the XmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("file-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable builder for the StdErrEventChannelBuilder
                EventChannelBuilder builder = new FileEventChannelBuilder();

                // configure the builder from the XmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("remote-cache-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable builder for the RemoteCacheEventChannelBuilder
                EventChannelBuilder builder = new RemoteCacheEventChannelBuilder();

                // configure the builder from the XmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("remote-cluster-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable builder for the RemoteCacheEventChannelBuilder
                RemoteClusterEventChannelBuilder builder = new RemoteClusterEventChannelBuilder();

                // configure the builder from the XmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("remote-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("parallel-local-cache-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable builder for the ParallelLocalCacheEventChannelBuilder
                ParallelLocalCacheEventChannelBuilder builder = new ParallelLocalCacheEventChannelBuilder();

                // configure the builder from the XmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("local-cache-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable builder for the LocalCacheEventChannelBuilder
                LocalCacheEventChannelBuilder builder = new LocalCacheEventChannelBuilder();

                // configure the builder from the XmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("cachestore-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                EventChannelBuilder builder = new CacheStoreEventChannelBuilder();

                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("cachestore-scheme", new ElementContentHandler()
        {
            @Override
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result instanceof ParameterizedBuilder
                    && ((ParameterizedBuilder<?>) result).realizesClassOf(CacheStore.class,
                                                                          SystemPropertyParameterProvider.INSTANCE))
                {
                    return result;
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The element %s will not produce a CacheStore/BinaryEntryStore instance as expected",
                                xmlElement),
                                                     "Please consult the documentation regarding use of the Event Distribution namespace");
                }
            }
        });

        registerContentHandler("binaryentry-store-channel-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                EventChannelBuilder builder = new BinaryEntryStoreEventChannelBuilder();

                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("binaryentry-store-scheme", new ElementContentHandler()
        {
            @Override
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result instanceof ParameterizedBuilder
                    && ((ParameterizedBuilder<?>) result).realizesClassOf(BinaryEntryStore.class,
                                                                          SystemPropertyParameterProvider.INSTANCE))
                {
                    return result;
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The element %s will not produce a CacheStore/BinaryEntryStore instance as expected",
                                xmlElement),
                                                     "Please consult the documentation regarding use of the Event Distribution namespace");
                }
            }
        });

        registerContentHandler("conflict-resolver-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("transformer-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("event-transformer", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return context.processOnlyElementOf(xmlElement);
            }
        });

        registerContentHandler("chained-transformer-scheme", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                ChainedEventIteratorTransformerBuilder builder     = new ChainedEventIteratorTransformerBuilder();

                ArrayList<XmlElement>                  elementList =
                    (ArrayList<XmlElement>) xmlElement.getElementList();

                for (XmlElement element : elementList)
                {
                    Object nestedBuilder = context.processElement(element);

                    if (nestedBuilder instanceof ParameterizedBuilder<?>
                        && ((ParameterizedBuilder<?>) nestedBuilder)
                            .realizesClassOf(EventIteratorTransformer.class, SystemPropertyParameterProvider.INSTANCE))
                    {
                        builder.addEventsTransformerBuilder((ParameterizedBuilder<EventIteratorTransformer>) nestedBuilder);
                    }
                    else
                    {
                        throw new ConfigurationException(String
                            .format("The element %s will not produce an EventTransformer instance in the <chained-transformer-scheme> declaration",
                                    element),
                                                         "Please consult the documentation regarding use of the Event Distribution namespace");
                    }

                }

                return builder;
            }
        });

        registerContentHandler("filter", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result != null && result instanceof Filter)
                {
                    return (Filter) result;
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The filter specified in %s does not implement the com.tangosol.util.Filter interface",
                                xmlElement),
                                                     "Please consult the documentation regarding use of the Event Distribution namespace");
                }
            }
        });

        registerContentHandler("entry-filter", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result != null && result instanceof EntryFilter)
                {
                    return new EventEntryFilter((EntryFilter) result);
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The filter specified in %s does not implement the com.tangosol.util.EntryFilter interface",
                                xmlElement),
                                                     "Please consult the documentation regarding use of the Event Distribution namespace");
                }
            }
        });

        registerContentHandler("original-entry-filter", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result != null && result instanceof EntryFilter)
                {
                    return new EventOriginalEntryFilter((EntryFilter) result);
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The filter specified in %s does not implement the com.tangosol.util.EntryFilter interface",
                                xmlElement),
                                                     "Please consult the documentation regarding use of the Event Distribution namespace");
                }
            }
        });

        registerContentHandler("event-type-filter", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // parse the event types
                String             sEventTypes   = xmlElement.getString("").toUpperCase();
                StringTokenizer    tokenizer     = new StringTokenizer(sEventTypes, ",");
                EnumSet<EventType> setEventTypes = EnumSet.noneOf(EventType.class);

                while (tokenizer.hasMoreTokens())
                {
                    String sEventType = tokenizer.nextToken().trim();

                    try
                    {
                        EventType eventType = EventType.valueOf(sEventType);

                        setEventTypes.add(eventType);
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException(String.format("The %s specified an illegal EventType.",
                                                                       xmlElement),
                                                         "Please consult the documentation regarding use of the EventTypeFilters",
                                                         e);
                    }
                }

                // create an EventTypeFilter given the events
                return new EventTypeFilter(setEventTypes);
            }
        });

        registerContentHandler("filtering-transformer-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // get the defined filter
                Object result = context.processOnlyElementOf(xmlElement);

                if (result != null && result instanceof Filter)
                {
                    // create a configurable FilteringEventsTransformerBuilder
                    FilteringEventIteratorTransformerBuilder builder = new FilteringEventIteratorTransformerBuilder();

                    builder.setFilter((Filter) result);

                    return builder;
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The filter specified in %s does not implement the com.tangosol.util.Filter interface",
                                xmlElement),
                                                     "Please consult the documentation regarding use of the Event Distribution namespace");
                }
            }
        });

        registerContentHandler("mutating-transformer-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable MutatingEventsTransformerBuilder
                MutatingEventIteratorTransformerBuilder builder = new MutatingEventIteratorTransformerBuilder();

                // configure the builder using the xmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });

        registerContentHandler("coalescing-transformer-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // create a configurable CoalescingEventsTransformerBuilder
                CoalescingEventIteratorTransformerBuilder builder = new CoalescingEventIteratorTransformerBuilder();

                // configure the builder using the xmlElement
                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartScope(ConfigurationContext context,
                             String               prefix,
                             URI                  uri)
    {
        super.onStartScope(context, prefix, uri);

        // register a EventChannelControllerManager with the environment
        context.getEnvironment().registerResource(EventChannelControllerManager.class,
                                                  new EventChannelControllerManager(context.getEnvironment()));

        // register the ResourceProviderManager with the environment (part of INC-757 fix and COH-3618 workaround)
        context.getEnvironment().registerResource(ResourceProviderManager.class, new ResourceProviderManager());
    }
}
