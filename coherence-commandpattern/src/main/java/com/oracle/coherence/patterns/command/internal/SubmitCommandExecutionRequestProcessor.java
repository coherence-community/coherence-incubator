/*
 * File: SubmitCommandExecutionRequestProcessor.java
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
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SubmitCommandExecutionRequestProcessor} is used to submit {@link Command}s
 * (represented as {@link CommandExecutionRequest}s) to a {@link CommandExecutor} for
 * execution.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SubmitCommandExecutionRequestProcessor extends AbstractProcessor implements ExternalizableLite,
                                                                                         PortableObject
{
    /**
     * The {@link CommandExecutionRequest} to submit for execution.
     */
    private CommandExecutionRequest commandExecutionRequest;

    /**
     * Signifies that a {@link CommandExecutionRequest} may be accepted (ie: submitted)
     * when the associated {@link Context} has yet to be registered.  This permits
     * {@link Command}s to be submitted before their {@link Context}s are created.
     */
    private boolean acceptCommandIfContextDoesNotExist;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public SubmitCommandExecutionRequestProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param commandExecutionRequest
     * @param acceptIfContextDoesNotExist
     */
    public SubmitCommandExecutionRequestProcessor(CommandExecutionRequest commandExecutionRequest,
                                                  boolean                 acceptIfContextDoesNotExist)
    {
        this.commandExecutionRequest            = commandExecutionRequest;
        this.acceptCommandIfContextDoesNotExist = acceptIfContextDoesNotExist;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        // we need the CommandExecutor so that we can submit the CommandExecutionRequest for execution
        CommandExecutor commandExecutor;
        Identifier      contextIdentifier = commandExecutionRequest.getContextIdentifier();

        if (acceptCommandIfContextDoesNotExist)
        {
            // TODO: The following line needs to be changed when we adopt Coherence 3.5.2 to use the BinaryEntry.getContext()
            BackingMapManagerContext backingMapManagerContext =
                ((DistributedCacheService) CacheFactory.getService("DistributedCacheForCommandPattern"))
                    .getBackingMapManager().getContext();

            commandExecutor = CommandExecutorManager.ensureCommandExecutor(contextIdentifier, backingMapManagerContext);
        }
        else
        {
            commandExecutor = CommandExecutorManager.getCommandExecutor(contextIdentifier);
        }

        // only accept the command execution request if there is a command executor
        if (commandExecutor == null &&!acceptCommandIfContextDoesNotExist)
        {
            return new SubmissionOutcome.UnknownContext();

        }
        else
        {
            // accept the command execution request for execution
            return commandExecutor.acceptCommandExecutionRequest(commandExecutionRequest);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.commandExecutionRequest = (CommandExecutionRequest) ExternalizableHelper.readExternalizableLite(in);
        this.acceptCommandIfContextDoesNotExist = in.readBoolean();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeExternalizableLite(out, commandExecutionRequest);
        out.writeBoolean(acceptCommandIfContextDoesNotExist);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.commandExecutionRequest            = (CommandExecutionRequest) reader.readObject(0);
        this.acceptCommandIfContextDoesNotExist = reader.readBoolean(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, commandExecutionRequest);
        writer.writeBoolean(1, acceptCommandIfContextDoesNotExist);
    }
}
