/*
 * File: FilterNamespaceContentHandler.java
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
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Filter;
import com.tangosol.util.QueryHelper;
import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.ArrayFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.filter.XorFilter;

import java.util.Map;

/**
 * The {@link FilterNamespaceContentHandler} provides the ability to declare (and construct) Coherence {@link Filter}
 * instances in a cache configuration file.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class FilterNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Construct a {@link FilterNamespaceContentHandler}.
     */
    public FilterNamespaceContentHandler()
    {
        registerContentHandler("custom", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                Object result = context.processOnlyElementOf(xmlElement);

                if (result != null && result instanceof ParameterizedBuilder<?>
                    && ((ParameterizedBuilder<?>) result).realizesClassOf(Filter.class,
                                                                          SystemPropertyParameterProvider.INSTANCE))
                {
                    return ((ParameterizedBuilder<Filter>) result).realize(SystemPropertyParameterProvider.INSTANCE);
                }
                else
                {
                    // the custom filter did not produce a Filter
                    throw new ConfigurationException(String
                        .format("The filter specified in %s does not implement Filter", xmlElement),
                                                     "Please consult the documentation regarding use of the Filter namespace");
                }
            }
        });

        registerContentHandler("where", new WhereFilterElementContentHandler());
        registerContentHandler("not", new NotFilterElementContentHandler());

        registerContentHandler("always", new EmptyFilterElementContentHandler(AlwaysFilter.INSTANCE));
        registerContentHandler("never", new EmptyFilterElementContentHandler(NeverFilter.INSTANCE));
        registerContentHandler("present", new EmptyFilterElementContentHandler(PresentFilter.INSTANCE));
        registerContentHandler("all", new ArrayFilterElementContentHandler<AllFilter>(AllFilter.class));
        registerContentHandler("any", new ArrayFilterElementContentHandler<AnyFilter>(AnyFilter.class));
        registerContentHandler("and", new LogicFilterElementContentHandler<AndFilter>(AndFilter.class));
        registerContentHandler("or", new LogicFilterElementContentHandler<OrFilter>(OrFilter.class));
        registerContentHandler("xor", new LogicFilterElementContentHandler<XorFilter>(XorFilter.class));
    }


    /**
     * An {@link ArrayFilterElementContentHandler} is a {@link ElementContentHandler}
     * for composite {@link Filter}s that contain multiple {@link Filter}s.
     */
    static class ArrayFilterElementContentHandler<T extends ArrayFilter> implements ElementContentHandler
    {
        /**
         * The type of {@link Filter} to create.
         */
        private Class<T> filter;


        /**
         * Construct an {@link ArrayFilterElementContentHandler}.
         *
         * @param filter the class of {@link Filter} create
         */
        ArrayFilterElementContentHandler(Class<T> filter)
        {
            this.filter = filter;
        }


        /**
         * {@inheritDoc}
         */
        public Object onElement(ConfigurationContext context,
                                QualifiedName        qualifiedName,
                                XmlElement           xmlElement) throws ConfigurationException
        {
            // process all of the contained filters
            Map<String, Filter> mapFilters = context.processElementsOf(xmlElement, Filter.class);
            Filter[]            filters    = new Filter[mapFilters.size()];
            int                 i          = 0;

            for (Filter filter : mapFilters.values())
            {
                filters[i++] = filter;
            }

            // construct the desired filter
            try
            {
                return filter.getConstructor(Filter[].class).newInstance((Object) filters);
            }
            catch (Exception e)
            {
                throw new ConfigurationException("Can't create a filter with the provided filters in " + xmlElement,
                                                 "Please ensure that all of the child elements are filters",
                                                 e);
            }
        }
    }


    /**
     * A {@link EmptyFilterElementContentHandler} is a simple {@link ElementContentHandler}
     * for specific types of {@link Filter}s that have no content/arguments
     */
    static class EmptyFilterElementContentHandler implements ElementContentHandler
    {
        /**
         * The {@link Filter} instance.
         */
        private Filter filter;


        /**
         * Default constructor.
         *
         * @param filter the {@link Filter} to be used by the {@link EmptyFilterElementContentHandler}.
         */
        EmptyFilterElementContentHandler(Filter filter)
        {
            this.filter = filter;
        }


        /**
         * {@inheritDoc}
         */
        public Object onElement(ConfigurationContext context,
                                QualifiedName        qualifiedName,
                                XmlElement           xmlElement) throws ConfigurationException
        {
            return filter;
        }
    }


    /**
     * A {@link LogicFilterElementContentHandler} is a {@link ElementContentHandler}
     * for composite {@link Filter}s that require two {@link Filter}s.
     */
    static class LogicFilterElementContentHandler<T extends ArrayFilter> implements ElementContentHandler
    {
        /**
         * The type of {@link Filter} to create.
         */
        private Class<T> filter;


        /**
         * Construct an {@link ArrayFilterElementContentHandler}.
         *
         * @param filter the class of {@link Filter} create
         */
        public LogicFilterElementContentHandler(Class<T> filter)
        {
            this.filter = filter;
        }


        /**
         * {@inheritDoc}
         */
        public Object onElement(ConfigurationContext context,
                                QualifiedName        qualifiedName,
                                XmlElement           xmlElement) throws ConfigurationException
        {
            // process all of the contained filters
            Map<String, Filter> mapFilters = context.processElementsOf(xmlElement, Filter.class);

            if (mapFilters.size() == 2)
            {
                Filter[] filters = new Filter[mapFilters.size()];
                int      i       = 0;

                for (Filter filter : mapFilters.values())
                {
                    filters[i++] = filter;
                }

                // construct the desired filter
                try
                {
                    return filter.getConstructor(Filter.class, Filter.class).newInstance(filters[0], filters[1]);
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("Can't create a filter with the provided filters in " + xmlElement,
                                                     "Please ensure that all of the child elements are filters",
                                                     e);
                }
            }
            else
            {
                throw new ConfigurationException("Two filters are required for " + xmlElement,
                                                 "Please ensure that the correct number of filters are defined");
            }
        }
    }


    /**
     * A {@link NotFilterElementContentHandler} builds a {@link NotFilter}.
     */
    static class NotFilterElementContentHandler implements ElementContentHandler
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Object onElement(ConfigurationContext context,
                                QualifiedName        qualifiedName,
                                XmlElement           xmlElement) throws ConfigurationException
        {
            // get the contained filter
            Object oFilter = context.processOnlyElementOf(xmlElement);

            if (oFilter instanceof Filter)
            {
                return new NotFilter((Filter) oFilter);
            }
            else
            {
                throw new ConfigurationException("Expected a Filter definition in: " + xmlElement,
                                                 "Please ensure a Filter is defined");
            }
        }
    }


    /**
     * A {@link WhereFilterElementContentHandler} builds a {@link Filter}
     * based on a a CohQL where clause.
     */
    static class WhereFilterElementContentHandler implements ElementContentHandler
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Object onElement(ConfigurationContext context,
                                QualifiedName        qualifiedName,
                                XmlElement           xmlElement) throws ConfigurationException
        {
            // get the expression for which we'll build a Filter
            String expression = xmlElement.getString("");

            // attempt to build the filter
            try
            {
                return QueryHelper.createFilter(expression);
            }
            catch (RuntimeException e)
            {
                throw new ConfigurationException("Failed to create a Filter from the where clause:" + xmlElement,
                                                 "Please ensure that the where clause is syntactically correct",
                                                 e);
            }
        }
    }
}
