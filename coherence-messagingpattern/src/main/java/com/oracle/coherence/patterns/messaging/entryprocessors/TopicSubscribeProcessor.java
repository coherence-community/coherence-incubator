/*
 * File: TopicSubscribeProcessor.java
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

package com.oracle.coherence.patterns.messaging.entryprocessors;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionConfiguration;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;
import com.oracle.coherence.patterns.messaging.Topic;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link TopicSubscribeProcessor} is used to create a {@link Subscription} to a {@link Topic}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <C> subscription configuration
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class TopicSubscribeProcessor<C extends SubscriptionConfiguration> extends AbstractProcessor
    implements ExternalizableLite,
               PortableObject
{
    /**
     * The proposed {@link Identifier} of the {@link Subscription} to the {@link Topic}.
     */
    private SubscriptionIdentifier subscriptionIdentifier;

    /**
     * The {@link SubscriptionConfiguration} to use to create the subscription.
     */
    private C subscriptionConfiguration;

    /**
     * The (optional) {@link Subscription} to register.
     */
    private Subscription subscription;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public TopicSubscribeProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier The proposed identifier for the {@link Subscription}
     * @param subscriptionConfiguration The proposed {@link SubscriptionConfiguration}
     */
    public TopicSubscribeProcessor(SubscriptionIdentifier subscriptionIdentifier,
                                   C                      subscriptionConfiguration)
    {
        this(subscriptionIdentifier, subscriptionConfiguration, null);
    }


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier The proposed identifier for the {@link Subscription}
     * @param subscriptionConfiguration The proposed {@link SubscriptionConfiguration}
     * @param subscription The object that will managed the state of the subscription
     */
    public TopicSubscribeProcessor(SubscriptionIdentifier subscriptionIdentifier,
                                   C                      subscriptionConfiguration,
                                   Subscription           subscription)
    {
        this.subscriptionIdentifier    = subscriptionIdentifier;
        this.subscriptionConfiguration = subscriptionConfiguration;
        this.subscription              = subscription;
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        Topic topic = (Topic) entry.getValue();

        if (topic == null)
        {
            Logger.log(Logger.ERROR,
                       "Subscription cannot be created because the destination is not found for key %s",
                       entry.getKey());

            return null;
        }

        topic.subscribe(subscriptionIdentifier, subscriptionConfiguration, subscription);
        entry.setValue(topic);

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        subscriptionIdentifier    = (SubscriptionIdentifier) ExternalizableHelper.readExternalizableLite(in);
        subscriptionConfiguration = (C) ExternalizableHelper.readExternalizableLite(in);
        subscription              = (Subscription) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeExternalizableLite(out, subscriptionIdentifier);
        ExternalizableHelper.writeExternalizableLite(out, (ExternalizableLite) subscriptionConfiguration);
        ExternalizableHelper.writeObject(out, subscription);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        subscriptionIdentifier    = (SubscriptionIdentifier) reader.readObject(0);
        subscriptionConfiguration = (C) reader.readObject(1);
        subscription              = (Subscription) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, subscriptionIdentifier);
        writer.writeObject(1, subscriptionConfiguration);
        writer.writeObject(2, subscription);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String
            .format("TopicSubscribeProcessor{subscriptionIdentifier=%s, subscriptionConfiguration=%s, subscription=%s}",
                    subscriptionIdentifier,
                    subscriptionConfiguration,
                    subscription);
    }
}
