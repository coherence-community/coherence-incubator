/*
 * File: JndiNamespaceHandlerTest.java
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

package com.oracle.coherence.common.namespace.jndi;

import com.oracle.tools.junit.AbstractTest;
import com.sun.jndi.fscontext.FSContext;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderHelper;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.URISyntaxException;

/**
 * The {@link JndiNamespaceHandlerTest} contains unit tests for the {@link JndiNamespaceHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("restriction")
public class JndiNamespaceHandlerTest extends AbstractTest
{
    /**
     * A simple test to ensure JNDI lookups are possible.
     *
     * @throws NamingException
     */
    @Test
    public void testSimpleJNDILookup() throws NamingException
    {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");

        Context ctx          = new InitialContext();

        String  javaHomePath = System.getProperty("java.home");

        Assert.assertNotNull(ctx.lookup(javaHomePath));
    }


    /**
     * A test to use the jndi:resource to lookup a resource via JNDI.
     *
     * @throws NamingException
     * @throws ConfigurationException
     * @throws URISyntaxException
     */
    @Test
    public void testJNDINamespaceResource() throws NamingException, ConfigurationException, URISyntaxException
    {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");

        DefaultDependencies dependencies = new DefaultDependencies();

        dependencies.setExpressionParser(new ParameterMacroExpressionParser());

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies);

        context.ensureNamespaceHandler("jndi", new JndiNamespaceHandler());

        String javaHomePath     = System.getProperty("java.home");
        String xml = "<jndi:resource><jndi:resource-name>" + javaHomePath + "</jndi:resource-name></jndi:resource>";

        Object processedElement = context.processElement(xml);

        Assert.assertNotNull(processedElement);
        Assert.assertTrue(processedElement instanceof JndiBasedParameterizedBuilder);

        ParameterizedBuilder<?> bldr = (ParameterizedBuilder<?>) processedElement;

        Assert.assertTrue(ParameterizedBuilderHelper.realizes(bldr, FSContext.class, null, null));
    }
}
