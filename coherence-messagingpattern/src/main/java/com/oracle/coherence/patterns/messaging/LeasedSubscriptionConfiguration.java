/*
 * File: LeasedSubscriptionConfiguration.java
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

/**
 * A {@link LeasedSubscriptionConfiguration} specifies the standard
 * type-safe parameters required to create a {@link LeasedSubscription}, typically
 * for a {@link Subscriber}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface LeasedSubscriptionConfiguration extends SubscriptionConfiguration
{
    /**
     * The standard duration of a {@link Lease} for a {@link LeasedSubscription}.
     */
    public static final long STANDARD_LEASE_DURATION = 2 * 60 * 1000;    // two minutes


    /**
     * The {@link Lease} duration specifies how long a {@link Subscription}
     * is initially valid for use.  If the {@link Lease} is not extended, then
     * the {@link Subscription} is cleaned up; including a). rolling back of
     * any received messages, b). being removed (ie: unsubscribed) from the
     * system.
     *
     * NOTE: The {@link Lease} duration has nothing to do with whether a
     * {@link Subscription} is durable.  Durable {@link Subscription}s
     * (only available on {@link Topic}s) will maintain their state until
     * unsubscribed.
     *
     * @return lease duration in milliseconds
     */
    public long getLeaseDuration();
}
