/*
 * File: JMSEventDistributorBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.distributors.jms;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder;
import com.tangosol.io.Serializer;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link JMSEventDistributorBuilder} is an JMS-based implementation of a {@link EventDistributorBuilder}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class JMSEventDistributorBuilder implements EventDistributorBuilder
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(JMSEventDistributorBuilder.class.getName());

    /**
     * A {@link ParameterizedBuilder} for JMS {@link ConnectionFactory}s that an {@link JMSEventDistributor}
     * will use to make JMS connections.
     */
    private ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder;


    /**
     * Standard Constructor.
     *
     * @throws JMSException When the JMS infrastructure is not available
     */
    public JMSEventDistributorBuilder() throws JMSException
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Using the JMS-based Event Distributor.");
        }

        this.connectionFactoryBuilder = null;
    }


    /**
     * Method description
     *
     * @param connectionFactoryBuilder
     */
    @Property("connection-factory-scheme")
    @Type(ParameterizedBuilder.class)
    @SubType(ConnectionFactory.class)
    @Mandatory
    public void setConnectionFactoryBuilder(ParameterizedBuilder<ConnectionFactory> connectionFactoryBuilder)
    {
        this.connectionFactoryBuilder = connectionFactoryBuilder;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventDistributor realize(String                           symbolicName,
                                    String                           suggestedExternalName,
                                    ParameterizedBuilder<Serializer> serializerBuilder)
    {
        return new JMSEventDistributor(symbolicName,
                                       suggestedExternalName,
                                       serializerBuilder,
                                       connectionFactoryBuilder);
    }
}
