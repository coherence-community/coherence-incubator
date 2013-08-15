/*
 * File: TaskDispatcherBuilder.java
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


import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.task.DefaultTaskDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.task.TaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.internal.Environment;
import com.tangosol.config.annotation.Injectable;

/**
 * The TaskDispatcherBuilder is responsible for building a Dispatcher.
 *
 * @author Paul Mackin
 */
public class TaskDispatcherBuilder implements DispatcherBuilder
{
    /**
     * The TaskDispatchPolicy.
     */
    private TaskDispatchPolicy m_policy;

    /**
     * The TaskDispatcher name.
     */
    private String m_sName;


    /**
     * Realize the Dispatcher.
     *
     * @param env  the Environment
     *
     * @return the Dispatcher
     */
    public Dispatcher realize(Environment env)
    {
        return new DefaultTaskDispatcher(env, m_sName, m_policy);
    }


    /**
     * Set the TaskDispatchPolicy.
     *
     * @param policy the TaskDispatchPolicy
     */
    public void setPolicy(TaskDispatchPolicy policy)
    {
        m_policy = policy;
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
}
