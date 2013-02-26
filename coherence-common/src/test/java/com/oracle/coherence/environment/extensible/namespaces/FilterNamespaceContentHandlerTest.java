/*
 * File: FilterNamespaceContentHandlerTest.java
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

import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.EntryFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.filter.XorFilter;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for the {@link FilterNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class FilterNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Establishes a {@link ConfigurationContext} for the {@link FilterNamespaceContentHandler}
     *
     * @return A {@link ConfigurationContext}
     *
     * @throws ConfigurationException if the {@link ConfigurationContext} can't be established
     */
    private ConfigurationContext getConfigurationContext() throws ConfigurationException
    {
        Environment env = mock(Environment.class);

        when(env.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        ConfigurationContext context = new DefaultConfigurationContext(env);

        try
        {
            context.ensureNamespaceContentHandler("filter",
                                                  new URI(String
                                                      .format("class:%s",
                                                              FilterNamespaceContentHandler.class.getName())));
        }
        catch (URISyntaxException e)
        {
            throw new ConfigurationException("Can't establish the ConfigurationContext", "Fix the exception!", e);
        }

        return context;
    }


    /**
     * Test {@link AlwaysFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testAlwaysFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:always/>");
        Object               result  = context.processElement(xml);

        assertEquals(AlwaysFilter.INSTANCE, result);
    }


    /**
     * Test {@link NeverFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testNeverFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:never/>");
        Object               result  = context.processElement(xml);

        assertEquals(NeverFilter.INSTANCE, result);
        assertFalse(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link PresentFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testPresentFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:present/>");
        Object               result  = context.processElement(xml);

        assertEquals(PresentFilter.INSTANCE, result);
    }


    /**
     * Test {@link AllFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testAllFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:all><filter:always/><filter:always/></filter:all>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof AllFilter);
        assertTrue(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link AnyFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testAnyFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement xml = XmlHelper.loadXml("<filter:any><filter:never/><filter:always/><filter:never/></filter:any>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof AnyFilter);

        assertTrue(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link AndFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testAndFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:and><filter:never/><filter:always/></filter:and>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof AndFilter);
        assertFalse(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link OrFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testOrFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:or><filter:never/><filter:always/></filter:or>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof OrFilter);
        assertTrue(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link XorFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testXorFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:xor><filter:never/><filter:always/></filter:xor>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof XorFilter);
        assertTrue(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link NotFilter} creation.
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testNotFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:not><filter:never/></filter:not>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof NotFilter);
        assertTrue(((Filter) result).evaluate(null));
    }


    /**
     * Test {@link Filter} construction based on a where clause
     *
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testWhereFilter() throws ConfigurationException
    {
        ConfigurationContext context = getConfigurationContext();
        XmlElement           xml     = XmlHelper.loadXml("<filter:where>key() like \"%koala%\"</filter:where>");
        Object               result  = context.processElement(xml);

        assertTrue(result instanceof EntryFilter);

        System.out.println(result);

        EntryFilter filter = (EntryFilter) result;

        Entry       entry  = mock(Entry.class);

        when(entry.getKey()).thenReturn("Angry koala because he's not a bear");

        assertTrue(filter.evaluateEntry(entry));
    }
}
