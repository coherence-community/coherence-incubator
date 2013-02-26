/*
 * File: SubmissionKeyPair.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@link SubmissionKeyPair} groups the keys to the {@link com.oracle.coherence.patterns.processing.internal.Submission} and to the
 * {@link com.oracle.coherence.patterns.processing.internal.SubmissionResult} in one object.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class SubmissionKeyPair implements ExternalizableLite, PortableObject
{
    /**
     * The key to the {@link Submission}.
     */
    private SubmissionKey submissionKey;

    /**
     * The key to the {@link SubmissionResult}.
     */
    private Identifier resultIdentifier;


    /**
     * Default constructor.
     */
    public SubmissionKeyPair()
    {
    }


    /**
     * Constructor taking parameters.
     *
     * @param key      the {@link SubmissionKey} for this {@link SubmissionKeyPair}
     * @param resultid the identifier which is the key for the {@link SubmissionResult}
     *                 associated with this {@link SubmissionKeyPair}
     */
    public SubmissionKeyPair(SubmissionKey key,
                             Identifier    resultid)
    {
        this.submissionKey    = key;
        this.resultIdentifier = resultid;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((resultIdentifier == null) ? 0 : resultIdentifier.hashCode());
        result = prime * result + ((submissionKey == null) ? 0 : submissionKey.hashCode());

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

        SubmissionKeyPair other = (SubmissionKeyPair) obj;

        if (resultIdentifier == null)
        {
            if (other.resultIdentifier != null)
            {
                return false;
            }
        }
        else if (!resultIdentifier.equals(other.resultIdentifier))
        {
            return false;
        }

        if (submissionKey == null)
        {
            if (other.submissionKey != null)
            {
                return false;
            }
        }
        else if (!submissionKey.equals(other.submissionKey))
        {
            return false;
        }

        return true;
    }


    /**
     * Returns the {@link SubmissionKey}.
     *
     * @return the {@link SubmissionKey}
     */
    public SubmissionKey getKey()
    {
        return submissionKey;
    }


    /**
     * Returns the {@link Identifier} for the {@link SubmissionResult}.
     *
     * @return the {@link Identifier} for the {@link SubmissionResult}
     */
    public Identifier getResultId()
    {
        return resultIdentifier;
    }


    /**
     * Convert the {@link SubmissionKeyPair} to a string.
     *
     * @return a String representing the {@link SubmissionKeyPair}
     */
    @Override
    public String toString()
    {
        return "SKP:{" + submissionKey.toString() + "," + resultIdentifier.toString() + "}";
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        submissionKey    = (SubmissionKey) ExternalizableHelper.readObject(in);
        resultIdentifier = (Identifier) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, submissionKey);
        ExternalizableHelper.writeObject(out, resultIdentifier);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        submissionKey    = (SubmissionKey) reader.readObject(0);
        resultIdentifier = (Identifier) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, submissionKey);
        writer.writeObject(1, resultIdentifier);
    }
}
