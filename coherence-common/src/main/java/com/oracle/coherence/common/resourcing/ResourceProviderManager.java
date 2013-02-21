/*
 * File: ResourceProviderManager.java
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

import java.util.concurrent.ConcurrentHashMap;

/**
 * The role of a {@link ResourceProviderManager} instance to so manage a named collection of {@link ResourceProvider}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ResourceProviderManager
{
    /**
     * The named collection of {@link ResourceProvider}s.
     */
    private ConcurrentHashMap<String, ResourceProvider<?>> resourceProvidersByName;


    /**
     * Standard Constructor.
     */
    public ResourceProviderManager()
    {
        this.resourceProvidersByName = new ConcurrentHashMap<String, ResourceProvider<?>>();
    }


    /**
     * Attempts to register the specified uniquely named (for the class) {@link ResourceProvider}.  If a
     * {@link ResourceProvider} with the specified name (and class) is already registered, the request to (re)register
     * is ignored.
     *
     * @param <T>                           The type of the {@link ResourceProvider}
     * @param clazz                         The class of the {@link ResourceProvider}
     * @param name                          The unique name (for the class) of the {@link ResourceProvider}
     * @param supervisedResourceProvider    The {@link ResourceProvider}
     */
    public <T> void registerResourceProvider(Class<T>            clazz,
                                             String              name,
                                             ResourceProvider<T> supervisedResourceProvider)
    {
        resourceProvidersByName.putIfAbsent(name + clazz.getName(), supervisedResourceProvider);
    }


    /**
     * Attempts to return a previously registered {@link ResourceProvider} with the specified name (and class)
     *
     * @param <T>
     * @param clazz The class of the {@link ResourceProvider}
     * @param name The unique name (for the class) of the {@link ResourceProvider}
     *
     * @return The registered {@link ResourceProvider} or <code>null</code> if the {@link ResourceProvider} is
     *         unknown.
     */
    @SuppressWarnings("unchecked")
    public <T> ResourceProvider<T> getResourceProvider(Class<T> clazz,
                                                       String   name)
    {
        return (ResourceProvider<T>) resourceProvidersByName.get(name + clazz.getName());
    }
}
