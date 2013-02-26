/*
 * File: ConfigurableSubscription.java
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
 * A {@link ConfigurableSubscription} is a {@link Subscription} that
 * supports configuration through the use of a {@link SubscriptionConfiguration}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <C> subscription configuration
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class ConfigurableSubscription<C extends SubscriptionConfiguration> extends Subscription
{
    /**
     * The {@link SubscriptionConfiguration} for the {@link ConfigurableSubscription}.
     */
    private C configuration;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public ConfigurableSubscription()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param configuration subscription configuratio
     */
    public ConfigurableSubscription(SubscriptionIdentifier subscriptionIdentifier,
                                    Status                 status,
                                    C                      configuration)
    {
        super(subscriptionIdentifier, status);
        this.configuration = configuration;
    }


    /**
     * Returns the {@link SubscriptionConfiguration} for the {@link ConfigurableSubscription}.
     *
     * @return <C> subscription configuration
     */
    public C getConfiguration()
    {
        return configuration;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.configuration = (C) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, configuration);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.configuration = (C) reader.readObject(100);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(100, configuration);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("ConfigurableSubscription{%s, configuration=%s}", super.toString(), configuration);
    }
}
