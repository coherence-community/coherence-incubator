/*
 * File: LoggingCommand.java
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

package com.oracle.coherence.patterns.functor.examples;

import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.ExecutionEnvironment;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link LoggingCommand} will write a message to the Coherence log file.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"serial"})
public class LoggingCommand implements Command, ExternalizableLite, PortableObject
{
    private String message;

    /**
     * The Coherence {@link CacheFactory} logging sensitivity.
     */
    private int severity;


    /**
     * Constructs a {@link LoggingCommand}.
     */
    public LoggingCommand()
    {
    }


    /**
     * Constructs a {@link LoggingCommand}.
     *
     *
     * @param message   the message to log
     * @param severity  the severity of the logger to use
     */
    public LoggingCommand(String message,
                          int    severity)
    {
        this.message  = message;
        this.severity = severity;
    }


    /**
     * {@inheritDoc}
     */
    public void execute(ExecutionEnvironment executionEnvironment)
    {
        Logger.log(severity,
                   "LoggingCommand: ContextIdentifier=%s, Context=%s, Message=%s",
                   executionEnvironment.getContextIdentifier(),
                   executionEnvironment.getContext(),
                   message);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.message  = ExternalizableHelper.readSafeUTF(in);
        this.severity = ExternalizableHelper.readInt(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, message);
        ExternalizableHelper.writeInt(out, severity);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.message  = reader.readString(0);
        this.severity = reader.readInt(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, message);
        writer.writeInt(1, severity);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("LoggingCommand{%s, message=%s, severity=%d}", super.toString(), message, severity);
    }
}
