/*
 * File: CompositePolicyBuilder.java
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

import com.oracle.coherence.patterns.processing.dispatchers.task.AttributeMatchTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.CompositeTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.RandomTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.RoundRobinTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.TaskDispatchPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * The CompositePolicyBuilder class builds a composite policy.
 *
 * @author Paul Mackin
 */
public class CompositePolicyBuilder
{
    /**
     * The list of policies.
     */
    private List<TaskDispatchPolicy> m_policies = new ArrayList<TaskDispatchPolicy>();


    /**
     * Realize a TaskDispatchPolicy.
     *
     * @return the TaskDispatchPolicy
     */
    public TaskDispatchPolicy realize()
    {
        if (m_policies.size() != 2)
        {
            throw new IllegalStateException("The composite policy must have 2 inner policies");
        }

        return new CompositeTaskDispatchPolicy(m_policies.get(0), m_policies.get(1));
    }


    /**
     * Add the AttributeMatchPolicy to the list of policies.
     */
    public void setAttributeMatchPolicy()
    {
        m_policies.add(new AttributeMatchTaskDispatchPolicy());
    }


    /**
     * Add the RoundRobinPolicy to the list of policies.
     */
    public void setRoundRobinPolicy()
    {
        m_policies.add(new RoundRobinTaskDispatchPolicy());
    }


    /**
     * Add the RandomPolicy to the list of policies.
     */
    public void setRandomPolicy()
    {
        m_policies.add(new RandomTaskDispatchPolicy());
    }


    /**
     * Add a policy to the list of policies.
     */
    public void setPolicy(TaskDispatchPolicy policy)
    {
        m_policies.add(policy);
    }
}
