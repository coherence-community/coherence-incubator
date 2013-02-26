/*
 * File: JavaApplicationBuilder.java
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
 * A {@link JavaApplicationBuilder} is Java specific {@link ApplicationBuilder}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A> The type of {@link JavaApplication} that the {@link JavaApplicationBuilder} will create.
 * @param <T> The type of the {@link JavaApplicationBuilder} from which default configuration may be retrieved.
 *
 * @see JavaApplication
 * @see ApplicationBuilder
 *
 * @author Brian Oliver
 */
@Deprecated
public interface JavaApplicationBuilder<A extends JavaApplication, T extends JavaApplicationBuilder<A, ?>>
    extends ApplicationBuilder<A, T>
{
    /**
     * Returns the {@link PropertiesBuilder} that will be used as a basis for configuring the Java System Properties
     * of the realized {@link JavaApplication}s from this {@link JavaApplicationBuilder}.
     *
     * @return {@link PropertiesBuilder}
     */
    public PropertiesBuilder getSystemPropertiesBuilder();
}
