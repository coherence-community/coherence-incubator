/*
 * File: EnvironmentNamespaceContentHandler.java
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
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;

/**
 * The {@link EnvironmentNamespaceContentHandler} allows for the following configuration options.
 * <ul>
 *     <li>
 *         <strong>environment:instance</strong> - returns a reference to the {@link Environment}.
 *     </li>
 *     <li>
 *         <strong>environment:resource</strong> - registers the object produced by interpreting a child element with the
 *         supplied class/interface attribute name in the {@link Environment}.
 *     </li>
 *     <li>
 *          <strong>environment:ref</strong> - provides a reference to an object of a specified interface with in an {@link Environment}.
 *     </li>
 * </ul>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
public class EnvironmentNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard Constructor.
     */
    public EnvironmentNamespaceContentHandler()
    {
        registerContentHandler("instance", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                return context.getEnvironment();
            }
        });

        registerContentHandler("resource", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                String     name         = xmlElement.getAttribute("id").getString();
                XmlElement childElement = (XmlElement) xmlElement.getElementList().get(0);

                if (childElement == null)
                {
                    throw new ConfigurationException(String
                        .format("Expected child element of [%s] to construct a ClassScheme resource for the environment.",
                                xmlElement),
                                                     "Please consult the documentation for the Environment namespace");

                }
                else
                {
                    Object objectToRegister = context.processElement(childElement);

                    try
                    {
                        // we need to realize ClassSchemes immediately so that we can register the resource
                        if (objectToRegister instanceof ParameterizedBuilder<?>)
                        {
                            objectToRegister =
                                ((ParameterizedBuilder<?>) objectToRegister)
                                    .realize(SystemPropertyParameterProvider.INSTANCE);
                        }

                        context.getEnvironment().registerResource((Class<Object>) Class.forName(name),
                                                                  objectToRegister);
                    }
                    catch (ClassNotFoundException classNotFoundException)
                    {
                        throw new ConfigurationException(String
                            .format("The class/interface name [%s] specified in the <%s:resource> element is unknown.",
                                    name, getPrefix()),
                                                         "Please ensure the class is on the classpath and/or the classname is correct",
                                                         classNotFoundException);
                    }

                    return objectToRegister;
                }
            }
        });

        registerContentHandler("ref", new ElementContentHandler()
        {
            @SuppressWarnings("unchecked")
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                String name = xmlElement.getAttribute("id").getString();

                try
                {
                    return context.getEnvironment().getResource((Class<Object>) Class.forName(name));
                }
                catch (ClassNotFoundException classNotFoundException)
                {
                    throw new ConfigurationException(String
                        .format("The class/interface name [%s] specified in the <%s:ref> element is unknown", name,
                                getPrefix()),
                                                     "Please ensure the class is on the classpath and/or the classname is correct",
                                                     classNotFoundException);
                }
            }
        });
    }
}
