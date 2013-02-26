/*
 * File: Context.java
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
 * A {@link Context} is an object that captures the shared state that
 * zero or more {@link Command}s may use during execution.
 * <p>
 * As {@link Context}s are cached objects (ie: placed in Coherence caches),
 * they need to at least implement {@link Serializable}, or better still,
 * implement {@link ExternalizableLite} or {@link PortableObject}.
 * <p>
 * {@link Context}s don't need to implement any special methods.  The
 * {@link Context} interface simply marks that an object is considered a
 * {@link Context} against which {@link Command}s may be executed.
 * <p>
 * To register {@link Context}s so that {@link Command}s may be executed
 * against them, use a {@link ContextsManager}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see ContextsManager
 * @see Command
 *
 * @author Brian Oliver
 */

public interface Context
{
    /**
     * As this is a marker interface, there are no declarations.
     */
}
