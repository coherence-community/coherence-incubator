/*
 * File: ProcessingPatternNamespaceHandler.java
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

package com.oracle.coherence.patterns.processing.config.xml;


import com.oracle.coherence.patterns.processing.config.builder.AttributeConfig;
import com.oracle.coherence.patterns.processing.config.builder.LocalExecutorDispatcherBuilder;
import com.oracle.coherence.patterns.processing.config.builder.LoggingDispatcherBuilder;
import com.oracle.coherence.patterns.processing.config.builder.TaskProcessorDefinitionBuilder;
import com.oracle.coherence.patterns.processing.config.builder.TaskProcessorDefinitionBuilder.DefaultTaskProcessorConfig;
import com.oracle.coherence.patterns.processing.config.xml.processor.CompositePolicyProcessor;
import com.oracle.coherence.patterns.processing.config.xml.processor.DispatchersProcessor;
import com.oracle.coherence.patterns.processing.config.xml.processor.ProcessingPatternConfigProcessor;
import com.oracle.coherence.patterns.processing.config.xml.processor.TaskDispatcherProcessor;
import com.oracle.coherence.patterns.processing.config.xml.processor.TaskProcessorsProcessor;
import com.oracle.coherence.patterns.processing.dispatchers.task.AttributeMatchTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.RandomTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.dispatchers.task.RoundRobinTaskDispatchPolicy;
import com.oracle.coherence.patterns.processing.internal.LifecycleInterceptor;
import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.RegistrationBehavior;

import java.net.URI;

/**
 * The {@link ProcessingPatternNamespaceHandler} is responsible for capturing
 * and creating the processing pattern configuration when processing a
 * processing pattern configuration file.
 *
 * @author Paul Mackin
 */
public class ProcessingPatternNamespaceHandler extends AbstractNamespaceHandler
{
    /**
     * Standard Constructor.
     */
    public ProcessingPatternNamespaceHandler()
    {
        // define the DocumentPreprocessor for the OperationalConfig namespace
        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        // add the system property pre-processor
        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        setDocumentPreprocessor(dep);

        // define the ElementProcessors for the Namespace

        registerProcessor(ProcessingPatternConfigProcessor.class);
        registerProcessor(DispatchersProcessor.class);
        registerProcessor(TaskProcessorsProcessor.class);
        registerProcessor(TaskDispatcherProcessor.class);
        registerProcessor(CompositePolicyProcessor.class);

        registerElementType("attribute-match-policy", AttributeMatchTaskDispatchPolicy.class);
        registerElementType("round-robin-policy", RoundRobinTaskDispatchPolicy.class);
        registerElementType("random-policy", RandomTaskDispatchPolicy.class);
        registerElementType("taskprocessordefinition", TaskProcessorDefinitionBuilder.class);
        registerElementType("default-taskprocessor", DefaultTaskProcessorConfig.class);

        registerElementType("attribute", AttributeConfig.class);

        registerElementType("logging-dispatcher", LoggingDispatcherBuilder.class);
        registerElementType("local-executor-dispatcher", LocalExecutorDispatcherBuilder.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartNamespace(ProcessingContext context,
                                 XmlElement        element,
                                 String            prefix,
                                 URI               uri)
    {
        super.onStartNamespace(context, element, prefix, uri);

        InterceptorRegistry registryInterceptor = context.getResourceRegistry().getResource(InterceptorRegistry.class);

        if (registryInterceptor != null)
        {
            registryInterceptor.registerEventInterceptor(new LifecycleInterceptor(), RegistrationBehavior.ALWAYS);
        }
    }
}
