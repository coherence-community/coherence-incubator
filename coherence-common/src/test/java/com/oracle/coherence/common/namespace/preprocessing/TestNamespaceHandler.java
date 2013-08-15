/*
 * File: TestNamespaceHandler.java
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

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.QualifiedName;
import com.tangosol.run.xml.XmlElement;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple NamespaceHandler that provides support for merging.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 */
public class TestNamespaceHandler extends AbstractNamespaceHandler implements ElementProcessor,
                                                                              XmlPreprocessingNamespaceHandler
                                                                                  .IntroduceCacheConfigSupport
{
    private Set<String> m_ids = new HashSet<String>();


    /**
     * Constructs a TestNamespaceHandler.
     *
     */
    public TestNamespaceHandler()
    {
        registerProcessor("element", this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(ProcessingContext context,
                          XmlElement        xmlElement) throws ConfigurationException
    {
        QualifiedName qualifiedName = xmlElement.getQualifiedName();
        String        idTag         = qualifiedName.getPrefix() + ":id";
        String        id            = xmlElement.getSafeElement(idTag).getString();

        m_ids.add(id);

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void mergeConfiguration(ProcessingContext context,
                                   String            sFromURI,
                                   XmlElement        xmlFromElement,
                                   XmlElement        xmlIntoCacheConfig,
                                   QualifiedName     originatedFrom)
    {
        // clone the element to merge
        XmlElement xmlMergeElement = (XmlElement) xmlFromElement.clone();

        // annotate the origin of the merging element
        xmlMergeElement.addAttribute(originatedFrom.getName()).setString(sFromURI);

        xmlIntoCacheConfig.getElementList().add(xmlMergeElement);
    }


    /**
     * Obtains the set of merged ids.
     *
     * @return the set of merged ids.
     */
    public Set<String> getIds()
    {
        return m_ids;
    }
}
