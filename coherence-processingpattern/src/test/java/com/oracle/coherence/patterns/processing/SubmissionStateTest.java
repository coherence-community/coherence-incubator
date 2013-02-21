/*
 * File: SubmissionStateTest.java
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

package com.oracle.coherence.patterns.processing;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A {@link SubmissionStateTest} verifies what {@link SubmissionState}s are considered final.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class SubmissionStateTest
{
    /**
     * Verifies what states are considered final.
     */
    @Test
    public void checkFinalStates()
    {
        assertTrue(SubmissionState.DONE.isFinalState());
        assertTrue(SubmissionState.FAILED.isFinalState());
    }


    /**
     * Verifies what states are considered non-final.
     */
    @Test
    public void checkNonFinalStates()
    {
        assertFalse(SubmissionState.ASSIGNED.isFinalState());
        assertFalse(SubmissionState.EXECUTING.isFinalState());
        assertFalse(SubmissionState.INITIAL.isFinalState());
        assertFalse(SubmissionState.RETRY.isFinalState());
        assertFalse(SubmissionState.SUBMITTED.isFinalState());
        assertFalse(SubmissionState.SUSPENDED.isFinalState());
    }
}
