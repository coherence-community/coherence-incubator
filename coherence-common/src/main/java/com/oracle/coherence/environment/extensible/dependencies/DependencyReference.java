/*
 * File: DependencyReference.java
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

package com.oracle.coherence.environment.extensible.dependencies;

/**
 * A {@link DependencyReference} captures information about a required object, named cache, service etc
 * that is required for a resource to operate.
 * <p>
 * The typical use for {@link DependencyReference}s is to define the objects, named caches and/or services
 * that a {@link DependentResource} requires for correct operation.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface DependencyReference
{
    /**
     * Determines if the {@link DependencyReference} is referring to the specified {@link Object}.
     *
     * @param object The object being checked.
     *
     * @return <code>true</code> if the {@link DependencyReference} is referring to the specified {@link Object}.
     *         Always returns <code>false</code> if the supplied object is <code>null</code>.
     */
    public boolean isReferencing(Object object);
}
