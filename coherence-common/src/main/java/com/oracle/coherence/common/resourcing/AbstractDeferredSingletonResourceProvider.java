/*
 * File: AbstractDeferredSingletonResourceProvider.java
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
 * An {@link AbstractDeferredSingletonResourceProvider} may be used to defer the creation
 * of a singleton resource, once and only once, to a later point in time.
 * <p>
 * Multiple calls to instances of this class are thread-safe.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractDeferredSingletonResourceProvider<R> extends AbstractResourceProvider<R>
{
    /**
     * Has the resource been initialized?  (a volatile to avoid synchronized blocks)
     */
    private volatile boolean isInitialized;

    /**
     * The resolved resource.
     */
    private R resource;


    /**
     * Standard Constructor
     *
     * @param name The name of the resource
     */
    public AbstractDeferredSingletonResourceProvider(String name)
    {
        super(name);
        isInitialized = false;
        resource      = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final R getResource() throws ResourceUnavailableException
    {
        if (isInitialized)
        {
            return resource;
        }
        else
        {
            synchronized (this)
            {
                if (isInitialized)
                {
                    return resource;
                }
                else
                {
                    resource      = ensureResource();
                    isInitialized = true;

                    return resource;
                }
            }
        }
    }
}
