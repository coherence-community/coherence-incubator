/*
 * File: AbstractClassLoaderAwareParameterizedBuilder.java
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

package com.oracle.coherence.common.builders;

import com.tangosol.io.ClassLoaderAware;

/**
 * A {@link AbstractClassLoaderAwareParameterizedBuilder} is a base implementation of a {@link ClassLoaderAware}
 * {@link ParameterizedBuilder}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractClassLoaderAwareParameterizedBuilder<T> implements ParameterizedBuilder<T>,
                                                                                 ClassLoaderAware
{
    /**
     * The {@link ClassLoader} that should be used for loading classes (defaults to the {@link ClassLoader}
     * used to load this class).
     */
    private ClassLoader contextClassLoader;


    /**
     * Standard Constructor.
     */
    public AbstractClassLoaderAwareParameterizedBuilder()
    {
        this.contextClassLoader = getClass().getClassLoader();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getContextClassLoader()
    {
        return contextClassLoader;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setContextClassLoader(ClassLoader contextClassLoader)
    {
        this.contextClassLoader = contextClassLoader;
    }
}
