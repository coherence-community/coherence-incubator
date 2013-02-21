/*
 * File: AbstractDeferredResourceProvider.java
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
 * An {@link AbstractDeferredResourceProvider} is a {@link ResourceProvider} implementation that provides the ability to
 * block and wait a specified period of time for a Resource to become available.
 * <p>
 * During the waiting time the {@link AbstractDeferredResourceProvider} implementation will attempt to make a
 * configurable number of requests to determine if the resource is available.  If it becomes available before the
 * specified time, the resource will be returned.  If it does not, a {@link ResourceUnavailableException} will be
 * thrown.
 * <p>
 * Extend this class to provide implementations for specific types of Resources.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractDeferredResourceProvider<T> extends AbstractResourceProvider<T>
{
    /**
     * The duration (in milliseconds) that we will wait between failed attempts to ensure a Resource.
     */
    private long retryDelayDurationMS;

    /**
     * The total duration (in milliseconds) that we will attempt to ensure that a Resource before we fail.
     */
    private long totalRetryDurationMS;


    /**
     * Standard Constructor
     * @param resourceName          The Resource name.
     * @param retryDelayDurationMS  The duration (in milliseconds) that we will wait between failed attempts to ensure a
     *                              Resource.
     * @param totalRetryDurationMS  The total duration (in milliseconds) that we will attempt to ensure that a Resource
     *                              before we fail.
     */
    public AbstractDeferredResourceProvider(String resourceName,
                                            long   retryDelayDurationMS,
                                            long   totalRetryDurationMS)
    {
        super(resourceName);

        this.retryDelayDurationMS = retryDelayDurationMS;
        this.totalRetryDurationMS = totalRetryDurationMS;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final T getResource() throws ResourceUnavailableException
    {
        long retryDuration = 0;

        while (totalRetryDurationMS < 0 ? true : retryDuration < totalRetryDurationMS)
        {
            try
            {
                T resource = ensureResource();

                if (resource != null)
                {
                    return resource;
                }
            }
            catch (ResourceUnavailableException resourceUnavailableException)
            {
                throw resourceUnavailableException;
            }
            catch (RuntimeException runtimeException)
            {
                // SKIP: Nothing to do here as we'll just retry
            }

            // wait for the value to become available
            try
            {
                Thread.sleep(retryDelayDurationMS);
                retryDuration += retryDelayDurationMS;
            }
            catch (InterruptedException interruptedException)
            {
                throw new ResourceUnavailableException(String
                    .format("The resource [%s] is unavailable (ensuring was interrupted).", getResourceName()),
                                                       interruptedException);
            }

        }

        throw new ResourceUnavailableException(getResourceName());
    }
}
