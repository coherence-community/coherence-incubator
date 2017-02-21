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
import java.util.Random;

/**
 * A simple restartable task that delays a random amount of time between 0 and 1 seconds.
 * <p>
 * Copyright (c) 2017. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class RestartTask implements ResumableTask, PortableObject
{
    private String name;


    /**
     * Constructs a {@link RestartTask}.
     */
    public RestartTask()
    {
    }


    /**
     * Constructs a {@link RestartTask}.
     *
     * @param name  the name of the task (that is returned)
     */
    public RestartTask(String name)
    {
        this.name = name;
    }


    @Override
    public String toString()
    {
        return "RestartTask [" + (name != null ? "sName=" + name : "") + "]";
    }


    @Override
    public Object run(TaskExecutionEnvironment oEnvironment)
    {
        CacheFactory.log(name + " processing...", CacheFactory.LOG_ALWAYS);

        Random random = new Random();

        try
        {
            Thread.sleep(random.nextInt(1000));
        }
        catch (InterruptedException e)
        {
            CacheFactory.log(name + " interrupted!");
        }

        CacheFactory.log(name + " processed...", CacheFactory.LOG_ALWAYS);
         
        return name;
    }


    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.name = reader.readString(0);
    }


    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, name);
    }
}
