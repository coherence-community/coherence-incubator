/*
 * File: CreateDestinationProcessor.java
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

package com.oracle.coherence.patterns.messaging.entryprocessors;

import com.oracle.coherence.patterns.messaging.Destination;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link EntryProcessor} that will place an {@link Destination} into the
 * {@link Destination#CACHENAME} Coherence Cache, if it does not already
 * exist.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
@SuppressWarnings("serial")
public class CreateDestinationProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link Destination} to be created.
     */
    private Destination destination;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public CreateDestinationProcessor()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param destination destination
     */
    public CreateDestinationProcessor(Destination destination)
    {
        this.destination = destination;

    }


    /**
     * Create the destination if it doesn't exist and set its new value in the
     * map. Return the destination to the caller.
     *
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        // The destination ID is the key. Only create the destination once.
        //
        if (!entry.isPresent())
        {
            entry.setValue(destination);
        }

        return entry.getValue();
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.destination = (Destination) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, destination);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.destination = (Destination) reader.readObject(0);

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, destination);

    }
}
