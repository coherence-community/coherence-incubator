/*
 * File: AbstractResourceProvider.java
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

/**
 * An {@link AbstractResourceProvider} is a base implementation of {@link ResourceProvider}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractResourceProvider<T> implements ResourceProvider<T>
{
    /**
     * The name of the Resource being provided.
     */
    private String resourceName;


    /**
     * Standard Constructor.
     *
     * @param resourceName
     */
    public AbstractResourceProvider(String resourceName)
    {
        this.resourceName = resourceName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceName()
    {
        return resourceName;
    }


    /**
     * Attempt to ensure that the Resource is available and can be provided to a caller.
     *
     * @throws RuntimeException When attempting to access the Resource failed, but is not fatal (can be retried)
     * @throws ResourceUnavailableException If the Resource is simply unavailable and won't ever be.
     *
     * @return <code>null</code> if the resource is not yet available but should be retried at a later point in time,
     *         otherwise returns the resource.
     */
    protected abstract T ensureResource() throws ResourceUnavailableException;
}
