/*
 * File: ProcessingPatternConfig.java
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

package com.oracle.coherence.patterns.processing.config;

import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;

import java.util.List;

/**
 * A {@link ProcessingPatternConfig} is the top-level container for Coherence
 * Incubator ProcessingPattern configuration.
 *
 * @author Paul Mackin
 */
public class ProcessingPatternConfig
{
    /**
     * The list of TaskProcessorDefinitions.
     */
    private final List<TaskProcessorDefinition> f_listDefinitions;

    /**
     * The list of Dispatchers.
     */
    private final List<Dispatcher> f_listDispatchers;


    /**
     * Construct a ProcessingPattern configuration object.
     *
     * @param listDispatchers the list of Dispatchers
     * @param listDefinitions the list of TaskProcessor definitions
     */
    public ProcessingPatternConfig(List<Dispatcher>              listDispatchers,
                                   List<TaskProcessorDefinition> listDefinitions)
    {
        f_listDispatchers = listDispatchers;
        f_listDefinitions = listDefinitions;
    }


    /**
     * Method description
     *
     * @return
     */
    public List<Dispatcher> getDispatchers()
    {
        return f_listDispatchers;
    }


    /**
     * Method description
     *
     * @return
     */
    public List<TaskProcessorDefinition> getDefinitions()
    {
        return f_listDefinitions;
    }
}
