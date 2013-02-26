/*
 * File: Application.java
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

import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link Application} provides a mechanism to represent, access and control an executing process, typically
 * managed by an operating system.
 *
 * @see ApplicationBuilder
 *
 * @author Brian Oliver
 */
@Deprecated
public interface Application
{
    /**
     * Obtains the environment variables that were supplied to the {@link Application} when it was realized.
     *
     * @return {@link Properties} of containing name value pairs, each one representing an environment variable provided
     *         to the {@link Application} when it was realized.
     */
    public Properties getEnvironmentVariables();


    /**
     * Obtains the name of the {@link Application}.
     *
     * @return The name of the {@link Application}.
     */
    public String getName();


    /**
     * Destroys the running {@link Application}.  Upon returning from this method you can safely assume the
     * {@link Application} is no longer running.
     */
    public void destroy();
}
