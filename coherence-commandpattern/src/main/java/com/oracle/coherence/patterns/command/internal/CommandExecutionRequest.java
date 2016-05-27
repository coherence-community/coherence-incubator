/*
 * File: CommandExecutionRequest.java
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
import com.oracle.coherence.patterns.command.CommandSubmitter;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ContextConfiguration.ManagementStrategy;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

/**
 * A {@link CommandExecutionRequest} represents a request to execute
 * a {@link Command} with in a specified {@link Context} (by a
 * {@link CommandExecutor}).
 * <p>
 * Along with the {@link Command}, a {@link CommandExecutionRequest} also
 * contains meta-data about the execution request, including
 * the globally unique {@link Identifier} of the request and the
 * issued {@link Ticket} (by the {@link CommandExecutor})
 * for the purposes of ordering the {@link Command} execution
 * on a first-come-first-served (FIFO) basis.
 * <p>
 * To have a {@link Command} executed, you should use the
 * {@link CommandSubmitter#submitCommand(Identifier, Command)}
 * method.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see Command
 * @see Context
 * @see CommandExecutor
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class CommandExecutionRequest implements ExternalizableLite, PortableObject, Comparable<CommandExecutionRequest>
{
    /**
     * The request can be in one of three states: Pending, Started, or Canceled
     */
    public static enum Status
    {
        /**
         * The request is pending to execute.
         */
        Pending,

        /**
         * The request is currently running.
         */
        Started,

        /**
         * The request has been canceled.
         */
        Canceled
    }


    /**
     * The names of the Coherence Caches that will store
     * {@link CommandExecutionRequest}s.
     */
    private static final String COLOCATED_COMMANDS_CACHENAME   = "coherence.commandpattern.commands.colocated";
    private static final String DISTRIBUTED_COMMANDS_CACHENAME = "coherence.commandpattern.commands.distributed";

    /**
     * The {@link Identifier} of the {@link Context} in which the
     * {@link CommandExecutionRequest} will execute its associated
     * {@link Command}.
     */
    private Identifier contextIdentifier;

    /**
     * The {@link Ticket} issued to the {@link CommandExecutionRequest} by a
     * {@link CommandExecutor} so that we may order the execution of
     * the said {@link CommandExecutionRequest}.
     */
    private Ticket ticket;

    /**
     * The developer provided {@link Command} to be executed by
     * the {@link CommandExecutor}.
     */
    private Command<?> command;

    /**
     * The time (in milliseconds) using cluster time (since the EPOC) when the
     * {@link CommandExecutionRequest} was queued for execution by a
     * {@link CommandExecutor}.  This is to allow us to determine the waiting
     * time for the {@link Command} before it is executed.
     */
    private long instantQueued;

    /**
     * The current status of the request
     */
    private Status status;

    /**
     * The checkpoint (state) that can be used when recovering
     * the execution of a {@link Command}.
     *
     * @see ExecutionEnvironment
     */
    private Object checkpoint;


    /**
     * For {@link ExternalizableLite} and {@link PortableObject}.
     */
    public CommandExecutionRequest()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param contextIdentifier
     * @param command
     */
    public CommandExecutionRequest(Identifier contextIdentifier,
                                   Command<?> command)
    {
        this.contextIdentifier = contextIdentifier;
        this.ticket            = null;
        this.command           = command;
        this.instantQueued     = 0;
        this.status            = Status.Pending;
        this.checkpoint        = null;
    }


    /**
     * Returns the {@link Identifier} of the {@link Context} in which the
     * {@link CommandExecutionRequest} {@link Command}
     * will be executed.
     */
    public Identifier getContextIdentifier()
    {
        return contextIdentifier;
    }


    /**
     * Returns the {@link Ticket} for the {@link CommandExecutionRequest}.
     * The {@link Ticket} is used to determine the order in which the
     * {@link CommandExecutionRequest} {@link Command} will be executed in the
     * {@link Context}.
     */
    public Ticket getTicket()
    {
        return ticket;
    }


    /**
     * Sets the {@link Ticket} for the {@link CommandExecutionRequest}.
     *
     * @param ticket
     */
    public void setTicket(Ticket ticket)
    {
        this.ticket = ticket;
    }


    /**
     * Returns the {@link Command} associated with the {@link CommandExecutionRequest}.
     */
    public Command<?> getCommand()
    {
        return command;
    }


    /**
     * Return the time (in milliseconds) since the EPOC when the
     * {@link CommandExecutionRequest} was queued for execution.
     */
    public long getInstantQueued()
    {
        return instantQueued;
    }


    /**
     * Sets the time (in milliseconds) since the EPOC when the
     * {@link CommandExecutionRequest} was queued for execution.
     */
    public void setInstantQueued(long instantQueued)
    {
        this.instantQueued = instantQueued;
    }


    /**
     * Return the current {@link Status} of this request.
     *
     * @return the current {@link Status} of this request
     */
    public Status getStatus()
    {
        return status;
    }


    /**
     * Set the {@link Status} for this request.
     * @param status  the {@link Status} for this request
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }


    /**
     * Returns if the {@link CommandExecutionRequest} has been canceled
     * and thus should not be executed (if it hasn't already been executed or hasn't started being executed).
     */
    public boolean isCanceled()
    {
        return getStatus() == Status.Canceled;
    }


    /**
     * Returns the current checkpoint for the {@link Command}.
     */
    public Object getCheckpoint()
    {
        return checkpoint;
    }


    /**
     * Sets the current checkpoint for the {@link Command}.
     *
     * @param checkpoint
     */
    public void setCheckpoint(Object checkpoint)
    {
        this.checkpoint = checkpoint;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.contextIdentifier = (Identifier) ExternalizableHelper.readObject(in);
        this.ticket            = (Ticket) ExternalizableHelper.readObject(in);
        this.command           = (Command<?>) ExternalizableHelper.readObject(in);
        this.instantQueued     = ExternalizableHelper.readLong(in);
        this.status            = Status.values()[ExternalizableHelper.readInt(in)];
        this.checkpoint        = ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, contextIdentifier);
        ExternalizableHelper.writeObject(out, ticket);
        ExternalizableHelper.writeObject(out, command);
        ExternalizableHelper.writeLong(out, instantQueued);
        ExternalizableHelper.writeInt(out, status.ordinal());
        ExternalizableHelper.writeObject(out, checkpoint);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.contextIdentifier = (Identifier) reader.readObject(0);
        this.ticket            = (Ticket) reader.readObject(1);
        this.command           = (Command<?>) reader.readObject(2);
        this.instantQueued     = reader.readLong(3);
        this.status            = Status.values()[reader.readInt(4)];
        this.checkpoint        = reader.readObject(5);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, contextIdentifier);
        writer.writeObject(1, ticket);
        writer.writeObject(2, command);
        writer.writeLong(3, instantQueued);
        writer.writeInt(4, status.ordinal());
        writer.writeObject(5, checkpoint);
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(CommandExecutionRequest other)
    {
        return this.getTicket().compareTo(other.getTicket());
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String
            .format("CommandExecutionRequest{contextIdentifier=%s, ticket=%s, command=%s, instantQueued=%s, status=%s, checkpoint=%s}",
                    contextIdentifier,
                    ticket,
                    command,
                    new Date(instantQueued),
                    status,
                    checkpoint);
    }


    /**
     * Returns the {@link NamedCache} for the {@link CommandExecutionRequest}
     * using the specified {@link ManagementStrategy}.
     *
     * @param managementStrategy
     */
    public static String getCacheName(ManagementStrategy managementStrategy)
    {
        return managementStrategy == ManagementStrategy.COLOCATED
               ? COLOCATED_COMMANDS_CACHENAME : DISTRIBUTED_COMMANDS_CACHENAME;
    }


    /**
     * A custom key implementation for the {@link CommandExecutionRequest} class
     * to support {@link KeyAssociation}.
     */
    public static class Key implements ExternalizableLite, PortableObject, KeyAssociation, Identifier
    {
        /**
         * The {@link Identifier} of the {@link Context} in which the {@link CommandExecutionRequest}
         * will be executed.  We require this as part of the {@link Key}
         * to support {@link KeyAssociation}, and thus ensure the {@link CommandExecutionRequest}s
         * for associated {@link Context}s are co-located (in the same cache partition)
         * on the same JVM for execution.
         */
        private Identifier contextIdentifier;

        /**
         * The {@link Ticket} issued to the {@link CommandExecutionRequest} by
         * the {@link CommandExecutor} that accepted the submission of the {@link Command}.
         */
        private Ticket ticket;

        /**
         * The {@link ManagementStrategy} for {@link CommandExecutionRequest}s.
         *
         * May be <code>null</code> if the context has not been configured
         */
        private ManagementStrategy managementStrategy;


        /**
         * For {@link ExternalizableLite}.
         */
        public Key()
        {
        }


        /**
         * Standard Constructor.
         *
         * @param contextIdentifier
         * @param ticket
         * @param managementStrategy
         */
        public Key(Identifier         contextIdentifier,
                   Ticket             ticket,
                   ManagementStrategy managementStrategy)
        {
            this.contextIdentifier  = contextIdentifier;
            this.ticket             = ticket;
            this.managementStrategy = managementStrategy;
        }


        /**
         * Returns the {@link Identifier} for the {@link CommandExecutionRequest}.
         */
        public Identifier getContextIdentifier()
        {
            return contextIdentifier;
        }


        /**
         * {@inheritDoc}
         */
        public Object getAssociatedKey()
        {
            // for COLOCATED management we use the contextIdentifer, otherwise we just use the ticket
            return managementStrategy == ManagementStrategy.COLOCATED ? contextIdentifier : ticket;
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            this.contextIdentifier  = (Identifier) ExternalizableHelper.readObject(in);
            this.ticket             = (Ticket) ExternalizableHelper.readExternalizableLite(in);
            this.managementStrategy = ManagementStrategy.values()[ExternalizableHelper.readInt(in)];
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeObject(out, contextIdentifier);
            ExternalizableHelper.writeExternalizableLite(out, ticket);
            ExternalizableHelper.writeInt(out, managementStrategy.ordinal());
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            this.contextIdentifier  = (Identifier) reader.readObject(0);
            this.ticket             = (Ticket) reader.readObject(1);
            this.managementStrategy = ManagementStrategy.values()[reader.readInt(2)];
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeObject(0, contextIdentifier);
            writer.writeObject(1, ticket);
            writer.writeInt(2, managementStrategy.ordinal());
        }


        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return String.format("CommandExecutionRequest.Key{contextIdentifier=%s, ticket=%s, managementStrategy=%s}",
                                 contextIdentifier,
                                 ticket,
                                 managementStrategy);
        }
    }
}
