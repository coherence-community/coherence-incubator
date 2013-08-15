/*
 * File: EventDistributionNamespaceHandler.java
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


import com.oracle.coherence.common.builders.SerializableInstanceBuilder;
import com.oracle.coherence.common.resourcing.ResourceProviderManager;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.EventTransformer;
import com.oracle.coherence.patterns.eventdistribution.channels.BinaryEntryStoreEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.CacheStoreEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.FileEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.RemoteClusterEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.StdErrEventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolver;
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
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Filter;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.filter.EntryFilter;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;


/**
 * The {@link EventDistributionNamespaceHandler} is responsible for capturing and
 * creating the event distribution pattern configuration when processing a event
 * distribution configuration file.
 *
 * @author Brian Oliver
 */
public class EventDistributionNamespaceHandler
        extends AbstractNamespaceHandler
        implements ElementPreprocessor
{
    /**
     * The namespace prefix.
     */
    private String prefix = "";

    /**
     * The map used to keep track of config that was introduced.
     */
    Map<ResourceRegistry, Set<String>> mapIntroduce = new HashMap<ResourceRegistry, Set<String>>();

    // ----- constructors ---------------------------------------------------

    /**
     * Standard Constructor.
     */
    public EventDistributionNamespaceHandler()
    {
        // define the DocumentPreprocessor for the OperationalConfig namespace
        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        // add the system property pre-processor
        // Coherence already registers this...
        // dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.addElementPreprocessor(this);

        setDocumentPreprocessor(dep);

        registerProcessor("instance", new InstanceProcessor());
        registerProcessor("init-param", new InitParamProcessor());
        registerProcessor("init-params", new InitParamsProcessor());


        registerProcessor("distributor", new ElementProcessor<EventDistributorTemplate>()
            {
                @Override
                public EventDistributorTemplate process(
                        ProcessingContext context,
                        XmlElement xmlElement)
                        throws ConfigurationException
                {
                    // configure the event distributor template
                    EventDistributorTemplate template = new EventDistributorTemplate();

                    context.inject(template, xmlElement);

                    ResourceRegistry registry = context.getResourceRegistry();

                    if (registry.getResource(EventDistributorTemplate.class) == null)
                    {
                        registry.registerResource(
                                EventDistributorTemplate.class,
                                template);
                    }

                    // when we're inside a cache mapping, enrich the CacheMapping with the distributor template id
                    if (xmlElement.getParent() != null
                            && xmlElement.getParent().getName().equals(
                            "cache-mapping"))
                    {
                        // determine the cache name from the cache-mapping
                        XmlElement cacheNameElement =
                                xmlElement.getParent()
                                        .findElement("cache-name");

                        if (cacheNameElement == null)
                        {
                            // the xmlElement should be in a cache-mapping
                            throw new ConfigurationException(String
                                    .format("The EventDistributor element in %s can't determine the <cache-name> from it's parent.",
                                            xmlElement.getParent()),
                                    "Please consult the documentation on how to use the Event Distribution namespace");
                        }

                        CacheMapping cacheMapping = context.getCookie(CacheMapping.class);

                        cacheMapping.getResourceRegistry().registerResource(
                                EventDistributorTemplate.class, template);
                    }

                    if (registry.getResource(XmlElement.class, "legacy-cache-config") != null)
                    {
                        // Unregister the legacy since it will be re-registered by Coherence
                        registry.unregisterResource(XmlElement.class, "legacy-cache-config");
                    }

                    return template;
                }
            });

        registerProcessor("coherence-based-distributor-scheme", new ElementProcessor<CoherenceEventDistributorBuilder>()
        {
            @Override
            public CoherenceEventDistributorBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                CoherenceEventDistributorBuilder builder =
                        new CoherenceEventDistributorBuilder();

                context.inject(builder, xmlElement);


                introduceOnce(context, "coherence-messagingpattern-cache-config.xml");

                ResourceRegistry registry = context.getResourceRegistry();
                if (registry.getResource(XmlElement.class, "legacy-cache-config") != null)
                {
                    // Unregister the legacy since it will be re-registered by Coherence
                    registry.unregisterResource(XmlElement.class, "legacy-cache-config");
                }

                return builder;
            }
        });


        registerProcessor("jms-based-distributor-scheme", new ElementProcessor<JMSEventDistributorBuilder>()
        {
            @Override
            public JMSEventDistributorBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                try
                {
                    // load the coherence-common cache configuration (as we use live objects)
                    introduceOnce(context, "coherence-common-cache-config.xml");

                    JMSEventDistributorBuilder builder = new JMSEventDistributorBuilder();

                    context.inject(builder, xmlElement);

                    ResourceRegistry registry = context.getResourceRegistry();
                    if (registry.getResource(XmlElement.class, "legacy-cache-config") != null)
                    {
                        // Unregister the legacy since it will be re-registered
                        registry.unregisterResource(XmlElement.class, "legacy-cache-config");
                    }

                    return builder;
                }
                catch (JMSException e)
                {
                    throw new ConfigurationException(String.format(
                            "Failed to establish JMS infrastructure for %s",
                            xmlElement),
                            "Please ensure the JMS infrastructure is available and correctly configured for access",
                            e);
                }
            }
        });


        registerProcessor("connection-factory-scheme", new ElementProcessor()
        {
            @Override
            public ParameterizedBuilder<ConnectionFactory> process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                return (ParameterizedBuilder<ConnectionFactory>) context.processOnlyElementOf(xmlElement);
            }
        });


        registerProcessor("distribution-channels", new ElementProcessor<List<EventChannelControllerDependenciesTemplate>>()
        {
            @Override
            public List<EventChannelControllerDependenciesTemplate> process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                List<EventChannelControllerDependenciesTemplate> listTemplates = new ArrayList<EventChannelControllerDependenciesTemplate>();

                for (XmlElement elementChild : ((List<XmlElement>) xmlElement.getElementList()))
                {
                    Object oValue = context.processElement(elementChild);

                    if (oValue instanceof EventChannelControllerDependenciesTemplate)
                    {
                        listTemplates.add((EventChannelControllerDependenciesTemplate) oValue);
                    }
                    else
                    {
                        throw new ConfigurationException(String.format("Invalid participant definition [%s] in [%s]",
                                elementChild, xmlElement), "Please ensure the participant is correctly defined");
                    }
                }

                return listTemplates;
            }
        });


        registerProcessor("distribution-channel", new ElementProcessor<EventChannelControllerDependenciesTemplate>()
        {
            @Override
            public EventChannelControllerDependenciesTemplate process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                EventChannelControllerDependenciesTemplate template =
                        new EventChannelControllerDependenciesTemplate();

                context.inject(template, xmlElement);

                return template;
            }
        });


        registerProcessor("channel-scheme", new ElementProcessor<ParameterizedBuilder<EventChannel>>()
        {
            @Override
            public ParameterizedBuilder<EventChannel> process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                return (ParameterizedBuilder<EventChannel>) context.processOnlyElementOf(xmlElement);
            }
        });

        registerProcessor("stderr-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                // create a configurable builder for the StdErrEventChannelBuilder
                EventChannelBuilder builder = new StdErrEventChannelBuilder();

                // configure the builder from the XmlElement
                context.inject(builder, xmlElement);

                return builder;
            }
        });

        registerProcessor("file-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                // create a configurable builder for the StdErrEventChannelBuilder
                EventChannelBuilder builder = new FileEventChannelBuilder();

                // configure the builder from the XmlElement
                context.inject(builder, xmlElement);

                return builder;
            }
        });

        registerProcessor("remote-cache-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // create a configurable builder for the RemoteCacheEventChannelBuilder
                        EventChannelBuilder builder = new RemoteCacheEventChannelBuilder();

                        // configure the builder from the XmlElement
                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });

        registerProcessor("remote-cluster-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // create a configurable builder for the RemoteCacheEventChannelBuilder
                        RemoteClusterEventChannelBuilder builder = new RemoteClusterEventChannelBuilder();

                        // configure the builder from the XmlElement
                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });


        registerProcessor("remote-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                return (EventChannelBuilder) context.processOnlyElementOf(
                        xmlElement);
            }
        });


        registerProcessor("parallel-local-cache-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // create a configurable builder for the ParallelLocalCacheEventChannelBuilder
                        EventChannelBuilder builder = new ParallelLocalCacheEventChannelBuilder();

                        // configure the builder from the XmlElement
                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });

        registerProcessor("local-cache-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // create a configurable builder for the LocalCacheEventChannelBuilder
                        LocalCacheEventChannelBuilder builder = new LocalCacheEventChannelBuilder();

                        // configure the builder from the XmlElement
                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });

        registerProcessor("cachestore-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                CacheStoreEventChannelBuilder builder = new CacheStoreEventChannelBuilder();

                context.inject(builder, xmlElement);

                return builder;
            }
        });

        registerProcessor("cachestore-scheme", new ElementProcessor<ParameterizedBuilder>()
        {
            @Override
            public ParameterizedBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result instanceof ParameterizedBuilder)
                {
                    return (ParameterizedBuilder) result;
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

        registerProcessor("binaryentry-store-channel-scheme", new ElementProcessor<EventChannelBuilder>()
        {
            @Override
            public EventChannelBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        EventChannelBuilder builder = new BinaryEntryStoreEventChannelBuilder();

                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });

        registerProcessor("binaryentry-store-scheme", new ElementProcessor<ParameterizedBuilder>()
        {
            @Override
            public ParameterizedBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result instanceof ParameterizedBuilder)
                {
                    return (ParameterizedBuilder) result;
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


        registerProcessor("conflict-resolver-scheme", new ElementProcessor<ParameterizedBuilder<ConflictResolver>>()
        {
            @Override
            public ParameterizedBuilder<ConflictResolver> process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                SerializableInstanceBuilder builder = new SerializableInstanceBuilder();

                context.inject(builder, xmlElement.getElement("event:instance"));

                return builder;
            }
        });

        registerProcessor("transformer-scheme", new ElementProcessor<ParameterizedBuilder<EventIteratorTransformer>>()
        {
            @Override
            public ParameterizedBuilder<EventIteratorTransformer> process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                return (ParameterizedBuilder<EventIteratorTransformer>) context.processOnlyElementOf(xmlElement);
            }
        });

        registerProcessor("event-transformer", new ElementProcessor<ParameterizedBuilder<EventTransformer>>()
        {
            @Override
            public ParameterizedBuilder<EventTransformer> process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                SerializableInstanceBuilder builder = new SerializableInstanceBuilder();

                context.inject(builder, xmlElement.getElement("event:instance"));

                return builder;
            }
        });


        registerProcessor("chained-transformer-scheme", new ElementProcessor<ChainedEventIteratorTransformerBuilder>()
        {
            @Override
            public ChainedEventIteratorTransformerBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        ChainedEventIteratorTransformerBuilder builder =
                                new ChainedEventIteratorTransformerBuilder();

                        ArrayList<XmlElement> elementList =
                                (ArrayList<XmlElement>) xmlElement
                                        .getElementList();

                        for (XmlElement element : elementList)
                        {
                            Object nestedBuilder =
                                    context.processElement(element);

                            if (nestedBuilder instanceof ParameterizedBuilder<?>)
                            {
                                builder.addEventsTransformerBuilder(
                                        (ParameterizedBuilder<EventIteratorTransformer>) nestedBuilder);
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

        registerProcessor("filter", new ElementProcessor<Filter>()
        {
            @Override
            public Filter process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
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

        registerProcessor("entry-filter", new ElementProcessor<EventEntryFilter>()
        {
            @Override
            public EventEntryFilter process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
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

        registerProcessor("original-entry-filter", new ElementProcessor<EventOriginalEntryFilter>()
        {
            @Override
            public EventOriginalEntryFilter process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
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

        registerProcessor("event-type-filter", new ElementProcessor<EventTypeFilter>()
        {
            @Override
            public EventTypeFilter process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                // parse the event types
                String sEventTypes = xmlElement.getString("").toUpperCase();
                StringTokenizer tokenizer =
                        new StringTokenizer(sEventTypes, ",");
                EnumSet<EventType> setEventTypes =
                        EnumSet.noneOf(EventType.class);

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
                        throw new ConfigurationException(String.format(
                                "The %s specified an illegal EventType.",
                                xmlElement),
                                "Please consult the documentation regarding use of the EventTypeFilters",
                                e);
                    }
                }

                // create an EventTypeFilter given the events
                return new EventTypeFilter(setEventTypes);
            }
        });

        registerProcessor("filtering-transformer-scheme", new ElementProcessor<FilteringEventIteratorTransformerBuilder>()
        {
            @Override
            public FilteringEventIteratorTransformerBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // get the defined filter
                        Object result =
                                context.processOnlyElementOf(xmlElement);

                        if (result != null && result instanceof Filter)
                        {
                            // create a configurable FilteringEventsTransformerBuilder
                            FilteringEventIteratorTransformerBuilder builder =
                                    new FilteringEventIteratorTransformerBuilder();

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

        registerProcessor("mutating-transformer-scheme", new ElementProcessor<MutatingEventIteratorTransformerBuilder>()
        {
            @Override
            public MutatingEventIteratorTransformerBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // create a configurable MutatingEventsTransformerBuilder
                        MutatingEventIteratorTransformerBuilder builder =
                                new MutatingEventIteratorTransformerBuilder();

                        // configure the builder using the xmlElement
                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });

        registerProcessor("coalescing-transformer-scheme", new ElementProcessor<CoalescingEventIteratorTransformerBuilder>()
        {
            @Override
            public CoalescingEventIteratorTransformerBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
                    {
                        // create a configurable CoalescingEventsTransformerBuilder
                        CoalescingEventIteratorTransformerBuilder builder =
                                new CoalescingEventIteratorTransformerBuilder();

                        // configure the builder using the xmlElement
                        context.inject(builder, xmlElement);

                        return builder;
                    }
                });

    }


    protected ParameterList getInitParams(ProcessingContext context, XmlElement xmlElement)
    {
        XmlElement xmlInitParams = xmlElement.getElement("init-params");
        return (xmlInitParams == null) ? null : (ParameterList) context.processElement(xmlInitParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartNamespace(ProcessingContext context, XmlElement element, String prefix, URI uri)
    {
        this.prefix = prefix;

        super.onStartNamespace(context, element, prefix, uri);

        ResourceRegistry registry = context.getResourceRegistry();

        // register a EventChannelControllerManager with the environment
        registry.registerResource(EventChannelControllerManager.class,
                new EventChannelControllerManager());

        // register the ResourceProviderManager with the environment (part of INC-757 fix and COH-3618 workaround)
        registry.registerResource(ResourceProviderManager.class, new ResourceProviderManager());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
    {
        XmlElement parent = element.getParent();
        if (parent == null ||
           !(parent.getQualifiedName().getPrefix().equals(prefix)))
        {
            // Only modify the elements if this is EVENT namespace
            return false;
        }

        if (element.getName().equals("instance"))
        {
            element.setName(prefix +":instance");
        }
        if (element.getName().equals("class-scheme"))
        {
            element.setName(prefix +":instance");
        }
        else if (element.getName().equals("class-name"))
        {
            element.setName(prefix +":class-name");
        }
        else if (element.getName().equals("init-params"))
        {
            element.setName(prefix +":init-params");
        }
        else if (element.getName().equals("init-param"))
        {
            element.setName(prefix +":init-param");
        }
        else if (element.getName().equals("param-name"))
        {
            element.setName(prefix +":param-name");
        }
        else if (element.getName().equals("param-value"))
        {
            element.setName(prefix +":param-value");
        }
        else if (element.getName().equals("param-type"))
        {
            element.setName(prefix +":param-type");
        }
        return false;
    }

    /**
     * Introduce a configuration file at most once for a given context.  This method
     * is needed to prevent an exception that would occur if a CacheMapping entry
     * was redefined because the same cache config was introduced twice (e.g. the
     * coherence-messagingpattern-cache-config.xml).
     *
     * @param context    the ProcessingContext
     * @param configURI  the configuration URI
     */
    private void introduceOnce(ProcessingContext context, String configURI)
    {
        ResourceRegistry registry = context.getResourceRegistry();

        Set<String> setConfig = mapIntroduce.get(registry);
        if (setConfig == null)
        {
            setConfig = new HashSet<String>();
            mapIntroduce.put(registry, setConfig);
        }

        if (!setConfig.contains(configURI))
        {
            setConfig.add(configURI);
            context.processDocumentAt(configURI);
        }
    }

}