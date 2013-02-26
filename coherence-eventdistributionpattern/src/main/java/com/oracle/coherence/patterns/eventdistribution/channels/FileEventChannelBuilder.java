/*
 * File: FileEventChannelBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link FileEventChannelBuilder} is an {@link EventChannelBuilder} for {@link FileEventChannel}s that
 * may be configured through dependency injection.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class FileEventChannelBuilder extends AbstractEventChannelBuilder
{
    private String  directoryName;
    private boolean isAppending;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public FileEventChannelBuilder()
    {
        super();
    }


    /**
     * Method description
     *
     * @return
     */
    public String getDirectoryName()
    {
        return directoryName;
    }


    /**
     * Method description
     *
     * @param directoryName
     */
    @Property("directory-name")
    @Mandatory
    public void setDirectoryName(String directoryName)
    {
        this.directoryName = directoryName;
    }


    /**
     * Method description
     *
     * @return
     */
    public boolean isAppending()
    {
        return isAppending;
    }


    /**
     * Method description
     *
     * @param isAppending
     */
    @Property("appending")
    public void setAppending(boolean isAppending)
    {
        this.isAppending = isAppending;
    }


    /**
     * {@inheritDoc}
     */
    public EventChannel realize(ParameterProvider parameterProvider)
    {
        return new FileEventChannel(getDirectoryName(), isAppending());
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.directoryName = ExternalizableHelper.readSafeUTF(in);
        this.isAppending   = in.readBoolean();
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeSafeUTF(out, directoryName);
        out.writeBoolean(isAppending);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.directoryName = reader.readString(100);
        this.isAppending   = reader.readBoolean(101);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeString(100, directoryName);
        writer.writeBoolean(101, isAppending);
    }
}
