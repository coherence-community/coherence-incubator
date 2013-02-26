/*
 * File: EventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution;

import com.oracle.coherence.common.events.Event;

import java.util.Iterator;

/**
 * An {@link EventChannel} is responsible for the sending a batch of {@link Event}s to an end-point.  The
 * mechanism, transport and end-point to which the {@link Event}s will be sent is implementation dependent.
 * <p>
 * Several example {@link EventChannel} implementations can be found in the
 * com.oracle.coherence.patterns.eventdistribution.channels package.
 * <p>
 * Instances of {@link EventChannel}s are themselves often controlled by {@link EventChannelController}s.  For each
 * {@link EventChannel} instance, there is one associated {@link EventChannelController} instance, whose responsibility
 * is to provide underlying infrastructure so that the {@link EventChannel} can send the {@link Event}s to the
 * appropriate end-point.  As {@link EventChannelController}s typically operate in an asynchronous manner,
 * sending {@link Event}s also occurs in an asynchronous manner.
 * <p>
 * {@link EventChannelController}s typically operate by waiting a specified amount of time and then, when the time has
 * expired, provides all of the available {@link Event}s in batches to be sent using their associated {@link EventChannel}.
 * <p>
 * Should there be less than the specified batch size available to send, only those that are available will be
 * sent. That is, an under-sized "batch" will be sent in favor of waiting for a full-sized batch to become
 * available.
 * <p>
 * When interacting with an {@link EventChannel}, an {@link EventChannelController} expects that calls to the
 * {@link #send(Iterator)} method are atomic. Should the method fail for any reason, the entire batch will retried,
 * possibly with more {@link Event}s in the batch than originally tried.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see EventChannelController
 *
 * @author Brian Oliver
 */
public interface EventChannel
{
    /**
     * Requests the {@link EventChannel} connect to its destination so that it may commence distributing {@link Event}s.
     *
     * @param distributorIdentifier The identifier of the {@link EventDistributor} to which the
     *                              {@link EventChannel} belongs.
     * @param controllerIdentifier  The identifier of the {@link EventChannelController} that is
     *                              managing the {@link EventChannel}.
     *
     * @throws EventChannelNotReadyException When the {@link EventChannel} is not ready for distributing {@link Event}s.
     */
    public void connect(EventDistributor.Identifier       distributorIdentifier,
                        EventChannelController.Identifier controllerIdentifier) throws EventChannelNotReadyException;


    /**
     * Requests the {@link EventChannel} to disconnect from its end-point.
     */
    public void disconnect();


    /**
     * Requests the {@link EventChannel} to send the specified {@link Event}s, in the order in which they
     * occur in the provided {@link Iterator}, to the implementation specific end-point.
     * <p>
     * NOTE: If there is a failure, the entire collection represented by the {@link Iterator} may be resent.
     * (not simply where it left off).  Consequently this method should be idempotent.
     *
     * @param events The {@link Event}s to send
     */
    public int send(Iterator<Event> events);
}
