/*
 * File: ContextWrapper.java
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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.common.ticketing.Ticket;

import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ContextConfiguration;
import com.oracle.coherence.patterns.command.DefaultContextsManager;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link ContextWrapper} is used to wrap and provide internal
 * execution information about an individual {@link Context}, including
 * how to identify the {@link Context}, the cluster member on which
 * the {@link Context} was originally created and the {@link Ticket} of
 * the last successfully executed {@link Command} (so we can know where
 * to recover from in case of fail-over, load-balancing or restart.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class ContextWrapper implements ExternalizableLite, PortableObject
{
    /**
     * The Coherence cache in which {@link ContextWrapper}s will be placed.
     */
    public static final String CACHENAME = "coherence.commandpattern.contexts";

    /**
     * The {@link Identifier} of the {@link Context}.
     */
    private Identifier m_ctxIdentifier;

    /**
     * The {@link Context} that is being wrapped.
     */
    private Context m_context;

    /**
     * The {@link ContextConfiguration} for the {@link Context} being wrapped.
     */
    private ContextConfiguration m_ctxConfiguration;

    /**
     * The version of a {@link ContextWrapper} is used to isolate and manage
     * the Coherence Cluster Member that "owns" a {@link Context}.  The principle
     * is to provide "versioning" of a {@link Context} when it moves between Members
     * (due to scale out or partition load-balancing).
     */
    private long m_ctxVersion;

    /**
     * The {@link Ticket} of the last successfully executed {@link Command}.
     */
    private Ticket m_lastExecutedTicket;

    /**
     * The total number of {@link Command}s that have been executed.
     */
    private long m_totalCommandsExecuted;

    /**
     * The total amount of time (in milliseconds) that {@link Command}s
     * have taken to execute.  This is the sum of
     * individual {@link Command} execution times.
     */
    private long m_totalCommandExecutionDuration;

    /**
     * The total amount of time (in milliseconds) that {@link Command}s
     * have waited before they have been executed.  This is the sum of
     * individual {@link Command} waiting times (in the queue).
     */
    private long m_totalCommandExecutionWaitingDuration;


    /**
     * Required for {@link ExternalizableLite}.
     */
    public ContextWrapper()
    {
    }


    /**
     * Standard Constructor.  Used by the {@link DefaultContextsManager}
     * class when creating {@link Context}s.
     *
     * @param ctxIdentifier
     * @param context
     */
    public ContextWrapper(Identifier ctxIdentifier,
                          Context context,
                          ContextConfiguration contextConfiguration)
    {
        m_ctxIdentifier                        = ctxIdentifier;
        m_context                              = context;
        m_ctxConfiguration                     = contextConfiguration;
        m_ctxVersion                           = 0;
        m_lastExecutedTicket                   = Ticket.NONE;
        m_totalCommandsExecuted                = 0;
        m_totalCommandExecutionDuration        = 0;
        m_totalCommandExecutionWaitingDuration = 0;
    }


    /**
     * Returns the unique {@link Identifier} that is used to represent the {@link Context}.
     */
    public Identifier getContentIdentifier()
    {
        return m_ctxIdentifier;
    }


    /**
     * Return the {@link Context} being wrapped (ie: managed).
     */
    public Context getContext()
    {
        return m_context;
    }


    /**
     * Replace the {@link Context} being wrapped (ie: managed).
     */
    public void setContext(Context context)
    {
        m_context = context;
    }


    /**
     * Returns the {@link ContextConfiguration} for the {@link Context} being wrapped.
     */
    public ContextConfiguration getContextConfiguration()
    {
        return m_ctxConfiguration;
    }


    /**
     * Return the current version number of the {@link ContextWrapper}.
     */
    public long getContextVersion()
    {
        return m_ctxVersion;
    }


    /**
     * Increments, stores and returns the next version of the {@link Context}.
     */
    public long nextVersion()
    {
        return ++m_ctxVersion;
    }


    /**
     * Returns the {@link Ticket} of the last successfully executed {@link Command}
     * for the {@link Context}.
     */
    public Ticket getLastExecutedTicket()
    {
        return m_lastExecutedTicket;
    }


    /**
     * Returns the total number of {@link Command}s that have been executed
     * for the {@link Context}.
     */
    public long getTotalCommandsExecuted()
    {
        return m_totalCommandsExecuted;
    }


    /**
     * Returns the total time (in milliseconds) that the
     * {@link Command}s executed thus far have taken to execute.
     */
    public long getTotalCommandExecutionDuration()
    {
        return m_totalCommandExecutionDuration;
    }


    /**
     * Returns the total time (in milliseconds) that the
     * {@link Command}s executed thus far have waited to execute (ie: the queuing time).
     */
    public long getTotalCommandExecutionWaitingDuration()
    {
        return m_totalCommandExecutionWaitingDuration;
    }


    /**
     * Updates the execution information for the {@link Context}
     * tracked by the {@link ContextWrapper} after a {@link Command}
     * has been executed.
     *
     * @param ticket
     * @param executionDuration
     * @param waitingDuration
     */
    public void updateExecutionInformation(Ticket ticket,
                                           long executionDuration,
                                           long waitingDuration)
    {
        m_lastExecutedTicket = ticket;
        m_totalCommandsExecuted++;
        m_totalCommandExecutionDuration        += executionDuration;
        m_totalCommandExecutionWaitingDuration += waitingDuration;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        m_ctxIdentifier                        = (Identifier) ExternalizableHelper.readObject(in);
        m_context                              = (Context) ExternalizableHelper.readObject(in);
        m_ctxConfiguration                     = (ContextConfiguration) ExternalizableHelper.readObject(in);
        m_ctxVersion                           = ExternalizableHelper.readLong(in);
        m_lastExecutedTicket                   = (Ticket) ExternalizableHelper.readExternalizableLite(in);
        m_totalCommandsExecuted                = ExternalizableHelper.readLong(in);
        m_totalCommandExecutionDuration        = ExternalizableHelper.readLong(in);
        m_totalCommandExecutionWaitingDuration = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, m_ctxIdentifier);
        ExternalizableHelper.writeObject(out, m_context);
        ExternalizableHelper.writeObject(out, m_ctxConfiguration);
        ExternalizableHelper.writeLong(out, m_ctxVersion);
        ExternalizableHelper.writeExternalizableLite(out, m_lastExecutedTicket);
        ExternalizableHelper.writeLong(out, m_totalCommandsExecuted);
        ExternalizableHelper.writeLong(out, m_totalCommandExecutionDuration);
        ExternalizableHelper.writeLong(out, m_totalCommandExecutionWaitingDuration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_ctxIdentifier                        = (Identifier) reader.readObject(0);
        m_context                              = (Context) reader.readObject(1);
        m_ctxConfiguration                     = (ContextConfiguration) reader.readObject(2);
        m_ctxVersion                           = reader.readLong(3);
        m_lastExecutedTicket                   = (Ticket) reader.readObject(4);
        m_totalCommandsExecuted                = reader.readLong(5);
        m_totalCommandExecutionDuration        = reader.readLong(6);
        m_totalCommandExecutionWaitingDuration = reader.readLong(7);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, m_ctxIdentifier);
        writer.writeObject(1, m_context);
        writer.writeObject(2, m_ctxConfiguration);
        writer.writeLong(3, m_ctxVersion);
        writer.writeObject(4, m_lastExecutedTicket);
        writer.writeLong(5, m_totalCommandsExecuted);
        writer.writeLong(6, m_totalCommandExecutionDuration);
        writer.writeLong(7, m_totalCommandExecutionWaitingDuration);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String
            .format("ContextWrapper{contextIdentifier=%s, context=%s, contextConfiguration=%s, contextVersion=%d, lastExecutedTicket=%s, totalCommandsExecuted=%d, "
                    + "totalCommandExecutionDuration=%d, totalCommandExecutionWaitingDuration=%d}",
                    m_ctxIdentifier,
                    m_context,
                    m_ctxConfiguration,
                    m_ctxVersion,
                    m_lastExecutedTicket,
                    m_totalCommandsExecuted,
                    m_totalCommandExecutionDuration,
                    m_totalCommandExecutionWaitingDuration);
    }
}
