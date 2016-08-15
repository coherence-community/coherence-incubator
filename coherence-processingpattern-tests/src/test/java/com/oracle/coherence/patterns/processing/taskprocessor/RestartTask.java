/*
 * File: RestartTask.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.processing.taskprocessor;

import com.oracle.coherence.patterns.processing.task.ResumableTask;
import com.oracle.coherence.patterns.processing.task.TaskExecutionEnvironment;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;

import java.io.IOException;

/**
 * A simple restartable task.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class RestartTask implements ResumableTask, PortableObject
{
    private String sName;


    /**
     * Constructs a {@link RestartTask}.
     */
    public RestartTask()
    {
    }


    /**
     * Constructs a {@link RestartTask}.
     *
     * @param sName
     */
    public RestartTask(String sName)
    {
        this.sName = sName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "RestartTask [" + (sName != null ? "sName=" + sName : "") + "]";
    }


    /**
     * Method description
     *
     * @param oEnvironment
     *
     * @return
     */
    @Override
    public Object run(TaskExecutionEnvironment oEnvironment)
    {
        CacheFactory.log(sName + " processed...", CacheFactory.LOG_ALWAYS);

        return sName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.sName = reader.readString(0);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, sName);
    }
}
