/*
 * File: TrackingEvent.java
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

package com.oracle.coherence.common.finitestatemachines;

/**
 * A {@link TrackingEvent} is a {@link LifecycleAwareEvent} that tracks
 * the processing of another {@link Event}.
 * <p>
 * Ultimately a {@link TrackingEvent} delegates interactions with
 * a {@link FiniteStateMachine} to the wrapped {@link Event}, during which
 * time is records certain interactions.   These interactions can then be
 * asserted during tests to ensure expected behavior.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>
 *
 * @author Brian Oliver
 */
public class TrackingEvent<S extends Enum<S>> implements LifecycleAwareEvent<S>
{
    /**
     * The {@link Event} that will be tracked.
     */
    private Event<S> m_event;

    /**
     * A flag to indicate if the {@link Event} was accepted.
     */
    private boolean m_wasAccepted;

    /**
     * A flag to indicate if the {@link Event} was evaluated (with
     * {@link #getDesiredState(Enum, SimpleContext)}.
     */
    private boolean m_wasEvaluated;

    /**
     * A flag to indicate if the {@link Event} was processed
     * (with {@link #onProcessed(SimpleContext)}).
     */
    private boolean m_wasProcessed;


    /**
     * Constructs a {@link TrackingEvent}.
     *
     * @param event  the {@link Event} to be tracked
     */
    public TrackingEvent(Event<S> event)
    {
        m_event        = event;
        m_wasAccepted  = false;
        m_wasEvaluated = false;
        m_wasProcessed = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public S getDesiredState(S                currentState,
                             ExecutionContext context)
    {
        m_wasEvaluated = true;

        return m_event.getDesiredState(currentState, context);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onAccept(ExecutionContext context)
    {
        if (m_event instanceof LifecycleAwareEvent)
        {
            m_wasAccepted = ((LifecycleAwareEvent<S>) m_event).onAccept(context);
        }
        else
        {
            // non-lifecycle aware events are always accepted
            m_wasAccepted = true;
        }

        return m_wasAccepted;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onProcessed(ExecutionContext context)
    {
        m_wasProcessed = true;

        if (m_event instanceof LifecycleAwareEvent)
        {
            ((LifecycleAwareEvent<S>) m_event).onProcessed(context);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onProcessing(ExecutionContext context)
    {
        if (m_event instanceof LifecycleAwareEvent)
        {
            ((LifecycleAwareEvent<S>) m_event).onProcessing(context);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("TrackingEvent{%s}", m_event);
    }


    /**
     * Determines if the tracked {@link Event} was accepted for processing.
     *
     * @return if the {@link Event} was accepted for processing
     */
    public boolean accepted()
    {
        return m_wasAccepted;
    }


    /**
     * Determines if the {@link #getDesiredState(Enum, SimpleContext)} was called
     * for the tracked {@link Event}.
     *
     * @return if {@link #getDesiredState(Enum, SimpleContext)} was called for the
     *         tracked {@link Event}
     */
    public boolean evaluated()
    {
        return m_wasEvaluated;
    }


    /**
     * Determines if the tracked {@link Event} was processed.
     *
     * @return if the tracked {@link Event} was processed
     */
    public boolean processed()
    {
        return m_wasProcessed;
    }
}
