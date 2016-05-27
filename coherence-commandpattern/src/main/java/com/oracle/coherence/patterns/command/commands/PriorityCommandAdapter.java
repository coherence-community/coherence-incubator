/*
 * File: PriorityCommandAdapter.java
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

package com.oracle.coherence.patterns.command.commands;

import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.oracle.coherence.patterns.command.PriorityCommand;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link PriorityCommandAdapter} turn a regular {@link Command} into
 * a PriorityCommand.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @param <C>
 */
@SuppressWarnings("serial")
public class PriorityCommandAdapter<C extends Context> implements PriorityCommand<C>, ExternalizableLite, PortableObject
{
    /**
     * The underlying {@link Command} to be made into a {@link PriorityCommand}.
     */
    private Command<C> command;


    /**
     * For {@link ExternalizableLite} and {@link PortableObject}.
     */
    public PriorityCommandAdapter()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param command
     */
    public PriorityCommandAdapter(Command<C> command)
    {
        this.command = command;
    }


    /**
     * {@inheritDoc}
     */
    public void execute(ExecutionEnvironment<C> executionEnvironment)
    {
        command.execute(executionEnvironment);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.command = (Command<C>) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, command);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.command = (Command<C>) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, command);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("PriorityCommandAdapter{%s}", command);
    }
}
