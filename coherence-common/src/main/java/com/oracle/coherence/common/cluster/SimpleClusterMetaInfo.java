/*
 * File: SimpleClusterMetaInfo.java
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

package com.oracle.coherence.common.cluster;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.Cluster;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@link SimpleClusterMetaInfo} is a simple implementation of the {@link ClusterMetaInfo} interface that uses
 * local environment information to determine {@link ClusterMetaInfo} properties.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see LocalClusterMetaInfo
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SimpleClusterMetaInfo implements ClusterMetaInfo, ExternalizableLite, PortableObject
{
    /**
     * The name of the site of the {@link Cluster}.
     */
    private String siteName;

    /**
     * The name of the cluster of the {@link Cluster}.
     */
    private String clusterName;


    /**
     * Standard Constructor (required for Serialization)
     */
    public SimpleClusterMetaInfo()
    {
        // deliberately empty
    }


    /**
     * Standard Constructor.
     *
     * @param siteName The name of the site of the {@link Cluster}.
     * @param clusterName The name of the cluster of the {@link Cluster}.
     */
    public SimpleClusterMetaInfo(String siteName,
                                 String clusterName)
    {
        this.siteName    = siteName == null ? "" : siteName.trim();
        this.clusterName = clusterName == null ? "" : clusterName.trim();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getUniqueName()
    {
        return siteName.isEmpty() ? getClusterName() : getSiteName() + "." + getClusterName();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSiteName()
    {
        return siteName.isEmpty() ? "<undefined>" : siteName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getClusterName()
    {
        return clusterName.isEmpty() ? "<undefined>" : clusterName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.siteName    = ExternalizableHelper.readSafeUTF(in);
        this.clusterName = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, siteName);
        ExternalizableHelper.writeSafeUTF(out, clusterName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.siteName    = reader.readString(1);
        this.clusterName = reader.readString(2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(1, siteName);
        writer.writeString(2, clusterName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((clusterName == null) ? 0 : clusterName.hashCode());
        result = prime * result + ((siteName == null) ? 0 : siteName.hashCode());

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

        SimpleClusterMetaInfo other = (SimpleClusterMetaInfo) obj;

        if (clusterName == null)
        {
            if (other.clusterName != null)
            {
                return false;
            }
        }
        else if (!clusterName.equals(other.clusterName))
        {
            return false;
        }

        if (siteName == null)
        {
            if (other.siteName != null)
            {
                return false;
            }
        }
        else if (!siteName.equals(other.siteName))
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
        return String.format("SimpleClusterInfo{siteName=%s, clusterName=%s}", siteName, clusterName);
    }
}
