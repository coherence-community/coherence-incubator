/*
 * File: CoherenceNamespaceContentHandler.java
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
import com.oracle.coherence.common.builders.ClassSchemeBasedParameterizedBuilder;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.builders.StaticFactoryClassSchemeBasedParameterizedBuilder;
import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.configuration.expressions.Constant;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.SimpleParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.Base;
import com.tangosol.util.UUID;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link CoherenceNamespaceContentHandler} is responsible for capturing and creating Coherence
 * Cache Configurations when processing a Coherence Configuration file.
 * <p>
 * NOTE 1: This is the default namespace handler for Coherence Cache Configuration files.
 * <p>
 * NOTE 2: This implementation has been refactored from the AdvancedConfigurableCacheFactory from
 * version 1.5.x of coherence-common.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(CoherenceNamespaceContentHandler.class.getName());

    /**
     * The currently collected set of cache scheme mappings as {@link XmlElement}s.
     */
    private LinkedHashMap<String, XmlElement> cacheSchemeMappings;

    /**
     * The currently collected set of cache scheme definitions as {@link XmlElement}s.
     */
    private LinkedHashMap<String, XmlElement> cachingSchemes;

    /**
     * The currently collected set of "defaults" declared using the Coherence namespace.  The key is the name
     * of the element.
     */
    private LinkedHashMap<String, XmlElement> defaults;


    /**
     * Standard Constructor.
     */
    public CoherenceNamespaceContentHandler()
    {
        this.cacheSchemeMappings = new LinkedHashMap<String, XmlElement>();
        this.cachingSchemes      = new LinkedHashMap<String, XmlElement>();
        this.defaults            = new LinkedHashMap<String, XmlElement>();

        registerContentHandler("class-scheme", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                ParameterizedBuilder<?> builder;

                // when there's a single element in a <class-scheme> and it is different from this (coherence)
                // namespace, then assume we're using a custom-namespace that will return a ClassScheme
                // NOTE: this permits use of things like <guice:bean>, <spring:bean> etc in Coherence.
                if (xmlElement.getElementList().size() == 1
                    &&!new QualifiedName((XmlElement) xmlElement.getElementList().get(0)).getPrefix()
                        .equals(getPrefix()))
                {
                    // process the custom-namespace element (it should return a ClassScheme)
                    Object processed = context.processOnlyElementOf(xmlElement);

                    // ensure we have a ClassScheme
                    if (processed instanceof ParameterizedBuilder<?>)
                    {
                        builder = (ParameterizedBuilder<?>) processed;
                    }
                    else
                    {
                        throw new ConfigurationException(String
                            .format("The element %s can't be used within a <class-scheme> as the associated namespace handler doesnt return a ClassScheme for the element.",
                                    xmlElement.getElementList().get(0)),
                                                         "Please ensure that the namespace handler for the element constructs a ClassScheme for the element.");
                    }
                }
                else
                {
                    // determine if we need to use a static factory or regular class-scheme
                    if (xmlElement.getElement(new QualifiedName(qualifiedName.getPrefix(),
                                                                "class-factory-name").getName()) == null)
                    {
                        // configure a regular a ClassScheme using the regular Coherence <class-scheme> schema
                        builder = new ClassSchemeBasedParameterizedBuilder();
                        context.configure(builder, qualifiedName, xmlElement);
                    }
                    else
                    {
                        // configure a static factory a ClassScheme using the regular Coherence <class-scheme> schema
                        builder = new StaticFactoryClassSchemeBasedParameterizedBuilder();
                        context.configure(builder, qualifiedName, xmlElement);
                    }
                }

                // create a unique id for the ClassScheme
                String id = new UUID().toString();

                // modify the <class-scheme> element so that it's identified as something "custom"
                xmlElement.addAttribute("use-scheme").setString(id);

                // register the class scheme with the associated id
                // (this is so we can look it up later when the DefaultConfigurableCacheFactory calls instantiateAny)
                context.getEnvironment().getResource(BuilderRegistry.class).registerBuilder(id, builder);

                return builder;
            }
        });

        registerContentHandler("cache-mapping", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // FUTURE: when we're only using CacheMappings throughout the ExtensibleEnvironment, we can remove the
                // following line.
                // remember the cache-mapping for the named cache so we can pass it to Coherence proper (DCCF)
                cacheSchemeMappings.put(xmlElement.getElement("cache-name").getString(""), xmlElement);

                // create and return the CacheMapping
                String cacheName  = context.getMandatoryProperty("cache-name", String.class, qualifiedName, xmlElement);

                String schemeName = context.getMandatoryProperty("scheme-name",
                                                                 String.class,
                                                                 qualifiedName,
                                                                 xmlElement);

                ParameterProvider parameterProvider = context.getOptionalProperty("init-params",
                                                                                  ParameterProvider.class,
                                                                                  SystemPropertyParameterProvider
                                                                                      .INSTANCE,
                                                                                  qualifiedName,
                                                                                  xmlElement);

                CacheMapping cacheMapping = new CacheMapping(cacheName, schemeName, parameterProvider);

                // register the CacheMapping with the Environment
                context.getEnvironment().getResource(CacheMappingRegistry.class).addCacheMapping(cacheMapping);

                // process all of the child elements (this allows them to modify the dom if required)
                context.processElementsOf(xmlElement);

                return cacheMapping;
            }
        });

        registerContentHandler("caching-schemes", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // process the child elements (this allows them to modify the dom if required)
                context.processElementsOf(xmlElement);

                // now remember the schemes defined in the caching-schemes element so we
                // can provide them to Coherence for configuration
                for (XmlElement xmlSchemeElement : (List<XmlElement>) xmlElement.getElementList())
                {
                    String     schemeName;
                    XmlElement schemeNameElement = xmlSchemeElement.getElement("scheme-name");

                    if (schemeNameElement == null)
                    {
                        schemeName = new UUID().toString();
                    }
                    else
                    {
                        schemeName = schemeNameElement.getString();
                    }

                    cachingSchemes.put(schemeName, xmlSchemeElement);
                }

                // this element handler doesn't produce a result
                return null;
            }
        });

        registerContentHandler("defaults", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            @Override
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // process the children of the defaults element adding their xml to the defaults maps so we
                // can later provide this to the DCCF
                for (Iterator<XmlElement> iter = (Iterator<XmlElement>) xmlElement.getElementList().iterator();
                    iter.hasNext(); )
                {
                    XmlElement childElement = iter.next();

                    // warn if we've seen this default declaration previously
                    if (defaults.containsKey(childElement.getName()))
                    {
                        logger.log(Level.WARNING,
                                   "WARNING: Overriding existing <defaults> definition for {0} with {1}",
                                   new Object[] {childElement.getName(), childElement.toString()});
                    }

                    // have the context process the element
                    context.processElement(childElement);

                    // remember/override the previous defaults declaration
                    defaults.put(childElement.getName(), childElement);
                }

                return null;
            }
        });

        registerContentHandler("init-param", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // when there is no param-name defined, use a newly generated UUID
                final String name = context.getOptionalProperty("param-name",
                                                                String.class,
                                                                new UUID().toString(),
                                                                qualifiedName,
                                                                xmlElement);

                final String className = context.processElementOf(xmlElement,
                                                                  new QualifiedName(getPrefix(), "param-type"),
                                                                  null);

                // potential pass-through
                final Expression expression = XmlElement.class.getName().equals(className)
                                              ? new Constant(xmlElement.getElement("param-value"))
                                              : context.getMandatoryProperty("param-value",
                                                                             Expression.class,
                                                                             qualifiedName,
                                                                             xmlElement);

                final ClassLoader classLoader = context.getClassLoader();

                // handle the special case when the type is a reference to another cache.
                if (className != null && className.trim().equals("{cache-ref}"))
                {
                    return new Parameter(name, NamedCache.class.getName(), new Expression()
                    {
                        @Override
                        public Value evaluate(ParameterProvider parameterProvider)
                        {
                            return new Value(CacheFactory.getCache(expression.evaluate(parameterProvider).getString(),
                                                                   classLoader));
                        }

                        @Override
                        public boolean isSerializable()
                        {
                            return false;
                        }
                    });
                }
                else
                {
                    return new Parameter(name, className, expression);
                }
            }
        });

        registerContentHandler("init-params", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // construct a scope to hold the specified init-params
                MutableParameterProvider parameterProvider = new SimpleParameterProvider();

                // resolve all of the Parameters
                for (Iterator<XmlElement> children = (Iterator<XmlElement>) xmlElement.getElementList().iterator();
                    children.hasNext(); )
                {
                    XmlElement childElement = children.next();

                    Object     childResult  = context.processElement(childElement);

                    if (childResult != null && childResult instanceof Parameter)
                    {
                        parameterProvider.addParameter((Parameter) childResult);
                    }
                    else
                    {
                        // the childElement is not a valid parameter
                        return new ConfigurationException(String.format("Invalid parameter definition '%s' in '%s'",
                                                                        childElement, xmlElement),
                                                          "Please ensure the parameter is correctly defined");
                    }
                }

                return parameterProvider;
            }
        });

        registerContentHandler("param-type", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                String className = xmlElement.getString();

                // attempt to convert Coherence short-hand class names into the fully qualified class name
                if (className.equalsIgnoreCase("string"))
                {
                    return String.class.getName();
                }
                else if (className.equalsIgnoreCase("boolean"))
                {
                    return Boolean.class.getName();
                }
                else if (className.equalsIgnoreCase("int"))
                {
                    return Integer.class.getName();
                }
                else if (className.equalsIgnoreCase("long"))
                {
                    return Long.class.getName();
                }
                else if (className.equalsIgnoreCase("double"))
                {
                    return Double.class.getName();
                }
                else if (className.equalsIgnoreCase("decimal"))
                {
                    return BigDecimal.class.getName();
                }
                else if (className.equalsIgnoreCase("file"))
                {
                    return File.class.getName();
                }
                else if (className.equalsIgnoreCase("date"))
                {
                    return Date.class.getName();
                }
                else if (className.equalsIgnoreCase("time"))
                {
                    return Time.class.getName();
                }
                else if (className.equalsIgnoreCase("datetime"))
                {
                    return Timestamp.class.getName();
                }
                else if (className.equalsIgnoreCase("xml"))
                {
                    return XmlElement.class.getName();
                }
                else if (className.equalsIgnoreCase("classloader")
                         || className.equalsIgnoreCase("java.lang.ClassLoader"))
                {
                    return ClassLoader.class.getName();
                }
                else
                {
                    return className;
                }
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object onUnknownElement(ConfigurationContext context,
                                   QualifiedName        qualifiedName,
                                   XmlElement           xmlElement) throws ConfigurationException
    {
        // when an element is unknown we just process/ignore it as Coherence is non-strict.
        return context.processElementsOf(xmlElement);
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

        // register the CacheMappings with the Environment
        context.getEnvironment().registerResource(CacheMappingRegistry.class, new CacheMappingRegistry());
    }


    /**
     * Builds a {@link String} representing an xml coherence cache configuration that has been resolved.
     *
     * @param builder the {@link StringBuilder} to use
     */
    public void build(StringBuilder builder)
    {
        // include the xml namespace declarations for the cache-config element
        builder.append("<cache-config>\n");

        if (!defaults.isEmpty())
        {
            builder.append("   <defaults>\n");

            for (String name : defaults.keySet())
            {
                build(builder, defaults.get(name), 2);
            }

            builder.append("   </defaults>\n");
        }

        builder.append("   <caching-scheme-mapping>\n");

        for (String cacheName : cacheSchemeMappings.keySet())
        {
            build(builder, cacheSchemeMappings.get(cacheName), 2);
        }

        builder.append("   </caching-scheme-mapping>\n");

        builder.append("\n");
        builder.append("   <caching-schemes>\n");

        for (String schemeName : cachingSchemes.keySet())
        {
            build(builder, cachingSchemes.get(schemeName), 2);
        }

        builder.append("   </caching-schemes>\n");

        builder.append("</cache-config>\n");
    }


    /**
     * Adds a string representation of the specified {@link XmlElement} to the builder that has been resolved.
     *
     * @param builder    The {@link StringBuilder} to use
     * @param xmlElement The {@link XmlElement} to build a string out of
     * @param indent     The number of tabs to use in the string representation
     */
    @SuppressWarnings("unchecked")
    private void build(StringBuilder builder,
                       XmlElement    xmlElement,
                       int           indent)
    {
        String padding = Base.dup(' ', indent * 4);

        builder.append(padding);

        builder.append("<");
        builder.append(xmlElement.getName());

        for (String attributeName : ((Map<String, XmlValue>) xmlElement.getAttributeMap()).keySet())
        {
            builder.append(" ");
            builder.append(attributeName);
            builder.append("=\"");
            builder.append(xmlElement.getAttribute(attributeName).toString());
            builder.append("\"");
        }

        if (xmlElement.getString("").trim().length() > 0)
        {
            builder.append(">");
            builder.append(xmlElement.getString("").trim());
            builder.append("</");
            builder.append(xmlElement.getName());
            builder.append(">\n");

        }
        else if (xmlElement.getElementList().size() == 0 && xmlElement.getString("").trim().length() == 0)
        {
            builder.append("/>\n");

        }
        else
        {
            builder.append(">\n");

            for (XmlElement xml : (List<XmlElement>) xmlElement.getElementList())
            {
                build(builder, xml, indent + 1);
            }

            if (xmlElement.getString("").length() == 0)
            {
                builder.append(padding);
            }

            builder.append("</");
            builder.append(xmlElement.getName());
            builder.append(">\n");
        }
    }


    /**
     * Determines if the specified cacheName is defined in the cache scheme mappings.
     *
     * NOTE: Does not support checking for wildcard mappings.  Just regular string matching (equals) is used.
     *
     * @param cacheName The name of the cache for with to search.
     *
     * @return <code>true</code> if the cache name exists in the cache scheme mappings, <code>false</code> otherwise.
     */
    public boolean isCacheNameDefined(String cacheName)
    {
        return cacheSchemeMappings.containsKey(cacheName);
    }


    /**
     * Determines if a scheme with the specified schemeName is defined.
     *
     * NOTE: Does not support checking for wildcard mappings.  Just regular string matching (equals) is used.
     *
     * @param schemeName The name of the scheme for with to search.
     *
     * @return <code>true</code> if the scheme name exists in the defined schemes, <code>false</code> otherwise.
     */
    public boolean isSchemeNameDefined(String schemeName)
    {
        return cachingSchemes.containsKey(schemeName);
    }
}
