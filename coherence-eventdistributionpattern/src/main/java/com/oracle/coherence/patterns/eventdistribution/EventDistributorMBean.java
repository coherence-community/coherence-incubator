/*
 * File: EventDistributorMBean.java
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

/**
 * The {@link EventDistributorMBean} specifies the JMX interface for {@link EventDistributor}s.
 * <p>
 * Copyright (c) 2015. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventDistributorMBean
{
    /**
     * Returns the name of the {@link EventDistributor} for the {@link EventChannelController}.
     *
     * @return A {@link String}.
     */
    public String getEventDistributorName();


    /**
     * Requests all {@link EventChannel}s for the {@link EventDistributor}
     * to start sending {@link Event}s.
     *
     * @see EventChannelController#start()
     */
    public void startAll();


    /**
     * Requests all {@link EventChannel}s for the {@link EventDistributor}
     * to suspend sending {@link Event}s.
     *
     * @see EventChannelController#suspend()
     */
    public void suspendAll();


    /**
     * Requests all {@link EventChannel}s for the {@link EventDistributor}
     * to disable sending {@link Event}s.
     *
     * @see EventChannelController#disable()
     */
    public void disableAll();
}
