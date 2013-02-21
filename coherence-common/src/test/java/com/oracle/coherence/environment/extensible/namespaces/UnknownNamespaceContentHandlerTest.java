/*
 * File: UnknownNamespaceContentHandlerTest.java
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
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.coherence.environment.extensible.NamespaceContentHandler;
import com.oracle.tools.junit.AbstractTest;
import junit.framework.Assert;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@link UnknownNamespaceContentHandlerTest} exercises {@link DefaultConfigurationContext} handling of
 * unknown {@link NamespaceContentHandler}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class UnknownNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Ensure that valid http URIs are allowed, especially for xsi definitions.
     *
     * @throws URISyntaxException if there is a problem with the URI
     * @throws ConfigurationException if there is a configuration error
     */
    @Test
    public void testHttpAndXsiScheme() throws URISyntaxException, ConfigurationException
    {
        Environment     env            = mock(Environment.class);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        String xml = "<note xmlns=\"http://www.w3schools.com\""
                     + "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                     + "      xsi:schemaLocation=\"http://www.w3schools.com/note.xsd\">" + "    <to>Skippy</to>"
                     + "    <from>Lassie</from>" + "    <heading>Reminder</heading>" + "    <body>Woof Woof!</body>"
                     + "</note>";

        Object document = context.processDocument(xml);

        Assert.assertNull(document);
    }
}
