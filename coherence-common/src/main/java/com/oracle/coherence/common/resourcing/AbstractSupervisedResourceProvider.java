/*
 * File: AbstractSupervisedResourceProvider.java
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
 * An {@link AbstractSupervisedResourceProvider} is a base implementation of a {@link SupervisedResourceProvider}.
 * <p>
 * Extend this class to provide implementations for specific types of Resources.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractSupervisedResourceProvider<T> extends AbstractResourceProvider<T>
    implements SupervisedResourceProvider<T>
{
    /**
     * The instant in time (since the epoc) when the last access to the Resource failed.
     */
    private volatile long instantOfLastFailure;

    /**
     * The minimum amount of time (in ms) that must pass before accessing the Resource can be retried.
     */
    private long minimumAccessRetryDelay;


    /**
     * Standard Constructor.
     *
     * @param minimumAccessRetryDelay The minimum number of milliseconds that must pass between access attempts
     */
    public AbstractSupervisedResourceProvider(String resourceName,
                                              long   minimumAccessRetryDelay)
    {
        super(resourceName);

        this.instantOfLastFailure    = 0;
        this.minimumAccessRetryDelay = minimumAccessRetryDelay;
    }


    /**
     * {@inheritDoc}
     */
    public final T getResource() throws ResourceUnavailableException
    {
        if (isResourceAccessible())
        {
            // we synchronize here to protect the underlying resource,
            // that may not be able to cope with a lot of concurrent access
            synchronized (this)
            {
                // the current thread may have be held up for a while waiting,
                // so we check again if the resource is still accessible
                if (isResourceAccessible())
                {
                    try
                    {
                        // attempt to get the resource
                        return ensureResource();
                    }
                    catch (ResourceUnavailableException resourceUnavailableException)
                    {
                        // when a resource is unavailable, we must re-throw it
                        instantOfLastFailure = System.currentTimeMillis();

                        throw resourceUnavailableException;
                    }
                    catch (RuntimeException runtimeException)
                    {
                        // for runtime exceptions we re-throw them
                        instantOfLastFailure = System.currentTimeMillis();

                        throw runtimeException;
                    }
                }
                else
                {
                    throw new ResourceUnavailableException(getResourceName());
                }
            }
        }
        else
        {
            throw new ResourceUnavailableException(getResourceName());
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isResourceAccessible()
    {
        return System.currentTimeMillis() > instantOfLastFailure + minimumAccessRetryDelay;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void resourceNoLongerAvailable()
    {
        instantOfLastFailure = System.currentTimeMillis();
    }
}
