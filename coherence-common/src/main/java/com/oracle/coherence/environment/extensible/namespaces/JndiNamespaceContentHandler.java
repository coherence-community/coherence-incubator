/*
 * File: JndiNamespaceContentHandler.java
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

import com.oracle.coherence.common.builders.JndiBasedParameterizedBuilder;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;

/**
 * The {@link JndiNamespaceContentHandler} provides the ability to work with JNDI and produce {@link ParameterizedBuilder}s
 * as required for integration with Coherence.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class JndiNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard Constructor.
     */
    public JndiNamespaceContentHandler()
    {
        registerContentHandler("resource", new ElementContentHandler()
        {
            @Override
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                JndiBasedParameterizedBuilder builder = new JndiBasedParameterizedBuilder();

                context.configure(builder, qualifiedName, xmlElement);

                return builder;
            }
        });
    }
}
