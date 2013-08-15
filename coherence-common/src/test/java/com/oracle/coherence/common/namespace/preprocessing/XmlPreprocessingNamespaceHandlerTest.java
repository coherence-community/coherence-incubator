/*
 * File: XmlPreprocessingNamespaceHandlerTest.java
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

package com.oracle.coherence.common.namespace.preprocessing;

import com.oracle.tools.junit.AbstractTest;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.scheme.DistributedScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for the {@link XmlPreprocessingNamespaceHandler}.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Jonathan Knight
 */
public class XmlPreprocessingNamespaceHandlerTest extends AbstractTest
{
    /**
     * Ensure that we can introduce several cache configs.
     */
    @Test
    public void testIntroduceCacheConfig()
    {
        DefaultDependencies dependencies = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dependencies.setResourceRegistry(new SimpleResourceRegistry());
        dependencies.setExpressionParser(new ParameterMacroExpressionParser());

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies);

        context.ensureNamespaceHandler("", new CacheConfigNamespaceHandler());
        context.ensureNamespaceHandler("element", new XmlPreprocessingNamespaceHandler());

        XmlElement  xmlElement  = XmlHelper.loadFileOrResource("test-introduce-cache-config-base.xml", "cache config");

        CacheConfig cacheConfig = (CacheConfig) context.processDocument(xmlElement);

        Assert.assertNotNull(cacheConfig);

        Assert.assertNotNull(cacheConfig.getCacheMappingRegistry().findCacheMapping("common-*"));
        Assert.assertNotNull(cacheConfig.getCacheMappingRegistry().findCacheMapping("a-*"));
        Assert.assertNotNull(cacheConfig.getCacheMappingRegistry().findCacheMapping("b-*"));

        Assert.assertNotNull(cacheConfig.getServiceSchemeRegistry().findSchemeBySchemeName("distributed-scheme"));
        Assert.assertNotNull(cacheConfig.getServiceSchemeRegistry().findSchemeBySchemeName("a-distributed-scheme"));
    }


    /**
     * Ensure that we can replace elements in a cache config.
     */
    @Test
    public void testReplaceWithFile()
    {
        DefaultDependencies dependencies = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dependencies.setResourceRegistry(new SimpleResourceRegistry());
        dependencies.setExpressionParser(new ParameterMacroExpressionParser());

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies);

        context.ensureNamespaceHandler("", new CacheConfigNamespaceHandler());
        context.ensureNamespaceHandler("element", new XmlPreprocessingNamespaceHandler());

        XmlElement  xmlElement = XmlHelper.loadFileOrResource("test-replace-with-file-cache-config.xml",
                                                              "cache config");

        CacheConfig cacheConfig = (CacheConfig) context.processDocument(xmlElement);

        Assert.assertNotNull(cacheConfig);

        Assert.assertNotNull(cacheConfig.getCacheMappingRegistry().findCacheMapping("common-*"));

        ServiceScheme scheme = cacheConfig.getServiceSchemeRegistry().findSchemeBySchemeName("distributed-scheme");

        Assert.assertNotNull(scheme);
        Assert.assertTrue(scheme instanceof DistributedScheme);
    }


    /**
     * Ensures that we can support customized merging of foreign namespaces.
     */
    @Test
    public void testMergingForeignNamespaces()
    {
        DefaultDependencies dependencies = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dependencies.setResourceRegistry(new SimpleResourceRegistry());
        dependencies.setExpressionParser(new ParameterMacroExpressionParser());

        TestNamespaceHandler     testNamespaceHandler = new TestNamespaceHandler();
        DefaultProcessingContext context              = new DefaultProcessingContext(dependencies);

        context.ensureNamespaceHandler("", new CacheConfigNamespaceHandler());
        context.ensureNamespaceHandler("element", new XmlPreprocessingNamespaceHandler());
        context.ensureNamespaceHandler("test", testNamespaceHandler);

        XmlElement  xmlElement  = XmlHelper.loadFileOrResource("test-introduce-cache-config-base.xml", "cache config");

        CacheConfig cacheConfig = (CacheConfig) context.processDocument(xmlElement);

        Assert.assertNotNull(cacheConfig);

        Set<String> idSet = testNamespaceHandler.getIds();
        Set<String> expectedIdSet = new HashSet<String>(Arrays.asList("test-a-1", "test-a-2", "test-a-3", "test-a-4",
                                                                      "test-b-1", "test-b-2", "test-b-3", "test-b-4"));

        Assert.assertThat(idSet, is(expectedIdSet));
    }
}
