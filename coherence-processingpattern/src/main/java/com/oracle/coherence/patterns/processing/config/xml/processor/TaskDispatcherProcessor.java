/*
 * File: TaskDispatcherProcessor.java
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

package com.oracle.coherence.patterns.processing.config.xml.processor;


import com.oracle.coherence.patterns.processing.config.builder.TaskDispatchPolicyBuilder;
import com.oracle.coherence.patterns.processing.config.builder.TaskDispatcherBuilder;
import com.tangosol.coherence.config.xml.processor.CustomizableBuilderProcessor;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link TaskDispatcherProcessor} is responsible for processing
 * &lt;task-dispatcher&gt; {@link XmlElement}s to produce a list of
 * TaskDispatcherBuilder.
 *
 * @author Paul Mackin
 */
@XmlSimpleName("task-dispatcher")
public class TaskDispatcherProcessor implements ElementProcessor<TaskDispatcherBuilder>
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public TaskDispatcherBuilder process(ProcessingContext context,
                                         XmlElement        element) throws ConfigurationException
    {
        // normally, the CustomizableBuilderProcessor would build the top level object (TaskDispatcher).  However
        // the XML specifies that the custom instance is the TaskDispatchPolicy which is an inner element.  This
        // is why the TaskDispatcherPolicyBuilder is used.
        CustomizableBuilderProcessor<TaskDispatchPolicyBuilder> policyProcessor =
            new CustomizableBuilderProcessor<TaskDispatchPolicyBuilder>(TaskDispatchPolicyBuilder.class);

        TaskDispatchPolicyBuilder bldrPolicy     = policyProcessor.process(context, element);

        TaskDispatcherBuilder     bldrDispatcher = new TaskDispatcherBuilder();

        bldrDispatcher.setPolicy(bldrPolicy.realize());

        context.inject(bldrDispatcher, element);

        return bldrDispatcher;
    }
}
