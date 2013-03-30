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

package com.oracle.coherence.patterns.pushreplication.examples.activeactive;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.HashMap;

/**
 * The {@link ActiveActiveSum} represents a sum of contributed values from
 * one or more sites.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class ActiveActiveSum implements Serializable, ExternalizableLite, PortableObject
{
    /**
     * A map of values contributed for each site (unique name).
     */
    private HashMap<String, Long> m_siteValues;

    /**
     * The sum of the contribute values.
     */
    private long m_sum;


    /**
     * Constructs an {@link ActiveActiveSum}.
     */
    public ActiveActiveSum()
    {
        m_siteValues = new HashMap<String, Long>();
        m_sum        = 0;
    }


    /**
     * Sets the value contributed from a specified site.
     *
     * @param site  the site
     * @param value the value
     * @return true if the value was changed, false if the value was the same.
     */
    public boolean setValue(String site,
                            long   value)
    {
        boolean valueChanges = false;

        // contribute the value for the site
        if (m_siteValues.containsKey(site))
        {
            valueChanges = m_siteValues.get(site) != value;
        }
        else
        {
            valueChanges = true;
        }

        if (valueChanges)
        {
            // update or remove the value
            if (value == 0)
            {
                m_siteValues.remove(site);
            }
            else
            {
                m_siteValues.put(site, value);
            }

            // now recalculate the sum
            m_sum = 0;

            for (Long aValue : m_siteValues.values())
            {
                m_sum += value;
            }
        }

        return valueChanges;
    }


    /**
     * Obtains the value contributed for the specified site
     *
     * @param site the site
     *
     * @return the value contributed or 0 if no value has been contributed
     */
    public long getValue(String site)
    {
        Long value = m_siteValues.get(site);

        return value == null ? 0 : value;
    }


    /**
     * Obtain the current sum of contributed values from all sites.
     *
     * @return the sum
     */
    public long getSum()
    {
        return m_sum;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_siteValues = new HashMap<String, Long>();
        reader.readMap(0, m_siteValues);
        m_sum = reader.readLong(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeMap(0, m_siteValues);
        writer.writeLong(1, m_sum);
    }


    /**
         * @inheritDoc
         */

    public void readExternal(DataInput in) throws IOException
    {
        m_siteValues = new HashMap<String, Long>();
        ExternalizableHelper.readMap(in, m_siteValues, Base.getContextClassLoader());
        m_sum = in.readLong();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeMap(out, m_siteValues);
        out.writeLong(m_sum);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder builder = new StringBuilder(this.getClass().getName());

        builder.append("{siteValues=[");

        int i = 0;

        for (String site : m_siteValues.keySet())
        {
            if (i > 0)
            {
                builder.append(", ");
            }

            builder.append("<");
            builder.append(site);
            builder.append("=");
            builder.append(m_siteValues.get(site));
            builder.append(">");
        }

        builder.append("], sum=");
        builder.append(m_sum);
        builder.append("}");

        return builder.toString();
    }
}
