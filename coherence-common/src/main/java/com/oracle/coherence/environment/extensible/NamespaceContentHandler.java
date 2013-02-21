/*
 * File: NamespaceContentHandler.java
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

import java.net.URI;

/**
 * A {@link NamespaceContentHandler} is responsible for handling XML content (represented as XML elements and
 * attributes) belonging to an explicitly declared XML namespace (using xmlns declarations) within a XML DOM.
 * <p>
 * For example, the following declares the namespace "mynamespace" to be available for the entire "cache-config"
 * element with the class "MyNamespaceContentHandler" as the {@link NamespaceContentHandler} that will process content
 * for the said namespace.
 * <code>
 *  &lt;cache-config xmlns:mynamespace="class://MyNameSpaceContentHandler"&gt
 * </code>
 * <p>
 * NOTE 1: To allow specific processing of XML elements occurring in the namespace, implementations of this
 * interface should additionally implement the {@link ElementContentHandler} interface, or alternatively, extend
 * the {@link com.oracle.coherence.environment.extensible.namespaces.AbstractNamespaceContentHandler}.
 * <p>
 * NOTE 2: Likewise, to allow specific processing of XML attributes occurring in the namespace, implementations of
 * this interface should additionally implement the {@link AttributeContentHandler} interface, or alternatively, extend
 * the {@link com.oracle.coherence.environment.extensible.namespaces.AbstractNamespaceContentHandler}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see ElementContentHandler
 * @see AttributeContentHandler
 * @see com.oracle.coherence.environment.extensible.namespaces.AbstractNamespaceContentHandler
 *
 * @author Brian Oliver
 * @author Christer Fahlgren
 */
public interface NamespaceContentHandler
{
    /**
     * Handle when the {@link NamespaceContentHandler} is first encountered (declared) in a configuration file,
     * ie: The beginning of the scope where a namespace can be used.  After this point the
     * {@link NamespaceContentHandler} will be used to handle XML content declared using the specified prefix.
     *
     * @param context
     *            The {@link ConfigurationContext} in which the
     *            {@link NamespaceContentHandler} was loaded
     * @param prefix
     *            The prefix that was used to declare the namespace
     * @param uri
     *            The {@link URI} specified for the
     *            {@link NamespaceContentHandler}s
     */
    public void onStartScope(ConfigurationContext context,
                             String               prefix,
                             URI                  uri);


    /**
     * Handle when the {@link NamespaceContentHandler} for a namespace is about to go out of scope in a
     * configuration file, after which no more XML content for the namespace will be processed.
     *
     * @param context
     *            The {@link ConfigurationContext} in which the
     *            {@link NamespaceContentHandler} was loaded
     * @param prefix
     *            The prefix that was used to declare the namespace
     * @param uri
     *            The {@link URI} specified for the
     *            {@link NamespaceContentHandler}
     */
    public void onEndScope(ConfigurationContext context,
                           String               prefix,
                           URI                  uri);
}
