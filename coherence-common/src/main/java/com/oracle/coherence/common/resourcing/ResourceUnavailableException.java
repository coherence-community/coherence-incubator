/*
 * File: ResourceUnavailableException.java
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
 * A {@link ResourceUnavailableException} is thrown when an attempt to request a Resource is made, but the
 * said Resource is unavailable.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class ResourceUnavailableException extends RuntimeException
{
    /**
     * The name of the resource that is unavailable.
     */
    private String resourceName;


    /**
     * Standard Constructor (without a causing exception)
     *
     * @param resourceName The name of the unavailable Resource
     */
    public ResourceUnavailableException(String resourceName)
    {
        this(resourceName, null);
    }


    /**
     * Standard Constructor (with a causing exception)
     *
     * @param resourceName  The name of the unavailable Resource
     * @param cause         The {@link Throwable} that caused the Resource to become unavailable.
     */
    public ResourceUnavailableException(String    resourceName,
                                        Throwable cause)
    {
        super(String.format("The resource [%s] is currently unavailable", resourceName), cause);
        this.resourceName = resourceName;
    }


    /**
     * Determines the name of the Resource that is unavailable.
     *
     * @return A {@link String}
     */
    public String getResourceName()
    {
        return resourceName;
    }
}
