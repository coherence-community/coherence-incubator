/*
 * File: TaskProcessorMediatorKey.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * The {@link TaskProcessorMediatorKey} is the key for a {@link TaskProcessorMediator}
 * object in the grid.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class TaskProcessorMediatorKey implements Serializable, PortableObject, ExternalizableLite
{
    /**
     * The identifier for the linked {@link com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition}.
     */
    private Identifier taskProcessorDefinitionIdentifier;

    /**
     * The MemberId identifies the cluster member for this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     * A member id of 0 means that the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} is not connected
     * to a particular node in the cluster.
     *
     */
    private int memberId;

    /**
     * The uniqueId is a UUID identifying this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} uniquely.
     */
    private UID uniqueId;


    /**
     * Default constructor.
     */
    public TaskProcessorMediatorKey()
    {
    }


    /**
     * Standard constructor.
     *
     * @param definitionIdentifier the identifier for the linked {@link com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition}
     * @param memberId             the id of the cluster member where this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} belongs
     * @param uniqueId             the unique id
     */
    public TaskProcessorMediatorKey(Identifier definitionIdentifier,
                                    int        memberId,
                                    UID        uniqueId)
    {
        this.taskProcessorDefinitionIdentifier = definitionIdentifier;
        this.memberId                          = memberId;
        this.uniqueId                          = uniqueId;
    }


    /**
     * Returns the {@link com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition} identifier.
     *
     * @return the {@link Identifier} to the related {@link com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition}
     */
    public Identifier getTaskProcessorDefinitionIdentifier()
    {
        return taskProcessorDefinitionIdentifier;
    }


    /**
     * Returns the cluster member id.
     *
     * An id of 0 means that this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} is not connected to a particular cluster
     * member.
     *
     * @return the member id.
     */
    public int getMemberId()
    {
        return memberId;
    }


    /**
     * Returns the unique member id.
     *
     * @return the member id.
     */
    public UID getUniqueId()
    {
        return uniqueId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + memberId;
        result = prime * result
                 + ((taskProcessorDefinitionIdentifier == null) ? 0 : taskProcessorDefinitionIdentifier.hashCode());
        result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        TaskProcessorMediatorKey other = (TaskProcessorMediatorKey) obj;

        if (memberId != other.memberId)
        {
            return false;
        }

        if (taskProcessorDefinitionIdentifier == null)
        {
            if (other.taskProcessorDefinitionIdentifier != null)
            {
                return false;
            }
        }
        else if (!taskProcessorDefinitionIdentifier.equals(other.taskProcessorDefinitionIdentifier))
        {
            return false;
        }

        if (uniqueId == null)
        {
            if (other.uniqueId != null)
            {
                return false;
            }
        }
        else if (!uniqueId.equals(other.uniqueId))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "TPSK:{" + taskProcessorDefinitionIdentifier.toString() + "," + memberId + "," + uniqueId + "}";
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        taskProcessorDefinitionIdentifier = (Identifier) reader.readObject(0);
        memberId                          = reader.readInt(1);
        uniqueId                          = (UID) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, taskProcessorDefinitionIdentifier);
        writer.writeInt(1, memberId);
        writer.writeObject(2, uniqueId);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final DataInput in) throws IOException
    {
        taskProcessorDefinitionIdentifier = (Identifier) ExternalizableHelper.readObject(in);
        memberId                          = in.readInt();
        uniqueId                          = (UID) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, taskProcessorDefinitionIdentifier);
        out.writeInt(memberId);
        ExternalizableHelper.writeObject(out, uniqueId);
    }
}
