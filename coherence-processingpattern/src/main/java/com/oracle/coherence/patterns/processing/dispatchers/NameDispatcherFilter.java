/*
 * File: NameDispatcherFilter.java
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

package com.oracle.coherence.patterns.processing.dispatchers;

import com.oracle.coherence.patterns.processing.DispatcherFilter;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * This {@link DispatcherFilter} filters out {@link Dispatcher}s whose name
 * is not equal to the supplied name.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class NameDispatcherFilter implements DispatcherFilter, ExternalizableLite, PortableObject
{
    /**
     * The Name to match the {@link Dispatcher} against.
     */
    private String name;


    /**
     * Constructor.
     *
     * @param name the name to filter against
     */
    public NameDispatcherFilter(String name)
    {
        this.name = name;
    }


    /**
     * {@inheritDoc}
     */
    public boolean filterDispatcher(Dispatcher dispatcher)
    {
        if (name.equals(dispatcher.getName()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.name = in.readUTF();

    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        out.writeUTF(name);

    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        name = reader.readString(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, name);
    }
}
