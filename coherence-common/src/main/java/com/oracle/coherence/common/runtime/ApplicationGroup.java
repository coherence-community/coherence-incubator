/*
 * File: ApplicationGroup.java
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

package com.oracle.coherence.common.runtime;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link ApplicationGroup} represents a collection of related {@link Application}s at runtime.
 * <p>
 * {@link ApplicationGroup}s are created using {@link ApplicationGroupBuilder}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A>  The type of the {@link Application}
 *
 * @author Brian Oliver
 */
@Deprecated
public interface ApplicationGroup<A extends Application> extends Iterable<A>
{
    /**
     * Destroys all of the {@link Application}s in the {@link ApplicationGroup}.  Upon returning from this method you
     * can safely assume all {@link Application}s in the {@link ApplicationGroup} are no longer running.
     */
    public void destroy();
}
