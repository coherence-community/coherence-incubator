/*
 * File: SubmissionRetentionPolicy.java
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

/**
 * A {@link SubmissionRetentionPolicy} specifies how the lifecycle of Submissions
 * and their results shall be handled.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public enum SubmissionRetentionPolicy
{
    /**
     * RemoveOnFinalState is the default policy. This means that as soon as a
     * {@link SubmissionOutcome} is signaled and has the result
     * taken over, the cache entries are removed.
     */
    RemoveOnFinalState,

    /**
     * ExplicitRemove is a policy which means that the cache entries are not
     * removed when a SubmissionOutcome is signaled. Instead
     * they need to be removed by an explicit call to remove them.
     */
    ExplicitRemove;
}
