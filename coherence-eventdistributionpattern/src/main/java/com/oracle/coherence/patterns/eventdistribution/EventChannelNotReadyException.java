/*
 * File: EventChannelNotReadyException.java
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

/**
 * An {@link EventChannelNotReadyException} may be thrown when attempting to start a {@link EventChannel}, simply because
 * the {@link EventChannel} may not yet be ready.  This is not a fatal failure on behalf of the {@link EventChannel}.  It
 * simply means a delay needs to occur before (re)attempting to start the said {@link EventChannel}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class EventChannelNotReadyException extends Exception
{
    /**
     * The {@link EventChannel} that was not ready.
     */
    private EventChannel channel;


    /**
     * Standard Constructor.
     *
     * @param channel The {@link EventChannel} that could not be started
     */
    public EventChannelNotReadyException(EventChannel channel)
    {
        this.channel = channel;
    }


    /**
     * Determine the {@link EventChannel} that could not be started.
     *
     * @return A {@link EventChannel}
     */
    public EventChannel getEventChannel()
    {
        return channel;
    }
}
