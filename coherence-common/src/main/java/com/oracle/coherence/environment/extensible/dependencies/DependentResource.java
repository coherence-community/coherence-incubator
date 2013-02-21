/*
 * File: DependentResource.java
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

import com.oracle.coherence.environment.Environment;

import java.util.Set;

/**
 * A {@link DependentResource} is an {@link Environment} registered resource that has an explicitly defined
 * {@link Set} of dependencies (represented as {@link DependencyReference}s) that must be
 * "satisfied" (started) before the said resource should be used.  Should one or more dependencies
 * be stopped (after they were all previously started), the dependencies for the {@link DependentResource}
 * are said to be "violated".
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see com.oracle.coherence.common.events.lifecycle.LifecycleEvent
 *
 * @author Brian Oliver
 */
public interface DependentResource
{
    /**
     * Returns the {@link Set} of {@link DependencyReference}s representing the dependencies
     * that <strong>all</strong>must be started before the {@link DependentResource} is satisfied.
     *
     * @return The {@link Set} of {@link DependencyReference}s representing required dependencies
     * (or <code>null</code>) if the {@link DependentResource} has no dependencies.
     */
    public Set<DependencyReference> getDependencyReferences();


    /**
     * The {@link #onDependenciesSatisfied(Environment)} is called when <strong>all</strong> of the
     * specified {@link DependencyReference}s in {@link #getDependencyReferences()} {@link Set}
     * have been started.
     *
     * @see com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent
     *
     * @param environment The {@link Environment} in which the dependencies have been satisfied.
     */
    public void onDependenciesSatisfied(Environment environment);


    /**
     * The {@link #onDependenciesViolated(Environment)} is called when the <strong>first</strong>
     * of one of the specified {@link DependencyReference}s in {@link #getDependencyReferences()} {@link Set}
     * of the <strong>previously satisfied</strong> {@link DependentResource}, has been stopped,
     * thus meaning the {@link DependentResource} has at least one of their dependencies violated.
     *
     * @see com.oracle.coherence.common.events.lifecycle.LifecycleStoppedEvent
     *
     * @param environment The {@link Environment} in which the dependencies have been violated.
     */
    public void onDependenciesViolated(Environment environment);
}
