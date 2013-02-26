/*
 * File: EventDistributionNamespaceContentHandlerTest.java
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

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.DefaultConfigurationContext;
import com.oracle.coherence.environment.extensible.namespaces.CoherenceNamespaceContentHandler;
import com.oracle.coherence.environment.extensible.namespaces.EnvironmentNamespaceContentHandler;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A {@link EventDistributionNamespaceContentHandlerTest} provides unit tests for the
 * {@link EventDistributionNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventDistributionNamespaceContentHandlerTest extends AbstractTest
{
    /**
     * Ensure we can establish a StdErrEventChannel
     *
     * @throws ConfigurationException
     * @throws URISyntaxException
     */
    @Test
    public void testStdErrEventChannelDeclaration() throws URISyntaxException, ConfigurationException
    {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        Environment     environment     = mock(Environment.class);

        when(environment.getResource(EventDispatcher.class)).thenReturn(eventDispatcher);

        ConfigurationContext context = new DefaultConfigurationContext(environment);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("env",
                                              new URI(String
                                                  .format("class:%s",
                                                          EnvironmentNamespaceContentHandler.class.getName())));
        context.ensureNamespaceContentHandler("event",
                                              new URI(String
                                                  .format("class:%s",
                                                          EventDistributionNamespaceContentHandler.class.getName())));

        String xml    = "<event:stderr-channel-scheme/>";

        Object result = context.processElement(XmlHelper.loadXml(xml));

        assertTrue(result instanceof EventChannelBuilder);
    }
}
