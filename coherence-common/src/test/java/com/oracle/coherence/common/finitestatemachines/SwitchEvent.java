/*
 * File: SwitchEvent.java
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

package com.oracle.coherence.common.finitestatemachines;

/**
 * A {@link SwitchEvent} describes the events that may occur on a light switch
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public enum SwitchEvent implements Event<Light>
{
    /**
     * When you flick the switch on, the light will become {@value Light#ON}.
     */
    SWITCH_ON(Light.ON),

    /**
     * When you flick the switch off, the light will become {@value Light#OFF}.
     */
    SWITCH_OFF(Light.OFF),

    /**
     * When you flick the switch on and off quickly, the light will become {@value Light#BROKEN}.
     */
    TOGGLE_TOO_FAST(Light.BROKEN);

    /**
     * The desired {@link Light} state after the {@link Event}.
     */
    private Light m_desiredState;


    /**
     * Constructs a {@link SwitchEvent}
     *
     * @param desiredState the desired {@link Light} state after the event
     */
    private SwitchEvent(Light desiredState)
    {
        m_desiredState = desiredState;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Light getDesiredState(Light            currentState,
                                 ExecutionContext context)
    {
        return m_desiredState;
    }
}
