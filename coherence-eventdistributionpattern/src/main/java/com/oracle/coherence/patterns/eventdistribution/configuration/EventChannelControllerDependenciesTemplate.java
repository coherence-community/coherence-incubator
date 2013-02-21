/*
 * File: EventChannelControllerDependenciesTemplate.java
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

package com.oracle.coherence.patterns.eventdistribution.configuration;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.expressions.MacroParameterExpression;
import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.configuration.parameters.ScopedParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController.Mode;
import com.oracle.coherence.patterns.eventdistribution.EventDistributorBuilder;
import com.oracle.coherence.patterns.eventdistribution.EventIteratorTransformer;
import com.oracle.coherence.patterns.eventdistribution.distributors.AbstractEventChannelController;
import com.oracle.coherence.patterns.eventdistribution.distributors.AbstractEventChannelController.DefaultDependencies;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

/**
 * A {@link EventChannelControllerDependenciesTemplate} captures configuration information for establishing
 * {@link com.oracle.coherence.patterns.eventdistribution.EventChannelController.Dependencies}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class EventChannelControllerDependenciesTemplate
    implements ParameterizedBuilder<EventChannelController.Dependencies>
{
    /**
     * The {@link Expression} that when evaluated will yield the name of the {@link EventChannel}.
     */
    private Expression eventChannelNameExpression;

    /**
     * (optional) The name the {@link EventDistributorBuilder} should use for the {@link EventChannelController}
     * when using external resources.  By default this will be {site-name}:{cluster-name}:{distributor-name}:{channel-name}
     */
    private Expression externalNameExpression;

    /**
     * The {@link EventChannelBuilder} for the {@link EventChannelControllerDependenciesTemplate}.
     */
    private ParameterizedBuilder<EventChannel> eventChannelBuilder;

    /**
     * The {@link ParameterizedBuilder} for the {@link EventIteratorTransformer} (may be null).
     */
    private ParameterizedBuilder<EventIteratorTransformer> eventIteratorTransformerBuilder;

    /**
     * The desired starting {@link Mode} of the {@link EventChannelController} constructed from this template.
     */
    private Mode startingMode;

    /**
     * The number of milliseconds to wait between attempts to distribute {@link Event}s.
     */
    private long batchDistributionDelayMS;

    /**
     * The maximum number of {@link Event}s that may be "batched" together in a single batch.
     */
    private int batchSize;

    /**
     * The number of milliseconds a {@link EventChannel} waits before retrying after an error has occurred.
     */
    private long restartDelay;

    /**
     * The number of consecutive failures that may occur with a {@link EventChannel} before it is suspended for
     * distribution.
     */
    private int totalConsecutiveFailuresBeforeSuspending;


    /**
     * Standard Constructor.
     * <p>
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public EventChannelControllerDependenciesTemplate()
    {
        this.externalNameExpression =
            new MacroParameterExpression("{site-name}:{cluster-name}:{distributor-name}:{channel-name}");
        this.startingMode = AbstractEventChannelController.DefaultDependencies.STARTING_MODE_DEFAULT;
        this.batchDistributionDelayMS =
            AbstractEventChannelController.DefaultDependencies.BATCH_DISTRIBUTION_DELAY_DEFAULT;
        this.batchSize = AbstractEventChannelController.DefaultDependencies.BATCH_SIZE_DEFAULT;
        this.restartDelay = AbstractEventChannelController.DefaultDependencies.RESTART_DELAY_DEFAULT;
        this.totalConsecutiveFailuresBeforeSuspending =
            AbstractEventChannelController.DefaultDependencies.TOTAL_CONSECUTIVE_FAILURES_BEFORE_SUSPENDING;
    }


    /**
     * Return the {@link Expression} that will produce the name of the {@link EventChannel}.
     *
     * @return An {@link Expression}
     */
    public Expression getChannelNameExpression()
    {
        return eventChannelNameExpression;
    }


    /**
     * Sets the {@link Expression} that will produce the name of the EventChannel.
     *
     * @param channelNameExpression A {@link String} {@link Expression}
     */
    @Property("channel-name")
    @Type(Expression.class)
    @SubType(String.class)
    @Mandatory
    public void setEventChannelNameExpression(Expression channelNameExpression)
    {
        this.eventChannelNameExpression = channelNameExpression;
    }


    /**
     * Method description
     *
     * @return
     */
    public Expression getExternalNameExpression()
    {
        return externalNameExpression;
    }


    /**
     * Method description
     *
     * @param externalName
     */
    @Property("channel-external-name")
    @Type(Expression.class)
    @SubType(String.class)
    public void setExternalNameExpression(Expression externalName)
    {
        this.externalNameExpression = externalName;
    }


    /**
     * Method description
     *
     * @return
     */
    public Mode getStartingMode()
    {
        return startingMode;
    }


    /**
     * Sets the starting {@link Mode} of a {@link EventChannelController} when realized.
     *
     * @param startingMode The starting {@link Mode}
     */
    @Property("starting-mode")
    public void setStartingMode(Mode startingMode)
    {
        this.startingMode = startingMode;
    }


    /**
     * Method description
     *
     * @return
     */
    public long getBatchDistributionDelay()
    {
        return batchDistributionDelayMS;
    }


    /**
     * Sets the number of milliseconds to wait between attempts to distribute batches of {@link Event}s.
     * <p>
     * NOTE: This value should be greater than zero.  If the value is close to zero, very little batching may occur.
     *
     * @param batchDistributionDelayMS The delay in milliseconds
     */
    @Property("distribution-delay")
    public void setBatchDistributionDelay(long batchDistributionDelayMS)
    {
        this.batchDistributionDelayMS = batchDistributionDelayMS;
    }


    /**
     * Method description
     *
     * @return
     */
    public int getBatchSize()
    {
        return batchSize;
    }


    /**
     * Sets the maximum number of {@link Event}s that may be "batched" together in an individual batch.
     * <p>
     * NOTE: This value should be greater than one.  If the value is one no batching will occur.
     *
     * @param batchSize
     */
    @Property("batch-size")
    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }


    /**
     * Method description
     *
     * @return
     */
    public long getRestartDelay()
    {
        return restartDelay;
    }


    /**
     * Sets the number of milliseconds the {@link EventChannel} should wait before we retry distribution
     * (after an error occurs).
     *
     * @param restartDelay
     */
    @Property("restart-delay")
    public void setRestartDelay(long restartDelay)
    {
        this.restartDelay = restartDelay;
    }


    /**
     * Method description
     *
     * @return
     */
    public int getTotalConsecutiveFailuresBeforeSuspending()
    {
        return totalConsecutiveFailuresBeforeSuspending;
    }


    /**
     * Sets the number of consecutive failures that may occur with a {@link EventChannel} before the
     * {@link EventChannelController} managing the said {@link EventChannel} is suspended
     * (and won't be automatically retried).
     * <p>
     * NOTE 1: Manual intervention is usually required once {@link EventChannelController} has become
     * suspended, typically via JMX.
     * <p>
     * NOTE 2: A value less than 0 means distribution will never be suspended after a failure, just a delay will occur
     * before retrying.
     *
     * @param totalConsecutiveFailuresBeforeSuspending
     */
    @Property("maximum-failures")
    public void setTotalConsecutiveFailuresBeforeSuspending(int totalConsecutiveFailuresBeforeSuspending)
    {
        this.totalConsecutiveFailuresBeforeSuspending = totalConsecutiveFailuresBeforeSuspending;
    }


    /**
     * Returns the {@link ParameterizedBuilder} to create {@link EventChannel}s for
     * the {@link EventChannelControllerDependenciesTemplate}.
     *
     * @return A {@link ParameterizedBuilder} for {@link EventChannel}s.
     */
    public ParameterizedBuilder<EventChannel> getEventChannelBuilder()
    {
        return eventChannelBuilder;
    }


    /**
     * Sets the {@link EventChannelBuilder} for the {@link EventChannelControllerDependenciesTemplate}.
     *
     * @param eventChannelBuilder The {@link EventChannelBuilder} for the {@link EventChannelControllerDependenciesTemplate}
     */
    @Property("channel-scheme")
    @Mandatory
    public void setEventChannelBuilder(ParameterizedBuilder<EventChannel> eventChannelBuilder)
    {
        this.eventChannelBuilder = eventChannelBuilder;
    }


    /**
     * Returns the {@link EventIteratorTransformer} {@link ParameterizedBuilder} for the {@link EventChannelControllerDependenciesTemplate}.
     *
     * @return A {@link EventIteratorTransformer} {@link ParameterizedBuilder}
     */
    public ParameterizedBuilder<EventIteratorTransformer> getEventsTransformerBuilder()
    {
        return eventIteratorTransformerBuilder;
    }


    /**
     * Sets the {@link EventIteratorTransformer} {@link ParameterizedBuilder} for the
     * {@link EventChannelControllerDependenciesTemplate}.
     *
     * @param builder A {@link EventIteratorTransformer} {@link ParameterizedBuilder}
     */
    @Property("transformer-scheme")
    @Type(ParameterizedBuilder.class)
    @SubType(EventIteratorTransformer.class)
    public void setEventIteratorTransformerBuilder(ParameterizedBuilder<EventIteratorTransformer> builder)
    {
        this.eventIteratorTransformerBuilder = builder;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizesClassOf(Class<?>          clazz,
                                   ParameterProvider parameterProvider)
    {
        return EventChannelController.Dependencies.class.isAssignableFrom(clazz);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dependencies realize(ParameterProvider parameterProvider)
    {
        String                   channelName = getChannelNameExpression().evaluate(parameterProvider).getString();

        MutableParameterProvider mutableParameterProvider = new ScopedParameterProvider(parameterProvider);

        mutableParameterProvider.addParameter(new Parameter("channel-name", channelName));

        String externalName = getExternalNameExpression().evaluate(mutableParameterProvider).getString();

        mutableParameterProvider.addParameter(new Parameter("channel-external-name", externalName));

        return new DefaultDependencies(channelName,
                                       externalName,
                                       eventChannelBuilder,
                                       eventIteratorTransformerBuilder,
                                       startingMode,
                                       batchDistributionDelayMS,
                                       batchSize,
                                       restartDelay,
                                       totalConsecutiveFailuresBeforeSuspending);
    }
}
