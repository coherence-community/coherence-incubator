/*
 * File: SubmissionCallback.java
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

package com.oracle.coherence.patterns.processing.examples.task;

import com.oracle.coherence.patterns.processing.SubmissionOutcomeListener;

/**
 * Callback class reporting on the results of a Task.
 * <p>
 * Copyright (c) 2009, 2019. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class SubmissionCallback implements SubmissionOutcomeListener
{
    /**
     * The name of the task.
     */
    private String m_sTaskName;


    /**
     * Constructor which takes a name of the task it is a callback for.
     *
     * @param sTaskName the name of the task
     */
    public SubmissionCallback(String sTaskName)
    {
        m_sTaskName = sTaskName;
    }


    /**
     * {@inheritDoc}
     */
    public void onDone(Object oResult)
    {
        System.out.println(m_sTaskName + ": Submission done");
    }


    /**
     * {@inheritDoc}
     */
    public void onFailed(Object oResult)
    {
        System.out.println(m_sTaskName + ": Submission failed");
    }


    /**
     * {@inheritDoc}
     */
    public void onProgress(Object oProgress)
    {
        System.out.println(m_sTaskName + ": Submission progress:" + oProgress.toString());
    }


    /**
     * {@inheritDoc}
     */
    public void onStarted()
    {
        System.out.println(m_sTaskName + ": Submission started");
    }


    /**
     * {@inheritDoc}
     */
    public void onSuspended()
    {
        System.out.println(m_sTaskName + ": Submission suspended");
    }
}
