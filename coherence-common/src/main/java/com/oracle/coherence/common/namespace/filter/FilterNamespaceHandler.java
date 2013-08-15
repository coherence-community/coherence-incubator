/*
 * File: FilterNamespaceHandler.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.common.namespace.filter;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderHelper;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

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
 * The {@link FilterNamespaceHandler} provides the ability to declare
 * (and construct) Coherence {@link Filter} instances in a cache configuration file.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class FilterNamespaceHandler extends AbstractNamespaceHandler
{
    /**
     * Construct a {@link FilterNamespaceHandler}.
     */
    public FilterNamespaceHandler()
    {
        registerProcessor("custom", new ElementProcessor<Filter>()
        {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Filter process(ProcessingContext context,
                                  XmlElement        xmlElement) throws ConfigurationException
            {
                Object oResult = context.processOnlyElementOf(xmlElement);

                if (oResult instanceof ParameterizedBuilder
                    && ParameterizedBuilderHelper.realizes((ParameterizedBuilder) oResult, Filter.class,
                                                           context.getDefaultParameterResolver(),
                                                           context.getContextClassLoader()))
                {
                    return ((ParameterizedBuilder<Filter>) oResult).realize(context.getDefaultParameterResolver(),
                                                                            context.getContextClassLoader(),
                                                                            null);
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

        registerProcessor("where", new WhereFilterElementProcessor());
        registerProcessor("not", new NotFilterElementProcessor());

        registerProcessor("always", new SimpleFilterElementProcessor(AlwaysFilter.INSTANCE));
        registerProcessor("never", new SimpleFilterElementProcessor(NeverFilter.INSTANCE));
        registerProcessor("present", new SimpleFilterElementProcessor(PresentFilter.INSTANCE));
        registerProcessor("all", new ArrayFilterElementProcessor<AllFilter>(AllFilter.class));
        registerProcessor("any", new ArrayFilterElementProcessor<AnyFilter>(AnyFilter.class));
        registerProcessor("and", new LogicFilterElementProcessor<AndFilter>(AndFilter.class));
        registerProcessor("or", new LogicFilterElementProcessor<OrFilter>(OrFilter.class));
        registerProcessor("xor", new LogicFilterElementProcessor<XorFilter>(XorFilter.class));
    }


    /**
     * An {@link ArrayFilterElementProcessor} is a {@link ElementProcessor}
     * for composite {@link Filter}s that contain multiple {@link Filter}s.
     */
    static class ArrayFilterElementProcessor<T extends ArrayFilter> implements ElementProcessor<Filter>
    {
        /**
         * The type of {@link Filter} to create.
         */
        private Class<T> m_clzFilter;


        /**
         * Construct an {@link ArrayFilterElementProcessor}.
         *
         * @param clzFilter the class of {@link Filter} create
         */
        ArrayFilterElementProcessor(Class<T> clzFilter)
        {
            m_clzFilter = clzFilter;
        }


        /**
         * {@inheritDoc}
         */
        public Filter process(ProcessingContext context,
                              XmlElement        xmlElement) throws ConfigurationException
        {
            // process all of the contained filters
            Map<String, ?> mapFilters = context.processElementsOf(xmlElement);
            Filter[]       filters    = new Filter[mapFilters.size()];
            int            i          = 0;

            for (Object oFilter : mapFilters.values())
            {
                if (oFilter instanceof Filter)
                {
                    filters[i++] = (Filter) oFilter;
                }
                else
                {
                    throw new ConfigurationException("Can't create a filter with the provided filters in " + xmlElement
                                                     + " as one of the specified elements does not produce a filter",
                                                     "Please ensure that all of the child elements are filters");
                }
            }

            // construct the desired filter
            try
            {
                return m_clzFilter.getConstructor(Filter[].class).newInstance((Object) filters);
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
     * A {@link LogicFilterElementProcessor} is a {@link ElementProcessor}
     * for composite {@link Filter}s that require two {@link Filter}s.
     */
    static class LogicFilterElementProcessor<T extends ArrayFilter> implements ElementProcessor<Filter>
    {
        /**
         * The type of {@link Filter} to create.
         */
        private Class<T> m_clzFilter;


        /**
         * Construct an {@link ArrayFilterElementContentHandler}.
         *
         * @param clzFilter the class of {@link Filter} create
         */
        public LogicFilterElementProcessor(Class<T> clzFilter)
        {
            m_clzFilter = clzFilter;
        }


        /**
         * {@inheritDoc}
         */
        public Filter process(ProcessingContext context,
                              XmlElement        xmlElement) throws ConfigurationException
        {
            // process all of the contained filters
            Map<String, ?> mapFilters = context.processElementsOf(xmlElement);

            if (mapFilters.size() == 2)
            {
                Filter[] filters = new Filter[mapFilters.size()];
                int      i       = 0;

                for (Object oFilter : mapFilters.values())
                {
                    if (oFilter instanceof Filter)
                    {
                        filters[i++] = (Filter) oFilter;
                    }
                    else
                    {
                        throw new ConfigurationException("Can't create a filter with the provided filters in "
                            + xmlElement + " as one of the specified elements does not produce a filter",
                                                         "Please ensure that all of the child elements are filters");
                    }
                }

                // construct the desired filter
                try
                {
                    return m_clzFilter.getConstructor(Filter.class, Filter.class).newInstance(filters[0], filters[1]);
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
     * A {@link NotFilterElementProcessor} builds a {@link NotFilter}.
     */
    static class NotFilterElementProcessor implements ElementProcessor<Filter>
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Filter process(ProcessingContext context,
                              XmlElement        xmlElement) throws ConfigurationException
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
     * A {@link SimpleFilterElementProcessor} is a simple {@link ElementProcessor}
     * for specific types of {@link Filter}s that have no content/arguments
     */
    static class SimpleFilterElementProcessor implements ElementProcessor<Filter>
    {
        /**
         * The {@link Filter} instance.
         */
        private Filter m_filter;


        /**
         * Constructs an {@link SimpleFilterElementProcessor}.
         *
         * @param filter the {@link Filter} to be used by the {@link SimpleFilterElementProcessor}
         */
        public SimpleFilterElementProcessor(Filter filter)
        {
            m_filter = filter;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Filter process(ProcessingContext context,
                              XmlElement        xmlElement) throws ConfigurationException
        {
            return m_filter;
        }
    }


    /**
     * A {@link WhereFilterElementProcessor} builds a {@link Filter}
     * based on a a CohQL where clause.
     */
    static class WhereFilterElementProcessor implements ElementProcessor<Filter>
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Filter process(ProcessingContext context,
                              XmlElement        xmlElement) throws ConfigurationException
        {
            // get the expression from which we'll attempt to create a Filter
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
