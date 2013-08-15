/*
 * File: TaskProcessorsProcessor.java
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


import com.oracle.coherence.patterns.processing.config.builder.TaskProcessorDefinitionBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link TaskProcessorsProcessor} is responsible for processing
 * &lt;dispatchers&gt; {@link XmlElement}s to produce a list of
 * TaskProcessors.
 *
 * @author Paul Mackin
 */
@XmlSimpleName("taskprocessors")
public class TaskProcessorsProcessor implements ElementProcessor<List<TaskProcessorDefinitionBuilder>>
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<TaskProcessorDefinitionBuilder> process(ProcessingContext context,
                                                        XmlElement        element) throws ConfigurationException
    {
        List<TaskProcessorDefinitionBuilder> listBldrs = new ArrayList<TaskProcessorDefinitionBuilder>();

        for (XmlElement elementChild : ((List<XmlElement>) element.getElementList()))
        {
            Object oValue = context.processElement(elementChild);

            if (oValue instanceof TaskProcessorDefinitionBuilder)
            {
                listBldrs.add((TaskProcessorDefinitionBuilder) oValue);
            }
            else
            {
                throw new ConfigurationException(String.format("Invalid dispatcher definition [%s] in [%s]",
                                                               elementChild, element),
                                                 "Please ensure the dispatcher is correctly defined");
            }
        }

        return listBldrs;
    }
}
