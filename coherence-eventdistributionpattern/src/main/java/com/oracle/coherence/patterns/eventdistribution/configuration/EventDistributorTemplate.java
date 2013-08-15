/*
 * File: EventDistributorTemplate.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.eventdistribution.configuration;


import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.common.expression.SerializableParameter;
import com.oracle.coherence.common.expression.SerializableParameterMacroExpression;
import com.oracle.coherence.common.expression.SerializableScopedParameterResolver;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.Serializer;
import com.tangosol.net.GuardSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link EventDistributorTemplate} captures the necessary information to define and create an
 * {@link EventDistributor}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventDistributorTemplate implements ParameterizedBuilder<EventDistributor>
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(EventDistributorTemplate.class.getName());

    /**
     * An {@link Expression} that when resolved will produce a suitable {@link EventDistributor} symbolic name.
     */
    private Expression<String> m_exprDistributorName;

    /**
     * An {@link Expression} that when resolved will produce a suitable {@link EventDistributor} external name.
     */
    private Expression<String> m_exprDistributorExternalName;

    /**
     * The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder} to construct the {@link EventDistributor}.
     */
    private com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder m_bldrDistributor;

    /**
     * The {@link ParameterizedBuilder} that will produce {@link Serializer}s for the {@link EventDistributor}.
     */
    private ParameterizedBuilder<Serializer> m_bldrSerializer;

    /**
     * The {@link Collection} of {@link EventChannelControllerDependenciesTemplate} that will be used to construct
     * {@link EventChannelController}s with {@link EventChannel}s.
     */
    private List<EventChannelControllerDependenciesTemplate> m_listChannelControllerDependenciesTemplates;


    /**
     * Standard Constructor.
     */
    public EventDistributorTemplate()
    {
        m_exprDistributorName = new SerializableParameterMacroExpression<String>("{cache-name}", String.class);
        m_exprDistributorExternalName = new SerializableParameterMacroExpression<String>("{cache-name}", String.class);
        m_listChannelControllerDependenciesTemplates = new ArrayList<EventChannelControllerDependenciesTemplate>();
        m_bldrSerializer                             = null;
    }


    /**
     * Determine the identity of the {@link EventDistributorTemplate}.  This will be used to refer to the
     * {@link EventDistributorTemplate} by other components and integrations.
     *
     * @return A {@link String}
     */
    public String getTemplateId()
    {
        return m_exprDistributorName.toString();
    }


    /**
     * Sets the {@link Expression} to be used to resolve the {@link EventDistributor} symbolic name.
     *
     * @param expression An {@link Expression}.
     */
    @Injectable
    public void setDistributorName(Expression expression)
    {
        m_exprDistributorName = SerializableExpressionHelper.ensureSerializable(
                expression);
    }


    /**
     * Sets the {@link Expression} to be used to resolve the {@link EventDistributor} external name.
     *
     * @param expression An {@link Expression}.
     */
    @Injectable
    public void setDistributorExternalName(Expression expression)
    {
        m_exprDistributorExternalName = SerializableExpressionHelper.ensureSerializable(
                expression);
        ;
    }


    /**
     * Sets the {@link com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder} for the {@link EventDistributorTemplate}.
     *
     * @param builder The {@link com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder}.
     */
    @Injectable("distributor-scheme")
    public void setEventDistributorBuilder(com.oracle.coherence.patterns.eventdistribution
        .EventDistributorBuilder builder)
    {
        m_bldrDistributor = builder;
    }


    /**
     * Method description
     *
     * @param templates
     */
    @Injectable("distribution-channels")
    public void setEventChannelControllerDependenciesTemplates(List<EventChannelControllerDependenciesTemplate> templates)
    {
        m_listChannelControllerDependenciesTemplates = templates;
    }


    /**
     * Determines the symbolic name of the {@link EventDistributor} that will be created by this template
     * given the specified {@link ParameterResolver}.
     *
     * @param parameterResolver The {@link ParameterResolver} used to resolve parameters.
     *
     * @return The name of the {@link EventDistributor}.
     */
    public String getDistributorName(ParameterResolver parameterResolver)
    {
        return m_exprDistributorName.evaluate(parameterResolver);
    }


    /**
     * Sets the {@link ParameterizedBuilder} of the {@link Serializer} to be used for {@link EventDistributor}s.
     *
     * @param serializerBuilder The {@link ParameterizedBuilder} for the {@link Serializer}.
     */
    @Injectable("serializer")
    public void setSerializerBuilder(ParameterizedBuilder<Serializer> serializerBuilder)
    {
        m_bldrSerializer = serializerBuilder;
    }


    /**
     * Determines the {@link ParameterizedBuilder} for the {@link Serializer}.
     *
     * @return {@link ParameterizedBuilder} for a {@link Serializer}.
     */
    public ParameterizedBuilder<Serializer> getSerializerBuilder()
    {
        return m_bldrSerializer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EventDistributor realize(ParameterResolver parameterResolver,
                                    ClassLoader       loader,
                                    ParameterList     parameterList)
    {
        String externalName = m_exprDistributorExternalName.evaluate(parameterResolver);

        // resolve the event distributor names
        String distributorName = getDistributorName(parameterResolver);

        // establish the event distributor
        GuardSupport.heartbeat();

        EventDistributor distributor = m_bldrDistributor.realize(distributorName,
                                                                 externalName,
                                                                 m_bldrSerializer,
                                                                 loader);

        GuardSupport.heartbeat();

        // establish the event channels for the event distributor using the event channel controller dependencies
        SerializableScopedParameterResolver scopedResolver = new SerializableScopedParameterResolver(parameterResolver);

        scopedResolver.add(new SerializableParameter("distributor-name",
                                                     distributor.getIdentifier().getSymbolicName()));
        scopedResolver.add(new SerializableParameter("distributor-external-name",
                                                     distributor.getIdentifier().getExternalName()));

        for (EventChannelControllerDependenciesTemplate template : m_listChannelControllerDependenciesTemplates)
        {
            GuardSupport.heartbeat();

            EventChannelController.Dependencies dependencies = template.realize(scopedResolver, loader, parameterList);

            if (logger.isLoggable(Level.CONFIG))
            {
                logger.config(String
                    .format("Establishing Event Channel [%s] for Event Distributor [%s (%s)] based on [%s].",
                            dependencies.getChannelName(), distributorName, externalName, dependencies));
            }

            distributor.establishEventChannelController(dependencies, scopedResolver, loader);
        }

        GuardSupport.heartbeat();

        return distributor;
    }
}
