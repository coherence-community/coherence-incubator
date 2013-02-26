/*
 * File: CoalescingEventIteratorTransformerBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.transformers;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

/**
 * A {@link CoalescingEventIteratorTransformerBuilder} is {@link ParameterizedBuilder} for
 * {@link CoalescingEventIteratorTransformer}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class CoalescingEventIteratorTransformerBuilder extends AbstractEventIteratorTransformerBuilder
{
    /**
     * Standard Constructor (required for {@link PortableObject} and {@link ExternalizableLite}).
     */
    public CoalescingEventIteratorTransformerBuilder()
    {
        // deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    public EventIteratorTransformer realize(ParameterProvider parameterProvider)
    {
        return new CoalescingEventIteratorTransformer();
    }
}
