/*
 * File: DefaultPendingSubmission.java
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
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.util.NoSuchElementException;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * The default implementation of a {@link PendingSubmission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class DefaultPendingSubmission implements PendingSubmission
{
    /**
     * The time the pending submission was created.
     */
    private final long creationTime;

    /**
     * The time to wait before dispatching the {@link PendingSubmission} to a
     * {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}.
     */
    private long dispatchDelay;

    /**
     * The {@link SubmissionConfiguration} associated with the {@link PendingSubmission}.
     */
    private final SubmissionConfiguration requestData;

    /**
     * The result identifier for the submission.
     */
    private final Identifier resultIdentifier;

    /**
     * The {@link SubmissionKey} of the {@link DefaultSubmission} that is pending.
     */
    private final SubmissionKey submissionKey;

    /**
     * The {@link TimeUnit} for the dispatch delay.
     */
    private TimeUnit timeUnit;

    /**
     * The cached payload as retrieved from the Submissions cache.
     */
    private transient Object payLoad;


    /**
     * Standard Constructor.
     *
     * @param submissionKey    the {@link SubmissionKey} for the {@link DefaultSubmission} that is
     *                         pending
     * @param resultIdentifier the result identifier for the submission
     * @param requestData      the {@link DefaultSubmissionConfiguration} of the {@link DefaultSubmission}
     * @param delay            the delay before execution of the submission
     */
    public DefaultPendingSubmission(final SubmissionKey           submissionKey,
                                    final Identifier              resultIdentifier,
                                    final SubmissionConfiguration requestData,
                                    final long                    delay)
    {
        this.submissionKey    = submissionKey;
        this.requestData      = requestData;
        this.resultIdentifier = resultIdentifier;
        this.dispatchDelay    = delay;
        this.timeUnit         = TimeUnit.MILLISECONDS;
        this.creationTime     = System.currentTimeMillis();
    }


    /**
     * {@inheritDoc}
     */
    public long getCreationTime()
    {
        return creationTime;
    }


    /**
     * {@inheritDoc}
     */
    public long getDelay(final TimeUnit oUnit)
    {
        return oUnit.convert((creationTime + dispatchDelay) - System.currentTimeMillis(), timeUnit);
    }


    /**
     * {@inheritDoc}
     */
    public Object getPayload()
    {
        if (payLoad == null)
        {
            // If it is null then retrieve it and cache it
            final NamedCache cache = CacheFactory.getCache(DefaultSubmission.CACHENAME);
            Submission       ct    = (Submission) cache.get(getSubmissionKey());

            if (ct != null)
            {
                SubmissionContent sct = ct.getContent();

                if (sct != null)
                {
                    payLoad = sct.getPayload();
                }
            }
            else
            {
                throw new NoSuchElementException("No Submission cache entry for " + getSubmissionKey());
            }
        }

        return payLoad;
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionConfiguration getSubmissionConfiguration()
    {
        return requestData;
    }


    /**
     * Gets the result identifier.
     *
     * @return the result identifier
     */
    public Identifier getResultIdentifier()
    {
        return resultIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionKey getSubmissionKey()
    {
        return submissionKey;
    }


    /**
     * {@inheritDoc}
     */
    public TimeUnit getTimeUnit()
    {
        return timeUnit;
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(final Delayed o)
    {
        final PendingSubmission otherSubmission = (PendingSubmission) o;

        return (int) ((creationTime + dispatchDelay)
                      - (otherSubmission.getCreationTime() + otherSubmission.getDelay(TimeUnit.MILLISECONDS)));
    }


    /**
     * {@inheritDoc}
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

        final DefaultPendingSubmission otherSubmission = (DefaultPendingSubmission) oOther;

        if (submissionKey == null)
        {
            if (otherSubmission.submissionKey != null)
            {
                return false;
            }
        }
        else
        {
            if (!submissionKey.equals(otherSubmission.submissionKey))
            {
                return false;
            }
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime   = 31;
        int       nResult = 1;

        nResult = prime * nResult + ((submissionKey == null) ? 0 : submissionKey.hashCode());

        return nResult;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "DefaultPendingSubmission [creationTime=" + creationTime + ", dispatchDelay=" + dispatchDelay + ", "
               + (requestData != null ? "requestData=" + requestData + ", " : "")
               + (resultIdentifier != null ? "resultIdentifier=" + resultIdentifier + ", " : "")
               + (submissionKey != null ? "submissionKey=" + submissionKey + ", " : "")
               + (timeUnit != null ? "timeUnit=" + timeUnit : "") + "]";
    }


    /**
     * Used internally to set the the amount of time to wait in before
     * retrying a {@link PendingSubmission} when a
     * {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher} has
     * requested a
     * {@link com.oracle.coherence.patterns.processing.dispatchers.DispatchOutcome.RetryLater}
     *
     * @param dispatchDelay  the amount of time to wait in milliseconds before
     *                       retrying a {@link PendingSubmission}
     */
    public void setDelay(final long dispatchDelay)
    {
        this.dispatchDelay = dispatchDelay;
    }


    /**
     * Set the TimeUnit for the dispatchDelay.
     *
     * @param timeUnit the TimeUnit for the dispatchDelay}
     */
    public void setTimeUnit(final TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }
}
