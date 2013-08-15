/*
 * File: TraceCommand.java
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

package com.oracle.coherence.patterns.functor.examples;

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
 * The {@link TraceCommand} will write a message to the Coherence log file.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"serial"})
public class TraceCommand implements Command, ExternalizableLite, PortableObject
{
    /**
     * The trace message.
     */
    private String m_message;


    /**
     * Constructs a {@link TraceCommand}.
     */
    public TraceCommand()
    {
    }


    /**
     * Constructs a {@link TraceCommand}.
     *
     *
     * @param message   the message to log
     * @param severity  the severity of the logger to use
     */
    public TraceCommand(String message)
    {
        m_message = message;
    }


    /**
     * {@inheritDoc}
     */
    public void execute(ExecutionEnvironment executionEnvironment)
    {
        System.out.printf("TraceCommand: ContextIdentifier=%s, Context=%s, Message=%s\n",
                          executionEnvironment.getContextIdentifier(),
                          executionEnvironment.getContext(),
                          m_message);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        m_message = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, m_message);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_message = reader.readString(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, m_message);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("TraceCommand{%s, message=%s}", super.toString(), m_message);
    }
}
