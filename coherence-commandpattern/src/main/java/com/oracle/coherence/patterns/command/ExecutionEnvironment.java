/*
 * File: ExecutionEnvironment.java
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

package com.oracle.coherence.patterns.command;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.ticketing.Ticket;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

import java.io.Externalizable;
import java.io.Serializable;

/**
 * An {@link ExecutionEnvironment} is provided to a {@link Command}
 * when it is being executed.  The {@link ExecutionEnvironment} is used
 * to provide the "environment" for the {@link Command}, including;
 * <p>
 * <ol>
 *      <li>The {@link Context} in which the execution is taking place</li>
 *
 *      <li>An {@link Identifier} that uniquely represents the
 *          specific execution request. This may be used to assist in
 *          ensuring idempotent {@link Command} execution when an
 *          execution is recovering.</li>
 *
 *      <li>A {@link Ticket} to determine the order in which the
 *          specific {@link Command} execution is taking place.</li>
 *
 *      <li>A flag to determine if the execution is being recovered.</li>
 * </ol>
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface ExecutionEnvironment<C extends Context>
{
    /**
     * Returns the {@link Identifier} of the {@link Context} in which
     * the execution is taking place.
     */
    public Identifier getContextIdentifier();


    /**
     * Returns the {@link Context} in which an execution is taking place.
     */
    public C getContext();


    /**
     * Use this method to update the {@link Context} (as necessary) during
     * the execution of a {@link Command}.
     *
     * @param context
     *
     * @throws IllegalStateException If the context value provided is <code>null</code>
     */
    public void setContext(C context);


    /**
     * Returns the {@link ContextConfiguration} for the {@link Context} in
     * which the execution is taking place.
     */
    public ContextConfiguration getContextConfiguration();


    /**
     * Returns the unique {@link Ticket} (within the scope of the
     * {@link Context}) issued to the execution taking place with this
     * {@link ExecutionEnvironment}.
     * <p>
     * This {@link Ticket} may be used to order and compare previous
     * executions against the same {@link Context}.
     */
    public Ticket getTicket();


    /**
     * Returns if the {@link ExecutionEnvironment} is recovering due to
     * either a Coherence Cluster Recovery or Repartitioning event.
     */
    public boolean isRecovering();


    /**
     * Determine if state has been previously saved (called a "checkpoint") for
     * the currently executing {@link Command}.
     */
    public boolean hasCheckpoint();


    /**
     * Immediately save the specified state as a "checkpoint" during the execution
     * of a {@link Command} so that it may later be retrieved (via {@link #loadCheckpoint()}).
     * <p>
     * Using "checkpoints" can simplify the recovery of previous attempts at executing
     * a {@link Command}.
     * <p>
     * NOTE: The state must be serializable in some manner, ie: with {@link Serializable},
     * {@link Externalizable}, {@link ExternalizableLite} and/or {@link PortableObject}.
     *
     * @param state
     */
    public void saveCheckpoint(Object state);


    /**
     * Load the previously saved "checkpoint" state for the currently executing {@link Command}
     * that was stored using {@link #saveCheckpoint(Object)}.
     *
     * @return <code>null</code> if there is no previously saved "checkpoint" state.
     */
    public Object loadCheckpoint();


    /**
     * Immediately removes any previously saved "checkpoint" state for the currently executing
     * {@link Command}.
     *
     * NOTE: This is functionally equivalent to {@link #saveCheckpoint(Object)} with <code>null</code>.
     */
    public void removeCheckpoint();
}
