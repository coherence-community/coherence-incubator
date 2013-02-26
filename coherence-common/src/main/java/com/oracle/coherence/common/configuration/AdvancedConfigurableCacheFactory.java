/*
 * File: AdvancedConfigurableCacheFactory.java
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

package com.oracle.coherence.common.configuration;

import com.oracle.coherence.environment.extensible.ExtensibleEnvironment;
import com.tangosol.run.xml.XmlElement;

/**
 * The purpose of the {@link AdvancedConfigurableCacheFactory} was initially to provide
 * the ability to "introduce" cache configurations from other files to construct a single
 * cache configuration.  This functionality, together with the ability to dynamically handle
 * new namespaces is now provided by the {@link ExtensibleEnvironment} implementation.
 * <p>
 * It is strongly advised that developers use {@link ExtensibleEnvironment}s instead
 * of {@link AdvancedConfigurableCacheFactory}s as we're deprecating them.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public class AdvancedConfigurableCacheFactory extends ExtensibleEnvironment
{
    /**
     * Constructor.
     */
    public AdvancedConfigurableCacheFactory()
    {
        super();
    }


    /**
     * Constructor.
     *
     * @param path The path to the configuration file
     */
    public AdvancedConfigurableCacheFactory(String path)
    {
        super(path);
    }


    /**
     * Constructor.
     *
     * @param xmlConfig The XML configuration to use for this cache factory
     */
    public AdvancedConfigurableCacheFactory(XmlElement xmlConfig)
    {
        super(xmlConfig);
    }


    /**
     * Constructor.
     *
     * @param path   The path to the configuration file
     * @param loader The classloader associated with this condfiguration
     */
    public AdvancedConfigurableCacheFactory(String      path,
                                            ClassLoader loader)
    {
        super(path, loader);
    }
}
