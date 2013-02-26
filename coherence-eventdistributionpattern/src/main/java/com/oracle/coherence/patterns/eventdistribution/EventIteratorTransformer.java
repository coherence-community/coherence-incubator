/*
 * File: EventIteratorTransformer.java
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

package com.oracle.coherence.patterns.eventdistribution;

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.util.IteratorTransformer;

import java.util.Iterator;

/**
 * An {@link EventIteratorTransformer} is an {@link IteratorTransformer} designed to transform batches of
 * {@link Event}s (represented by an {@link Iterator}), into another {@link Iterator} over {@link Event}s.
 * <p>
 * The output of the transformation process may include none, some or all of the input {@link Event}s. That is, the
 * transformation process may filter the {@link Event}s and/or produce new {@link Event}s, related or not, to the
 * input {@link Event}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventIteratorTransformer extends IteratorTransformer<Event>
{
    // deliberately empty
}
