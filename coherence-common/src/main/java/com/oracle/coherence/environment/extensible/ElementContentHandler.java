/*
 * File: ElementContentHandler.java
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

package com.oracle.coherence.environment.extensible;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementContentHandler} is responsible for handling XML element content for a
 * defined xml namespace.
 * <p>
 * NOTE: Implementations of this interface typically also implement {@link NamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see NamespaceContentHandler
 * @see AttributeContentHandler
 *
 * @author Brian Oliver
 */
public interface ElementContentHandler
{
    /**
     * Handle when an {@link XmlElement} is encountered in a namespace.
     *
     * @param context The {@link ConfigurationContext} for the namespace
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement}
     * @param xmlElement The {@link XmlElement} encountered
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return As part of the processing of the {@link XmlElement}, a value may be returned.
     *         This value is typically used by parent {@link XmlElement}s.  If not required, return <code>null</code>
     */
    public Object onElement(ConfigurationContext context,
                            QualifiedName        qualifiedName,
                            XmlElement           xmlElement) throws ConfigurationException;
}
