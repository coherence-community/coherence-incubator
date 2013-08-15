/*
 * File: TaskProcessorDefinitionBuilder.java
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


import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.patterns.processing.internal.Environment;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.task.TaskProcessorType;
import com.oracle.coherence.patterns.processing.taskprocessor.DefaultTaskProcessor;
import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.util.Base;

import java.util.HashMap;
import java.util.Map;

/**
 * The TaskProcessorDefinitionBuilder is responsible for building a
 * TaskProcessorDefinition.
 *
 * @author Paul Mackin
 */
public class TaskProcessorDefinitionBuilder implements BuilderCustomization<TaskProcessor>
{
    /**
     * The {@link ParameterizedBuilder} used to build the custom instance.
     */
    volatile private ParameterizedBuilder<TaskProcessor> m_bldrCustom;

    /**
     * The task processor id.
     */
    private String m_sId;

    /**
     * The task processor name.
     */
    private String m_sName;

    /**
     * The task processor type.
     */
    private TaskProcessorType m_type;

    /**
     * The task processor config.
     */
    private DefaultTaskProcessorConfig m_taskProcessorConfig = new DefaultTaskProcessorConfig();

    /**
     * The task processor attributes.
     */
    private final Map<String, String> m_mapAttributes = new HashMap<String, String>();


    /**
     * Realize the TaskDispatchPolicy.
     *
     * @param env  the Environment
     *
     * @return the TaskDispatchPolicy
     */
    public TaskProcessorDefinition realize(Environment env)
    {
        TaskProcessor taskProcessor;

        if (m_bldrCustom == null)
        {
            taskProcessor = new DefaultTaskProcessor(m_taskProcessorConfig.m_sId, m_taskProcessorConfig.m_cThreads);
        }
        else
        {
            taskProcessor = m_bldrCustom.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        }

        return new DefaultTaskProcessorDefinition(StringBasedIdentifier.newInstance(m_sId),
                                                  m_sName,
                                                  m_type,
                                                  taskProcessor,
                                                  m_mapAttributes);
    }


    /**
     * Set the display name.
     *
     * @param sName the display name
     */
    @Injectable("displayname")
    public void setDisplayName(String sName)
    {
        m_sName = sName;
    }


    /**
     * Set the ID.
     *
     * @param sId the id
     */
    @Injectable
    public void setId(String sId)
    {
        m_sId = sId;
    }


    /**
     * Set the type.
     *
     * @param sType the type
     */
    @Injectable
    public void setType(String sType)
    {
        if (sType.equalsIgnoreCase("GRID"))
        {
            m_type = TaskProcessorType.GRID;
        }
        else if (sType.equalsIgnoreCase("SINGLE"))
        {
            m_type = TaskProcessorType.SINGLE;
        }
        else
        {
            throw new ConfigurationException(String.format("Invalid TaskProcessorType %s", sType),
                                             String.format("Specify either 'GRID' or 'SINGLE"));
        }

    }


    /**
     * Set the default task processor config.
     *
     * @param config the config
     */
    @Injectable("default-taskprocessor")
    public void setDefaultTaskProcessorConfig(DefaultTaskProcessorConfig config)
    {
        m_taskProcessorConfig = config;
    }


    /**
     * Set the attribute config.
     *
     * @param attributeConfig the attribute config
     */
    @Injectable
    public void setAttribute(AttributeConfig attributeConfig)
    {
        // todo - make sure CODI handles multiple instances of
        m_mapAttributes.put(attributeConfig.getName(), attributeConfig.getsAttribute());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<TaskProcessor> getCustomBuilder()
    {
        return m_bldrCustom;
    }


    /**
     * {@inheritDoc}
     */
    public void setCustomBuilder(ParameterizedBuilder<TaskProcessor> bldr)
    {
        m_bldrCustom = bldr;
    }


    /**
     * The TaskProcessorConfig class specifies the configuration of the
     * default task processor.
     */
    public static class DefaultTaskProcessorConfig
    {
        private String m_sId      = "Default";
        private int    m_cThreads = 1;


        /**
         * Set the id.
         *
         * @param sId the ID
         */
        @Injectable
        public void setId(String sId)
        {
            m_sId = sId;
        }


        /**
         * Set the thread pool size.
         *
         * @param cThreads the number of threads in the thread pool
         */
        @Injectable("threadpoolsize")
        public void setThreadPoolSize(int cThreads)
        {
            m_cThreads = cThreads;
        }
    }
}
