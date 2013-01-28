/*
 * File: ActiveActiveComputeSum.java
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

package com.oracle.coherence.patterns.pushreplication.examples.activeactive;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.InvocableMap.Entry;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An EntryProcessor to compute the sum of two values
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class ActiveActiveComputeSum extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    private static final long serialVersionUID = 9195305573455982252L;

    private long              m_latestValue;


    /**
     * Constructs an {@link ActiveActiveComputeSum}.
     */
    public ActiveActiveComputeSum()
    {
    }


    /**
     * Constructs an {@link ActiveActiveComputeSum}.
     *
     * @param value
     */
    public ActiveActiveComputeSum(long value)
    {
        m_latestValue = value;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(final Entry entry)
    {
        if (entry.isPresent() && entry.getValue() instanceof ActiveActiveRunningSum)
        {
            ActiveActiveRunningSum runningSumItem = (ActiveActiveRunningSum) entry.getValue();

            String                 currentKey     = (String) entry.getKey();

            if (CacheFactory.isLogEnabled(CacheFactory.LOG_DEBUG))
            {
                CacheFactory.log("Processing local update of [" + currentKey + "] = [" + runningSumItem.m_runningSum
                                 + "]",
                                 CacheFactory.LOG_DEBUG);

                CacheFactory.log("Adding the following value = [" + m_latestValue + "]", CacheFactory.LOG_DEBUG);

            }

            runningSumItem.computeNewSum(m_latestValue);

            if (CacheFactory.isLogEnabled(CacheFactory.LOG_DEBUG))
            {
                CacheFactory.log("New sum [" + runningSumItem.m_runningSum + "]", CacheFactory.LOG_DEBUG);
            }

            entry.setValue(runningSumItem);
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_latestValue = reader.readLong(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, m_latestValue);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput reader) throws IOException
    {
        m_latestValue = reader.readLong();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        writer.writeLong(m_latestValue);
    }
}
