/*
 * File: QueueProxyMBean.java
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

package com.oracle.coherence.patterns.messaging.management;

import com.oracle.coherence.patterns.messaging.Queue;

/**
 * MBean interface for {@link Queue}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
public interface QueueProxyMBean
{
    /**
     * Get the queue name.
     *
     * @return queue name
     */
    public String getName();


    /**
     * Get the number of messages received since the queue was created.
     *
     * @return number of messages received
     */
    public long getNumMessagesReceived();


    /**
     * Get the number of messages delivered since the queue was created.
     *
     * @return number of messages delivered
     */
    public long getNumMessagesDelivered();


    /**
     * Get the number of messages currently waiting to be delivered.
     *
     * @return number of messages waiting to be delivered
     */
    public long getNumMessagesWaitingToDeliver();


    /**
     * Get the number of messages currently waiting to be re-delivered, after having been rolled back.
     *
     * @return number of messages waiting to be re-delivered
     */
    public long getNumMessagesWaitingToRedeliver();


    /**
     * Get the number of subscription waiting for messages.
     *
     * @return number of subscription waiting for messages
     */
    public long getNumWaitingSubscriptions();
}
