/*
 * File: SubmissionOutcome.java
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
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ContextConfiguration.ManagementStrategy;
import com.oracle.coherence.patterns.command.internal.CommandExecutionRequest.Key;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SubmissionOutcome} represents the result of
 * an attempt to submit a {@link Command} to a {@link Context}
 * (via a {@link CommandExecutor} using a {@link CommandExecutionRequest}).
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see SubmitCommandExecutionRequestProcessor
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class SubmissionOutcome implements ExternalizableLite, PortableObject
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public SubmissionOutcome()
    {
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
    }


    /**
     * An {@link Accepted} {@link SubmissionOutcome} represents
     * that a {@link Command} has been successfully submitted
     * to a {@link Context} and is awaiting execution by a
     * {@link CommandExecutor}.
     */
    public static class Accepted extends SubmissionOutcome implements Identifier
    {
        /**
         * The {@link Key} that was created for the {@link Command}
         * that was submitted.  This may be used (by clients) to
         * cancel execution of a pending {@link Command}.
         */
        private CommandExecutionRequest.Key commandExecutionRequestKey;

        /**
         * The {@link ManagementStrategy} that was used to store
         * the accepted {@link Command}.
         */
        private ManagementStrategy managementStrategy;


        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public Accepted()
        {
        }


        /**
         * Standard Constructor.
         *
         * @param commandExecutionRequestKey
         */
        public Accepted(CommandExecutionRequest.Key commandExecutionRequestKey,
                        ManagementStrategy          managementStrategy)
        {
            this.commandExecutionRequestKey = commandExecutionRequestKey;
            this.managementStrategy         = managementStrategy;
        }


        /**
         * Returns the {@link Key} that was created for the submitted
         * {@link Command}.
         */
        public CommandExecutionRequest.Key getCommandExecutionRequestKey()
        {
            return commandExecutionRequestKey;
        }


        /**
         * Returns the {@link ManagementStrategy} that was used to store
         * the {@link Command}.
         */
        public ManagementStrategy getManagementStrategy()
        {
            return managementStrategy;
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            super.readExternal(in);
            this.commandExecutionRequestKey = (CommandExecutionRequest.Key) ExternalizableHelper.readObject(in);
            this.managementStrategy         = ManagementStrategy.valueOf(ExternalizableHelper.readSafeUTF(in));
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            super.writeExternal(out);
            ExternalizableHelper.writeObject(out, commandExecutionRequestKey);
            ExternalizableHelper.writeSafeUTF(out, this.managementStrategy.toString());
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            super.readExternal(reader);
            this.commandExecutionRequestKey = (CommandExecutionRequest.Key) reader.readObject(100);
            this.managementStrategy         = ManagementStrategy.valueOf(reader.readString(101));
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            super.writeExternal(writer);
            writer.writeObject(100, commandExecutionRequestKey);
            writer.writeString(101, managementStrategy.toString());
        }
    }


    /**
     * An {@link UnknownContext} {@link SubmissionOutcome} represents
     * that a {@link Command} has failed being submitted to a
     * {@link Context} as the said {@link Context} does not exist.
     */
    public static class UnknownContext extends SubmissionOutcome
    {
        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public UnknownContext()
        {
        }
    }
}
