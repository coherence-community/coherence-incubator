/*
 * File: ConflictResolution.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;

/**
 * A {@link ConflictResolution} represents the action should be taken as a result of executing a {@link ConflictResolver}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ConflictResolution
{
    /**
     * An {@link Operation} represents action that to be taken as a result of {@link ConflictResolution}.
     */
    public enum Operation
    {
        /**
         * <code>Remove</code> indicates that the entry is to be removed.
         */
        Remove,

        /**
         * <code>UseLocalValue</code> indicates that the value in the local cache is to be used
         */
        UseLocalValue,

        /**
         * <code>UseInComingValue</code> indicates that the in coming value (typically from a remote cluster) is to be used.
         */
        UseInComingValue,

        /**
         * <code>UseMergedValue</code> indicates that a modified value created by the {@link ConflictResolver} is to be
         * used.
         */
        UseMergedValue
    }


    /**
     * The {@link ConflictResolution.Operation} for this {@link ConflictResolution}
     */
    private Operation m_operation = Operation.UseInComingValue;

    /**
     * The merged value associated with this {@link ConflictResolution}
     */
    private Object m_mergedValue = null;


    /**
     * Standard Constructor.
     */
    public ConflictResolution()
    {
        m_operation = Operation.UseInComingValue;
    }


    /**
     * Set the resolution to {@link Operation#Remove}.
     */
    public void remove()
    {
        m_operation = Operation.Remove;
    }


    /**
     * Set the resolution to {@link Operation#UseInComingValue}.
     */
    public void useInComingValue()
    {
        m_operation = Operation.UseInComingValue;
    }


    /**
     * Set the resolution to {@link Operation#UseLocalValue}.
     */
    public void useLocalValue()
    {
        m_operation = Operation.UseLocalValue;
    }


    /**
     * Set the resolution to {@link Operation#UseMergedValue}.
     */
    public void useMergedValue(Object mergedValue)
    {
        m_operation   = Operation.UseMergedValue;
        m_mergedValue = mergedValue;
    }


    /**
     * Get the {@link Operation} for this {@link ConflictResolution}.
     *
     * @return The {@link Operation}
     */
    public Operation getOperation()
    {
        return m_operation;
    }


    /**
     * Get the merged value for this {@link ConflictResolution}.
     *
     * @return the merged value
     */
    public Object getMergedValue()
    {
        return m_mergedValue;
    }
}
