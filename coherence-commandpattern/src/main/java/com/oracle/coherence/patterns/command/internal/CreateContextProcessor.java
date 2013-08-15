/*
 * File: CreateContextProcessor.java
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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.patterns.command.Context;
import com.oracle.coherence.patterns.command.ContextConfiguration;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link EntryProcessor} that will place an {@link Context}
 * into the {@link ContextWrapper#CACHENAME} Coherence Cache,
 * if it does not already exist.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class CreateContextProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link Context} to be created.
     */
    private Context m_context;

    /**
     * The {@link ContextConfiguration} for the {@link Context}.
     */
    private ContextConfiguration m_ctxConfiguration;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public CreateContextProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param context
     */
    public CreateContextProcessor(Context context,
                                  ContextConfiguration ctxConfiguration)
    {
        m_context          = context;
        m_ctxConfiguration = ctxConfiguration;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        if (!entry.isPresent())
        {
            Identifier     contextIdentifier = (Identifier) entry.getKey();
            ContextWrapper contextWrapper    = new ContextWrapper(contextIdentifier, m_context, m_ctxConfiguration);

            entry.setValue(contextWrapper);
        }

        return entry.getValue();
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        m_context          = (Context) ExternalizableHelper.readObject(in);
        m_ctxConfiguration = (ContextConfiguration) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, m_context);
        ExternalizableHelper.writeObject(out, m_ctxConfiguration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_context          = (Context) reader.readObject(0);
        m_ctxConfiguration = (ContextConfiguration) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, m_context);
        writer.writeObject(1, m_ctxConfiguration);
    }
}
