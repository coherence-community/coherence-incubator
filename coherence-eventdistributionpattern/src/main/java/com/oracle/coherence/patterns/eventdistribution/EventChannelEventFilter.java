/*
 * File: EventChannelEventFilter.java
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

import com.oracle.coherence.common.cluster.ClusterMetaInfo;

import com.oracle.coherence.common.events.Event;

import java.util.Iterator;

/**
 * An {@link EventChannelEventFilter} provides a mechanism to filter {@link Event}s prior to them
 * being dispatched to an {@link EventChannel}.  Typically {@link EventChannel}s perform their
 * own filtering through the use of {@link EventTransformer}s, however there are circumstances
 * where filtering may be required outside of the capabilities provided by {@link EventChannel}s
 * themselves.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see EventChannel
 * @see EventChannelBuilder
 *
 * @author Brian Oliver
 */
public interface EventChannelEventFilter
{
    /**
     * Determines if the specified {@link Event} should be dispatched for an {@link EventChannel}.
     *
     * @param event   The {@link com.oracle.coherence.common.events.Event} to conditionally accept
     *                for distribution to an {@link EventChannel}
     * @param source  The {@link ClusterMetaInfo} for the source of the {@link Event}
     *                (or <code>null</code> if unknown)
     * @param local   The {@link ClusterMetaInfo} for the local cluster
     *                (never <code>null</code>)
     */
    public boolean accept(Event           event,
                          ClusterMetaInfo source,
                          ClusterMetaInfo local);
}
