/*
 * File: ClientLeaseMaintainer.java
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

package com.oracle.coherence.patterns.processing.taskprocessor;

import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;

/**
 * The {@link ClientLeaseMaintainer} maintains a registered {@link Lease}
 * automatically, by extending it well before the expiry.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface ClientLeaseMaintainer
{
    /**
     * Adds a {@link Lease} for maintenance.
     *
     * @param taskProcessorKey the key to the owner
     * @param lease            the {@link Lease} to be maintained
     */
    public void addLease(TaskProcessorMediatorKey taskProcessorKey,
                         Lease                    lease);


    /**
     * Removes a {@link Lease} from maintenance.
     *
     * @param taskProcessorKey the key to the owner
     * @param lease the {@link Lease} to remove
     */
    public void removeLease(TaskProcessorMediatorKey taskProcessorKey,
                            Lease                    lease);
}
