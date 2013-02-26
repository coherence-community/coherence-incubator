/*
 * File: SubscriberInterruptedException.java
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

package com.oracle.coherence.patterns.messaging.exceptions;

import com.oracle.coherence.patterns.messaging.Subscriber;
import com.oracle.coherence.patterns.messaging.Subscription;
import com.oracle.coherence.patterns.messaging.SubscriptionIdentifier;

/**
 * The {@link SubscriberInterruptedException} is thrown when attempts
 * to use a {@link Subscriber} or {@link Subscription} have failed,
 * typically due to a {@link Subscription} being removed from the system.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class SubscriberInterruptedException extends RuntimeException
{
    /**
     * The {@link SubscriptionIdentifier} that has been lost.
     */
    private SubscriptionIdentifier subscriptionIdentifier;


    /**
     * Standard Constructor.
     *
     * @param subscriptionIdentifier subscription identifier
     * @param message error message
     * @param cause cause of error
     */
    public SubscriberInterruptedException(SubscriptionIdentifier subscriptionIdentifier,
                                          String                 message,
                                          Throwable              cause)
    {
        super(message, cause);
        this.subscriptionIdentifier = subscriptionIdentifier;
    }


    /**
     * Returns the {@link SubscriptionIdentifier} that was lost.
     *
     * @return SubscriptionIdentifier
     */
    public SubscriptionIdentifier getSubscriptionIdentifier()
    {
        return subscriptionIdentifier;
    }
}
