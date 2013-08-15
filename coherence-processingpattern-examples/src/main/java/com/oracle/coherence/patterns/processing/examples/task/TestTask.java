/*
 * File: TestTask.java
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

package com.oracle.coherence.patterns.processing.examples.task;

import com.oracle.coherence.patterns.processing.task.ResumableTask;
import com.oracle.coherence.patterns.processing.task.TaskExecutionEnvironment;
import com.oracle.coherence.patterns.processing.task.Yield;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The TestTask will retrieve the member ID
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class TestTask implements ResumableTask, ExternalizableLite, PortableObject
{
    private String m_sMessage;


    /**
     * Constructs a {@link TestTask}
     */
    public TestTask()
    {
        super();
    }


    /**
     * Constructs {@link TestTask}
     *
     * @param message
     */
    public TestTask(String message)
    {
        m_sMessage = message;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        m_sMessage = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, m_sMessage);

    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_sMessage = reader.readString(10);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(10, m_sMessage);

    }


    /**
     * {@inheritDoc}
     */
    public Object run(TaskExecutionEnvironment environment)
    {
        if (!environment.isResuming())
        {
            Cluster cluster = CacheFactory.getCluster();
            String MemberID = "Member ID:" + cluster.getLocalMember().getId() + " Cluster name:"
                              + cluster.getClusterName();

            System.out.println(MemberID);
            environment.reportProgress(50);

            return new Yield(MemberID, 1000);
        }
        else
        {
            String messageToReturn = (String) environment.loadCheckpoint();

            environment.reportProgress(100);

            return messageToReturn;
        }
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "TestTask:" + getType() + ": " + m_sMessage;
    }


    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return "";
    }
}
