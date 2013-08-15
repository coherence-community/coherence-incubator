/*
 * File: InitParamsProcessor.java
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

package com.oracle.coherence.patterns.eventdistribution.configuration;


import com.oracle.coherence.common.expression.SerializableResolvableParameterList;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

import java.util.List;

/**
 * An {@link InitParamsProcessor} is responsible for processing &lt;init-params&gt; {@link XmlElement}s to produce
 * {@link SerializableResolvableParameterList}s.
 *
 * @author Brian Oliver
 */
@XmlSimpleName("init-params")
public class InitParamsProcessor implements ElementProcessor<ParameterResolver>
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public SerializableResolvableParameterList process(ProcessingContext context,
                                                       XmlElement        element) throws ConfigurationException
    {
        SerializableResolvableParameterList listParameters = new SerializableResolvableParameterList();

        for (XmlElement elementChild : ((List<XmlElement>) element.getElementList()))
        {
            Object oValue = context.processElement(elementChild);

            if (oValue instanceof Parameter)
            {
                listParameters.add((Parameter) oValue);
            }
            else
            {
                throw new ConfigurationException(String.format("Invalid parameter definition [%s] in [%s]",
                                                               elementChild, element),
                                                 "Please ensure the parameter is correctly defined");
            }
        }

        return listParameters;
    }
}
