/*
 * File: Environment.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.processing.internal;


import java.util.Map;
import java.util.logging.Logger;


/**
 * An {@link Environment} provides access to strongly typed resources and contextual information for one or
 * more applications, components and/or extensions <strong>within</strong> a Coherence-based process (ie: JVM).
 * <p>
 * In most cases the {@link com.tangosol.net.ConfigurableCacheFactory} implementations are responsible for implementing
 * the {@link Environment}.  Consequently the typical approach to locate and access a configured {@link Environment}
 * is to simply perform the following;
 * <P>
 * <code>({@link Environment}){@link com.tangosol.net.CacheFactory#getConfigurableCacheFactory()}</code>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface Environment
{
    /**
     * Attempts to resolve and return a resource registered with the specified interface. Returns
     * <code>null</code> if the resource can't be resolved.
     *
     * @param <R>   The type of the expected resource
     *
     * @param clazz The interface {@link Class} of the expected resource.
     *
     * @return A resource of type <R> or <code>null</code> if one could not resolved.
     */
    public <R> R getResource(Class<R> clazz);


    /**
     * Attempts to resolve and return a uniquely name resource registered with the specified interface. Returns
     * <code>null</code> if the resource can't be resolved.
     *
     * @param <R>   The type of the expected resource
     *
     * @param clazz The interface {@link Class} of the expected resource.
     * @param name  The name of the resource (can't be <code>null</code>)
     *
     * @return A resource of type <R> or <code>null</code> if one could not resolved.
     */
    public <R> R getResource(Class<R> clazz,
                             String name);


    /**
     * Registers a resource with the {@link Environment} that may be later retrieved using the
     * {@link #getResource(Class)} method using the specified {@link Class}.
     * <p>
     * Note: The resource will automatically be given the name of the fully-qualified-class-name of the resource.
     *
     * @param <R>       The type of the interface to be associated with the resource
     *
     * @param clazz     The interface {@link Class} of the resource to register
     * @param resource  The resource to register
     *
     * @return The resource that already exists or was newly registered
     */
    public <R> R registerResource(Class<R> clazz,
                                  Object resource);


    /**
     * Registers a uniquely named resource with the {@link Environment} that may be later retrieved using the
     * {@link #getResource(Class, String)} method using the specified {@link Class} and name.
     * <p>
     * Note: Multiple resources of the same {@link Class} may be registered so long as the names for each resource
     * is unique for the type.
     *
     * @param <R>       The type of the interface to be associated with the resource.
     *
     * @param clazz     The interface {@link Class} of the resource to register.
     * @param name      The name of the resource (can't be <code>null</code>).
     * @param resource  The resource to register.
     *
     * @return The resource that already exists or was newly registered
     */
    public <R> R registerResource(Class<R> clazz,
                                  String name,
                                  Object resource);
}
