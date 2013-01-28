/*
 * File: CustomerReference.java
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

package com.oracle.coherence.patterns.pushreplication.examples.auction;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * A {@link CustomerReference} details a particular customer and the site
 * to which they belong.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class CustomerReference implements Comparable<CustomerReference>,
                                          Serializable,
                                          ExternalizableLite,
                                          PortableObject
{
    private String site;
    private int    id;


    /**
     * Constructs a {@link CustomerReference}.
     */
    public CustomerReference()
    {
    }


    /**
     * Constructs a {@link CustomerReference}.
     *
     * @param site
     * @param id
     */
    public CustomerReference(String site,
                             int    id)
    {
        this.site = site;
        this.id   = id;
    }


    /**
     * Obtains the site on which the {@link CustomerReference} was created.
     *
     * @return the site
     */
    public String getSite()
    {
        return site;
    }


    /**
     * Obtains the identity of the customer
     *
     * @return the identity of the customer
     */
    public int getId()
    {
        return id;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + id;
        result = prime * result + ((site == null) ? 0 : site.hashCode());

        return result;
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

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        CustomerReference other = (CustomerReference) obj;

        if (id != other.id)
        {
            return false;
        }

        if (site == null)
        {
            if (other.site != null)
            {
                return false;
            }
        }
        else if (!site.equals(other.site))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("CustomerReference{site=%s, id=%d}", getSite(), getId());
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(CustomerReference other)
    {
        int delta = this.getId() - other.getId();

        return delta < 0 ? -1 : (delta > 0 ? +1 : 0);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput reader) throws IOException
    {
        site = reader.readUTF();
        id   = reader.readInt();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput writer) throws IOException
    {
        writer.writeUTF(site);
        writer.writeInt(id);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        site = reader.readString(0);
        id   = reader.readInt(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, site);
        writer.writeInt(1, id);
    }
}
