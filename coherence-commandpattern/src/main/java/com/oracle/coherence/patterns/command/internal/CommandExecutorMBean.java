/*
 * File: CommandExecutorMBean.java
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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.ticketing.Ticket;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.internal.CommandExecutor.State;

/**
 * The {@link CommandExecutorMBean} specifies the JMX monitoring
 * and managability interface for {@link CommandExecutor}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface CommandExecutorMBean
{
    /**
     * Returns the {@link Context} {@link Identifier} as a string.
     */
    public String getContextIdentity();


    /**
     * Returns the version number of the {@link Context} that the {@link CommandExecutor}
     * is managing.
     */
    public long getContextVersion();


    /**
     * Returns the current id that is being used to issue {@link Ticket}s.
     * <p>
     * NOTE: If another {@link CommandExecutor} is created for same
     * {@link Context} with the same {@link Identifier} in the cluster (due
     * to recovery / load-balancing of {@link Context}s), a <b>new</b> {@link Ticket}
     * issuer id will be allocated to the new {@link CommandExecutor}.
     * This ensures that for each instance of a {@link CommandExecutor}, the
     * {@link Ticket} issuer id will be different, thus allowing us to "order"
     * {@link Command}s send to particular {@link CommandExecutor}s.
     */
    public long getTicketIssuerId();


    /**
     * Returns the current {@link State} of the {@link CommandExecutor}.
     */
    public String getStatus();


    /**
     * Returns the total number of {@link Command}s that are yet to be executed
     */
    public long getTotalCommandsPendingExecution();


    /**
     * Returns the total number of {@link Command}s for the {@link Context}
     * that have been executed (regardless of the owner)
     */
    public long getTotalCommandsExecuted();


    /**
     * Returns the total time (in milliseconds) that the
     * {@link Command}s executed thus far have taken to execute.
     */
    public long getTotalCommandExecutionDuration();


    /**
     * Returns the total time (in milliseconds) that the
     * {@link Command}s executed thus far have waited to execute (ie: the queuing time).
     */
    public long getTotalCommandExecutionWaitingDuration();


    /**
     * Returns the number of {@link Command}s that have been submitted locally to the
     * current owner of the {@link Context} (ie: local {@link CommandExecutor}).
     */
    public long getLocalCommandsSubmitted();


    /**
     * Returns the number of {@link Command}s that have been executed locally by the
     * current owner of the {@link Context} (ie: local {@link CommandExecutor}).
     */
    public long getLocalCommandsExecuted();


    /**
     * Returns the average execution time (in milliseconds)
     * for the {@link Command}s executed locally by the current owner of the {@link Context}
     * (ie: local {@link CommandExecutor}).
     */
    public double getLocalAverageCommandExecutionDuration();


    /**
     * Returns the minimum time (in milliseconds) that a {@link Command} has taken
     * to execute locally by the current owner of the {@link Context} (ie: the local {@link CommandExecutor})
     */
    public double getLocalMinimumCommandExecutionDuration();


    /**
     * Returns the maximum time (in milliseconds) that a {@link Command} has taken
     * to execute locally by the current owner of the {@link Context} (ie: the local {@link CommandExecutor})
     */
    public double getLocalMaximumCommandExecutionDuration();


    /**
     * Returns the execution time (in milliseconds) for the last {@link Command} executed
     * locally by the current owner of the {@link Context} (ie: the local {@link CommandExecutor})
     */
    public double getLocalLastCommandExecutionDuration();


    /**
     * Returns the local command execution service duration (in milliseconds). This includes
     * the time to get the command as well as the time to execute the command.
     */
    public double getLocalCommandExecutionServiceDuration();
}
