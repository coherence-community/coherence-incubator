/*
 * File: ConfigurationContext.java
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

import com.oracle.coherence.common.builders.ReflectiveBuilder;
import com.oracle.coherence.common.util.EnrichmentSupport;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.environment.Environment;
import com.tangosol.run.xml.XmlElement;

import java.net.URI;
import java.util.Map;

/**
 * A {@link ConfigurationContext} provides facilities to aid in the processing
 * of Coherence Cache Configuration {@link XmlElement}s, including the creation and management
 * of {@link NamespaceContentHandler}s as they are encountered during said processing.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ConfigurationContext extends EnrichmentSupport
{
    /**
     * Ensures that a {@link NamespaceContentHandler} with the specified {@link URI} is available for the
     * specified prefix.  If a {@link NamespaceContentHandler} does not already exist, one is established
     * by the {@link ConfigurationContext} and returned.
     *
     * @param prefix The prefix of the XML Namespace for the {@link NamespaceContentHandler}.
     * @param uri The {@link URI} detailing the location of the {@link NamespaceContentHandler}. Typically this
     *            will be a java class URI, specified as "class://fully.qualified.class.name".
     *
     * @return An instance of the {@link NamespaceContentHandler} that is suitable for processing the prefix and {@link URI}.
     */
    public NamespaceContentHandler ensureNamespaceContentHandler(String prefix,
                                                                 URI    uri);


    /**
     * Locates and returns the {@link NamespaceContentHandler} that is capable of processing the namespace with the
     * specified prefix.
     *
     * @param prefix The XML namespace prefix of the {@link NamespaceContentHandler} to locate
     *
     * @return Returns <code>null</code> if a {@link NamespaceContentHandler} could not be located for the specified prefix.
     */
    public NamespaceContentHandler getNamespaceContentHandler(String prefix);


    /**
     * Locates and returns the current {@link NamespaceContentHandler} that is capable of processing the
     * namespace defined with the specified {@link URI}.
     *
     * @param uri The XML namespace {@link URI} of the {@link NamespaceContentHandler} to locate.
     *
     * @return Returns <code>null</code> if a {@link NamespaceContentHandler} could not be located for the specified {@link URI}.
     */
    public NamespaceContentHandler getNamespaceContentHandler(URI uri);


    /**
     * Locates and returns the {@link URI} that is currently associated with the specified prefix.
     *
     * @param prefix The XML namespace prefix of the {@link URI} to locate
     *
     * @return Returns <code>null</code> if a {@link URI} could not be located for the specified {@link URI}.
     */
    public URI getNamespaceURI(String prefix);


    /**
     * Returns the {@link Environment} in which the {@link ConfigurationContext} was defined.
     *
     * @return The {@link Environment} in which the {@link ConfigurationContext} was defined.
     */
    public Environment getEnvironment();


    /**
     * Returns the {@link ClassLoader} that should be used to load any classes used by the
     * {@link ConfigurationContext}.
     *
     * @return The {@link ClassLoader} for the {@link ConfigurationContext}.
     */
    public ClassLoader getClassLoader();


    /**
     * Request that the document specified by the URI/filename (containing the <strong>root</strong> of an XmlDocument)
     * be processed with appropriate {@link NamespaceContentHandler}s.
     * <p>
     * NOTE 1: Should the document root contain any unseen xml namespaces, appropriate {@link NamespaceContentHandler}s
     * will be loaded an initialized to process any associated xml elements in the document
     * <p>
     * NOTE 2: Prior to processing the document root, all of the "system-property"s used in the document
     * <strong>will be replaced</strong> with appropriate System properties.
     *
     * @param uri The {@link URI} of the XmlDocument to process.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of the {@link ElementContentHandler#onElement(ConfigurationContext, QualifiedName, XmlElement)}
     *         method that handled the XmlDocument
     */
    public Object processDocumentAt(URI uri) throws ConfigurationException;


    /**
     * Request that the document specified by the URI/filename (containing the <strong>root</strong> of an XmlDocument)
     * be processed with appropriate {@link NamespaceContentHandler}s.
     * <p>
     * NOTE 1: Should the document root contain any unseen xml namespaces, appropriate {@link NamespaceContentHandler}s
     * will be loaded an initialized to process any associated xml elements in the document
     * <p>
     * NOTE 2: Prior to processing the document root, all of the "system-property"s used in the document
     * <strong>will be replaced</strong> with appropriate System properties.
     *
     * @param location The URI/filename of the XmlDocument to process.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of the {@link ElementContentHandler#onElement(ConfigurationContext, QualifiedName, XmlElement)}
     *         method that handled the XmlDocument
     */
    public Object processDocumentAt(String location) throws ConfigurationException;


    /**
     * Request that the specified {@link XmlElement} representing the <strong>root</strong> of an XmlDocument
     * to be processed with appropriate {@link NamespaceContentHandler}s.
     * <p>
     * NOTE 1: Should the document root contain any unseen xml namespaces, appropriate {@link NamespaceContentHandler}s
     * will be loaded an initialized to process any associated xml elements in the document
     * <p>
     * NOTE 2: Prior to processing the document root, all of the "system-property"s used in the document
     * <strong>will be replaced</strong> with appropriate System properties.
     *
     * @param xmlElement The root {@link XmlElement} of the XmlDocument to process.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of the {@link ElementContentHandler#onElement(ConfigurationContext, QualifiedName, XmlElement)}
     *         method that handled the root {@link XmlElement} of the XmlDocument
     */
    public Object processDocument(XmlElement xmlElement) throws ConfigurationException;


    /**
     * Request that the specified xml string representing the <strong>root</strong> of an xml document
     * to be processed with appropriate {@link NamespaceContentHandler}s.
     * <p>
     * NOTE 1: Should the document root contain any unseen xml namespaces, appropriate {@link NamespaceContentHandler}s
     * will be loaded an initialized to process any associated xml elements in the document
     * <p>
     * NOTE 2: Prior to processing the document root, all of the "system-property"s used in the document
     * <strong>will be replaced</strong> with appropriate System properties.
     *
     * @param xml A string containing an xml document to process.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of the {@link ElementContentHandler#onElement(ConfigurationContext, QualifiedName, XmlElement)}
     *         method that handled the root xml element in the document
     */
    public Object processDocument(String xml) throws ConfigurationException;


    /**
     * Request the specified {@link XmlElement} to be processed with an appropriate {@link NamespaceContentHandler}
     * known by the {@link ConfigurationContext}.
     * <p>
     * NOTE: Should the element use any unseen xml namespaces, appropriate {@link NamespaceContentHandler}s
     * will be loaded an initialized to process associated xml element (and child elements).
     *
     * @param xmlElement The {@link XmlElement} to process
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of the {@link ElementContentHandler#onElement(ConfigurationContext, QualifiedName, XmlElement)}
     *         method that handled the {@link XmlElement}.
     */
    public Object processElement(XmlElement xmlElement) throws ConfigurationException;


    /**
     * Request the specified xml string be processed with an appropriate {@link NamespaceContentHandler}
     * known by the {@link ConfigurationContext}.
     * <p>
     * NOTE: Should the element use any unseen xml namespaces, appropriate {@link NamespaceContentHandler}s
     * will be loaded an initialized to process associated xml element (and child elements).
     *
     * @param xml The xml to process
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of the {@link ElementContentHandler#onElement(ConfigurationContext, QualifiedName, XmlElement)}
     *         method that handled the xml element.
     */
    public Object processElement(String xml) throws ConfigurationException;


    /**
     * Request that the child element (with the {@link QualifiedName}) contained within the {@link XmlElement}
     * be processed using an appropriate {@link NamespaceContentHandler} known by the {@link ConfigurationContext}.
     * <p>
     * NOTE: If there is more than one child element with the specified {@link QualifiedName}, only the first child
     * encountered is processed.
     *
     * @param <T> The expected type of value produced by processing the child element.
     * @param xmlElement The {@link XmlElement} in which the child is defined.
     * @param qualifiedName The {@link QualifiedName} of the child element.
     * @param defaultValue The default value to use if the child element is not located.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of processing the child element or the defaultValue if the child is not in the {@link XmlElement}.
     */
    public <T> T processElementOf(XmlElement    xmlElement,
                                  QualifiedName qualifiedName,
                                  T             defaultValue) throws ConfigurationException;


    /**
     * Request that the child element (identified by id attribute ie: an attribute where id="...") contained within
     * the {@link XmlElement} be processed using an appropriate {@link NamespaceContentHandler} known by
     * the {@link ConfigurationContext}.
     *
     * @param <T> The expected type of value produced by processing the child element.
     * @param xmlElement The {@link XmlElement} in which the child is defined.
     * @param id The identity of the child element.  ie: the child element has an id="..." attribute
     * @param defaultValue The default value to use if the child element is not located.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return The result of processing the child element or the defaultValue if the child is not in the {@link XmlElement}.
     */
    public <T> T processElementOf(XmlElement xmlElement,
                                  String     id,
                                  T          defaultValue) throws ConfigurationException;


    /**
     * Request that the <strong>only</strong> child element contained within the {@link XmlElement} is processed
     * using an appropriate {@link NamespaceContentHandler} known by the {@link ConfigurationContext}.
     *
     * @param xmlElement The {@link XmlElement} in which the child is defined.
     *
     * @throws ConfigurationException when a configuration problem was encountered,
     *         especially if there is zero or more than one child.
     *
     * @return The result of processing the child element.
     */
    public Object processOnlyElementOf(XmlElement xmlElement) throws ConfigurationException;


    /**
     * Request that all of the child elements contained within the specified {@link XmlElement} are to be
     * processed using appropriate {@link NamespaceContentHandler}s known by the {@link ConfigurationContext}.
     * <p>
     * This is a convenience method to aid in the processing of all children of an {@link XmlElement}. The keys
     * of the returned {@link Map} represent the id attributes each child {@link XmlElement}.  If an {@link XmlElement}
     * does not have a specified id attribute, a UUID is generated in it's place.
     *
     * @param xmlElement    The parent {@link XmlElement} of the children to process
     * @param except        An array of immediate child {@link QualifiedName}s to skip processing.
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return A {@link Map} from identifiable child {@link XmlElement}s (with id="..." attributes)
     *         and their corresponding processed values.
     */
    public Map<String, ?> processElementsOf(XmlElement       xmlElement,
                                            QualifiedName... except) throws ConfigurationException;


    /**
     * Request that all of the child elements contained within the specified {@link XmlElement} are to be
     * processed using appropriate {@link NamespaceContentHandler}s known by the {@link ConfigurationContext}.
     * <p>
     * This is a convenience method to aid in the processing of all children of an {@link XmlElement}. The keys
     * of the returned {@link Map} represent the id attributes each child {@link XmlElement}.  If an {@link XmlElement}
     * does not have a specified id attribute, a UUID is generated in it's place.
     *
     * @param xmlElement    The parent {@link XmlElement} of the children to process
     * @param expectedType  The expected type of value produced by processing a child (must not be null)
     *
     * @throws ConfigurationException When a child is not of the expected type (or could not be processed)
     *
     * @return A {@link Map} from identifiable child {@link XmlElement}s (with id="..." attributes)
     *         and their corresponding processed values.
     */
    public <T> Map<String, T> processElementsOf(XmlElement xmlElement,
                                                Class<T>   expectedType) throws ConfigurationException;


    /**
     * Sets the annotated {@link Property}s of the object with those properties
     * that are available in the provided {@link XmlElement}.
     *
     * @param object        The object that requires properties to be set.
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement    The {@link XmlElement} containing the properties for the object.
     *
     * @throws ConfigurationException if a configuration is not valid
     */
    public void configure(Object        object,
                          QualifiedName qualifiedName,
                          XmlElement    xmlElement) throws ConfigurationException;


    /**
     * Determines if the specified property is defined in the {@link XmlElement}.
     *
     * @param propertyName  The name of the property
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement}
     * @param xmlElement    The {@link XmlElement} in which to search for the property
     *
     * @return <code>true</code> if the property is defined in the {@link XmlElement}, <code>false</code> otherwise.
     * @throws ConfigurationException if a configuration is not valid
     */
    public boolean isPropertyDefined(String        propertyName,
                                     QualifiedName qualifiedName,
                                     XmlElement    xmlElement) throws ConfigurationException;


    /**
     * Attempts to return the value for the specified property declared within the provided {@link XmlElement}.
     * <p>
     * PRE-CONDITION: {@link #isPropertyDefined(String, QualifiedName, XmlElement)}.
     *
     * @param <T>             The type of the Property
     * @param propertyName    The name of the property
     * @param propertyType    The type of the property
     * @param propertySubType If the propertyType is generic, like a {@link ReflectiveBuilder}, the genericType may be used for further type checking.  Set to null if not required.
     * @param qualifiedName   The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement      The {@link XmlElement} containing the properties for the object.
     *
     * @return the mandatory property
     * @throws ConfigurationException if a configuration is not valid
     */
    public <T> T getMandatoryProperty(String        propertyName,
                                      Class<T>      propertyType,
                                      Class<?>      propertySubType,
                                      QualifiedName qualifiedName,
                                      XmlElement    xmlElement) throws ConfigurationException;


    /**
     * Attempts to return the value for the specified property declared within the provided {@link XmlElement}.
     *
     * @param <T>           The type of the Property
     * @param propertyName  The name of the property
     * @param propertyType  The type of the property
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement    The {@link XmlElement} containing the properties for the object.
     *
     * @return the mandatory property
     * @throws ConfigurationException if a configuration is not valid
     */
    public <T> T getMandatoryProperty(String        propertyName,
                                      Class<T>      propertyType,
                                      QualifiedName qualifiedName,
                                      XmlElement    xmlElement) throws ConfigurationException;


    /**
     * Attempts to return the value for the optional property declared within the provided {@link XmlElement}.
     *
     * @param <T>             The type of the optional property
     * @param propertyName    The name of the property
     * @param propertyType    The type of the property
     * @param propertySubType If the propertyType is generic, like a {@link ReflectiveBuilder}, the genericType may be
     *                        used for further type checking.  Set to {@link Void} if not required.
     * @param defaultValue    The returned value if no property is found
     * @param qualifiedName   The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement      The {@link XmlElement} containing the properties for the object.
     *
     * @return the optional property
     * @throws ConfigurationException if a configuration is not valid
     */
    public <T> T getOptionalProperty(String        propertyName,
                                     Class<T>      propertyType,
                                     Class<?>      propertySubType,
                                     T             defaultValue,
                                     QualifiedName qualifiedName,
                                     XmlElement    xmlElement) throws ConfigurationException;


    /**
     * Attempts to return the value for the optional property declared within the provided {@link XmlElement}.
     *
     * @param <T>           The type of the optional property
     * @param propertyName  The name of the property
     * @param propertyType  The type of the property
     * @param defaultValue  The returned value if no property is found
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement    The {@link XmlElement} containing the properties for the object.
     *
     * @return the optional property
     * @throws ConfigurationException if a configuration is not valid
     */
    public <T> T getOptionalProperty(String        propertyName,
                                     Class<T>      propertyType,
                                     T             defaultValue,
                                     QualifiedName qualifiedName,
                                     XmlElement    xmlElement) throws ConfigurationException;
}
