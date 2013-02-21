/*
 * File: CoherenceEventDistributorBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.distributors.coherence;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder;
import com.tangosol.io.Serializer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link CoherenceEventDistributorBuilder} is an implementation of the {@link EventDistributorBuilder}
 * that uses Coherence itself to manage {@link Event}s to be distributed.
 * <p>
 * <strong>NOTE:</strong> This implementation depends on the Coherence Messaging Pattern as part of it's implementation.
 * Consequently the Coherence Messaging Pattern must be correctly configured in-order for this
 * {@link EventDistributorBuilder} to operate.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceEventDistributorBuilder implements EventDistributorBuilder
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(CoherenceEventDistributorBuilder.class.getName());


    /**
     * Standard Constructor.
     */
    public CoherenceEventDistributorBuilder()
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Using the Coherence-based Event Distributor.");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventDistributor realize(String                           symbolicName,
                                    String                           suggestedExternalName,
                                    ParameterizedBuilder<Serializer> serializerBuilder)
    {
        return new CoherenceEventDistributor(symbolicName, suggestedExternalName, serializerBuilder);
    }
}
