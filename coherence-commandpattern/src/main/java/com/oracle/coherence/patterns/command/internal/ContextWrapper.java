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
    private Identifier contextIdentifier;

    /**
     * The {@link Context} that is being wrapped.
     */
    private Context context;

    /**
     * The {@link ContextConfiguration} for the {@link Context} being wrapped.
     */
    private ContextConfiguration contextConfiguration;

    /**
     * The version of a {@link ContextWrapper} is used to isolate and manage
     * the Coherence Cluster Member that "owns" a {@link Context}.  The principle
     * is to provide "versioning" of a {@link Context} when it moves between Members
     * (due to scale out or partition load-balancing).
     */
    private long contextVersion;

    /**
     * The {@link Ticket} of the last successfully executed {@link Command}.
     */
    private Ticket lastExecutedTicket;

    /**
     * The total number of {@link Command}s that have been executed.
     */
    private long totalCommandsExecuted;

    /**
     * The total amount of time (in milliseconds) that {@link Command}s
     * have taken to execute.  This is the sum of
     * individual {@link Command} execution times.
     */
    private long totalCommandExecutionDuration;

    /**
     * The total amount of time (in milliseconds) that {@link Command}s
     * have waited before they have been executed.  This is the sum of
     * individual {@link Command} waiting times (in the queue).
     */
    private long totalCommandExecutionWaitingDuration;


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
     * @param contextIdentifier
     * @param context
     */
    public ContextWrapper(Identifier           contextIdentifier,
                          Context              context,
                          ContextConfiguration contextConfiguration)
    {
        this.contextIdentifier                    = contextIdentifier;
        this.context                              = context;
        this.contextConfiguration                 = contextConfiguration;
        this.contextVersion                       = 0;
        this.lastExecutedTicket                   = Ticket.NONE;
        this.totalCommandsExecuted                = 0;
        this.totalCommandExecutionDuration        = 0;
        this.totalCommandExecutionWaitingDuration = 0;
    }


    /**
     * Returns the unique {@link Identifier} that is used to represent the {@link Context}.
     */
    public Identifier getContentIdentifier()
    {
        return contextIdentifier;
    }


    /**
     * Return the {@link Context} being wrapped (ie: managed).
     */
    public Context getContext()
    {
        return context;
    }


    /**
     * Replace the {@link Context} being wrapped (ie: managed).
     */
    public void setContext(Context context)
    {
        this.context = context;
    }


    /**
     * Returns the {@link ContextConfiguration} for the {@link Context} being wrapped.
     */
    public ContextConfiguration getContextConfiguration()
    {
        return contextConfiguration;
    }


    /**
     * Return the current version number of the {@link ContextWrapper}.
     */
    public long getContextVersion()
    {
        return contextVersion;
    }


    /**
     * Increments, stores and returns the next version of the {@link Context}.
     */
    public long nextVersion()
    {
        return ++contextVersion;
    }


    /**
     * Returns the {@link Ticket} of the last successfully executed {@link Command}
     * for the {@link Context}.
     */
    public Ticket getLastExecutedTicket()
    {
        return lastExecutedTicket;
    }


    /**
     * Returns the total number of {@link Command}s that have been executed
     * for the {@link Context}.
     */
    public long getTotalCommandsExecuted()
    {
        return totalCommandsExecuted;
    }


    /**
     * Returns the total time (in milliseconds) that the
     * {@link Command}s executed thus far have taken to execute.
     */
    public long getTotalCommandExecutionDuration()
    {
        return totalCommandExecutionDuration;
    }


    /**
     * Returns the total time (in milliseconds) that the
     * {@link Command}s executed thus far have waited to execute (ie: the queuing time).
     */
    public long getTotalCommandExecutionWaitingDuration()
    {
        return totalCommandExecutionWaitingDuration;
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
                                           long   executionDuration,
                                           long   waitingDuration)
    {
        this.lastExecutedTicket = ticket;
        this.totalCommandsExecuted++;
        this.totalCommandExecutionDuration        += executionDuration;
        this.totalCommandExecutionWaitingDuration += waitingDuration;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.contextIdentifier                    = (Identifier) ExternalizableHelper.readObject(in);
        this.context                              = (Context) ExternalizableHelper.readObject(in);
        this.contextConfiguration                 = (ContextConfiguration) ExternalizableHelper.readObject(in);
        this.contextVersion                       = ExternalizableHelper.readLong(in);
        this.lastExecutedTicket                   = (Ticket) ExternalizableHelper.readExternalizableLite(in);
        this.totalCommandsExecuted                = ExternalizableHelper.readLong(in);
        this.totalCommandExecutionDuration        = ExternalizableHelper.readLong(in);
        this.totalCommandExecutionWaitingDuration = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, contextIdentifier);
        ExternalizableHelper.writeObject(out, context);
        ExternalizableHelper.writeObject(out, contextConfiguration);
        ExternalizableHelper.writeLong(out, contextVersion);
        ExternalizableHelper.writeExternalizableLite(out, lastExecutedTicket);
        ExternalizableHelper.writeLong(out, totalCommandsExecuted);
        ExternalizableHelper.writeLong(out, totalCommandExecutionDuration);
        ExternalizableHelper.writeLong(out, totalCommandExecutionWaitingDuration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.contextIdentifier                    = (Identifier) reader.readObject(0);
        this.context                              = (Context) reader.readObject(1);
        this.contextConfiguration                 = (ContextConfiguration) reader.readObject(2);
        this.contextVersion                       = reader.readLong(3);
        this.lastExecutedTicket                   = (Ticket) reader.readObject(4);
        this.totalCommandsExecuted                = reader.readLong(5);
        this.totalCommandExecutionDuration        = reader.readLong(6);
        this.totalCommandExecutionWaitingDuration = reader.readLong(7);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, contextIdentifier);
        writer.writeObject(1, context);
        writer.writeObject(2, contextConfiguration);
        writer.writeLong(3, contextVersion);
        writer.writeObject(4, lastExecutedTicket);
        writer.writeLong(5, totalCommandsExecuted);
        writer.writeLong(6, totalCommandExecutionDuration);
        writer.writeLong(7, totalCommandExecutionWaitingDuration);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String
            .format("ContextWrapper{contextIdentifier=%s, context=%s, contextConfiguration=%s, contextVersion=%d, lastExecutedTicket=%s, totalCommandsExecuted=%d, "
                    + "totalCommandExecutionDuration=%d, totalCommandExecutionWaitingDuration=%d}",
                    contextIdentifier,
                    context,
                    contextConfiguration,
                    contextVersion,
                    lastExecutedTicket,
                    totalCommandsExecuted,
                    totalCommandExecutionDuration,
                    totalCommandExecutionWaitingDuration);
    }
}
