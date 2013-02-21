/*
 * File: SubscriptionIdentifierSet.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link SubscriptionIdentifierSet} is a serializable set of {@link SubscriptionIdentifier}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SubscriptionIdentifierSet implements Identifier, ExternalizableLite, PortableObject
{
    /**
     * The set of {@link SubscriptionIdentifier}s.
     */
    private Set<SubscriptionIdentifier> subscriptionIdentifiers;


    /**
     * Required for {@link ExternalizableLite}.
     */
    public SubscriptionIdentifierSet()
    {
    }


    /**
     * Required for {@link ExternalizableLite}.
     */
    public SubscriptionIdentifierSet(Set<SubscriptionIdentifier> subscriptionIdentifiers)
    {
        this.subscriptionIdentifiers = subscriptionIdentifiers;
    }


    /**
     * Return the subscriptionIdentifiers.
     *
     * @return the subscriptionIdentifiers
     */
    public Set<SubscriptionIdentifier> getSubscriptionIdentifiers()
    {
        return subscriptionIdentifiers;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        subscriptionIdentifiers = new HashSet<SubscriptionIdentifier>();
        ExternalizableHelper.readCollection(in,
                                            this.subscriptionIdentifiers,
                                            Thread.currentThread().getContextClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeCollection(out, subscriptionIdentifiers);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        subscriptionIdentifiers = new HashSet<SubscriptionIdentifier>();
        reader.readCollection(1, subscriptionIdentifiers);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeCollection(1, subscriptionIdentifiers);
    }
}
