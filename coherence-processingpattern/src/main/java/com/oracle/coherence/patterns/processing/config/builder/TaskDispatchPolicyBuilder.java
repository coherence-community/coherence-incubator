/*
 * File: TaskDispatchPolicyBuilder.java
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


import com.oracle.coherence.patterns.processing.dispatchers.task.RoundRobinTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.TaskDispatchPolicy;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.NullParameterResolver;

/**
 * The TaskDispatcherBuilder is responsible for building a
 * TaskDispatchPolicy.
 *
 * @author Paul Mackin
 */
public class TaskDispatchPolicyBuilder implements BuilderCustomization<TaskDispatchPolicy>
{
    /**
     * The {@link ParameterizedBuilder} used to build the custom instance.
     */
    volatile private ParameterizedBuilder<TaskDispatchPolicy> m_bldrCustom;

    /**
     * The (@link TaskDispatchPolicy}.
     */
    private TaskDispatchPolicy m_policy;


    /**
     * Realize the TaskDispatchPolicy.
     *
     * @return the TaskDispatchPolicy
     */
    public TaskDispatchPolicy realize()
    {
        TaskDispatchPolicy policy = m_policy;

        if (policy == null)
        {
            if (m_bldrCustom == null)
            {
                policy = new RoundRobinTaskDispatchPolicy();
            }
            else
            {
                policy = m_bldrCustom.realize(new NullParameterResolver(),
                                              Thread.currentThread().getContextClassLoader(),
                                              new ResolvableParameterList());
            }
        }

        return policy;
    }


    /**
     * Set the policy to a be an AttributeMatchPolicy.
     *
     * @param policy the AttributeMatchPolicy
     */
    @Injectable
    public void setAttributeMatchPolicy(TaskDispatchPolicy policy)
    {
        m_policy = policy;
    }


    /**
     * Set the policy to be a RoundRobinPolicy.
     *
     * @param policy the RoundRobinPolicy
     */
    @Injectable
    public void setRoundRobinPolicy(TaskDispatchPolicy policy)
    {
        m_policy = policy;
    }


    /**
     * Set the policy to be a RandomPolicy.
     *
     * @param policy the RandomPolicy
     */
    @Injectable
    public void setRandomPolicy(TaskDispatchPolicy policy)
    {
        m_policy = policy;
    }


    /**
     * Add the CompositePolicy to the list of policies.
     *
     * @param bldr the CompositePolicyBuilder
     */
    @Injectable
    public void setCompositePolicy(CompositePolicyBuilder bldr)
    {
        m_policy = bldr.realize();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<TaskDispatchPolicy> getCustomBuilder()
    {
        return m_bldrCustom;
    }


    /**
     * {@inheritDoc}
     */
    public void setCustomBuilder(ParameterizedBuilder<TaskDispatchPolicy> bldr)
    {
        m_bldrCustom = bldr;
    }
}
