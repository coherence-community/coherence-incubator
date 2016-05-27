/*
 * File: ContextConfiguration.java
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

package com.oracle.coherence.patterns.command;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

import java.io.Serializable;

/**
 * A {@link ContextConfiguration} defines the properties that specify
 * how a {@link Context} is managed, together with the expected semantics
 * of the environment in which the {@link Context} is operating.
 * <p>
 * As {@link ContextConfiguration}s are cached objects (ie: placed in Coherence caches),
 * they need to at least implement {@link Serializable}, or better still, implement
 * {@link ExternalizableLite} or {@link PortableObject}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ContextConfiguration
{
    /**
     * The {@link ManagementStrategy} of a {@link Context}
     * is used to specify how work (like {@link Command}s) are
     * stored and managed.
     */
    public enum ManagementStrategy
    {
        /**
         * The <code>COLOCATED</code> strategy ensures that
         * all work (like {@link Command}s) are managed in the
         * JVM where the {@link Context} is being managed.
         * <p>
         * This has the benefit of minimizing network traffic to
         * execute work (meaning higher-performance), but means more
         * JVM tuning needs to occur ensure all work (like {@link Command}s)
         * can co-exist in the same JVM with the {@link Context}.
         */
        COLOCATED,

        /**
         * The <code>DISTRIBUTED</code> strategy ensures that
         * all work (like {@link Command}s) are stored and
         * managed in a distributed manner across the available
         * resources (ie: clustered JVMs) instead of being "colocated".
         * <p>
         * This has the benefit of increasing the amount of work
         * that may be submitted for execution (meaning greater capacity),
         * but also means lower performance as it introduces more
         * network traffic between JVMs.
         */
        DISTRIBUTED;
    }


    /**
     * Returns the {@link ManagementStrategy} for the
     * {@link Context} being configured.
     */
    public ManagementStrategy getManagementStrategy();
}
