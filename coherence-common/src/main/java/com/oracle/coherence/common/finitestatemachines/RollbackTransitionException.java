/*
 * File: RollbackTransitionException.java
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
 * A {@link RollbackTransitionException} may be thrown during an {@link TransitionAction} for a {@link Transition}
 * if the said {@link Transition} should be aborted.  ie: no state change should occur
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class RollbackTransitionException extends Exception
{
    /**
     * The state from which the {@link Transition} was being made.
     */
    private Enum<?> m_stateFrom;

    /**
     * The start to which the {@link Transition} was being made.
     */
    private Enum<?> m_stateTo;


    /**
     * Constructs a {@link RollbackTransitionException} with the specified from and to states.
     *
     * @param stateFrom  the state from which the {@link Transition} was being made
     * @param stateTo    the state to which the {@link Transition} was being made
     * @param rationale  the reason for the rollback
     */
    public RollbackTransitionException(Enum<?> stateFrom,
                                       Enum<?> stateTo,
                                       String  rationale)
    {
        super(String.format("Rollback of transition from %s to %s occurred. %s", stateFrom, stateTo, rationale));
        m_stateFrom = stateFrom;
        m_stateTo   = stateTo;
    }


    /**
     * Constructs a {@link RollbackTransitionException} with the specified from and to states.
     *
     * @param stateFrom  the state from which the {@link Transition} was being made
     * @param stateTo    the state to which the {@link Transition} was being made
     * @param cause      the underlying exception that caused the rollback
     */
    public RollbackTransitionException(Enum<?>   stateFrom,
                                       Enum<?>   stateTo,
                                       Throwable cause)
    {
        super(String.format("Rollback of transition from %s to %s occured due to an exception.", stateFrom, stateTo),
              cause);
        m_stateFrom = stateFrom;
        m_stateTo   = stateTo;
    }


    /**
     * Obtain the state from which the {@link Transition} was rolled back.
     *
     * @return the state from which the {@link Transition} was rolled back
     */
    public Enum<?> getStateFrom()
    {
        return m_stateFrom;
    }


    /**
     * Obtain the state to which the {@link Transition} did not occur.
     *
     * @return the state to which the {@link Transition} did not occur
     */
    public Enum<?> getStateTo()
    {
        return m_stateTo;
    }
}
