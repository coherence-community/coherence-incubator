/*
 * File: StartCommandProcessor.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link StartCommandProcessor} is responsible for setting the status
 * of a command to the Started state.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
@SuppressWarnings("serial")
public class StartCommandProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}
     */
    public StartCommandProcessor()
    {
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent())
        {
            CommandExecutionRequest request = (CommandExecutionRequest) entry.getValue();

            if (request.isCanceled())
            {
                entry.remove(false);
            }
            else
            {
                request.setStatus(CommandExecutionRequest.Status.Started);
                entry.setValue(request);
            }

            return request;
        }
        else
        {
            return null;
        }
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
}
