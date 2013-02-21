/*
 * File: JndiNamespaceContentHandlerTest.java
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
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.tools.junit.AbstractTest;
import com.sun.jndi.dns.DnsContext;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@link JndiNamespaceContentHandlerTest} contains unit tests for the {@link JndiNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class JndiNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * A simple test to ensure JNDI dns lookups are possible.
     *
     * @throws NamingException
     */
    @Test
    public void testSimpleJNDILookup() throws NamingException
    {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        Context ctx = new InitialContext();

        Assert.assertNotNull(ctx.lookup("dns:///www.oracle.com"));
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
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("jndi",
                                              new URI(String.format("class:%s",
                                                                    JndiNamespaceContentHandler.class.getName())));

        String xml = "<jndi:resource><jndi:resource-name>dns:///www.oracle.com</jndi:resource-name></jndi:resource>";

        Object processedElement = context.processElement(xml);

        Assert.assertNotNull(processedElement);
        Assert.assertTrue(processedElement instanceof ParameterizedBuilder<?>);

        ParameterizedBuilder<?> classScheme = (ParameterizedBuilder<?>) processedElement;

        Assert.assertTrue(classScheme.realizesClassOf(DnsContext.class, null));
    }
}
