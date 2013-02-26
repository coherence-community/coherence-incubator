/*
 * File: PiCalculationExample.java
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

/**
 * Illustrates how to submit a Task that yields and stores state.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class PiCalculationExample
{
    /**
     * Main method of sample.
     *
     * @param args input arguments to application
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable
    {
        PiCalculationExample sample = new PiCalculationExample();

        try
        {
            sample.executeSample();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Method that executes the sample.
     *
     * @throws Throwable
     *
     * @throws ExecutorAlreadyExistsException    if executor is already registered
     */
    public void executeSample() throws Throwable
    {
        System.out.println("Testing PI calculation");

        // Create the ProcessingSession
        ProcessingSession session =
            new DefaultProcessingSession(StringBasedIdentifier
                .newInstance("PICalculationSample "
                             + DateFormat.getDateTimeInstance().format(System.currentTimeMillis())));

        // Then we create three tasks that calculate Pi
        SubmissionOutcome pioutcome = session.submit(new CalculatePITask(2000,
                                                                         "2K-PI"),
                                                     new DefaultSubmissionConfiguration());
        SubmissionOutcome pioutcome2 = session.submit(new CalculatePITask(1000,
                                                                          "1K-PI"),
                                                      new DefaultSubmissionConfiguration());
        SubmissionOutcome pioutcome3 = session.submit(new CalculatePITask(100,
                                                                          "100-PI"),
                                                      new DefaultSubmissionConfiguration());

        // Now get (and wait for) the result of the calculation.
        System.out.println("Pi calculated in 2000 iterations is: " + pioutcome.get().toString());
        System.out.println("Pi calculated in 1000 iterations is: " + pioutcome2.get().toString());
        System.out.println("Pi calculated in 100  iterations is: " + pioutcome3.get().toString());

        CacheFactory.shutdown();
    }
}
