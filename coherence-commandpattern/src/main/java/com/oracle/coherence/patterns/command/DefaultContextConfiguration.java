/*
 * File: DefaultContextConfiguration.java
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

package com.oracle.coherence.patterns.command;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The default implementation of a {@link ContextConfiguration}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class DefaultContextConfiguration implements ContextConfiguration, ExternalizableLite, PortableObject
{
    /**
     * The {@link ContextConfiguration.ManagementStrategy} for the {@link ContextConfiguration}.
     */
    private ManagementStrategy managementStrategy;


    /**
     * Standard Constructor (with default values).
     *
     * The {@link ContextConfiguration.ManagementStrategy} is set to {@link ContextConfiguration.ManagementStrategy#DISTRIBUTED} by default.
     */
    public DefaultContextConfiguration()
    {
        this.managementStrategy = ManagementStrategy.DISTRIBUTED;
    }


    /**
     * Standard Constructor (developer provided values).
     *
     * @param managementStrategy
     */
    public DefaultContextConfiguration(ManagementStrategy managementStrategy)
    {
        this.managementStrategy = managementStrategy;
    }


    /**
     * {@inheritDoc}
     */
    public ManagementStrategy getManagementStrategy()
    {
        return managementStrategy;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        managementStrategy = ManagementStrategy.values()[ExternalizableHelper.readInt(in)];
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeInt(out, managementStrategy.ordinal());
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        managementStrategy = ManagementStrategy.values()[reader.readInt(0)];
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeInt(0, managementStrategy.ordinal());
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("DefaultContextConfiguration{managementStrategy=%s}", managementStrategy);
    }
}
