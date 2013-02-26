/*
 * File: CommandBatch.java
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
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CommandBatch} allows a batch (List) of {@link Command}s to be
 * submitted to a single {@link Context} for execution in an all or nothing mode.
 * <p>
 * NOTE: Any exception caused by a {@link Command} in the {@link CommandBatch} will
 * cause the fail-fast of the processing.  {@link Command}s not yet processed will be discarded.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class CommandBatch<C extends Context> implements Command<C>, ExternalizableLite, PortableObject
{
    /**
     * The list of {@link Command}s to be executed.
     */
    private List<Command<C>> commands;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public CommandBatch()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param commands
     */
    public CommandBatch(List<Command<C>> commands)
    {
        this.commands = commands;
    }


    /**
     * {@inheritDoc}
     */
    public void execute(ExecutionEnvironment<C> executionEnvironment)
    {
        for (Command<C> command : commands)
        {
            command.execute(executionEnvironment);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        int capacity = ExternalizableHelper.readInt(in);

        if (capacity > 0)
        {
            commands = new ArrayList<Command<C>>(capacity);
            ExternalizableHelper.readCollection(in, commands, this.getClass().getClassLoader());
        }
        else
        {
            commands = new ArrayList<Command<C>>();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeInt(out, this.commands.size());

        if (commands.size() > 0)
        {
            ExternalizableHelper.writeCollection(out, commands);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        int capacity = reader.readInt(0);

        if (capacity > 0)
        {
            commands = new ArrayList<Command<C>>(capacity);
            reader.readCollection(1, commands);
        }
        else
        {
            commands = new ArrayList<Command<C>>();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeInt(0, commands.size());

        if (commands.size() > 0)
        {
            writer.writeCollection(1, commands);
        }
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("CommandBatch{commands=%s}", commands);
    }
}
