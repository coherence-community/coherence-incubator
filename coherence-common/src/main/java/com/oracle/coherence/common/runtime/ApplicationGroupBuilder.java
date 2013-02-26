/*
 * File: ApplicationGroupBuilder.java
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

import java.io.IOException;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link ApplicationGroupBuilder} is a builder for {@link ApplicationGroup}s.  That is, collections of related
 * {@link Application}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A>  The type of {@link Application}s that will be built by the {@link ApplicationGroupBuilder}.
 *
 * @author Brian Oliver
 */
@Deprecated
public interface ApplicationGroupBuilder<A extends Application, S extends ApplicationSchema<A, S>,
                                         B extends ApplicationBuilder<A, S, B>, G extends ApplicationGroup<A>>
{
    /**
     * Adds a {@link ApplicationBuilder} to the {@link ApplicationGroupBuilder} that will be used to realize
     * a type of {@link Application} when the {@link ApplicationGroup} is realized with {@link #realize(ApplicationConsole)}.
     *
     * @param builder             The {@link ApplicationBuilder} for the {@link Application}s.
     * @param schema              The {@link ApplicationSchema} from which to realize/configure the {@link Application}s.
     * @param sPrefix             The {@link Application} name prefix for each of the realized {@link Application}.
     * @param cRequiredInstances  The number of instances of the {@link Application} that should be realized for the
     *                            {@link ApplicationGroup} when {@link #realize(ApplicationConsole)} is called.
     */
    public void addBuilder(B      builder,
                           S      schema,
                           String sPrefix,
                           int    cRequiredInstances);


    /**
     * Realizes an instance of an {@link ApplicationGroup}.
     *
     * @param console           The {@link ApplicationConsole} that will be used for I/O by the
     *                          {@link Application}s realized in the {@link ApplicationGroup}.
     *                          This may be <code>null</code> if not required.
     *
     * @return An {@link ApplicationGroup} representing the collection of realized {@link Application}s.
     *
     * @throws IOException Thrown if a problem occurs while realizing the application
     */
    public G realize(ApplicationConsole console) throws IOException;


    /**
     * Realizes an instance of an {@link ApplicationGroup} (without a console).
     *
     * @return An {@link ApplicationGroup} representing the collection of realized {@link Application}s.
     *
     * @throws IOException Thrown if a problem occurs while realizing the application
     */
    public G realize() throws IOException;
}
