/*
 * File: InstanceNamespaceContentHandler.java
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

import com.oracle.coherence.common.builders.Builder;
import com.oracle.coherence.common.builders.VarArgsParameterizedBuilder;
import com.oracle.coherence.common.util.ReflectionHelper;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import java.util.Iterator;
import java.util.List;

/**
 * The {@link InstanceNamespaceContentHandler} processes an &lt;instance:class&gt {@link XmlElement}
 * to produce a {@link VarArgsParameterizedBuilder}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
@Deprecated
public class InstanceNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard Constructor.
     */
    public InstanceNamespaceContentHandler()
    {
        registerContentHandler("class", new ElementContentHandler()
        {
            @SuppressWarnings({"unchecked"})
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement) throws ConfigurationException
            {
                // attempt to create a Scheme based on the information in the xmlElement

                // find the classname for the instance to create
                if (xmlElement.getAttributeMap().containsKey("classname"))
                {
                    XmlValue     value     = xmlElement.getAttribute("classname");
                    final String className = value.getString();

                    // create a list of parameters for the constructor of the instance
                    List<XmlElement>     elementList              = xmlElement.getElementList();
                    final Object[]       constructorParameterList = new Object[elementList.size()];
                    Iterator<XmlElement> iter                     = elementList.iterator();
                    int                  i                        = 0;

                    while (iter.hasNext())
                    {
                        XmlElement childElement = iter.next();

                        constructorParameterList[i] = context.processElement(childElement);
                        i++;
                    }

                    try
                    {
                        // determine if the class already is a scheme
                        Class<?> clazz = Class.forName(className, false, context.getClassLoader());

                        if (Builder.class.isAssignableFrom(clazz))
                        {
                            // already a scheme, so just create an instance and return it
                            return ReflectionHelper.createObject(className,
                                                                 constructorParameterList,
                                                                 context.getClassLoader());
                        }
                        else
                        {
                            // make the specified class and parameters into a class scheme
                            return new VarArgsParameterizedBuilder(className, constructorParameterList);
                        }
                    }
                    catch (Exception exception)
                    {
                        throw new ConfigurationException(String
                            .format("Failed to instantiate the class '%s' specified by element '%s' with the classloader '%s'",
                                    className, xmlElement, context.getClassLoader()),
                                                         "Please ensure the said class is available",
                                                         exception);
                    }
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The InstanceNamespaceHandler expected a 'classname' attribute in the element [%s].",
                                xmlElement),
                                                     "Please consult the documentation for correct use of the Instance namespace.");
                }
            }
        });
    }
}
