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

import com.oracle.coherence.common.cluster.ClusterMetaInfo;
import com.oracle.coherence.common.cluster.LocalClusterMetaInfo;
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
 * An EntryProcessor that updates the value a site contributes to a multi-site
 * sum.
 * <p/>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 */
public class ActiveActiveUpdaterProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The value to update for the site in which the processor executes.
     */
    private long m_value;


    /**
     * Constructs an {@link ActiveActiveUpdaterProcessor}.
     */
    public ActiveActiveUpdaterProcessor()
    {
    }


    /**
     * Constructs an {@link ActiveActiveUpdaterProcessor}.
     *
     * @param value  the value to update for the site
     */
    public ActiveActiveUpdaterProcessor(long value)
    {
        m_value = value;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(final Entry entry)
    {
        ActiveActiveSum sum;

        if (entry.isPresent()) {
            sum = (ActiveActiveSum)entry.getValue();
        } else {
            sum = new ActiveActiveSum();
        }

        //determine the local site information
        ClusterMetaInfo clusterMetaInfo = LocalClusterMetaInfo.getInstance();

        //update the contributed value for the local site
        sum.setValue(clusterMetaInfo.getUniqueName(), m_value);

        entry.setValue(sum);

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_value = reader.readLong(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, m_value);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput reader) throws IOException
    {
        m_value = reader.readLong();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        writer.writeLong(m_value);
    }
}
