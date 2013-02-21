/*
 * File: ProcessorStateManager.java
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

import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ProcessorStateManager} is used to limit the number of event processors that are queued up for
 * a specific destination.  This is done to prevent the queue from getting backed up resulting in steadily
 * increasing memory consumption.  Only 1 processor needs to be queued in order for the event processor to
 * ultimate run.  The event processors for both {@link Message} and {@link Queue} do batch processing, executing
 * all work that needs to be done for the destination so the only requirement is that a single processor be
 * scheduled for execution at any time.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
class ProcessorStateManager
{
    /**
     * The state of the processor for a given destination.
     *
     * A {@link State}
     */
    private enum State
    {
        /**
         * There is no event processor scheduled or running.
         */
        Idle,

        /**
         * An event processor is scheduled to run.
         */
        Scheduled,

        /**
         *  An event processor is running.
         */
        Running
    }


    /**
     * A map of states indexed by the destination identifier.
     */
    private ConcurrentHashMap<Object, State> stateMap = new ConcurrentHashMap<Object, State>();


    /**
     * Get the state from the map.  If it doesn't exist then create it.
     *
     * @param key map key
     * @return State
     */
    public State ensureState(Object key)
    {
        State state = stateMap.get(key);

        if (state == null)
        {
            stateMap.put(key, State.Idle);
        }

        return state;
    }


    /**
     * Return true if an event processor is in the dispatch queue waiting to run.
     *
     * @param key map key
     * @return boolean true if an event processor is in the dispatch queue
     */
    public boolean isScheduled(Object key)
    {
        State state = ensureState(key);

        return (state == State.Scheduled);
    }


    /**
     * Set the state to idle.
     *
     * @param key map key
     */
    public void setIdle(Object key)
    {
        setState(key, State.Idle);
    }


    /**
     * Set the state to running.
     *
     * @param key map key
     */
    public void setRunning(Object key)
    {
        setState(key, State.Running);
    }


    /**
     * Set the state to scheduled.
     *
     * @param key map key
     */
    public void setScheduled(Object key)
    {
        setState(key, State.Scheduled);
    }


    /**
     * Set the state.
     *
     * @param key map key
     * @param state state
     */
    private void setState(Object key,
                          State  state)
    {
        stateMap.put(key, state);
    }
}
