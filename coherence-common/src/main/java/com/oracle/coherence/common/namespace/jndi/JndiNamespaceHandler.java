/*
 * File: JndiBasedNamespaceHandler.java
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

package com.oracle.coherence.common.namespace.jndi;


import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;

import java.util.Hashtable;
import java.util.Iterator;


/**
 * The {@link JndiNamespaceHandler} is responsible for capturing and
 * creating the event distribution pattern configuration when processing a event
 * distribution configuration file.
 *
 * @author Brian Oliver
 *
 */
public class JndiNamespaceHandler
        extends AbstractNamespaceHandler
{
    // ----- constructors ---------------------------------------------------

    /**
     * Standard Constructor.
     */
    public JndiNamespaceHandler()
    {
        // define the DocumentPreprocessor for the OperationalConfig namespace
        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        // add the system property pre-processor
        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        setDocumentPreprocessor(dep);

        registerProcessor("resource"
                , new ElementProcessor<JndiBasedParameterizedBuilder>()
        {
            @Override
            public JndiBasedParameterizedBuilder process(
                    ProcessingContext context,
                    XmlElement xmlElement)
                    throws ConfigurationException
            {
                Hashtable<String, Object> env = new Hashtable<String, Object>();

                XmlElement xmlInitParams = xmlElement.getElement("init-params");

                if (xmlInitParams != null)
                {
                    ParameterList listParameters = (ParameterList) context.processElement(xmlInitParams);

                    ParameterResolver nullResolver = new NullParameterResolver();

                    Iterator<Parameter> iter =  listParameters.iterator();
                    while (iter.hasNext())
                    {
                        Parameter param = iter.next();
                        env.put(param.getName(),param.evaluate(nullResolver).get());
                    }
                }

                JndiBasedParameterizedBuilder bldrJndi = context.inject(new JndiBasedParameterizedBuilder(),xmlElement);
                bldrJndi.setParams(env);

                return bldrJndi;
            }
        });

    }
}