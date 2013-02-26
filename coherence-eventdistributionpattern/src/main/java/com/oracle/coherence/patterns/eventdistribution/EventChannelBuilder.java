/*
 * File: EventChannelBuilder.java
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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

/**
 * An {@link EventChannelBuilder} is a {@link ParameterizedBuilder} for {@link EventChannel}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventChannelBuilder extends ParameterizedBuilder<EventChannel>, PortableObject, ExternalizableLite
{
    /**
     * Realizes an instance of an {@link EventChannel}.
     *
     * @param parameterProvider The {@link ParameterProvider} to use when evaluating any {@link Expression}s.
     *
     * @return An {@link EventChannel}.
     */
    public EventChannel realize(ParameterProvider parameterProvider);
}
