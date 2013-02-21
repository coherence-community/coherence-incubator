/*
 * File: Yield.java
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

package com.oracle.coherence.patterns.processing.task;

/**
 * The {@link Yield} object is used to suspend execution and store away an
 * intermediate state. When execution resumes, the stored state is passed in
 * to the Task. A Delay can be passed in to indicate that resumption should
 * wait at least Delay milliseconds.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class Yield
{
    /**
     * The intermediate state.
     */
    private final Object m_intermediateState;

    /**
     * The number of milliseconds to yield.
     */
    private final long m_lDelay;


    /**
     * The constructor taking an intermediate state as input.
     *
     * @param oIntermediatestate the state to store away
     * @param lDelay             the delay in milliseconds before rescheduling
     */
    public Yield(final Object oIntermediatestate,
                 final long   lDelay)
    {
        m_intermediateState = oIntermediatestate;
        m_lDelay            = lDelay;
    }


    /**
     * Return the delay.
     *
     * @return the Delay
     */
    public long getDelay()
    {
        return m_lDelay;
    }


    /**
     * Returns the intermediate state.
     *
     * @return the intermediate state
     */
    public Object getIntermediateState()
    {
        return m_intermediateState;
    }
}
