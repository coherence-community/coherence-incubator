/*
 * File: TaskProcessorMediatorProxy.java
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

package com.oracle.coherence.patterns.processing.internal.task;

/**
 * The {@link TaskProcessorMediatorProxy} acts as a JVM local proxy
 * for the MBean interface to a {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator} in the grid.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class TaskProcessorMediatorProxy implements TaskProcessorMediatorProxyMBean
{
    /**
     * The key to the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator} this proxy represents.
     */
    private TaskProcessorMediatorKey key;

    /**
     * The proxy value kept as a shadow of the real {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator}.
     */
    private TaskProcessorMediatorProxyMBean value;

    /**
     * The MBean name as registered with the MBeanServer.
     */
    private String mBeanName;


    /**
     * Constructor.
     *
     * @param key the {@link TaskProcessorMediatorKey} to the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator}
     */
    public TaskProcessorMediatorProxy(TaskProcessorMediatorKey key)
    {
        this.key   = key;
        this.value = null;
    }


    /**
     * {@inheritDoc}
     */
    public String getKey()
    {
        return key.toString();
    }


    /**
     * Sets the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorProxyMBean} acting as a proxy for the real {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator}.
     *
     * @param value the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorProxyMBean} value
     */
    public void setValue(TaskProcessorMediatorProxyMBean value)
    {
        this.value = value;
    }


    /**
     * <p>Sets the cluster-wide unique JMX bean name for
     * the {@link com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator}.</p>
     *
     * @param mBeanName the name of the mBean
     */
    protected void setMBeanName(String mBeanName)
    {
        this.mBeanName = mBeanName;
    }


    /**
     * @return the registered mBean name
     */
    protected String getMBeanName()
    {
        return mBeanName;
    }


    /**
     * {@inheritDoc}
     */
    public int getAcceptedTaskCount()
    {
        if (value != null)
        {
            return value.getAcceptedTaskCount();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int getExecutedTaskCount()
    {
        if (value != null)
        {
            return value.getExecutedTaskCount();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int getYieldTaskCount()
    {
        if (value != null)
        {
            return value.getYieldTaskCount();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getIdsOfCurrentlyExecutingTask()
    {
        if (value != null)
        {
            return value.getIdsOfCurrentlyExecutingTask();
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public double getAverageTaskExecutionDuration()
    {
        if (value != null)
        {
            return value.getAverageTaskExecutionDuration();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getLastTaskExecutionDuration()
    {
        if (value != null)
        {
            return value.getLastTaskExecutionDuration();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getMaximumTaskExecutionDuration()
    {
        if (value != null)
        {
            return value.getMaximumTaskExecutionDuration();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getMinimumTaskExecutionDuration()
    {
        if (value != null)
        {
            return value.getMinimumTaskExecutionDuration();
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalTaskExecutionDuration()
    {
        if (value != null)
        {
            return value.getTotalTaskExecutionDuration();
        }
        else
        {
            return 0;
        }
    }
}
