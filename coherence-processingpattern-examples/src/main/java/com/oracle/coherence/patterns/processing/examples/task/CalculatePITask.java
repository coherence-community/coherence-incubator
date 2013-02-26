/*
 * File: CalculatePITask.java
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

import com.oracle.coherence.common.tuples.Triple;
import com.oracle.coherence.patterns.processing.task.ResumableTask;
import com.oracle.coherence.patterns.processing.task.TaskExecutionEnvironment;
import com.oracle.coherence.patterns.processing.task.Yield;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The CalculatePITask calculates PI using the Leibnitz formula:
 * PI * 4 = 1 - 1/3 + 1/5 - 1/7 + 1/9 - 1/11...
 * <p>
 * This particular implementation leverages the ability to
 * store a checkpoint and {@link Yield} the processing for each iteration.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class CalculatePITask implements ResumableTask, PortableObject, ExternalizableLite
{
    /**
     * The number of iterations in the algorithm.
     */
    private int m_nIterations;

    /**
     * The name of the task.
     */
    private String name;


    /**
     * Default Constructor
     */
    public CalculatePITask()
    {
    }


    /**
     * Constructor taking the number of iterations as a parameter.
     *
     * @param nIterations the number of iterations in the algorithm
     */
    public CalculatePITask(int    nIterations,
                           String name)
    {
        m_nIterations = nIterations;
        this.name     = name;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object run(TaskExecutionEnvironment oEnvironment)
    {
        System.out.println("CalculatePI");

        // First we initialize the seed values for the algorithm.
        double denominator   = 1.0,
               pi            = 1.0;
        int    currIteration = 1;

        if (oEnvironment.isResuming())
        {
            //
            // If we are resuming we try to get a stored checkpoint.
            //
            Triple<Integer, Double, Double> triple = (Triple<Integer, Double, Double>) oEnvironment.loadCheckpoint();

            if (triple != null)
            {
                currIteration = triple.getX();
                denominator   = triple.getY();
                pi            = triple.getZ();
            }
        }

        System.out.println(name + ":" + currIteration + " denominator:" + denominator + " current pi:" + pi * 4);

        // Now do one more iteration of the algorithm
        denominator += 2.0;

        if (currIteration % 2 == 1)
        {
            pi -= (double) (1 / denominator);
        }
        else
        {
            pi += (double) (1 / denominator);
        }

        currIteration++;

        // Check if we are done
        if (currIteration >= m_nIterations)
        {
            // Return the result
            return pi * 4;
        }
        else
        {
            // {@link Yield} and store away the current state
            Triple<Integer, Double, Double> newTriple = new Triple<Integer, Double, Double>(currIteration,
                                                                                            denominator,
                                                                                            pi);

            return new Yield(newTriple, 0);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        m_nIterations = ExternalizableHelper.readInt(in);
        name          = ExternalizableHelper.readUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeInt(out, m_nIterations);
        ExternalizableHelper.writeUTF(out, name);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        m_nIterations = reader.readInt(10);
        name          = reader.readString(11);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeInt(10, m_nIterations);
        writer.writeString(11, name);
    }
}
