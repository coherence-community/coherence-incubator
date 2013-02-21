/*
 * File: AbstractNamespaceContentHandler.java
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

import com.oracle.coherence.environment.extensible.AttributeContentHandler;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.ElementContentHandler;
import com.oracle.coherence.environment.extensible.NamespaceContentHandler;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import java.net.URI;
import java.util.HashMap;

/**
 * An {@link AbstractNamespaceContentHandler} provides a base implementation of a {@link NamespaceContentHandler}
 * with support for registering {@link ElementContentHandler} and {@link AttributeContentHandler}s for the said
 * {@link NamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractNamespaceContentHandler implements NamespaceContentHandler,
                                                                 ElementContentHandler,
                                                                 AttributeContentHandler
{
    /**
     * The xmlns prefix that the {@link NamespaceContentHandler} is handling.
     */
    private String prefix;

    /**
     * The registered {@link ElementContentHandler}s.
     */
    private HashMap<String, ElementContentHandler> elementContentHandlers;

    /**
     * The registered {@link AttributeContentHandler}s.
     */
    private HashMap<String, AttributeContentHandler> attributeContentHandlers;


    /**
     * Standard Constructor.
     *
     * NOTE: This method should be overriden by sub-classes to register Element and Attribute Content Handlers.
     */
    public AbstractNamespaceContentHandler()
    {
        this.elementContentHandlers   = new HashMap<String, ElementContentHandler>();
        this.attributeContentHandlers = new HashMap<String, AttributeContentHandler>();
    }


    /**
     * Registers an {@link ElementContentHandler} for the specified local name.
     *
     * @param localName The local name of the {@link ElementContentHandler}
     * @param elementContentHandler The {@link ElementContentHandler} for the local name
     */
    public void registerContentHandler(String                localName,
                                       ElementContentHandler elementContentHandler)
    {
        // TODO: ensure that we've not already registered the element content handler
        this.elementContentHandlers.put(localName, elementContentHandler);
    }


    /**
     * Registers an {@link AttributeContentHandler} for the specified local name.
     *
     * @param localName The local name of the {@link AttributeContentHandler}
     * @param attributeContentHandler The {@link AttributeContentHandler} for the local name
     */
    public void registerContentHandler(String                  localName,
                                       AttributeContentHandler attributeContentHandler)
    {
        // TODO: ensure that we've not already registered the attribute content handler
        this.attributeContentHandlers.put(localName, attributeContentHandler);
    }


    /**
     * Returns the xmlns prefix used to define the instance of the {@link NamespaceContentHandler}.
     *
     * @return The xmlns prefix for which the {@link NamespaceContentHandler} is used.
     */
    public String getPrefix()
    {
        return prefix;
    }


    /**
     * {@inheritDoc}
     */
    public void onStartScope(ConfigurationContext context,
                             String               prefix,
                             URI                  uri)
    {
        this.prefix = prefix;
    }


    /**
     * {@inheritDoc}
     */
    public void onEndScope(ConfigurationContext context,
                           String               prefix,
                           URI                  uri)
    {
        // nothing to do here. may be overridden by sub-classes
    }


    /**
     * {@inheritDoc}
     */
    public final Object onElement(ConfigurationContext context,
                                  QualifiedName        qualifiedName,
                                  XmlElement           xmlElement) throws ConfigurationException
    {
        // look up the ElementContentHandler and have it process the XmlElement
        if (elementContentHandlers.containsKey(qualifiedName.getLocalName()))
        {
            return elementContentHandlers.get(qualifiedName.getLocalName()).onElement(context,
                                                                                      qualifiedName,
                                                                                      xmlElement);
        }
        else
        {
            return onUnknownElement(context, qualifiedName, xmlElement);
        }
    }


    /**
     * Handles when an element is not known to the {@link NamespaceContentHandler}.  Override this method to provide
     * specialized foreign element processing.  By default, unknown elements will throw a {@link RuntimeException}.
     *
     * @param context The {@link ConfigurationContext} in which the {@link XmlElement} was unknown
     * @param qualifiedName The {@link QualifiedName} of the unknown {@link XmlElement}
     * @param xmlElement The {@link XmlElement} that was unknown.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return An object to be used by the parent {@link XmlElement} or <code>null</code> if nothing required.
     */
    public Object onUnknownElement(ConfigurationContext context,
                                   QualifiedName        qualifiedName,
                                   XmlElement           xmlElement) throws ConfigurationException
    {
        throw new ConfigurationException(String
            .format("The NamespaceContentHandler [%s] can't process the element [%s] as it is unknown.",
                    this.getClass().getName(), xmlElement),
                                         String
                                         .format("Please consult the documentation for the declared %s namespace",
                                             getPrefix()));
    }


    /**
     * {@inheritDoc}
     */
    public final void onAttribute(ConfigurationContext context,
                                  QualifiedName        qualifiedName,
                                  XmlValue             xmlValue) throws ConfigurationException
    {
        // look up the AttributeContentHandler and have it process the xmlValue
        if (attributeContentHandlers.containsKey(qualifiedName.getLocalName()))
        {
            attributeContentHandlers.get(qualifiedName.getLocalName()).onAttribute(context, qualifiedName, xmlValue);
        }
        else
        {
            onUnknownAttribute(context, qualifiedName, xmlValue);
        }
    }


    /**
     * Handles when an attribute is not known to the {@link NamespaceContentHandler}.  Override this method to provide
     * specialized foreign attribute processing.  By default, values for unknown attributes will simply be returned
     * without further processing.
     *
     * @param context The {@link ConfigurationContext} in which the {@link XmlElement} was unknown.
     * @param qualifiedName The {@link QualifiedName} of the unknown attribute
     * @param xmlValue The value of the unknown attribute
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public final void onUnknownAttribute(ConfigurationContext context,
                                         QualifiedName        qualifiedName,
                                         XmlValue             xmlValue) throws ConfigurationException
    {
        // SKIP: by default we don't do anything if the attribute is unknown
    }
}
