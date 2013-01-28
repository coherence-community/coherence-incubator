/*
 * File: ActiveExampleValue.java
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

package com.oracle.coherence.patterns.pushreplication.examples;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

/**
 * The {@link ActiveExampleValue} shows how we can decorate our objects to
 * support intelligent conflict resolution. In this case we add the site origin
 * information so the conflict resolution process can override conflicts from
 * site2.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
@SuppressWarnings("serial")
public class ActiveExampleValue implements Serializable, PortableObject
{
    private static long masterSeqNum = 0;

    // Sequence number of ActiveExampleValue
    private long seqNum = 0;

    /**
     * The site that this value originated on.
     */
    private String siteOrigin;

    /**
     * The underlying value that we want to preserve
     */
    private long value;


    /**
     * Constructs an {@link ActiveExampleValue}.
     */
    public ActiveExampleValue()
    {
    }


    /**
     * Constructs an {@link ActiveExampleValue}.
     *
     * @param siteOrigin  the site this value was created on
     * @param value       the value itself
     */
    public ActiveExampleValue(String siteOrigin,
                              long   value)
    {
        this.siteOrigin = siteOrigin;
        this.value      = value;
        this.seqNum     = ++masterSeqNum;
    }


    /**
     * Return the site origin.
     *
     * @return the site origin
     */
    public String getSiteOrigin()
    {
        return siteOrigin;
    }


    /**
     * Return the value.
     *
     * @return the value
     */
    public long getValue()
    {
        return value;
    }


    /**
     * Return the sequence number.
     *
     * @return the sequence number
     */
    public long getSequenceNumber()
    {
        return seqNum;
    }


    /**
    * @inheritDoc
    */
    public void readExternal(PofReader reader) throws IOException
    {
        siteOrigin = reader.readString(0);
        value      = reader.readLong(1);
        seqNum     = reader.readLong(2);
    }


    /**
    * @inheritDoc
    */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, siteOrigin);
        writer.writeLong(1, value);
        writer.writeLong(2, seqNum);
    }


    /**
     * Return a string representation of this Object.
     *
     * @return a string representation of this Object
     */
    @Override
    public String toString()
    {
        return "Example Value[" + siteOrigin + ", " + seqNum + ", " + value + "]";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ActiveExampleValue))
        {
            return false;
        }

        ActiveExampleValue exampleValueObj = (ActiveExampleValue) obj;

        if (!(this.seqNum == exampleValueObj.seqNum))
        {
            return false;
        }

        if (!(this.value == exampleValueObj.value))
        {
            return false;
        }

        if (!(this.siteOrigin.equals(exampleValueObj.siteOrigin)))
        {
            return false;
        }

        return true;
    }
}
