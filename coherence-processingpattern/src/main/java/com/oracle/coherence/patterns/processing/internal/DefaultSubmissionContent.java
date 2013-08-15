/*
 * File: DefaultSubmissionContent.java
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

package com.oracle.coherence.patterns.processing.internal;

import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@link DefaultSubmissionContent} is a container for the data bearing parts of
 * a {@link Submission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class DefaultSubmissionContent implements SubmissionContent, ExternalizableLite, PortableObject
{
    /**
     * The payload of the {@link DefaultSubmissionContent}.
     */
    private Object payload;

    /**
     * The {@link SubmissionConfiguration} of the {@link DefaultSubmissionContent}.
     */
    private SubmissionConfiguration submissionConfiguration;


    /**
     * Default constructor.
     */
    public DefaultSubmissionContent()
    {
    }


    /**
     * Standard constructor taking parameters.
     *
     * @param payLoad                 the payLoad of the content
     * @param submissionConfiguration the {@link SubmissionConfiguration}
     */
    public DefaultSubmissionContent(Object                  payLoad,
                                    SubmissionConfiguration submissionConfiguration)
    {
        this.payload                 = payLoad;
        this.submissionConfiguration = submissionConfiguration;
    }


    /**
     * {@inheritDoc}
     */
    public Object getPayload()
    {
        return payload;
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionConfiguration getSubmissionConfiguration()
    {
        return submissionConfiguration;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final DataInput in) throws IOException
    {
        this.payload                 = ExternalizableHelper.readObject(in);
        this.submissionConfiguration = (SubmissionConfiguration) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, this.payload);
        ExternalizableHelper.writeObject(out, this.submissionConfiguration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final PofReader reader) throws IOException
    {
        this.payload                 = reader.readObject(0);
        this.submissionConfiguration = (SubmissionConfiguration) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final PofWriter writer) throws IOException
    {
        writer.writeObject(0, this.payload);
        writer.writeObject(1, this.submissionConfiguration);
    }
}
