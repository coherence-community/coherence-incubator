/*
 * File: ServerLeaseMonitor.java
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

package com.oracle.coherence.patterns.processing.internal.task;

import com.oracle.coherence.common.leasing.Lease;

/**
 * The {@link ServerLeaseMonitor} keeps track of {@link Lease}s for
 * {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator} objects.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface ServerLeaseMonitor
{
    /**
     * Registers a lease for monitoring for a particular {@link TaskProcessorMediator}.
     *
     * @param leaseOwner the {@link TaskProcessorMediatorKey} identifying the owner
     * @param lease      the {@link Lease} to register for monitoring
     */
    public void registerLease(TaskProcessorMediatorKey leaseOwner,
                              Lease                    lease);


    /**
     * De-register a lease for a particular {@link TaskProcessorMediatorKey}.
     *
     * @param leaseOwner the {@link TaskProcessorMediatorKey}
     */
    public void deregisterLease(TaskProcessorMediatorKey leaseOwner);
}
