/*
 * File: StdErrEventChannelBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

/**
 * A {@link StdErrEventChannelBuilder} is an {@link EventChannelBuilder} for the {@link StdErrEventChannel}s
 * that may be configured through dependency injection.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @auuthor Brian Oliver
 */
@SuppressWarnings("serial")
public class StdErrEventChannelBuilder extends AbstractEventChannelBuilder
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public StdErrEventChannelBuilder()
    {
        // SKIP: deliberately empty
    }


    /**
     * {@inheritDoc}
     */
    public EventChannel realize(ParameterProvider parameterProvider)
    {
        return new StdErrEventChannel();
    }
}
