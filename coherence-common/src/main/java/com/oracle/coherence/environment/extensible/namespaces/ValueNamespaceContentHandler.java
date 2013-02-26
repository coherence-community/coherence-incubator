/*
 * File: ValueNamespaceContentHandler.java
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

import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ValueNamespaceContentHandler} returns type-safe representations of simple values, including; string,
 * integer, long, boolean, double, float, short, character and byte.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
@Deprecated
public class ValueNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * Standard Constructor.
     */
    public ValueNamespaceContentHandler()
    {
        registerContentHandler("string", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return xmlElement.getValue() != null ? xmlElement.getValue().toString() : null;
            }
        });

        registerContentHandler("integer", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Integer.parseInt(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });

        registerContentHandler("long", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Long.parseLong(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });

        registerContentHandler("boolean", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Boolean.parseBoolean(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });

        registerContentHandler("double", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Double.parseDouble(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });

        registerContentHandler("float", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Float.parseFloat(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });

        registerContentHandler("short", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Short.parseShort(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });

        registerContentHandler("character", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return new Character(xmlElement.getValue() != null ? xmlElement.getValue().toString().charAt(0) : null);
            }
        });

        registerContentHandler("byte", new ElementContentHandler()
        {
            public Object onElement(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlElement           xmlElement)
            {
                return Byte.parseByte(xmlElement.getValue() != null ? xmlElement.getValue().toString() : null);
            }
        });
    }
}
