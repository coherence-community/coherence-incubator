/*
 * File: DefaultTaskProcessorDefinition.java
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
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.task.TaskProcessorType;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation of a {@link TaskProcessorDefinition}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class DefaultTaskProcessorDefinition implements TaskProcessorDefinition,
                                                       Serializable,
                                                       PortableObject,
                                                       ExternalizableLite
{
    /**
     * The name of the {@link TaskProcessorDefinition} cache.
     */
    public static final String CACHENAME = "coherence.patterns.processing.taskprocessordefinitions";

    /**
     * The {@link Identifier} uniquely identifying this {@link TaskProcessorDefinition}.
     */
    private Identifier identifier;

    /**
     * The name of the TaskProcessorDefinition for display purposes.
     */
    private String name;

    /**
     * Defines the {@link TaskProcessorType} of this {@link TaskProcessorDefinition}.
     */
    private TaskProcessorType taskProcessorType;

    /**
     * The particular {@link TaskProcessor} that shall be launched for this {@link TaskProcessorDefinition}.
     */
    private TaskProcessor taskProcessor;

    /**
     * The attributes used to match a TaskProcessor to a Submission.
     */
    private Map<String, String> attributeMap;


    /**
     * Default constructor.
     */
    public DefaultTaskProcessorDefinition()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param identifier        the unique {@link Identifier} for this {@link TaskProcessorDefinition}
     * @param name              the display name of this {@link TaskProcessorDefinition}
     * @param taskProcessorType the {@link TaskProcessorType} of this {@link TaskProcessorDefinition}
     * @param taskProcessor     the {@link TaskProcessor} for this {@link TaskProcessorDefinition}
     * @param attributeMap      the attribute map used to match TaskProcessors against Submissions
     */
    public DefaultTaskProcessorDefinition(Identifier          identifier,
                                          String              name,
                                          TaskProcessorType   taskProcessorType,
                                          TaskProcessor       taskProcessor,
                                          Map<String, String> attributeMap)
    {
        this.identifier        = identifier;
        this.name              = name;
        this.taskProcessorType = taskProcessorType;
        this.taskProcessor     = taskProcessor;
        this.attributeMap      = attributeMap;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getIdentifier()
    {
        return identifier;
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }


    /**
     * {@inheritDoc}
     */
    public TaskProcessorType getTaskProcessorType()
    {
        return taskProcessorType;
    }


    /**
     * {@inheritDoc}
     */
    public TaskProcessor getTaskProcessor()
    {
        return taskProcessor;
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, String> getAttributeMap()
    {
        return attributeMap;
    }


    /**
     * Convert the key to a string.
     *
     * @return a String representing the {@link TaskProcessorDefinition}
     */
    @Override
    public String toString()
    {
        return "TPD:{" + identifier.toString() + "," + name + "," + taskProcessorType.toString() + "}";
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        identifier        = (Identifier) reader.readObject(0);
        name              = reader.readString(1);
        taskProcessorType = TaskProcessorType.valueOf(reader.readString(2));
        taskProcessor     = (TaskProcessor) reader.readObject(3);
        attributeMap      = new HashMap<String, String>();
        reader.readMap(4, attributeMap);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, identifier);
        writer.writeString(1, name);
        writer.writeString(2, taskProcessorType.name());
        writer.writeObject(3, taskProcessor);
        writer.writeMap(4, attributeMap);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final DataInput in) throws IOException
    {
        identifier        = (Identifier) ExternalizableHelper.readObject(in);
        name              = in.readUTF();
        taskProcessorType = TaskProcessorType.valueOf(in.readUTF());
        taskProcessor     = (TaskProcessor) ExternalizableHelper.readObject(in);
        attributeMap      = new HashMap<String, String>();
        ExternalizableHelper.readMap(in, attributeMap, this.getClass().getClassLoader());

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, identifier);
        out.writeUTF(name);
        out.writeUTF(taskProcessorType.name());
        ExternalizableHelper.writeObject(out, taskProcessor);
        ExternalizableHelper.writeMap(out, attributeMap);

    }
}
