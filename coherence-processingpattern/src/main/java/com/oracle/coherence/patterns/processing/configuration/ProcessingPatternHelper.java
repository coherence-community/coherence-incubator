/*
 * File: ProcessingPatternHelper.java
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

package com.oracle.coherence.patterns.processing.configuration;

import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.friends.DispatcherManager;
import com.oracle.coherence.patterns.processing.internal.DefaultDispatchController;
import com.oracle.coherence.patterns.processing.internal.DefaultDispatcherManager;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmission;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmissionResult;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.DefaultServerLeaseMonitor;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorDefinitionManager;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.ServerLeaseMonitor;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorDefinitionManager;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMBeanManager;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.oracle.coherence.patterns.processing.taskprocessor.DefaultClientLeaseMaintainer;
import com.tangosol.net.ConfigurableCacheFactory;

/**
 * A {@link ProcessingPatternHelper} helping out in setting up the
 * {@link Environment} for the Processing Pattern.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ProcessingPatternHelper
{
    /**
     * Configures the dispatching logic.
     *
     * @param ccFactory the {@link ConfigurableCacheFactory} to use
     * @param environment the {@link Environment} to use
     */
    @SuppressWarnings("unchecked")
    public static void configureDispatchingLogic(ConfigurableCacheFactory ccFactory,
                                                 Environment              environment)
    {
        environment.registerResource(DispatcherManager.class, new DefaultDispatcherManager(ccFactory));
        environment.registerResource(TaskProcessorDefinitionManager.class,
                                     new DefaultTaskProcessorDefinitionManager(environment, ccFactory));
        environment.registerResource(DispatchController.class,
                                     new DefaultDispatchController(ccFactory,
                                                                   (ObjectProxyFactory<SubmissionResult>) environment
                                                                       .getResource(SubmissionResult.class)));
    }


    /**
     * Configures the task dispatching on the server side.
     *
     * @param ccFactory the {@link ConfigurableCacheFactory} to use
     * @param environment the {@link Environment} to use
     */
    public static void configureTaskProcessingServerSide(ConfigurableCacheFactory ccFactory,
                                                         Environment              environment)
    {
        environment.registerResource(ServerLeaseMonitor.class, new DefaultServerLeaseMonitor(environment, 20000));
        environment.registerResource(TaskProcessorMBeanManager.class, new TaskProcessorMBeanManager());
    }


    /**
     * Configures the dispatching logic.
     *
     * @param ccFactory the {@link ConfigurableCacheFactory} to use
     * @param environment the {@link Environment} to use
     */
    public static void configureTaskProcessing(ConfigurableCacheFactory ccFactory,
                                               Environment              environment)
    {
        environment.registerResource(TaskProcessorDefinitionManager.class,
                                     new DefaultTaskProcessorDefinitionManager(environment, ccFactory));
        environment.registerResource(ClientLeaseMaintainer.class, new DefaultClientLeaseMaintainer(environment, 10000));
    }


    /**
     * Configures the basic {@link ObjectProxyFactory} instances.
     *
     * @param ccFactory the {@link ConfigurableCacheFactory} to use
     * @param environment the {@link Environment} to use
     */
    public static void configureCore(ConfigurableCacheFactory ccFactory,
                                     Environment              environment)
    {
        environment.registerResource(TaskProcessorMediator.class,
                                     new ObjectProxyFactory<TaskProcessorMediator>(DefaultTaskProcessorMediator
                                         .CACHENAME,
                                                                                   TaskProcessorMediator.class));

        environment.registerResource(Submission.class,
                                     new ObjectProxyFactory<Submission>(DefaultSubmission.CACHENAME, Submission.class));

        environment.registerResource(SubmissionResult.class,
                                     new ObjectProxyFactory<SubmissionResult>(DefaultSubmissionResult.CACHENAME,
                                                                              SubmissionResult.class));
    }


    /**
     * Configures all the parts for a full cluster node.
     *
     * @param ccFactory the {@link ConfigurableCacheFactory} to use
     * @param environment the {@link Environment} to use
     */
    public static void configureFramework(ConfigurableCacheFactory ccFactory,
                                          Environment              environment)
    {
        configureCore(ccFactory, environment);
        configureDispatchingLogic(ccFactory, environment);
        configureTaskProcessingServerSide(ccFactory, environment);
        configureTaskProcessing(ccFactory, environment);
    }
}
