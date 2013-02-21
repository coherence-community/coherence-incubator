/*
 * File: XmlElementProcessingNamespaceContentHandler.java
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

import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.expressions.MacroParameterExpression;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.extensible.AttributeContentHandler;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import java.util.HashSet;
import java.util.List;

/**
 * The {@link XmlElementProcessingNamespaceContentHandler} provides specialized {@link XmlElement}
 * pre and post processing capabilities for configurations files, useful for
 * transforming {@link XmlElement}s on the fly.
 * <p>
 * Note 1: This namespace is designed to replace the need for the &lt;introduce:config&gt; element
 * defined by the now deprecated IntroduceNamespaceContentHandler.
 * <p>
 * Note 2: If you wish to use this namespace, you must declare it.  It's not implicitly defined.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class XmlElementProcessingNamespaceContentHandler extends AbstractNamespaceContentHandler
{
    /**
     * The set of currently introduced cache configuration file uris.
     * (we keep track of this so we don't re-introduce previously introduced urs).
     */
    private HashSet<String> introducedUris;


    /**
     * Standard Constructor.
     */
    public XmlElementProcessingNamespaceContentHandler()
    {
        this.introducedUris = new HashSet<String>();

        registerContentHandler("introduce-cache-config", new AttributeContentHandler()
        {
            @Override
            public void onAttribute(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlValue             xmlValue) throws ConfigurationException
            {
                // make sure we're in a "cache-config".  if we're not throw an exception
                if (xmlValue.getParent().getName().equals("cache-config"))
                {
                    // split the comma separated files into separate files.
                    String[] uris = xmlValue.getString().split(",");

                    for (String uri : uris)
                    {
                        uri = uri == null ? "" : uri.trim();

                        if (!uri.isEmpty())
                        {
                            uri = new MacroParameterExpression(uri).evaluate(SystemPropertyParameterProvider.INSTANCE)
                                .getString().trim();

                            if (!introducedUris.contains(uri))
                            {
                                XmlDocument document = DefaultConfigurableCacheFactory.loadConfigAsResource(uri,
                                                                                                            context.getClassLoader());

                                introducedUris.add(uri);
                                context.processDocument(document);
                            }
                        }
                    }
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("Attempted to use an %s in a non-<cache-config> element", qualifiedName),
                                                     "The introduce-cache-config attribute is only applicable to <cache-config> elements.");
                }
            }
        });

        registerContentHandler("replace-with-file", new AttributeContentHandler()
        {
            @SuppressWarnings("unchecked")
            @Override
            public void onAttribute(ConfigurationContext context,
                                    QualifiedName        qualifiedName,
                                    XmlValue             xmlValue) throws ConfigurationException
            {
                // determine the parent element we're going to replace.
                XmlElement xmlElement = xmlValue.getParent();

                // load the specified file to inherit
                Expression exprUri = new MacroParameterExpression(xmlValue.getString());
                String     uri     = exprUri.evaluate(SystemPropertyParameterProvider.INSTANCE).getString();
                XmlElement rootElement = XmlHelper.loadFileOrResource(uri,
                                                                      "replace-with-file",
                                                                      context.getClassLoader());

                // replace all of the "system-property" uses in the root element
                XmlHelper.replaceSystemProperties(rootElement, "system-property");

                // ensure the rootElement and the current element are the same type
                if (xmlElement.getName().equals(rootElement.getName()))
                {
                    // remove all of the current elements (as we're going to replace them all)
                    for (XmlElement childElement : ((List<XmlElement>) xmlElement.getElementList()))
                    {
                        XmlHelper.replaceElement(xmlElement, childElement);
                    }

                    // add all of the elements from the root into the current element
                    XmlHelper.addElements(xmlElement, rootElement.getElementList().iterator());
                }
                else
                {
                    throw new ConfigurationException(String
                        .format("The root element <%s> of the file [%s] is not a <%s> element as expected in [%s]",
                                rootElement.getRoot().getName(), uri, xmlElement.getName(), xmlElement.getParent()),
                                                     "To replace an element with a file, the element and root element of the file must be the same type");
                }
            }
        });
    }
}
