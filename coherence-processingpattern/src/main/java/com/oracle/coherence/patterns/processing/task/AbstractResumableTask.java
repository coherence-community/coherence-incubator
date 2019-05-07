/*
 * File: AbstractResumableTask.java
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

package com.oracle.coherence.patterns.processing.task;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link AbstractResumableTask} helps implement the {@link ResumableTask}
 * interface.
 * <p>
 * Copyright (c) 2009, 2019. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public abstract class AbstractResumableTask implements ResumableTask, ExternalizableLite, PortableObject
{
    /**
     * Default constructor, required by POF.
     */
    public AbstractResumableTask()
    {
    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(final DataInput in) throws IOException
    {
    }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(final DataOutput out) throws IOException
    {
    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(final PofReader reader) throws IOException
    {
    }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(final PofWriter writer) throws IOException
    {
    }
}
