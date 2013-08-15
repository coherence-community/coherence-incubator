/*
 * File: SubmissionKey.java
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

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A key for the {@link DefaultSubmission} object which supports KeyAssociation.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
@SuppressWarnings("serial")
public class SubmissionKey implements KeyAssociation, ExternalizableLite, PortableObject
{
    /**
     * The associated key.
     */
    private Object associatedKey;

    /**
     * The {@link UUID} of the {@link DefaultSubmission}.
     */
    private UUID submissionId;


    /**
     * Used by the {@link ExternalizableLite} and {@link PortableObject}
     * interfaces.
     */
    public SubmissionKey()
    {
        associatedKey = null;
    }


    /**
     * Construct a new {@link SubmissionKey}.
     *
     * @param associatedKey  the associatedKey to use
     * @param submissionId  the {@link UUID} of the {@link DefaultSubmission}
     */
    public SubmissionKey(final Object associatedKey,
                         final UUID   submissionId)
    {
        this.associatedKey = associatedKey;
        this.submissionId  = submissionId;
    }


    /**
     * Checks for equality for this
     * {@link com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission}
     *
     * @param oOther the object to compare with
     *
     * @return whether two keys are equal
     */
    @Override
    public boolean equals(final Object oOther)
    {
        if (this == oOther)
        {
            return true;
        }

        if (oOther == null)
        {
            return false;
        }

        if (getClass() != oOther.getClass())
        {
            return false;
        }

        final SubmissionKey otherKey = (SubmissionKey) oOther;

        if (associatedKey == null)
        {
            if (otherKey.associatedKey != null)
            {
                return false;
            }
        }
        else if (!associatedKey.equals(otherKey.associatedKey))
        {
            return false;
        }

        if (submissionId == null)
        {
            if (otherKey.submissionId != null)
            {
                return false;
            }
        }
        else if (!submissionId.equals(otherKey.submissionId))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public Object getAssociatedKey()
    {
        return associatedKey == null ? submissionId : associatedKey;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int nPrime  = 31;
        int       nResult = 1;

        nResult = nPrime * nResult + ((associatedKey == null) ? 0 : associatedKey.hashCode());
        nResult = nPrime * nResult + ((submissionId == null) ? 0 : submissionId.hashCode());

        return nResult;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return submissionId.toString();
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final DataInput in) throws IOException
    {
        this.associatedKey = ExternalizableHelper.readObject(in);
        this.submissionId  = (UUID) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, this.associatedKey);
        ExternalizableHelper.writeObject(out, this.submissionId);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final PofReader reader) throws IOException
    {
        this.associatedKey = reader.readObject(0);
        this.submissionId  = (UUID) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final PofWriter writer) throws IOException
    {
        writer.writeObject(0, this.associatedKey);
        writer.writeObject(1, this.submissionId);
    }
}
