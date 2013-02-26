/*
 * File: Configurator.java
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

package com.oracle.coherence.configuration;

import com.oracle.coherence.common.builders.ReflectiveBuilder;
import com.oracle.coherence.environment.extensible.ConfigurationContext;
import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.oracle.coherence.environment.extensible.QualifiedName;
import com.tangosol.run.xml.XmlElement;

/**
 * A {@link Configurator} provides the ability to extract and inject annotated {@link Property}s for
 * {@link Configurable} objects from an {@link XmlElement}.
 * <p>
 * NOTE: This class is now deprecated.  All of the methods are now available on a {@link ConfigurationContext}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Configurable
 * @see Property
 *
 * @author Brian Oliver
 */
@Deprecated
public class Configurator
{
    /**
     * Sets the annotated {@link Property}s of the {@link Configurable} object with those properties
     * that are available in the provided {@link XmlElement}.
     *
     * @param object        The {@link Configurable} object that requires properties to be set.
     * @param context       The {@link ConfigurationContext} that may be used for processing XML.
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement    The {@link XmlElement} containing the properties for the object.
     *
     * @throws ConfigurationException if a configuration is not valid
     */
    public static void configure(Object               object,
                                 ConfigurationContext context,
                                 QualifiedName        qualifiedName,
                                 XmlElement           xmlElement) throws ConfigurationException
    {
        // TODO: Add a warning that this method is deprecated
        context.configure(object, qualifiedName, xmlElement);
    }


    /**
     * Determines if the specified property is defined in the {@link XmlElement}.
     *
     * @param propertyName  The name of the property
     * @param context       The {@link ConfigurationContext} that may be used for processing XML.
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement}
     * @param xmlElement    The {@link XmlElement} in which to search for the property
     *
     * @return <code>true</code> if the property is defined in the {@link XmlElement}, <code>false</code> otherwise.
     * @throws ConfigurationException if a configuration is not valid
     */
    @Deprecated
    public static boolean isPropertyDefined(String               propertyName,
                                            ConfigurationContext context,
                                            QualifiedName        qualifiedName,
                                            XmlElement           xmlElement) throws ConfigurationException
    {
        // TODO: Add a warning that this method is deprecated
        return context.isPropertyDefined(propertyName, qualifiedName, xmlElement);
    }


    /**
     * Attempts to return the value for the specified property declared within the provided {@link XmlElement}.
     *
     * PRE-CONDITION: {@link #isPropertyDefined(String, ConfigurationContext, QualifiedName, XmlElement)}.
     *
     * @param <T>             The type of the Property
     * @param propertyName    The name of the property
     * @param propertyType    The type of the property
     * @param propertySubType If the propertyType is generic, like a {@link ReflectiveBuilder}, the genericType may be used for further type checking.  Set to null if not required.
     * @param context         The {@link ConfigurationContext} that may be used for processing XML.
     * @param qualifiedName   The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement      The {@link XmlElement} containing the properties for the object.
     *
     * @return the mandatory property
     * @throws ConfigurationException if a configuration is not valid
     */
    public static <T> T getMandatoryProperty(String               propertyName,
                                             Class<T>             propertyType,
                                             Class<?>             propertySubType,
                                             ConfigurationContext context,
                                             QualifiedName        qualifiedName,
                                             XmlElement           xmlElement) throws ConfigurationException
    {
        // TODO: Add a warning that this method is deprecated
        return (T) context.getMandatoryProperty(propertyName, propertyType, propertySubType, qualifiedName, xmlElement);
    }


    /**
     * Attempts to return the value for the specified property declared within the provided {@link XmlElement}.
     *
     * @param <T>           The type of the Property
     * @param propertyName  The name of the property
     * @param propertyType  The type of the property
     * @param context       The {@link ConfigurationContext} that may be used for processing XML.
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement    The {@link XmlElement} containing the properties for the object.
     *
     * @return the mandatory property
     * @throws ConfigurationException if a configuration is not valid
     */
    public static <T> T getMandatoryProperty(String               propertyName,
                                             Class<T>             propertyType,
                                             ConfigurationContext context,
                                             QualifiedName        qualifiedName,
                                             XmlElement           xmlElement) throws ConfigurationException
    {
        // TODO: Add a warning that this method is deprecated
        return (T) context.getMandatoryProperty(propertyName, propertyType, null, qualifiedName, xmlElement);
    }


    /**
     * Attempts to return the value for the optional property declared within the provided {@link XmlElement}.
     *
     * @param <T>             The type of the optional property
     * @param propertyName    The name of the property
     * @param propertyType    The type of the property
     * @param propertySubType If the propertyType is generic, like a {@link ReflectiveBuilder}, the genericType may be used for further type checking.  Set to {@link Void} if not required.
     * @param defaultValue    The returned value if no property is found
     * @param context         The {@link ConfigurationContext} that may be used for processing XML.
     * @param qualifiedName   The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement      The {@link XmlElement} containing the properties for the object.
     *
     * @return the optional property
     * @throws ConfigurationException if a configuration is not valid
     */
    public static <T> T getOptionalProperty(String               propertyName,
                                            Class<T>             propertyType,
                                            Class<?>             propertySubType,
                                            T                    defaultValue,
                                            ConfigurationContext context,
                                            QualifiedName        qualifiedName,
                                            XmlElement           xmlElement) throws ConfigurationException
    {
        // TODO: Add a warning that this method is deprecated
        return (T) context.getOptionalProperty(propertyName,
                                               propertyType,
                                               propertySubType,
                                               defaultValue,
                                               qualifiedName,
                                               xmlElement);
    }


    /**
     * Attempts to return the value for the optional property declared within the provided {@link XmlElement}.
     *
     * @param <T>           The type of the optional property
     * @param propertyName  The name of the property
     * @param propertyType  The type of the property
     * @param defaultValue  The returned value if no property is found
     * @param context       The {@link ConfigurationContext} that may be used for processing XML.
     * @param qualifiedName The {@link QualifiedName} of the {@link XmlElement} we're processing.
     * @param xmlElement    The {@link XmlElement} containing the properties for the object.
     *
     * @return the optional property
     * @throws ConfigurationException if a configuration is not valid
     */
    public static <T> T getOptionalProperty(String               propertyName,
                                            Class<T>             propertyType,
                                            T                    defaultValue,
                                            ConfigurationContext context,
                                            QualifiedName        qualifiedName,
                                            XmlElement           xmlElement) throws ConfigurationException
    {
        // TODO: Add a warning that this method is deprecated
        return (T) context.getOptionalProperty(propertyName, propertyType, defaultValue, qualifiedName, xmlElement);
    }
}
