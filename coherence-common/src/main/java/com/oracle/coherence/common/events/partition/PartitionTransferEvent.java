/*
 * File: PartitionTransferEvent.java
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

package com.oracle.coherence.common.events.partition;

import com.tangosol.net.Member;

/**
 * A {@link PartitionTransferEvent} provide information about the source {@link Member} and destination
 * {@link Member} when one or more partitions are being transferred.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface PartitionTransferEvent extends PartitionEvent
{
    /**
     * The {@link Member} from which the set of partitions is moving.
     *
     * @return the {@link Member} from which the set of partitions is moving.
     */
    public Member getMemberFrom();


    /**
     * The {@link Member} to which the set of partitions is moving.
     *
     * @return the {@link Member} to which the set of partitions is moving.
     */
    public Member getMemberTo();
}
