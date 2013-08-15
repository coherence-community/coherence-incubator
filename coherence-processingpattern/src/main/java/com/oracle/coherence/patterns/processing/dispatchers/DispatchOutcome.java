/*
 * File: DispatchOutcome.java
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

import java.util.concurrent.TimeUnit;

/**
 * A {@link DispatchOutcome} captures the result of an attempt to dispatch a
 * {@link PendingSubmission} with a {@link Dispatcher} for processing.
 * <p>
 * Internally a {@link DispatchOutcome} is used to signal to a
 * {@link DispatchController} what a {@link Dispatcher} did with a
 * {@link PendingSubmission} during a call to
 * {@link Dispatcher#dispatch(PendingSubmission)} and/or advice to the
 * {@link DispatchController} on what to do next with the said
 * {@link PendingSubmission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Christer Fahlgren
 */
public interface DispatchOutcome
{
    /**
     * Pre-constructed {@link DispatchOutcome.Accepted} object.
     */
    public static final DispatchOutcome ACCEPTED = new DispatchOutcome.Accepted();

    /**
     * Pre-constructed {@link DispatchOutcome.Rejected} object.
     */
    public static final DispatchOutcome REJECTED = new DispatchOutcome.Rejected();

    /**
     * Preconstructed {@link DispatchOutcome.RetryLater} object.
     */
    public static final DispatchOutcome CONTINUE = new DispatchOutcome.RetryLater();


    /**
     * An {@link Abort} {@link DispatchOutcome} signals that a
     * {@link Dispatcher} believes a {@link PendingSubmission} should no
     * longer be attempted to be dispatched or processed (typically due to
     * some error condition).
     * <p>
     * The {@link DispatchController} should assume that the
     * {@link PendingSubmission} has "finished" processing and set the result
     * of the processing for the said {@link PendingSubmission} be set to the
     * value provided by the {@link DispatchOutcome}.
     */
    public static final class Abort implements DispatchOutcome
    {
        /**
         * The rationale for aborting the submission.
         */
        private String m_sRationale;

        /**
         * The result of the abortion of the submission.
         */
        private Object result;


        /**
         * Constructor taking rationale and result as input parameters.
         *
         * @param sRationale the rationale for the abortion
         * @param oResult the potential result at the point of abortion
         */
        public Abort(String sRationale,
                     Object oResult)
        {
            this.m_sRationale = sRationale;
            this.result       = oResult;
        }


        /**
         * Returns the rationale.
         *
         * @return the rationale
         */
        public String getRationale()
        {
            return m_sRationale;
        }


        /**
         * Returns the result.
         *
         * @return the result object
         */
        public Object getResult()
        {
            return result;
        }
    }


    /**
     * An {@link Accepted} {@link DispatchOutcome} signals that a
     * {@link Dispatcher} has successfully dispatched a
     * {@link PendingSubmission} for processing and that no further attempts
     * to dispatch the said {@link PendingSubmission} should be made by the
     * said {@link Dispatcher}.
     */
    public static final class Accepted implements DispatchOutcome
    {
    }


    /**
     * A {@link Rejected} {@link DispatchOutcome} signals that a
     * {@link Dispatcher} could not and may never be able to process a
     * {@link PendingSubmission}. The {@link DispatchController} may (and
     * should) attempt to use another {@link Dispatcher} to have the said
     * {@link PendingSubmission} processed.
     * <p>
     * Should all known {@link Dispatcher}s reject a {@link PendingSubmission}
     * (ie: not be able to dispatch it) for processing, the said
     * {@link PendingSubmission} will be suspended until a new {@link Dispatcher} is
     * registered / updated with the underlying {@link DispatchController}.
     */
    public static final class Rejected implements DispatchOutcome
    {
    }


    /**
     * A {@link RetryLater} {@link DispatchOutcome} signals that a
     * {@link Dispatcher} is capable of processing a {@link PendingSubmission}
     * but is currently unavailable to do so at a point in time.
     * <p>
     * The {@link DispatchController} may (and should) attempt to use another
     * {@link Dispatcher} to have the said {@link PendingSubmission}
     * processed, but if all those fail, the {@link DispatchController} should
     * only wait for the "shortest" possible time (indicated by the
     * "retryDelay" attribute) before attempting to "redispatch" the said
     * {@link PendingSubmission}.
     */
    public static final class RetryLater implements DispatchOutcome
    {
        /**
         * The delay to apply before resubmitting.
         */
        private long delay;

        /**
         * The time unit for the delay.
         */
        private TimeUnit timeUnit;


        /**
         * Default constructor.
         */
        public RetryLater()
        {
            this.delay    = 0;
            this.timeUnit = TimeUnit.SECONDS;
        }


        /**
         * Constructor taking the delay and time unit as input.
         *
         * @param lDelay     the delay
         * @param oTimeUnit  the unit of time
         */
        public RetryLater(long     lDelay,
                          TimeUnit oTimeUnit)
        {
            this.delay    = lDelay;
            this.timeUnit = oTimeUnit;
        }


        /**
         * Returns the delay.
         *
         * @return the delay
         */
        public long getDelay()
        {
            return delay;
        }


        /**
         * Returns the {@link TimeUnit}.
         *
         * @return the TimeUnit of the delay
         */
        public TimeUnit getTimeUnit()
        {
            return timeUnit;
        }
    }
}
