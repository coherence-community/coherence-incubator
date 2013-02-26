/*
 * File: Leasing.java
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

package com.oracle.coherence.common.leasing;

/**
 * A set of static methods to help with the construction of {@link Lease}s.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public final class Leasing
{
    /**
     * Creates and returns a new indefinite {@link Lease} with the acquisition time being the system time at the time
     * of the call.
     *
     * @return a new indefinite {@link Lease} with the  acquisition time being the system time at the time of the call.
     */
    public static Lease newIndefiniteLease()
    {
        return new Lease(-3);
    }


    /**
     * Creates and returns a new {@link Lease} with the specified duration (in ms) from the time this method was
     * called.
     *
     * @param duration the duration in ms when the {@link Lease} should expire
     *
     * @return a new {@link Lease} with the specified duration (in ms) from the time this method was called
     */
    public static Lease newLease(long duration)
    {
        return new Lease(duration);
    }


    /**
     * Creates and returns a new {@link Lease} with the specified acquisition time (from the EPOC) and duration
     * (in ms).
     *
     * @param acquisitionTime the time the {@link Lease} was acquired
     * @param duration        the time in ms that the {@link Lease} should last beyond acquisitionTime
     *
     * @return a new {@link Lease} with the specified acquisition time (from the EPOC) and duration (in ms)
     */
    public static Lease newLease(long acquisitionTime,
                                 long duration)
    {
        return new Lease(acquisitionTime, duration);
    }
}
