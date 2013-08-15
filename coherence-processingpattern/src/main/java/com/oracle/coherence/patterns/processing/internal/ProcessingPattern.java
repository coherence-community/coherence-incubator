/*
 * File: ProcessingPattern.java
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

package com.oracle.coherence.patterns.processing.internal;


import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.config.ProcessingPatternConfig;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.friends.DispatcherManager;
import com.oracle.coherence.patterns.processing.internal.task.DefaultServerLeaseMonitor;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorDefinitionManager;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.ServerLeaseMonitor;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorDefinitionManager;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMBeanManager;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.oracle.coherence.patterns.processing.taskprocessor.DefaultClientLeaseMaintainer;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.util.Base;


/**
 * The ProcessingPattern class represents the high level Processing Pattern and
 * is responsible for performing the infrastructure bootstrap.
 *
 * @author Paul Mackin
 */
public class ProcessingPattern
{
    ConfigurableCacheFactory m_ccf;
    Environment              m_env;
    ProcessingPatternConfig  m_config;

    // core data
    DefaultDispatcherManager               m_mgrDispatcher;
    DefaultTaskProcessorDefinitionManager  m_mgrTaskProcessorDefinition;
    DefaultDispatchController              m_dispatchController;

    // dispatching data
    ObjectProxyFactory<SubmissionResult>       m_submissionResultProxyFactory;
    ObjectProxyFactory<TaskProcessorMediator>  m_taskProcessorProxyFactory;

    // server side processing data
    ServerLeaseMonitor        m_serverLeaseMonitor;
    TaskProcessorMBeanManager m_taskProcessorMBeanManager;

    /**
     * Flag indicating if the ProcessingPattern infrastructure has been started.
     */
    private static boolean m_fStarted = false;


    /**
     * Construct a ProcessingPattern
     *
     * @param ccf     the ConfigurableCacheFactory
     * @param config  the ProcessingPattern configuration
     */
    public ProcessingPattern(ConfigurableCacheFactory ccf, Environment env, ProcessingPatternConfig config)
    {
        m_ccf    = ccf;
        m_env    = env;
        m_config = config;
    }

    /**
     * Ensure that the ProcessingPattern infrastructure has been setup up.
     *
     * @param ccf  the ConfigurableCacheFactory used by Processing Pattern
     */
    public static void ensureInfrastructureStarted(ConfigurableCacheFactory ccf)
    {
        if (m_fStarted)
            return;

        synchronized (ProcessingPatternConfig.class)
        {
            if (!m_fStarted)
            {
                // These objects are put in registry by ProcessingPatternConfigProcessor
                ProcessingPatternConfig config = ccf.getResourceRegistry().getResource(ProcessingPatternConfig.class);
                Base.checkNotNull(config, "ProcessingPatternConfig");

                Environment env = ccf.getResourceRegistry().getResource(Environment.class);
                Base.checkNotNull(env, "Environment");

                ProcessingPattern pp = new ProcessingPattern(ccf, env, config);

                // Start the ProcessingPattern
                pp.start();

                m_fStarted = true;
            }
        }
    }

    /**
     * Start the processing pattern infrastructure.
     */
    private void start()
    {
        Environment env = m_env;

        // config core
        m_taskProcessorProxyFactory = new ObjectProxyFactory<TaskProcessorMediator>(DefaultTaskProcessorMediator.CACHENAME,
                TaskProcessorMediator.class);
        env.registerResource(TaskProcessorMediator.class, m_taskProcessorProxyFactory);

        // configure dispatcher
        m_mgrDispatcher = new DefaultDispatcherManager(m_ccf);
        env.registerResource(DispatcherManager.class, m_mgrDispatcher);

        m_mgrTaskProcessorDefinition = new DefaultTaskProcessorDefinitionManager(env, m_ccf);
        env.registerResource(TaskProcessorDefinitionManager.class, m_mgrTaskProcessorDefinition);

        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory = 
                (ObjectProxyFactory<SubmissionResult>)env.getResource(SubmissionResult.class);
        m_dispatchController = new DefaultDispatchController(m_ccf, submissionResultProxyFactory);
        env.registerResource(DispatchController.class, m_dispatchController);

        // configure task processing server side
        m_serverLeaseMonitor = new DefaultServerLeaseMonitor(m_env, 20000);
        env.registerResource(ServerLeaseMonitor.class, m_serverLeaseMonitor);

        m_taskProcessorMBeanManager = new TaskProcessorMBeanManager();
        env.registerResource(TaskProcessorMBeanManager.class, m_taskProcessorMBeanManager);


        // Register task processor definitions and load them into the cache
        try
        {
            // Create the configured PP objects and put them int the caches
            for (TaskProcessorDefinition definition : m_config.getDefinitions())
            {
                m_mgrTaskProcessorDefinition.registerTaskProcessor(definition);
            }
            m_mgrTaskProcessorDefinition.onDependenciesSatisfied(m_env);
        }
        catch (Throwable t)
        {
            throw Base.ensureRuntimeException(t);
        }

        // Register task dispatchers and load them into the cache
        try
        {
            // Create the configured PP objects and put them int the caches
            int i = 0;
            for (Dispatcher dispatcher : m_config.getDispatchers())
            {
                // todo - get priority
                m_mgrDispatcher.registerDispatcher(i++, dispatcher);
            }
            m_mgrDispatcher.onDependenciesSatisfied(env);

        }
        catch (Throwable t)
        {
            throw Base.ensureRuntimeException(t);
        }
        
        m_dispatchController.onDependenciesSatisfied(env);
    }
    
    /**
     * Create the client side processing pattern objects.
     *
     * @param env  the Environment
     */
    public static void createClientSideObjects(Environment env)
    {
        // Create and load objects needed by the client
        ObjectProxyFactory<Submission> submissionProxyFactory =
            new ObjectProxyFactory<Submission>(DefaultSubmission.CACHENAME,
                                               Submission.class);

        env.registerResource(Submission.class, submissionProxyFactory);

        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory =
            new ObjectProxyFactory<SubmissionResult>(DefaultSubmissionResult.CACHENAME,
                                                     SubmissionResult.class);

        env.registerResource(SubmissionResult.class, submissionResultProxyFactory);

        DefaultClientLeaseMaintainer clientLeaseMaintainer = new DefaultClientLeaseMaintainer(env, 10000);

        env.registerResource(ClientLeaseMaintainer.class, clientLeaseMaintainer);
    }

}
