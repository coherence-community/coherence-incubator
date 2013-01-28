/*
 * File: ActiveActiveRunningSum.java
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * The {@link ActiveActiveRunningSum} is used to compute a running sum
 * of values propagated by this object and contained in the m_latestUpdateValue.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Robert Hanckel
 */
@SuppressWarnings("serial")
public class ActiveActiveRunningSum implements Serializable, ExternalizableLite, PortableObject
{
    /**
     * Latest update value being propagated from one site to another.
     */
    public long m_latestUpdateValue;

    /**
     *  Total of updates aggregated between two sites
     */
    public long m_updateCount;

    /**
     * Total of sums aggregated between two sites
     */
    public long m_runningSum;


    /**
     * Constructs an {@link ActiveActiveRunningSum}.
     */
    public ActiveActiveRunningSum()
    {
    }


    /**
     * Obtains the last updated value.
     *
     * @return  the last updated value
     */
    public long getLatestUpdateValue()
    {
        return m_latestUpdateValue;
    }


    /**
     * Adds a new value to the computed sum.
     *
     * @param newValue
     */
    public void computeNewSum(long newValue)
    {
        m_latestUpdateValue = newValue;
        m_runningSum        = m_runningSum + newValue;
        m_updateCount++;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_latestUpdateValue = reader.readLong(0);
        m_updateCount       = reader.readLong(1);
        m_runningSum        = reader.readLong(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, m_latestUpdateValue);
        writer.writeLong(1, m_updateCount);
        writer.writeLong(2, m_runningSum);
    }


    /**
         * @inheritDoc
         */

    public void readExternal(DataInput reader) throws IOException
    {
        m_latestUpdateValue = reader.readLong();
        m_updateCount       = reader.readLong();
        m_runningSum        = reader.readLong();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        writer.writeLong(m_latestUpdateValue);
        writer.writeLong(m_updateCount);
        writer.writeLong(m_runningSum);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "ActiveActiveRunningSum[LU:" + m_latestUpdateValue + " UC: " + m_updateCount + " RS: " + m_runningSum
               + "]";
    }


    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        ActiveActiveRunningSum runningSumObj = (ActiveActiveRunningSum) obj;

        if (this.m_updateCount == runningSumObj.m_updateCount && this.m_runningSum == runningSumObj.m_runningSum)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
