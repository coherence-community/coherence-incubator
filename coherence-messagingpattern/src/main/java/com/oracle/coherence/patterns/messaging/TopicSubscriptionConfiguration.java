/*
 * File: TopicSubscriptionConfiguration.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link TopicSubscriptionConfiguration} provides specific configuration
 * parameters for {@link Subscription}s to a {@link Topic}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class TopicSubscriptionConfiguration extends DefaultSubscriptionConfiguration
{
    /**
     * The name of the {@link TopicSubscription}.
     */
    private String name;

    /**
     * Is the {@link Topic} {@link Subscription} durable.
     */
    private boolean isDurable;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public TopicSubscriptionConfiguration()
    {
    }


    /**
     * Private Constructor.
     *
     * @param name The name of the {@link TopicSubscription}
     * @param isDurable true if subscription is durable
     * @param leaseDuration duration in milliseconds
     */
    private TopicSubscriptionConfiguration(String  name,
                                           boolean isDurable,
                                           long    leaseDuration)
    {
        super(leaseDuration);

        this.name      = name;
        this.isDurable = isDurable;
    }


    /**
     * Creates and returns a {@link TopicSubscriptionConfiguration} specifically
     * for use by durable topic {@link Subscription}s.
     *
     * @param name configuration name
     * @return subscription configuration
     */
    public static final TopicSubscriptionConfiguration newDurableConfiguration(String name)
    {
        return new TopicSubscriptionConfiguration(name, true, LeasedSubscriptionConfiguration.STANDARD_LEASE_DURATION);
    }


    /**
     * Creates and returns a {@link TopicSubscriptionConfiguration} specifically
     * for use by durable topic {@link Subscription}s.
     *
     * @param name configuration name
     * @param leaseDuration duration in milliseconds
     * @return subscription configuration
     */
    public static final TopicSubscriptionConfiguration newDurableConfiguration(String name,
                                                                               long   leaseDuration)
    {
        return new TopicSubscriptionConfiguration(name, true, leaseDuration);
    }


    /**
     * Creates and returns a {@link TopicSubscriptionConfiguration} specifically
     * for use by non-durable topic {@link Subscription}s.
     *
     * @return subscription configuration
     */
    public static final TopicSubscriptionConfiguration newNonDurableConfiguration()
    {
        return new TopicSubscriptionConfiguration(null, false, LeasedSubscriptionConfiguration.STANDARD_LEASE_DURATION);
    }


    /**
     * Creates and returns a {@link TopicSubscriptionConfiguration} specifically
     * for use by non-durable topic {@link Subscription}s.
     *
     * @param leaseDuration duration in milliseconds
     * @return subscription configuration
     */
    public static final TopicSubscriptionConfiguration newNonDurableConfiguration(long leaseDuration)
    {
        return new TopicSubscriptionConfiguration(null, false, leaseDuration);
    }


    /**
     * Returns the name of the {@link TopicSubscription}.
     *
     * @return name of configuration
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns if the {@link Topic} {@link Subscription} is to be durable.
     *
     * @return true if subscription is durable
     */
    public boolean isDurable()
    {
        return isDurable;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.name      = ExternalizableHelper.readSafeUTF(in);
        this.isDurable = in.readBoolean();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeSafeUTF(out, name);
        out.writeBoolean(isDurable);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.name      = reader.readString(100);
        this.isDurable = reader.readBoolean(101);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeString(100, name);
        writer.writeBoolean(101, isDurable);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("TopicSubscriptionConfiguration{%s, name=%s, isDurable=%s}",
                             super.toString(),
                             name == null ? "(n/a)" : name,
                             isDurable);
    }
}
