/*
 * File: EventChannelControllerBuilder.java
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

import com.oracle.coherence.common.builders.Builder;
import com.oracle.coherence.environment.Environment;

/**
 * A {@link EventChannelControllerBuilder} is a {@link Builder} for {@link EventChannelController}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventChannelControllerBuilder extends Builder<EventChannelController>
{
    /**
     * Realizes an instance of a {@link EventChannelController}.
     *
     * @param distributorIdentifier The {@link EventDistributor.Identifier} in which the
     *                              {@link EventChannelController} should be realized.
     * @param controllerIdentifier  The {@link EventChannelController.Identifier} for the resulting
     *                              {@link EventChannelController}.
     * @param dependencies          The {@link EventChannelController.Dependencies} for the resulting
     *                              {@link EventChannelController}.
     * @param environment           The {@link Environment} in which the {@link EventChannelController} will operate.
     *
     * @return A {@link EventChannelController}
     */
    public EventChannelController realize(EventDistributor.Identifier         distributorIdentifier,
                                          EventChannelController.Identifier   controllerIdentifier,
                                          EventChannelController.Dependencies dependencies,
                                          Environment                         environment);
}
