/*
 * File: DefaultSubscriptionConfiguration.java
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

package com.oracle.coherence.patterns.messaging;

import com.oracle.coherence.common.leasing.Lease;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A default implementation of a {@link LeasedSubscriptionConfiguration}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class DefaultSubscriptionConfiguration implements LeasedSubscriptionConfiguration,
                                                         ExternalizableLite,
                                                         PortableObject
{
    /**
     * The {@link Lease} duration.
     */
    private long leaseDuration;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public DefaultSubscriptionConfiguration()
    {
        this.leaseDuration = LeasedSubscriptionConfiguration.STANDARD_LEASE_DURATION;
    }


    /**
     * Standard Constructor.
     *
     * @param leaseDuration lease duration
     */
    public DefaultSubscriptionConfiguration(long leaseDuration)
    {
        this.leaseDuration = leaseDuration;
    }


    /**
     * {@inheritDoc}
     */
    public long getLeaseDuration()
    {
        return leaseDuration;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.leaseDuration = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, leaseDuration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.leaseDuration = reader.readLong(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, leaseDuration);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("DefaultSubscriptionConfiguration{leaseDuration=%d}", leaseDuration);
    }
}
