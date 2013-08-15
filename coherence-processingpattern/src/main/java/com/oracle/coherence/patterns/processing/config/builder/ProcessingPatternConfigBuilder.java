/*
 * File: ProcessingPatternConfigBuilder.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.processing.config.builder;


import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.patterns.processing.config.ProcessingPatternConfig;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmission;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmissionResult;
import com.oracle.coherence.patterns.processing.internal.Environment;
import com.oracle.coherence.patterns.processing.internal.ProcessingPattern;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.oracle.coherence.patterns.processing.taskprocessor.DefaultClientLeaseMaintainer;
import com.tangosol.config.annotation.Injectable;

import java.util.ArrayList;
import java.util.List;

/**
 * The ProcessingPatternBuilder is responsible for building the high level ProcessingPattern
 * configuration object.
 *
 * @author Paul Mackin
 */
public class ProcessingPatternConfigBuilder
{
    /**
     * The list of DispatchBuilder(s).
     */
    private volatile List<DispatcherBuilder> m_listDispatchBuilders = new ArrayList<DispatcherBuilder>();

    /**
     * The list of TaskProcessorDefinitionBuilder(s).
     */
    private volatile List<TaskProcessorDefinitionBuilder> m_listTaskProcessBuilders =
        new ArrayList<TaskProcessorDefinitionBuilder>();


    /**
     * Realize the ProcessingPatternConfig.
     *
     * @param env  the Environment
     *
     * @return the ProcessingPatternConfig
     */
    public ProcessingPatternConfig realize(Environment env)
    {
        ProcessingPattern.createClientSideObjects(env);

        List<Dispatcher>              listDispatchers          = new ArrayList<Dispatcher>();
        List<TaskProcessorDefinition> listProcessorDefinitions = new ArrayList<TaskProcessorDefinition>();

        for (DispatcherBuilder bldr : m_listDispatchBuilders)
        {
            listDispatchers.add(bldr.realize(env));
        }

        for (TaskProcessorDefinitionBuilder bldr : m_listTaskProcessBuilders)
        {
            listProcessorDefinitions.add(bldr.realize(env));
        }

        return new ProcessingPatternConfig(listDispatchers, listProcessorDefinitions);
    }


    /**
     * Set the list of DispatcherBuilder(s).
     *
     * @param listBldrs  the list of DispatcherBuilder(s)
     */
    @Injectable("dispatchers")
    public void setDispatchers(List<DispatcherBuilder> listBldrs)
    {
        m_listDispatchBuilders = listBldrs;
    }


    /**
     * Set the list of TaskProcessorDefinitionBuilder(s).
     *
     * @param listBldrs  the list of TaskProcessorDefinitionBuilder(s)
     */
    @Injectable("taskprocessors")
    public void setTaskProcessors(List<TaskProcessorDefinitionBuilder> listBldrs)
    {
        m_listTaskProcessBuilders = listBldrs;
    }


}
