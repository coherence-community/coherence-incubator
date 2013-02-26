/*
 * File: ApplicationSchema.java
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

package com.oracle.coherence.common.runtime;

import java.util.List;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * {@link ApplicationSchema} defines the schema encapsulating configuration and operational settings that an
 * {@link ApplicationBuilder} will use to correctly realize an {@link Application}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A>  The type of the {@link Application} that will be realized by an {@link ApplicationBuilder} using
 *             this {@link ApplicationSchema}.
 *
 * @param <S>  The type of {@link ApplicationSchema} that will be returned from setter calls
 *             on this interface.  This is to permit extensible type-safe fluent-style definitions of
 *             setter methods in sub-interfaces.
 *
 * @author Brian Oliver
 */
@Deprecated
public interface ApplicationSchema<A extends Application, S extends ApplicationSchema<A, S>>
{
    /**
     * Obtains the {@link PropertiesBuilder} that will be used to determine operating system environment
     * variables to be provided to the {@link Application}.
     *
     * @return {@link PropertiesBuilder}
     */
    public PropertiesBuilder getEnvironmentVariablesBuilder();


    /**
     * Obtains the arguments for the {@link Application}.
     *
     * @return A {@link List} of {@link String}s
     */
    public List<String> getArguments();
}
