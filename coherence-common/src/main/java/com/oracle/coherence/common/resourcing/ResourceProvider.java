/*
 * File: ResourceProvider.java
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

package com.oracle.coherence.common.resourcing;

import com.oracle.coherence.common.builders.Builder;

/**
 * A {@link ResourceProvider} manages the provision and access to a runtime resource.
 * <p>
 * {@link ResourceProvider}s are commonly used for resources that may not be immediately available at the time of
 * request, make take some time to reach a certain required state, or may need to be lazily constructed.
 * <p>
 * {@link ResourceProvider}s are not {@link Builder}s.  An individual {@link ResourceProvider} instance is designed to
 * manage a single Resource, where as a {@link Builder} is designed to realize, build or allocate multiple resources.
 * <p>
 * For example, a {@link ResourceProvider} may be used to represent a connection to a specific server that has not yet
 * been started, where as a Connection {@link Builder} may be used to create connections to any number of servers.
 * <p>
 * There may be many implementations of {@link ResourceProvider}s, each with their own strategy for dealing
 * with resource provision and what happens when a resource is not available.  Consequently careful consideration as to
 * the choice of {@link ResourceProvider} implementation should be made to ensure the correct semantics are achieved.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ResourceProvider<T>
{
    /**
     * Determines the name of the Resource.  This is often used for logging and display purposes.
     *
     * @return A {@link String}
     */
    public String getResourceName();


    /**
     * Requests the Resource.
     *
     * @return A Resource of type T (never <code>null</code>)
     *
     * @throws ResourceUnavailableException When the {@link ResourceProvider} implementation can't produce the resource.
     */
    public T getResource() throws ResourceUnavailableException;
}
