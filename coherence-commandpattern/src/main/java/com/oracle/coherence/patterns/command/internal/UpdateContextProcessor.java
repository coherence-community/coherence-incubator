/*
 * File: UpdateContextProcessor.java
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

import com.oracle.coherence.common.ticketing.Ticket;
import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link UpdateContextProcessor} is used to update the state
 * of a {@link Context} within a {@link ContextWrapper} (if and only if
 * the specified version matches the {@link Context} version) after
 * a {@link Command} has been executed.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class UpdateContextProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The version of the {@link Context} we are expecting.
     */
    private long contextVersion;

    /**
     * The new {@link Context} value (null means don't update)
     */
    private Context context;

    /**
     * The {@link Ticket} just executed.
     */
    private Ticket lastExecutedTicket;

    /**
     * The time (in milliseconds) that the last executed {@link Command} took to execute.
     */
    private long executionDuration;

    /**
     * The time (in milliseconds) that the last executed {@link Command} was waiting to be executed.
     */
    private long waitingDuration;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}
     */
    public UpdateContextProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param contextVersion
     * @param context (if null, the context value won't be updated).
     * @param lastExecutedTicket
     * @param executionDuration The time (in milliseconds) that the last executed {@link Ticket} took to execute
     * @param waitingDuration The time (in milliseconds) that the last executed {@link Ticket} waited before being executed
     */
    public UpdateContextProcessor(long    contextVersion,
                                  Context context,
                                  Ticket  lastExecutedTicket,
                                  long    executionDuration,
                                  long    waitingDuration)
    {
        this.contextVersion     = contextVersion;
        this.context            = context;
        this.lastExecutedTicket = lastExecutedTicket;
        this.executionDuration  = executionDuration;
        this.waitingDuration    = waitingDuration;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent())
        {
            ContextWrapper contextWrapper = (ContextWrapper) entry.getValue();

            if (contextWrapper.getContextVersion() == contextVersion)
            {
                contextWrapper.updateExecutionInformation(lastExecutedTicket, executionDuration, waitingDuration);

                if (context != null)
                {
                    contextWrapper.setContext(context);
                }

                entry.setValue(contextWrapper);

                return true;
            }
            else
            {
                return false;
            }

        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.contextVersion     = ExternalizableHelper.readLong(in);
        this.context            = (Context) ExternalizableHelper.readObject(in);
        this.lastExecutedTicket = (Ticket) ExternalizableHelper.readExternalizableLite(in);
        this.executionDuration  = ExternalizableHelper.readLong(in);
        this.waitingDuration    = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, contextVersion);
        ExternalizableHelper.writeObject(out, context);
        ExternalizableHelper.writeExternalizableLite(out, lastExecutedTicket);
        ExternalizableHelper.writeLong(out, executionDuration);
        ExternalizableHelper.writeLong(out, waitingDuration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.contextVersion     = reader.readLong(0);
        this.context            = (Context) reader.readObject(1);
        this.lastExecutedTicket = (Ticket) reader.readObject(2);
        this.executionDuration  = reader.readLong(3);
        this.waitingDuration    = reader.readLong(4);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, contextVersion);
        writer.writeObject(1, context);
        writer.writeObject(2, lastExecutedTicket);
        writer.writeLong(3, executionDuration);
        writer.writeLong(4, waitingDuration);
    }
}
