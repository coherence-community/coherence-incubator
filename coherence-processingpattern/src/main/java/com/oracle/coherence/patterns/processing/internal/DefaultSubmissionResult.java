/*
 * File: DefaultSubmissionResult.java
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
import com.oracle.coherence.common.util.ChangeIndication;
import com.oracle.coherence.patterns.processing.SubmissionState;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link DefaultSubmissionResult} captures the result of processing a
 * {@link DefaultSubmission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class DefaultSubmissionResult implements ExternalizableLite, PortableObject, SubmissionResult, ChangeIndication
{
    /**
     * The name of the Coherence Cache that will store {@link DefaultSubmissionResult}s.
     */
    public static final String CACHENAME = "coherence.patterns.processing.submissionresults";

    /**
     * The {@link Logger} to use.
     */
    private static final Logger logger = Logger.getLogger(DefaultSubmissionResult.class.getName());

    /**
     * The time in milliseconds it took to execute the Submission.
     */
    private long executionTime;

    /**
     * The time it took in milliseconds until execution started.
     */
    private long latency;

    /**
     * The time this item was submitted, in milliseconds since the epoch.
     */
    private long submissionTime;

    /**
     * The time this item started executing, in milliseconds since the epoch.
     */
    private long startTime;

    /**
     * The progress of this particular Submission.
     */
    private Object progress;

    /**
     * The identifier of the result, used as the key for the Result.
     */
    private Identifier submissionResultID;

    /**
     * The result produced by processing the associated {@link DefaultSubmission}.
     */
    private Object result;

    /**
     * The {@link SubmissionKey} of the {@link DefaultSubmission} that produced the
     * {@link DefaultSubmissionResult}.
     */
    private SubmissionKey submissionKey;

    /**
     * The {@link SubmissionState} represents the current state of the
     * submission.
     */
    private SubmissionState submissionState;

    /**
     * The {@link Identifier} of the
     * {@link com.oracle.coherence.patterns.processing.ProcessingSession} that
     * is expecting this {@link DefaultSubmissionResult}.
     */
    private Identifier sessionIdentifier;

    /**
     * The owner of the {@link SubmissionResult}.
     */
    private Object owner;

    /**
     * Set if we are about to resume execution.
     */
    private boolean isResuming;

    /**
     * used to keep track of changes related to the {@link ChangeIndication} implementation.
     */
    private transient boolean changed;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public DefaultSubmissionResult()
    {
        submissionState = SubmissionState.INITIAL;
        progress        = 0;
    }


    /**
     * Standard Constructor.
     *
     * @param submissionResultID  the unique ID of the {@link DefaultSubmissionResult},
     *                            also used as key.
     * @param submissionKey       the {@link SubmissionKey} of the
     *                            {@link DefaultSubmission} for which this
     *                            {@link DefaultSubmissionResult} is the result
     * @param submitterIdentifier the {@link Identifier} of the
     *                            {@link com.oracle.coherence.patterns.processing.ProcessingSession}
     */
    public DefaultSubmissionResult(final Identifier    submissionResultID,
                                   final SubmissionKey submissionKey,
                                   final Identifier    submitterIdentifier)
    {
        this.submissionResultID = submissionResultID;
        this.submissionKey      = submissionKey;
        this.sessionIdentifier  = submitterIdentifier;
        this.result             = null;
        this.submissionState    = SubmissionState.INITIAL;
        this.progress           = null;
    }


    /**
     * Standard Constructor.
     *
     * @param submissionResultID  the unique ID of the {@link DefaultSubmissionResult},
     *                            also used as key.
     * @param submissionKey       the {@link SubmissionKey} of the
     *                            {@link DefaultSubmission} for which this
     *                            {@link DefaultSubmissionResult} is the result
     * @param sessionid          the identifier of the session
     * @param submissionState     is the {@link SubmissionState} of the
     *                            processing for a Submission
     */
    public DefaultSubmissionResult(final Identifier    submissionResultID,
                                   final SubmissionKey submissionKey,
                                   final Identifier    sessionid,
                                   final String        submissionState)
    {
        this(submissionResultID, submissionKey, sessionid);
        this.submissionState = (SubmissionState) Enum.valueOf(SubmissionState.class, submissionState);
        this.progress        = null;

        if (this.submissionState == SubmissionState.SUBMITTED)
        {
            setSubmissionTime(System.currentTimeMillis());
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getExecutionTime()
    {
        return executionTime;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getID()
    {
        return submissionResultID;
    }


    /**
     * {@inheritDoc}
     */
    public long getLatency()
    {
        return latency;
    }


    /**
     * {@inheritDoc}
     */
    public Object getProgress()
    {
        return progress;
    }


    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return result;
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
    public SubmissionState getSubmissionState()
    {
        return submissionState;
    }


    /**
     * {@inheritDoc}
     */
    public long getSubmissionTime()
    {
        return submissionTime;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier getSessionIdentifier()
    {
        return sessionIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public void changeSessionIdentifier(Identifier newIdentifier)
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, " Changing sessionIdentifier from:" + sessionIdentifier + " to:" + newIdentifier);
        }

        sessionIdentifier = newIdentifier;
        setChanged();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFinalState()
    {
        return ((submissionState != null) && (submissionState.isFinalState()));
    }


    /**
     * Sets the execution time.
     *
     * @param lExecutionTime the execution time
     */
    public void setExecutionTime(final long lExecutionTime)
    {
        executionTime = lExecutionTime;
        setChanged();
    }


    /**
     * Sets the latency.
     *
     * @param lLatency the latency
     */
    public void setLatency(final long lLatency)
    {
        latency = lLatency;
        setChanged();
    }


    /**
     * Sets the start time of the execution.
     * @param currentTimeMillis the start time in milliseconds since the epoch.
     */
    private void setStartTime(long currentTimeMillis)
    {
        startTime = currentTimeMillis;
        setChanged();
    }


    /**
     * Sets the progress.
     *
     * @param oProgress the progress for the submission.
     */

    public void setProgress(final Object oProgress)
    {
        progress = oProgress;
        setChanged();
    }


    /**
     * Set the result of this {@link DefaultSubmissionResult}.
     *
     * @param oResult the result
     */
    public void setResult(final Object oResult)
    {
        this.result = oResult;
        setChanged();
    }


    /**
     * Sets the state of the {@link DefaultSubmissionResult}.
     *
     * @param state the new state
     */
    private void setSubmissionState(final SubmissionState state)
    {
        this.submissionState = state;
        setChanged();
    }


    /**
     * Set the submission time.
     *
     * @param lSubmissionTime the submission time
     */
    public void setSubmissionTime(final long lSubmissionTime)
    {
        submissionTime = lSubmissionTime;
        setChanged();
    }


    /**
     * {@inheritDoc}
     */
    public boolean processingStarted(Object processingOwner)
    {
        if (owner != processingOwner)
        {
            if (owner == null)
            {
                return false;
            }

            if (!owner.equals(processingOwner))
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER,
                               "Can not start processing {0} a different owner {1} is assigned, {2} tried to process it.",
                               new Object[] {this, owner, processingOwner});
                }

                return false;
            }
        }

        if (getSubmissionState() == SubmissionState.ASSIGNED)
        {
            setLatency(System.currentTimeMillis() - getSubmissionTime());
        }

        if ((getSubmissionState() == SubmissionState.ASSIGNED) || (getSubmissionState() == SubmissionState.SUSPENDED)
            || (getSubmissionState() == SubmissionState.EXECUTING))
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.EXECUTING});
            }

            setStartTime(System.currentTimeMillis());
            setSubmissionState(SubmissionState.EXECUTING);
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "Could not set {0} to EXECUTING because it was not SUSPENDED or SUBMITTED or ASSIGNED",
                           this);
            }

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean processingFailed(Object oFailureResult)
    {
        if ((getSubmissionState() == SubmissionState.EXECUTING) || (getSubmissionState() == SubmissionState.RETRY)
            || (getSubmissionState() == SubmissionState.SUBMITTED)
            || (getSubmissionState() == SubmissionState.ASSIGNED)
            || (getSubmissionState() == SubmissionState.SUSPENDED))
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.FAILED});
            }

            setSubmissionState(SubmissionState.FAILED);
            setResult(oFailureResult);
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "Could not set {0} to FAILED because it was not EXECUTING, RETRY, SUBMITTED, ASSIGNED or SUSPENDED",
                           this);
            }

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object assign(Object owner)
    {
        SubmissionState state = getSubmissionState();

        if ((state == SubmissionState.SUBMITTED) || (state == SubmissionState.SUSPENDED)
            || (state == SubmissionState.RETRY))
        {
            if ((state == SubmissionState.SUSPENDED) || (state == SubmissionState.RETRY))
            {
                isResuming = true;
            }

            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.ASSIGNED});
            }

            setSubmissionState(SubmissionState.ASSIGNED);
            setOwner(owner);
            setChanged();

            return null;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "Could not set {0} to ASSIGNED because it was not in SUBMITTED, SUSPENDED or RETRY state",
                           this);
            }

            return owner;
        }
    }


    /**
     * Sets the owner.
     *
     * @param owner the new owner
     */
    private void setOwner(Object owner)
    {
        this.owner = owner;
    }


    /**
     * {@inheritDoc}
     */
    public boolean processingSucceeded(Object oResult)
    {
        if (getSubmissionState() == SubmissionState.EXECUTING)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.DONE});
            }

            setSubmissionState(SubmissionState.DONE);
            setResult(oResult);
            setExecutionTime(getExecutionTime() + (System.currentTimeMillis() - getStartTime()));
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Could not set {0} to DONE because it was not EXECUTING", this);
            }

            return false;
        }

    }


    /**
     * {@inheritDoc}
     */
    public boolean retry()
    {
        if (!getSubmissionState().isFinalState())
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.RETRY});
            }

            setSubmissionState(SubmissionState.RETRY);
            this.owner = null;
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Could not set {0} to RETRY because it was in final state", this);
            }

            return false;
        }

    }


    /**
     * {@inheritDoc}
     */
    public boolean cancelSubmission()
    {
        if (!getSubmissionState().isFinalState())
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.CANCELLED});
            }

            setSubmissionState(SubmissionState.CANCELLED);
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.FINER,
                           "Couldn't set SubmissionResult {0} to CANCELLED because it was in the final state {1}",
                           new Object[] {this, this.getSubmissionState()});
            }

            return false;
        }
    }


    /**
     * Returns the start time in milliseconds since the epoch.
     *
     * @return the start time in milliseconds since the epoch
     */
    private long getStartTime()
    {
        return startTime;
    }


    /**
     * {@inheritDoc}
     */
    public boolean suspendExecution(Object oResult)
    {
        if ((getSubmissionState() == SubmissionState.EXECUTING) || (getSubmissionState() == SubmissionState.RETRY))
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.SUSPENDED});
            }

            setExecutionTime(getExecutionTime() + (System.currentTimeMillis() - getStartTime()));
            setSubmissionState(SubmissionState.SUSPENDED);
            setResult(oResult);
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           "Could not set {0} to SUSPENDED because it was not EXECUTING or in RETRY",
                           this);
            }

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean submitted(long currentTimeMillis)
    {
        if (getSubmissionState() == SubmissionState.INITIAL)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "SubmissionResult {0} changing state from {1} to {2} ",
                           new Object[] {this, this.getSubmissionState(), SubmissionState.SUBMITTED});
            }

            setSubmissionState(SubmissionState.SUBMITTED);
            setSubmissionTime(currentTimeMillis);
            setChanged();

            return true;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Could not set {0} to SUBMITTED because it was not INITIAL", this);
            }

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isResuming()
    {
        return isResuming;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "DefaultSubmissionResult [" + (owner != null ? "owner=" + owner + ", " : "")
               + (submissionState != null ? "submissionState=" + submissionState + ", " : "")
               + (sessionIdentifier != null ? "sessionIdentifier=" + sessionIdentifier + ", " : "")
               + (submissionKey != null ? "submissionKey=" + submissionKey + ", " : "")
               + (submissionResultID != null ? "submissionResultID=" + submissionResultID + ", " : "")
               + (progress != null ? "progress=" + progress + ", " : "")
               + (result != null ? "result=" + result + ", " : "") + "latency=" + latency + ", executionTime="
               + executionTime + ", isResuming=" + isResuming + ", startTime=" + startTime + ", submissionTime="
               + submissionTime + "]";
    }


    /**
     * {@inheritDoc}
     */
    public void beforeChange()
    {
        changed = false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean changed()
    {
        return changed;
    }


    /**
     * Sets the state to be changed for the benefit of the {@link ChangeIndication} implementation.
     */
    private void setChanged()
    {
        changed = true;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(final DataInput in) throws IOException
    {
        this.submissionResultID = (Identifier) ExternalizableHelper.readObject(in);
        this.submissionKey      = (SubmissionKey) ExternalizableHelper.readObject(in);
        this.sessionIdentifier  = (Identifier) ExternalizableHelper.readObject(in);
        this.result             = ExternalizableHelper.readObject(in);
        this.submissionState = (SubmissionState) Enum.valueOf(SubmissionState.class,
                                                              ExternalizableHelper.readSafeUTF(in));
        this.progress       = ExternalizableHelper.readObject(in);
        this.submissionTime = ExternalizableHelper.readLong(in);
        this.latency        = ExternalizableHelper.readLong(in);
        this.executionTime  = ExternalizableHelper.readLong(in);
        this.startTime      = ExternalizableHelper.readLong(in);
        this.isResuming     = in.readBoolean();
        this.owner          = ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, this.submissionResultID);
        ExternalizableHelper.writeObject(out, this.submissionKey);
        ExternalizableHelper.writeObject(out, this.sessionIdentifier);
        ExternalizableHelper.writeObject(out, this.result);
        ExternalizableHelper.writeSafeUTF(out, submissionState.name());
        ExternalizableHelper.writeObject(out, this.progress);
        ExternalizableHelper.writeLong(out, this.submissionTime);
        ExternalizableHelper.writeLong(out, this.latency);
        ExternalizableHelper.writeLong(out, this.executionTime);
        ExternalizableHelper.writeLong(out, this.startTime);
        out.writeBoolean(isResuming);
        ExternalizableHelper.writeObject(out, this.owner);
    }


    /**
    * {@inheritDoc}
    */
    public void readExternal(final PofReader reader) throws IOException
    {
        this.submissionResultID = (Identifier) reader.readObject(0);
        this.submissionKey      = (SubmissionKey) reader.readObject(1);
        this.sessionIdentifier  = (Identifier) reader.readObject(2);
        this.result             = reader.readObject(3);
        this.submissionState    = (SubmissionState) Enum.valueOf(SubmissionState.class, reader.readString(4));
        this.progress           = reader.readObject(5);
        this.submissionTime     = reader.readLong(6);
        this.latency            = reader.readLong(7);
        this.executionTime      = reader.readLong(8);
        this.startTime          = reader.readLong(9);
        this.isResuming         = reader.readBoolean(10);
        this.owner              = reader.readObject(11);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final PofWriter writer) throws IOException
    {
        writer.writeObject(0, submissionResultID);
        writer.writeObject(1, submissionKey);
        writer.writeObject(2, sessionIdentifier);
        writer.writeObject(3, result);
        writer.writeString(4, submissionState.name());
        writer.writeObject(5, progress);
        writer.writeLong(6, submissionTime);
        writer.writeLong(7, latency);
        writer.writeLong(8, executionTime);
        writer.writeLong(9, startTime);
        writer.writeBoolean(10, isResuming);
        writer.writeObject(11, owner);
    }
}
