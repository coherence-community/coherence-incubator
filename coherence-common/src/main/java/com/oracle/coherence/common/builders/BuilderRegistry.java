/*
 * File: BuilderRegistry.java
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

package com.oracle.coherence.common.builders;

import org.apache.http.conn.scheme.Scheme;

import java.util.LinkedHashMap;

/**
 * A {@link BuilderRegistry} is responsible for managing the known set of {@link Builder}s
 * in an {@link com.oracle.coherence.environment.Environment}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class BuilderRegistry
{
    /**
     * The {@link Builder}s currently known by the {@link BuilderRegistry}.
     * <p>
     * NOTE: This is a {@link LinkedHashMap} as order of registration of {@link Scheme}s is often important
     * to Coherence.
     */
    private LinkedHashMap<String, Builder<?>> builders;


    /**
     * Standard Constructor.
     */
    public BuilderRegistry()
    {
        this.builders = new LinkedHashMap<String, Builder<?>>();
    }


    /**
     * Registers the specified {@link Builder} with the specified identity.
     * <p>
     * NOTE: If a {@link Builder} with the same identity is already registered, a call
     * to this method will silently override the existing registration.
     *
     * @param id        The identity for the {@link Builder}.  This identity may be used later to retrieve the {@link Builder}.
     * @param builder   The {@link Builder} to register.
     */
    public void registerBuilder(String     id,
                                Builder<?> builder)
    {
        builders.put(id, builder);
    }


    /**
     * Requests a reference to the {@link Builder} that was previously registered with
     * the specified identity.
     * <p>
     * NOTE: Returns <code>null</code> if the {@link Builder} requested has not be registered/is
     * unknown.
     *
     * @param id The identity of the {@link Builder} being requested.
     * @return A {@link Builder} or <code>null</code> if not found.
     */
    public Builder<?> getBuilder(String id)
    {
        return builders.get(id);
    }
}
