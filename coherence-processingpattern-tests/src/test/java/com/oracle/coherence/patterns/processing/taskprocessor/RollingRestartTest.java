/*
 * File: RollingRestartTest.java
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

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.StorageDisabledMember;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.concurrent.runnable.RuntimeHalt;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.patterns.processing.ProcessingSession;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.SubmissionOutcome;
import com.oracle.coherence.patterns.processing.SubmissionRetentionPolicy;
import com.oracle.coherence.patterns.processing.internal.DefaultProcessingSession;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmissionConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.text.DateFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Rolling Restart Test
 * <p>
 * Copyright (c) 2017. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class RollingRestartTest
{
    /**
     * The number of rolling-restart iterations.
     */
    private final static int iterations = 5;

    /**
     * The number of task submissions per iteration.
     */
    private final static int submissionsPerIteration = 250;

    /**
     * Establish a {@link CoherenceClusterResource} for our test.
     */
    @Rule
    public CoherenceClusterResource coherenceResource =
            new CoherenceClusterResource()
                    .using(LocalPlatform.get())
                    .with(LocalHost.only(),
                          Multicast.ttl(0),
                          ClusterName.of("RollingRestart"),
                          ClusterPort.automatic(),
                          CacheConfig.of("coherence-processingpattern-restart-cache-config.xml"),
                          Pof.config("coherence-processingpattern-test-pof-config.xml"),
                          SystemProperty.of("tangosol.coherence.extend.address", LocalPlatform.get().getLoopbackAddress().getHostAddress()),
                          SystemProperty.of("tangosol.coherence.extend.port", Capture.of(LocalPlatform.get().getAvailablePorts())))
                    .include(4,
                            DisplayName.of("storage"),
                            RoleName.of("storage"),
                            LocalStorage.enabled(),
                            SystemProperty.of("tangosol.coherence.extend.enabled", false));


    /**
     * Ensure that the Processing Pattern can recover tests during a rolling restart.
     */
    @Test
    @Ignore
    public void shouldRecoverTasksDuringRollingRestart() throws Throwable
    {
        // acquire a storage disabled cache factory for the cluster
        ConfigurableCacheFactory cacheFactory = coherenceResource.createSession(new StorageDisabledMember());

        // establish a processing session
        ProcessingSession session =
            new DefaultProcessingSession(StringBasedIdentifier.newInstance("TaskExecutionSample"
                                                                           + DateFormat.getDateTimeInstance()
                                                                           .format(System.currentTimeMillis())),
                                         cacheFactory);

        SubmissionConfiguration config = new DefaultSubmissionConfiguration();

        // perform a number of rolling restart iterations
        for (int x = 0; x < iterations; ++x)
        {
            SubmissionOutcome[] results = new SubmissionOutcome[submissionsPerIteration];

            System.out.println("Submitting work to the cluster...");

            // submit a number of tasks for processing
            for (int y = 0; y < submissionsPerIteration; y++)
            {
                String id = "Task[" + x + ":" + y + "]";

                results[y] = session.submit(new RestartTask(id),
                                            config,
                                            StringBasedIdentifier.newInstance(id),
                                            SubmissionRetentionPolicy.RemoveOnFinalState,
                                            null);
            }

            // wait for results to come back
            for (int y = 0; y < submissionsPerIteration; y++)
            {
                String id = "Task[" + x + ":" + y + "]";

                System.out.println("Waiting for result... " + id);

                assertThat(results[y].get(), is(id));

                System.out.println("Received result... " + id);
                
                // System.out.println("Client received result from: " + results[y].get());
            }

            // randomly terminate and relaunch all of the servers
            coherenceResource.getCluster().relaunch();

            // we're done submitting work for this iteration
            System.out.println("Iteration " + x + " done successfully...");
        }

        System.out.println("All done successfully...");
    }
}
