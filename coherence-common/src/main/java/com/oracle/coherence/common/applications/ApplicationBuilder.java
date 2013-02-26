/*
 * File: ApplicationBuilder.java
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

import com.oracle.coherence.common.resourcing.ResourcePreparer;

import java.io.IOException;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link ApplicationBuilder} is responsible the creation of one or more {@link Application}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A> The type of {@link Application} that the {@link ApplicationBuilder} will create.
 * @param <T> The type of the {@link ApplicationBuilder} from which default configuration may be retrieved.
 *
 * @author Brian Oliver
 */
@Deprecated
public interface ApplicationBuilder<A extends Application, T extends ApplicationBuilder<A, ?>>
{
    /**
     * Returns the {@link PropertiesBuilder} that will be used to configure the operating system environment
     * variables for the realized {@link Application}.
     *
     * @return {@link PropertiesBuilder}
     */
    public PropertiesBuilder getEnvironmentVariablesBuilder();


    /**
     * Sets the {@link ResourcePreparer} to use with the built {@link Application}s prior to returning
     * from realize.
     *
     * @param preparer The {@link ResourcePreparer}
     *
     * @return The resulting {@link ApplicationBuilder}.
     */
    public T setApplicationPreparer(ResourcePreparer<A> preparer);


    /**
     * Determines the current {@link ResourcePreparer} for {@link Application}s built by this {@link ApplicationBuilder}.
     *
     * @return A {@link ResourcePreparer}
     */
    public ResourcePreparer<A> getApplicationPreparer();


    /**
     * Realizes an instance of an {@link Application}, optionally using the information provided by
     * parent {@link ApplicationBuilder}.
     *
     * @param defaultBuilder    An {@link ApplicationBuilder} that should be used to determine default environment
     *                          variable declarations.  This may be <code>null</code> if not required.
     *
     * @param name              The name of the application.
     *
     * @param console           The {@link ApplicationConsole} that will be used for I/O by the
     *                          realized {@link Application}. This may be <code>null</code> if not required.
     *
     * @return An {@link Application} representing the application realized by the {@link ApplicationBuilder}.
     *
     * @throws IOException Thrown if a problem occurs while realizing the application
     */
    public A realize(T                  defaultBuilder,
                     String             name,
                     ApplicationConsole console) throws IOException;
}
