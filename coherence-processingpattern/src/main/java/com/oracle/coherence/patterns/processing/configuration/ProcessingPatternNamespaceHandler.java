/*
 * File: ProcessingPatternNamespaceHandler.java
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

package com.oracle.coherence.patterns.processing.configuration;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.tuples.Pair;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.oracle.coherence.environment.extensible.namespaces.AbstractNamespaceContentHandler;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.local.LocalExecutorDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.logging.LoggingDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.task.AttributeMatchTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.CompositeTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.DefaultTaskDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.task.RoundRobinTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.TaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.friends.DispatcherManager;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorDefinitionManager;
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.task.TaskProcessorType;
import com.oracle.coherence.patterns.processing.taskprocessor.DefaultTaskProcessor;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.Base;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A {@link ProcessingPatternNamespaceHandler} for the Processing Pattern configuration.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ProcessingPatternNamespaceHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard constructor.
     */
    public ProcessingPatternNamespaceHandler()
    {
        registerContentHandler("cluster-config", new ElementContentHandler()

        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                try
                {
                    parseProcessingPatternClusterConfig(context, xmlElement);
                }
                catch (Throwable e)
                {
                    throw Base.ensureRuntimeException(e);
                }

                return null;
            }
        });

        registerContentHandler("extend-config", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                try
                {
                    parseProcessingPatternExtendConfig(context, xmlElement);
                }
                catch (Throwable e)
                {
                    throw Base.ensureRuntimeException(e);
                }

                return null;
            }
        });

        registerContentHandler("dispatchers", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                try
                {
                    parseProcessingPatternDispatchers(context, xmlElement);
                }
                catch (Throwable e)
                {
                    throw Base.ensureRuntimeException(e);
                }

                return null;
            }
        });
        registerContentHandler("taskprocessors", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                try
                {
                    parseProcessingPatternTaskProcessors(context, xmlElement);
                }
                catch (Throwable e)
                {
                    throw Base.ensureRuntimeException(e);
                }

                return null;
            }
        });

        registerContentHandler("taskprocessordefinition", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return parseProcessingPatternTaskProcessorDefinition(context, xmlElement);
            }
        });

        registerContentHandler("default-taskprocessor", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return parseDefaultTaskProcessor(context, xmlElement);
            }
        });

        registerContentHandler("logging-dispatcher", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return parseProcessingPatternLoggingDispatcher(context, xmlElement);
            }
        });

        registerContentHandler("local-executor-dispatcher", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return parseProcessingPatternLocalExecutorDispatcher(context, xmlElement);
            }
        });

        registerContentHandler("task-dispatcher", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return parseProcessingPatternTaskDispatcher(context, xmlElement);
            }
        });

        registerContentHandler("round-robin-policy", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return new RoundRobinTaskDispatchPolicy();
            }
        });

        registerContentHandler("attribute-match-policy", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return new AttributeMatchTaskDispatchPolicy();
            }
        });

        registerContentHandler("composite-policy", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return parseCompositeTaskDispatchPolicy(context, xmlElement);
            }
        });

        registerContentHandler("attribute", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return new Pair<String, String>(xmlElement.getAttribute("name").toString(),
                                                xmlElement.getValue().toString());
            }
        });

    }


    /**
     * Parse the extend-config element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param xmlElement         the {@link XmlElement} containing the config element
     *
     * @throws Throwable if an exception was thrown
     */
    @SuppressWarnings("unchecked")
    private void parseProcessingPatternExtendConfig(ConfigurationContext context,
                                                    XmlElement           xmlElement) throws Throwable
    {
        try
        {
            // Set up the environment for a grid node
            ProcessingPatternHelper.configureCore((ConfigurableCacheFactory) context.getEnvironment(),
                                                  context.getEnvironment());

            // Parse the child elements
            List<XmlElement> elementList = xmlElement.getElementList();

            if (elementList != null)
            {
                Iterator<XmlElement> iter = elementList.iterator();

                while (iter.hasNext())
                {
                    XmlElement element = (XmlElement) iter.next();

                    if (element.getName().equals("initiator-config"))
                    {
                        StringBuilder builder = new StringBuilder();

                        addExtendPreAmble(builder);
                        builder.append(element.toString());
                        addExtendPostAmble(builder);

                        String      docstring   = builder.toString();
                        XmlDocument xmlDocument = XmlHelper.loadXml(docstring);

                        context.processDocument(xmlDocument);
                    }
                    else
                    {
                        if (element.getName().equals("processing:taskprocessors"))
                        {
                            ProcessingPatternHelper
                                .configureTaskProcessing((ConfigurableCacheFactory) context.getEnvironment(),
                                                         context.getEnvironment());
                            parseProcessingPatternTaskProcessors(context, element);

                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }

    }


    /**
     * Adds a pre-amble to the config file.
     *
     * @param builder the {@link StringBuilder} to use
     */
    private void addExtendPreAmble(StringBuilder builder)
    {
        // include the xml namespace declarations for the cache-config element
        builder.append("<cache-config>\n");

        builder.append("   <caching-scheme-mapping>\n");
        builder.append("      <cache-mapping>\n");

        builder.append("         <cache-name>coherence.patterns.processing.*</cache-name>\n");
        builder.append("         <scheme-name>extend-dist</scheme-name>\n");
        builder.append("      </cache-mapping>\n");
        builder.append("   </caching-scheme-mapping>\n");

        builder.append("\n");
        builder.append("   <caching-schemes>\n");
        builder.append("      <remote-cache-scheme>\n");
        builder.append("      <scheme-name>extend-dist</scheme-name>\n");

    }


    /**
     * Adds a post-amble to the config file.
     *
     * @param builder the {@link StringBuilder} to use
     */
    private void addExtendPostAmble(StringBuilder builder)
    {
        builder.append("      </remote-cache-scheme>\n");
        builder.append("   </caching-schemes>\n");
        builder.append("</cache-config>\n");
    }


    /**
     * Parse the cluster-config element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param xmlElement         the {@link XmlElement} containing the config element
     *
     * @throws Throwable if an exception was thrown
     */
    @SuppressWarnings("unchecked")
    private void parseProcessingPatternClusterConfig(ConfigurationContext context,
                                                     XmlElement           xmlElement) throws Throwable
    {
        XmlDocument document =
            DefaultConfigurableCacheFactory.loadConfigAsResource("coherence-processingpattern-cache-config.xml",
                                                                 context.getClass().getClassLoader());

        context.processDocument(document);

        try
        {
            // Set up the environment for a grid node
            ProcessingPatternHelper.configureFramework((ConfigurableCacheFactory) context.getEnvironment(),
                                                       context.getEnvironment());

            // Parse the children
            List<XmlElement> elementList = xmlElement.getElementList();

            if (elementList != null)
            {
                Iterator<XmlElement> iter = elementList.iterator();

                while (iter.hasNext())
                {
                    context.processElement((XmlElement) iter.next());
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * Parse the taskprocessordefinition element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param xmlElement         the {@link XmlElement} containing the config element
     *
     * @throws Throwable if an exception was thrown
     * @throws ConfigurationException if the configuration is invalid
     *
     */
    @SuppressWarnings("unchecked")
    private void parseProcessingPatternTaskProcessors(ConfigurationContext context,
                                                      XmlElement           xmlElement) throws ConfigurationException, Throwable
    {
        List<XmlElement> elementList = xmlElement.getElementList();

        if (elementList != null)
        {
            LinkedList<TaskProcessorDefinition> tpdList = new LinkedList<TaskProcessorDefinition>();
            Iterator<XmlElement>                iter    = elementList.iterator();

            while (iter.hasNext())
            {
                XmlElement childElem = (XmlElement) iter.next();
                Object     res       = context.processElement(childElem);

                // ensure that ClassSchemes are realized
                if (res instanceof ParameterizedBuilder<?>)
                {
                    res = ((ParameterizedBuilder<?>) res).realize(EmptyParameterProvider.INSTANCE);
                }

                if (res instanceof TaskProcessorDefinition)
                {
                    tpdList.add((TaskProcessorDefinition) res);
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("Object returned by element %s is not a TaskProcessorDefinition", childElem),
                                                     "Please consult the documentation concerning the use of the Processing Pattern namespace");
                }
            }

            // Now register the collected dispatchers
            Iterator<TaskProcessorDefinition> tpdIter = tpdList.iterator();
            TaskProcessorDefinitionManager mgr =
                context.getEnvironment().getResource(TaskProcessorDefinitionManager.class);

            while (tpdIter.hasNext())
            {
                mgr.registerTaskProcessor(tpdIter.next());

            }
        }
    }


    /**
     * Parse the taskprocessordefinition element.
     *
     * @param context the {@link ConfigurationContext} to use
     * @param element the {@link XmlElement} containing the config element
     *
     * @return the {@link TaskProcessorDefinition}
     *
     * @throws ConfigurationException if the configuration is invalid
     */
    @SuppressWarnings("unchecked")
    private Object parseProcessingPatternTaskProcessorDefinition(ConfigurationContext context,
                                                                 XmlElement           element) throws ConfigurationException
    {
        XmlValue            value        = element.getAttribute("id");
        String              identifier   = value.getString();
        String              displayname  = element.getAttribute("displayname").getValue().toString();
        String              type         = element.getAttribute("type").getValue().toString();
        TaskProcessorType   tpType       = TaskProcessorType.valueOf(type);
        TaskProcessor       tp           = null;
        Map<String, String> attributeMap = new HashMap<String, String>();
        List<XmlElement>    elementList  = element.getElementList();

        if (elementList.size() < 1)
        {
            throw new ConfigurationException(String
                .format("No TaskProcessor specified for the TaskProcessorDefinition in %s", element),
                                             "Please consult the documentation concerning the use of the Processing Pattern namespace");
        }

        Iterator<XmlElement> iter = elementList.iterator();

        while (iter.hasNext())
        {
            XmlElement childElem = iter.next();

            Object     res       = context.processElement(childElem);

            // ensure that ParameterizedBuilder are realized
            if (res instanceof ParameterizedBuilder<?>)
            {
                res = ((ParameterizedBuilder<?>) res).realize(EmptyParameterProvider.INSTANCE);
            }

            if (res instanceof TaskProcessor)
            {
                tp = (TaskProcessor) res;
            }
            else if (res instanceof Pair)
            {
                attributeMap.put(((Pair<String, String>) res).getX(), ((Pair<String, String>) res).getY());
            }
            else
            {
                throw new ConfigurationException(String
                    .format("Child element to %s must be a TaskProcessor or an Attribute Pair, element was %s",
                            element, childElem),
                                                 "Please consult the documentation concerning the use of the Processing Pattern namespace");
            }
        }

        TaskProcessorDefinition def = new DefaultTaskProcessorDefinition(StringBasedIdentifier.newInstance(identifier),
                                                                         displayname,
                                                                         tpType,
                                                                         tp,
                                                                         attributeMap);

        return def;
    }


    /**
     * Parse the default-taskprocessor element.
     *
     * @param context the {@link ConfigurationContext} to use
     * @param element the {@link XmlElement} containing the config element
     *
     * @return a TaskProcessor if there is one
     */
    private Object parseDefaultTaskProcessor(ConfigurationContext context,
                                             XmlElement           element)
    {
        String        id            = element.getAttribute("id").getValue().toString();
        Integer       noThreads     = Integer.parseInt(element.getAttribute("threadpoolsize").getValue().toString());
        TaskProcessor taskprocessor = new DefaultTaskProcessor(id, noThreads);

        return taskprocessor;
    }


    /**
     * Parse the dispatchers element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param xmlElement         the {@link XmlElement} containing the config element
     *
     * @throws ConfigurationException if there is a parsing error
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void parseProcessingPatternDispatchers(ConfigurationContext context,
                                                   XmlElement           xmlElement) throws ConfigurationException
    {
        List<XmlElement> elementList = xmlElement.getElementList();
        int              i           = 1;

        if (elementList != null)
        {
            Iterator iter = elementList.iterator();

            while (iter.hasNext())
            {
                Object     dispatcher = null;
                XmlElement element    = (XmlElement) iter.next();

                dispatcher = context.processElement(element);

                // ensure that ParameterizedBuilder are realized
                if (dispatcher instanceof ParameterizedBuilder<?>)
                {
                    dispatcher = ((ParameterizedBuilder<?>) dispatcher).realize(EmptyParameterProvider.INSTANCE);
                }

                if (dispatcher instanceof Dispatcher)
                {
                    DispatcherManager mgr = context.getEnvironment().getResource(DispatcherManager.class);

                    mgr.registerDispatcher(i, (Dispatcher) dispatcher);
                }
                else
                {
                    throw new ConfigurationException(String.format("Element %s doesn't implement Dispatcher.", element),
                                                     "Please consult the documentation concerning the use of the Processing Pattern namespace");
                }

                i++;
            }
        }
    }


    /**
     * Parse the task-dispatcher element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param element            the {@link XmlElement} containing the config element
     *
     * @return the created task dispatcher
     *
     * @throws ConfigurationException if the configuration is invalid
     */
    @SuppressWarnings("unchecked")
    private Object parseProcessingPatternTaskDispatcher(ConfigurationContext context,
                                                        XmlElement           element) throws ConfigurationException
    {
        String             displayname = element.getAttribute("displayname").getValue().toString();

        TaskDispatchPolicy tdp         = null;
        List<XmlElement>   elementList = element.getElementList();

        if (elementList.size() == 0)
        {
            tdp = new RoundRobinTaskDispatchPolicy();
        }
        else
        {
            if (elementList.size() > 1)
            {
                throw new ConfigurationException(String
                    .format("Only one TaskDispatchPolicy child element allowed for a TaskDispatcher  in %s", element),
                                                 "Please consult the documentation concerning the use of the Processing Pattern namespace");
            }    // Now we have one element in the list

            XmlElement childElem = elementList.get(0);
            Object     res       = context.processElement(childElem);

            // ensure that ParameterizedBuilder are realized
            if (res instanceof ParameterizedBuilder<?>)
            {
                res = ((ParameterizedBuilder<?>) res).realize(EmptyParameterProvider.INSTANCE);
            }

            if (res instanceof TaskDispatchPolicy)
            {
                tdp = (TaskDispatchPolicy) res;
            }
            else
            {
                throw new ConfigurationException(String
                    .format("Child element to %s must be a TaskProcessor, element was %s", element, childElem),
                                                 "Please consult the documentation concerning the use of the Processing Pattern namespace");
            }
        }

        Dispatcher disp = new DefaultTaskDispatcher(context.getEnvironment(), displayname, (TaskDispatchPolicy) tdp);

        return disp;
    }


    /**
     * Parse the composite-policy element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param element            the {@link XmlElement} containing the config element
     *
     * @return the created task dispatcher
     *
     * @throws ConfigurationException if the configuration is invalid
     */
    @SuppressWarnings("unchecked")
    private Object parseCompositeTaskDispatchPolicy(ConfigurationContext context,
                                                    XmlElement           element) throws ConfigurationException
    {
        List<XmlElement> elementList = element.getElementList();

        if (elementList.size() != 2)
        {
            throw new ConfigurationException(String
                .format("The composite Task dispatch policy %s requires two TaskDispatchPolicy child elements to compose.",
                        element),
                                             "Please consult the documentation concerning the use of the Processing Pattern namespace");
        }

        XmlElement childElem1   = elementList.get(0);
        XmlElement childElem2   = elementList.get(1);
        Object     firstPolicy  = context.processElement(childElem1);
        Object     secondPolicy = context.processElement(childElem2);

        // ensure that ClassSchemes are realized
        if (firstPolicy instanceof ParameterizedBuilder<?>)
        {
            firstPolicy = ((ParameterizedBuilder<?>) firstPolicy).realize(EmptyParameterProvider.INSTANCE);
        }

        if (secondPolicy instanceof ParameterizedBuilder<?>)
        {
            secondPolicy = ((ParameterizedBuilder<?>) secondPolicy).realize(EmptyParameterProvider.INSTANCE);
        }

        if (!(firstPolicy instanceof TaskDispatchPolicy))
        {
            throw Base
                .ensureRuntimeException(new ParseException(String
                    .format("Object %s as returned by %s is not a TaskDispatchPolicy", firstPolicy, childElem1),
                                                           0));
        }

        if (!(secondPolicy instanceof TaskDispatchPolicy))
        {
            throw Base
                .ensureRuntimeException(new ParseException(String
                    .format("Object %s as returned by %s is not a TaskDispatchPolicy", secondPolicy, childElem2),
                                                           0));
        }

        return new CompositeTaskDispatchPolicy((TaskDispatchPolicy) firstPolicy, (TaskDispatchPolicy) secondPolicy);
    }


    /**
     * Parse the logging-dispatcher element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param element            the {@link XmlElement} containing the config element
     *
     * @return the created logging dispatcher
     *
     * @throws ConfigurationException if the configuration is invalid
     */
    private Object parseProcessingPatternLoggingDispatcher(ConfigurationContext context,
                                                           XmlElement           element) throws ConfigurationException
    {
        String            displayname = element.getAttribute("displayname").getValue().toString();
        LoggingDispatcher disp        = new LoggingDispatcher(context.getEnvironment(), displayname);

        return disp;
    }


    /**
     * Parse the executor-dispatcher element.
     *
     * @param context            the {@link ConfigurationContext} to use
     * @param element            the {@link XmlElement} containing the config element
     *
     * @return the created local executor dispatcher
     *
     * @throws ConfigurationException if the configuration is invalid
     */
    private Object parseProcessingPatternLocalExecutorDispatcher(ConfigurationContext context,
                                                                 XmlElement           element) throws ConfigurationException
    {
        String   displayname     = element.getAttribute("displayname").getValue().toString();
        int      threadPoolSize  = 1;    // Default no threads is 1
        XmlValue threadAttribute = element.getAttribute("threadpoolsize");

        if (threadAttribute != null)
        {
            try
            {
                threadPoolSize = Integer.parseInt(threadAttribute.getString());
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("Failed to format:" + threadAttribute.getString() + " as an integer.", e);
            }
        }

        LocalExecutorDispatcher disp = new LocalExecutorDispatcher(context.getEnvironment(),
                                                                   displayname,
                                                                   threadPoolSize);

        return disp;

    }
}
