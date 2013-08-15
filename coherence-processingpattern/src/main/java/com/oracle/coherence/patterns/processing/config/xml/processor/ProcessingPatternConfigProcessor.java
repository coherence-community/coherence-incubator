/*
 * File: ProcessingPatternConfigProcessor.java
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


import com.oracle.coherence.patterns.processing.config.ProcessingPatternConfig;
import com.oracle.coherence.patterns.processing.config.builder.ProcessingPatternConfigBuilder;
import com.oracle.coherence.patterns.processing.internal.DefaultEnvironment;
import com.oracle.coherence.patterns.processing.internal.Environment;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ProcessingPatternConfigProcessor} is responsible for processing a
 * cluster-config {@link XmlElement} to produce a {@link
 * ProcessingPatternConfig} object.
 *
 * @author Paul Mackin
 */
@XmlSimpleName("cluster-config")
public class ProcessingPatternConfigProcessor implements ElementProcessor<ProcessingPatternConfig>
{
    // ----- ElementProcessor interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessingPatternConfig process(ProcessingContext context,
                                           XmlElement        element) throws ConfigurationException
    {
        // get the CacheConfig into which we'll inject configuration
        ProcessingPatternConfigBuilder bldr = new ProcessingPatternConfigBuilder();

        // inject the configuration in the cache config
        context.inject(bldr, element);

        // process the foreign namespace elements in this context so that they
        // may customize the processing pattern config
        context.processForeignElementsOf(element);

        // Add the environment since it is accessed by the PP session
        Environment env = new DefaultEnvironment();

        context.getResourceRegistry().registerResource(Environment.class, env);

        // Add the config since it is accessed by the ProcessingPattern
        ProcessingPatternConfig config = bldr.realize(env);

        context.getResourceRegistry().registerResource(ProcessingPatternConfig.class, config);

        return config;
    }
}
