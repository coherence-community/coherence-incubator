/*
 * File: SetValueCommand.java
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

package com.oracle.coherence.patterns.command.examples;

import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link SetValueCommand} will set the value of a {@link GenericContext} used by the
 * {@link CommandPatternExample}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <T> the type of value to set on the {@link GenericContext}
 */
@SuppressWarnings("serial")
public class SetValueCommand<T> implements Command<GenericContext<T>>, ExternalizableLite, PortableObject
{
    private T value;


    /**
     * Constructs a {@link SetValueCommand}.
     */
    public SetValueCommand()
    {
    }


    /**
     * Constructs a {@link SetValueCommand}.
     *
     *
     * @param value  the value to set
     */
    public SetValueCommand(T value)
    {
        this.value = value;
    }


    /**
     * {@inheritDoc}
     */
    public void execute(ExecutionEnvironment<GenericContext<T>> executionEnvironment)
    {
        GenericContext<T> context = executionEnvironment.getContext();

        context.setValue(value);

        executionEnvironment.setContext(context);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.value = (T) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, value);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.value = (T) reader.readObject(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, value);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("SetValueCommand{value=%s}", value);
    }
}
