/*
 * File: SimpleCallableExample.java
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

package com.oracle.coherence.patterns.processing.examples.simplecallable;

import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.patterns.processing.ProcessingSession;
import com.oracle.coherence.patterns.processing.SubmissionOutcome;
import com.oracle.coherence.patterns.processing.internal.DefaultProcessingSession;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmissionConfiguration;
import com.tangosol.net.CacheFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * A sample demonstrating a Callable using the processing pattern.
 *  <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class SimpleCallableExample
{
    /**
     * The executeTask method initializes the pattern and submits the Callable
     * for execution, then waits for the result.
     *
     * @throws Throwable
     */
    public void runSample() throws Throwable
    {
        try
        {
            // Retrieve the ProcessingSessionFactory to get a Session.

            final ProcessingSession oProcessingSession =
                new DefaultProcessingSession(StringBasedIdentifier.newInstance("SimpleCallableExample "
                                                                               + System.currentTimeMillis()));

            // Then we create and submit 10 callables
            ArrayList<SubmissionOutcome> submissionlist = new ArrayList<SubmissionOutcome>();

            for (int i = 0; i < 10; i++)
            {
                // We create a callable
                Callable aCallable = new SimpleCallable();

                // Then we submit this callable for execution.
                SubmissionOutcome outcome = oProcessingSession.submit(aCallable,
                                                                      new DefaultSubmissionConfiguration(),
                                                                      new SubmissionCallback("Submission:" + i));

                // And add it to a list to keep track of it
                submissionlist.add(outcome);
            }

            // And finally we will get all the results from all submissions
            for (int i = 0; i < submissionlist.size(); i++)
            {
                // The call to the SubmissionOutcome.get() will block if the
                // Submission is not completed.
                System.out.println("Result:" + Integer.toString(i) + ":" + submissionlist.get(i).get().toString());

                Date submittime = new Date(submissionlist.get(i).getSubmissionTime());

                System.out.println("Submitted at:" + submittime.toString() + " Latency:"
                                   + Long.toString(submissionlist.get(i).getWaitDuration()) + " Execution took "
                                   + Long.toString(submissionlist.get(i).getExecutionDuration()) + " msec.");
            }

            Thread.sleep(4000);

            CacheFactory.shutdown();
        }
        catch (Exception e)
        {
            System.out.println("Exception caught:" + e.getMessage());

            e.printStackTrace();
        }

    }


    /**
     * Main method of the Sample.
     *
     * @param args input arguments
     */
    public static void main(final String[] args)
    {
        final SimpleCallableExample oSample = new SimpleCallableExample();

        try
        {
            oSample.runSample();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }

    }
}
