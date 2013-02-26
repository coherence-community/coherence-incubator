/*
 * File: CoherenceApplicationBuilder.java
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

package com.oracle.coherence.common.applications;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * A {@link CoherenceApplicationBuilder} is a specialized {@link AbstractCoherenceJavaApplicationBuilder} that's designed
 * explicitly for realizing Coherence applications with custom main classes.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
@Deprecated
public class CoherenceApplicationBuilder extends AbstractCoherenceJavaApplicationBuilder<CoherenceApplicationBuilder>
{
    /**
     * Create a {@link CoherenceApplicationBuilder} for the specified class.
     *
     * @param applicationClassName the main class to instantiate when this builder is realized
     */
    public CoherenceApplicationBuilder(String applicationClassName)
    {
        super(applicationClassName);
    }


    /**
     * Create a {@link CoherenceApplicationBuilder} for the specified class.
     *
     * @param applicationClassName  the main class to instantiate when this builder is realized
     * @param classPath             the classPath to use when this build is realized
     */
    public CoherenceApplicationBuilder(String applicationClassName,
                                       String classPath)
    {
        super(applicationClassName, classPath);
    }
}
