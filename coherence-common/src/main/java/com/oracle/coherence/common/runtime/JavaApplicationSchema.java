/*
 * File: JavaApplicationSchema.java
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
 * A {@link JavaApplicationSchema} is a Java specific {@link ApplicationSchema}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public interface JavaApplicationSchema<A extends JavaApplication, S extends JavaApplicationSchema<A, S>>
    extends ApplicationSchema<A, S>
{
    /**
     * Obtains the {@link PropertiesBuilder} that will be used as a basis for configuring the Java System Properties
     * of the realized {@link JavaApplication}s from an {@link JavaApplicationBuilder}.
     *
     * @return {@link PropertiesBuilder}
     */
    public PropertiesBuilder getSystemPropertiesBuilder();


    /**
     * Obtains the JVM options to be used for starting the {@link JavaApplication}.
     *
     * @return A {@link List} of {@link String}s
     */
    public List<String> getJVMOptions();


    /**
     * Obtains the class path to be used for the {@link JavaApplication}
     *
     * @return {@link String}
     */
    public String getClassPath();


    /**
     * Obtain the application main class name.
     *
     * @return {@link String}
     */
    public String getApplicationClassName();
}
