/*
 * File: SubmissionConfiguration.java
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

import java.util.Map;

/**
 * A {@link SubmissionConfiguration} provides additional configuration to a
 * particular
 * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}.
 * <p>
 * Configuration data includes a group affinity object, which can be
 * used to co-locate submissions on a particular node, a SubmissionDelay
 * which is used to schedule the execution at a later time and a Map which
 * can be used to provide arbitrary data.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface SubmissionConfiguration
{
    /**
     * Return the groupAffinity object used to co-locate
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}s
     * that should be processed on the same node.
     *
     * @return the groupAffinity object used to co-locate
     *         {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     *         s that should be processed on the same node.
     */
    public Object getGroupAffinity();


    /**
     * Return the configuration data map.
     *
     * @return the configuration data map
     */
    @SuppressWarnings("unchecked")
    public Map getConfigurationDataMap();


    /**
     * The amount of time a submission should wait before being executed in
     * milliseconds.
     *
     * @return the amount of time a submission should wait before being
     *         executed in milliseconds
     */
    public long getSubmissionDelay();


    /**
     * Returns the associated {@link DispatcherFilter}.
     *
     * @return the {@link DispatcherFilter}
     */
    public DispatcherFilter getDispatcherFilter();
}
