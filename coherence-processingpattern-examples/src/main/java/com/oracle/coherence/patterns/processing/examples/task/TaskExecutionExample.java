/*
 * File: TaskExecutionExample.java
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

import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.patterns.processing.ProcessingSession;
import com.oracle.coherence.patterns.processing.SubmissionOutcome;
import com.oracle.coherence.patterns.processing.internal.DefaultProcessingSession;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmissionConfiguration;
import com.tangosol.net.CacheFactory;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This sample illustrates submitting a simple MessageTask and
 * submitting two different task types, which is executed by two different executors.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class TaskExecutionExample
{
    /**
     * Example Entry Point
     *
     * @param args
     *
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable
    {
        TaskExecutionExample sample = new TaskExecutionExample();

        try
        {
            sample.executeTask();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * The example task
     *
     * @throws Throwable
     */
    public void executeTask() throws Throwable
    {
        System.out.println("Testing TaskExecution");

        // We will use a ProcessingSession that we can use for submitting work.
        ProcessingSession session =
            new DefaultProcessingSession(StringBasedIdentifier
                .newInstance("TaskExecutionSample"
                             + DateFormat.getDateTimeInstance().format(System.currentTimeMillis())));

        // We submit 10 TestTasks of type "grid"
        ArrayList<SubmissionOutcome> tasklist = new ArrayList<SubmissionOutcome>();

        for (int i = 0; i < 10; i++)
        {
            String              taskName = "Grid Testtask:" + Integer.toString(i);
            Map<String, String> attrMap  = new HashMap<String, String>();

            attrMap.put("type", "grid");

            SubmissionOutcome outcome = session.submit(new TestTask(taskName),
                                                       new DefaultSubmissionConfiguration(attrMap),
                                                       new SubmissionCallback(taskName));

            tasklist.add(outcome);
        }

        // And then we will submit 10 TestTasks of type "single"
        for (int i = 0; i < 10; i++)
        {
            String              taskName = "Single Testtask:" + Integer.toString(i);
            Map<String, String> attrMap  = new HashMap<String, String>();

            attrMap.put("type", "single");

            SubmissionOutcome outcome = session.submit(new TestTask(taskName),
                                                       new DefaultSubmissionConfiguration(),
                                                       new SubmissionCallback(taskName));

            tasklist.add(outcome);
        }

        // And then we get all the results.
        // Calling get on the SubmissionOutcome will wait for the Task to finish.
        for (int i = 0; i < tasklist.size(); i++)
        {
            System.out.println("Result:" + Integer.toString(i) + ":" + tasklist.get(i).get().toString());
        }

        System.out.println("Finished");

        CacheFactory.shutdown();

    }
}
