/*
 * File: Lease.java
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

package com.oracle.coherence.common.leasing;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link Lease} represents the mutable conditions (typically time)
 * that the owner of the said {@link Lease} may use an entity.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Lease implements ExternalizableLite, PortableObject
{
    /**
     * The {@link Logger} for the class.
     */
    private static final Logger logger = Logger.getLogger(Lease.class.getName());

    /**
     * The instant in time (since the epoc) when the {@link Lease}
     * was acquired.
     */
    private long acquisitionTime;

    /**
     * The instant in time (since te epoc) when the {@link Lease}
     * was last updated.
     */
    private long lastUpdateTime;

    /**
     * The duration for which the {@link Lease} will be valid
     * (from the last update time).
     */
    private long duration;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public Lease()
    {
        this.acquisitionTime = 0;
        this.lastUpdateTime  = 0;
        this.duration        = 0;
    }


    /**
     * Package Level Constructor to create a {@link Lease} from the
     * current time for a specified duration (in milliseconds).
     *
     * @param duration The duration of the {@link Lease} in milliseconds.
     */
    Lease(long duration)
    {
        this.acquisitionTime = System.currentTimeMillis();
        this.lastUpdateTime  = this.acquisitionTime;
        this.duration        = duration;
    }


    /**
     * Package Level Constructor to create a {@link Lease} with a
     * specified time of acquisition and duration.
     *
     * @param acqusitionTime The time (in milliseconds since the epoc) when the lease was acquired.
     * @param duration The duration of the {@link Lease} in milliseconds.
     */
    Lease(long acqusitionTime,
          long duration)
    {
        this.acquisitionTime = acqusitionTime;
        this.lastUpdateTime  = acqusitionTime;
        this.duration        = duration;
    }


    /**
     * Returns the time (since the epoc in milliseconds) since the {@link Lease} was initially acquired.
     *
     * @return the time (since the epoc in milliseconds) since the {@link Lease} was initially acquired
     */
    public long getAcquisitionTime()
    {
        return acquisitionTime;
    }


    /**
     * Returns the time (since the epoc in milliseconds) since the {@link Lease} was last updated.
     *
     * @return the time (since the epoc in milliseconds) since the {@link Lease} was last updated
     */
    public long getLastUpdateTime()
    {
        return lastUpdateTime;
    }


    /**
     * Returns the duration (in milliseconds) that the {@link Lease} is valid (after the last update time).
     *
     * @return the duration (in milliseconds) that the {@link Lease} is valid (after the last update time).
     */
    public long getDuration()
    {
        return duration;
    }


    /**
     * Returns if the {@link Lease} has been canceled.
     *
     * @return if the {@link Lease} has been canceled
     */
    public boolean isCanceled()
    {
        return duration == -1;
    }


    /**
     * Attempts to cancel the ownership of the {@link Lease}.
     * <p>
     * NOTE: If the {@link Lease} has previously been canceled
     * this method does nothing.
     *
     * @param cancel the value to set
     */
    public void setIsCanceled(boolean cancel)
    {
        if (!isCanceled() && cancel)
        {
            this.lastUpdateTime = System.currentTimeMillis();
        }

        this.duration = -1;
    }


    /**
     * Returns if the {@link Lease} is suspended.
     * <p>
     * NOTE: Unlike canceled {@link Lease}s, suspended {@link Lease}s may
     * be revived (extended)
     *
     * @return if the {@link Lease} is suspended
     */
    public boolean isSuspended()
    {
        return duration == -2;
    }


    /**
     * Sets that the {@link Lease} ownership has been suspended
     * (but it may be recovered again at some later point in time).
     * <p>
     * NOTE: to unsuspend a {@link Lease} you should simply extend it.
     *
     * @param suspend the value to set
     */
    public void setIsSuspended(boolean suspend)
    {
        if (!isCanceled() &&!isSuspended() && suspend)
        {
            this.lastUpdateTime = System.currentTimeMillis();
            this.duration       = -2;
        }
    }


    /**
     * Returns if the {@link Lease} has been issued indefinitely and thus never expires.
     *
     * @return if the {@link Lease} has been issued indefinitely and thus never expires.
     */
    public boolean isIndefinite()
    {
        return duration == -3;
    }


    /**
     * Returns if the {@link Lease} is valid at the specified time.
     *
     * @param time The time in milliseconds (since the epoc)
     *
     *  @return if the {@link Lease} is valid at the specified time.
     */
    public boolean isValidAt(long time)
    {
        return isIndefinite() || (!isCanceled() &&!isSuspended() && lastUpdateTime + duration >= time);
    }


    /**
     * Attempts to extend the {@link Lease} by a specified number of milliseconds from the current point in time.
     *
     * @param duration the number of milliseconds to extend the lease by (from the current point in time)
     *
     * @return <code>true</code> if extending the {@link Lease} was permitted.
     */
    public boolean extend(long duration)
    {
        if (isIndefinite())
        {
            this.lastUpdateTime = System.currentTimeMillis();

            return true;

        }
        else if (isCanceled())
        {
            return false;

        }
        else if (duration >= 0)
        {
            this.duration       = duration;
            this.lastUpdateTime = System.currentTimeMillis();

            return true;

        }
        else
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.finest(String.format("Attempted to extend %s by a negative duration %d. Ignoring request", this,
                                            duration));
            }

            return false;
        }
    }


    /**
     * Attempts to extend the {@link Lease} by the currently specified duration from the current point in time.
     *
     * @return <code>true</code> if extending the {@link Lease} was permitted.
     */
    public boolean extend()
    {
        return this.extend(getDuration());
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.acquisitionTime = ExternalizableHelper.readLong(in);
        this.lastUpdateTime  = ExternalizableHelper.readLong(in);
        this.duration        = ExternalizableHelper.readLong(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, acquisitionTime);
        ExternalizableHelper.writeLong(out, lastUpdateTime);
        ExternalizableHelper.writeLong(out, duration);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.acquisitionTime = reader.readLong(0);
        this.lastUpdateTime  = reader.readLong(1);
        this.duration        = reader.readLong(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeLong(0, acquisitionTime);
        writer.writeLong(1, lastUpdateTime);
        writer.writeLong(2, duration);
    }


    /**
     * {@inheritDoc}
     */
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

        if (!(obj instanceof Lease))
        {
            return false;
        }

        Lease other = (Lease) obj;

        if (acquisitionTime != other.acquisitionTime)
        {
            return false;
        }

        if (duration != other.duration)
        {
            return false;
        }

        if (lastUpdateTime != other.lastUpdateTime)
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        if (isIndefinite())
        {
            return String.format("Lease{acquistionTime=%s, lastUpdateTime=%s, duration=INDEFINITE}",
                                 new Date(acquisitionTime),
                                 new Date(lastUpdateTime));

        }
        else if (isSuspended())
        {
            return String.format("Lease{acquistionTime=%s, lastUpdateTime=%s, duration=SUSPENDED}",
                                 new Date(acquisitionTime),
                                 new Date(lastUpdateTime));

        }
        else if (isCanceled())
        {
            return String.format("Lease{acquistionTime=%s, lastUpdateTime=%s, duration=CANCELED}",
                                 new Date(acquisitionTime),
                                 new Date(lastUpdateTime));

        }
        else
        {
            return String.format("Lease{acquistionTime=%s, lastUpdateTime=%s, duration=%d}",
                                 new Date(acquisitionTime),
                                 new Date(lastUpdateTime),
                                 duration);
        }
    }
}
