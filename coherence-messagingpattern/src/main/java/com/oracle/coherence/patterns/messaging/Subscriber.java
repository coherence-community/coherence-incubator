/*
 * File: Subscriber.java
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
import com.oracle.coherence.patterns.messaging.exceptions.SubscriberInterruptedException;
import com.oracle.coherence.patterns.messaging.exceptions.SubscriptionLostException;

/**
 * A {@link Subscriber} provides mechanisms to retrieve and acknowledge
 * allocated {@link Message}s from a {@link Destination}.
 * <p>
 * {@link Subscriber} instances are created and managed through the use
 * of a {@link MessagingSession}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface Subscriber
{
    /**
     * Returns the {@link MessagingSession} that created the {@link Subscriber}.
     *
     * @return MessagingSession
     */
    public MessagingSession getMessagingSession();


    /**
     * Returns the {@link Identifier} of the {@link Subscription}
     * created and used by the {@link Subscriber}.
     *
     * @return SubscriptionIdentifier
     */
    public SubscriptionIdentifier getSubscriptionIdentifier();


    /**
     * Returns the {@link Identifier} for the {@link Destination} to which
     * this {@link Subscriber} is subscribed.
     *
     * @return Identifier
     */
    public Identifier getDestinationIdentifier();


    /**
     * Returns if the {@link Subscriber} may be used to receive messages.
     *
     * @return true if active
     */
    public boolean isActive();


    /**
     * Unsubscribes the {@link Subscription} for the {@link Subscriber} from the
     * associated {@link Destination} and releases any necessary resources.
     *
     * NOTE 1: After making a call to this method the {@link Subscriber} and it's associated
     * {@link Subscription} can not be used to receive messages.
     *
     * NOTE 2: Calling this method on a durable {@link Topic} {@link Subscription} will result
     * in the {@link Subscription} being removed.
     *
     * @throws SubscriptionLostException
     */
    public void unsubscribe();


    /**
     * Releases the {@link Subscriber} and any associated resources from the
     * {@link Destination}.  If the {@link Subscriber} {@link Subscription} has
     * been been configured as being durable (using a {@link TopicSubscriptionConfiguration}),
     * the underlying {@link Subscription} is not removed.  However, if the
     * {@link Subscriber} is non-durable, the underlying {@link Subscription} is
     * also removed, making this method semantically equivalent to calling {@link #unsubscribe()}.
     *
     * NOTE: After making a call to this method the {@link Subscriber} can not be used any further.
     *
     * @see TopicSubscriptionConfiguration
     *
     * @throws SubscriptionLostException
     */
    public void release();


    /**
     * Sets whether messages received (via {@link #getMessage()}) will
     * automatically be acknowledged for the {@link Subscriber}.
     *
     * The default is set to <code>true</code>
     *
     * NOTE: changing this value to <code>true</code> when there are
     * uncommitted messages for this {@link Subscriber} will automatically
     * rollback the said messages.
     *
     * @param autoCommit true if autocommit should be enabled.
     *
     * @throws SubscriptionLostException
     */
    public void setAutoCommit(boolean autoCommit);


    /**
     * Returns if the {@link Subscriber} will auto-commit (ie: acknowledge) any messages
     * it receives using {@link #getMessage()}.
     *
     * @return true if autoCommitting
     */
    public boolean isAutoCommitting();


    /**
     * Requests and waits (indefinitely) for a {@link Message} to be
     * delivered to the {@link Subscriber}.
     *
     * If {@link #isAutoCommitting()}, once a {@link Message} has arrived,
     * it is marked as being acknowledged and thus can't be rolled back onto the {@link Destination}.
     *
     * @return message
     * @throws SubscriberInterruptedException
     * @throws SubscriptionLostException
     */
    public Object getMessage();


    /**
     * Commits any received messages when using {@link #getMessage()}.
     *
     * NOTE: Calls to this method are only useful when not {@link #isAutoCommitting()}.
     *
     * @throws SubscriptionLostException
     */
    public void commit();


    /**
     * Places messages delivered to the {@link Subscriber} (but not acknowledged)
     * back onto the front of the {@link Destination} to which the {@link Subscriber}
     * is subscribed, in the order received.
     *
     * NOTE: Calls to this method are only useful when not {@link #isAutoCommitting()}.
     *
     * @throws SubscriptionLostException
     */
    public void rollback();
}
