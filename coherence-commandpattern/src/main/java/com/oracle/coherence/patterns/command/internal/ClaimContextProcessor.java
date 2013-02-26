/*
 * File: ClaimContextProcessor.java
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

import com.oracle.coherence.patterns.command.Command;
import com.oracle.coherence.patterns.command.Context;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.UID;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link ClaimContextProcessor} is responsible for "claiming"
 * a {@link Context} for execution with a {@link CommandExecutor}.
 * <p>
 * This enables us to determine during {@link CommandExecutionRequest}
 * execute if another thread, possibly on another Member, has also claimed
 * the said {@link ContextWrapper}.  In turn this allows us to fail-fast,
 * and stop processing on the original {@link CommandExecutor} thread
 * to avoid where possible, multiple execution of a {@link Command}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class ClaimContextProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link UID} of the {@link Member} on which we should be claiming the {@link Context}.
     */
    private UID memberUID;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}
     */
    public ClaimContextProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param memberUID The {@link UID} of the {@link Member} on which we should be claiming the {@link Context}.
     */
    public ClaimContextProcessor(UID memberUID)
    {
        this.memberUID = memberUID;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (entry.isPresent() && CacheFactory.getCluster().getLocalMember().getUid().equals(memberUID))
        {
            ContextWrapper contextWrapper = (ContextWrapper) entry.getValue();

            contextWrapper.nextVersion();
            entry.setValue(contextWrapper);

            return contextWrapper;
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
        this.memberUID = new UID(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        memberUID.save(out);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.memberUID = new UID(reader.readString(0));
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, memberUID.toString());
    }
}
