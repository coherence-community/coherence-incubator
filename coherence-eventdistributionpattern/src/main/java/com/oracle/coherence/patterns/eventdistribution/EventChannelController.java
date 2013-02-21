/*
 * File: EventChannelController.java
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

package com.oracle.coherence.patterns.eventdistribution;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link EventChannelController} is responsible for managing the infrastructure to enable an {@link EventChannel}
 * to send {@link Event}s.
 * <p>
 * Note: All work performed by implementations of this interface are assumed to occur  <strong>asynchronously</strong>.
 * It should always be possible to call implementations of this interface from any thread, without concern for deadlock,
 * especially with in Coherence.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventChannelController
{
    /**
     * Each {@link EventChannelController} has a {@link Mode} that indicates it's ability to send {@link Event}s.
     */
    public enum Mode
    {
        /**
         * An {@link #ENABLED} {@link EventChannelController} may send {@link Event}s using its associated
         * {@link EventChannel} as and when it requires.
         */
        ENABLED,

        /**
         * A {@link #DISABLED} {@link EventChannelController} will not send {@link Event}s provided to it.
         * Furthermore it:
         * <ol>
         *     <li><strong>will not queue</strong> pending {@link Event}s, and</li>
         *     <li><strong>will discard</strong> all pending {@link Event}s when it becomes disabled.</li>
         * </ol>
         */
        DISABLED,

        /**
         * A {@link #SUSPENDED} {@link EventChannelController} will not send {@link Event}s provided to it. However
         * it <strong>will queue</strong> {@link Event}s, in the order in which they are provided, until it either
         * becomes {@link #ENABLED} or {@link #DISABLED}.
         */
        SUSPENDED
    }


    /**
     * Requests the {@link EventChannelController} to start sending {@link Event}s as soon as possible.
     */
    public void start();


    /**
     * Requests the {@link EventChannelController} to stop sending {@link Event}s as soon as possible.
     * <p>
     * Note: Once stopped a {@link EventChannelController} can't be restarted.
     */
    public void stop();


    /**
     * Notifies the {@link EventChannelController} that {@link Event}s have arrived and are waiting to be distributed.
     * If the {@link EventChannelController} is currently distributing {@link Event}s, is disabled, suspended or
     * stopped, a call to this method will be ignored.
     * <p>
     * Note: It's up to the {@link EventChannelController} implementation to decide when to distribute {@link Event}s.
     * This method simply requests that an {@link EventChannelController} should consider distributing {@link Event}s
     * as soon as possible.
     */
    public void preempt();


    /**
     * Requests the {@link EventChannelController} to suspend sending {@link Event}s as soon as possible.  All {@link Event}s
     * arriving for the said {@link EventChannelController} will be queued until the {@link EventChannelController} is
     * resumed.
     */
    public void suspend();


    /**
     * Requests the {@link EventChannelController} to disable the sending of {@link Event}s as soon as possible.
     * <p>
     * Note: When disabled {@link Event}s <strong>will not be queued</strong> for sending.
     */
    public void disable();


    /**
     * Requests the {@link EventChannelController} to drain all pending {@link Event}s, none of which will
     * be sent.  This method can only be called when a {@link EventChannelController} has been suspended.
     */
    public void drain();


    /**
     * The {@link EventChannelController.Dependencies} for a {@link EventChannelController}.
     */
    public interface Dependencies
    {
        /**
         * Determines the application specified symbolic name of the {@link EventChannelController}.  This is often a
         * user-friendly name for the {@link EventChannelController}.  It will be used in application logs and for
         * monitoring/management.
         *
         * @return A {@link String}
         */
        public String getChannelName();


        /**
         * Determines the name a {@link EventChannelController} may use when referring to external resources.
         * <p>
         * Note: {@link EventChannelController} implementations, especially those using customized, non-Coherence-based
         * {@link EventDistributorBuilder}s, occasionally need to be bound/communicate with external resources
         * (like messaging topics). In order to provide this information to a {@link EventChannelController},
         * an application developer would specify this using an external name.
         *
         * @return A {@link String}
         */
        public String getExternalName();


        /**
         * Determines the {@link ParameterizedBuilder} that the {@link EventChannelController} may use to build
         * an {@link EventChannel} when required.
         *
         * @return An {@link EventChannelBuilder}.
         */
        public ParameterizedBuilder<EventChannel> getEventChannelBuilder();


        /**
         * Determines the {@link ParameterizedBuilder} that may be used to build an {@link EventIteratorTransformer} when
         * required.
         *
         * @return An {@link ParameterizedBuilder} for {@link EventIteratorTransformer}s.
         */
        public ParameterizedBuilder<EventIteratorTransformer> getEventsTransformerBuilder();


        /**
         * Determines desired starting {@link Mode} of a {@link EventChannelController}.
         *
         * @return A {@link Mode}
         */
        public Mode getStartingMode();


        /**
         * Determines the number of milliseconds the {@link EventChannelController} should wait when checking for
         * new {@link Event}s to distribute.
         *
         * @return time (in milliseconds).
         */
        public long getBatchDistributionDelay();


        /**
         * Determines the maximum number of {@link Event}s to distribute in a single batch using an {@link EventChannel}.
         *
         * @return An {@link Integer}.
         */
        public int getBatchSize();


        /**
         * Determines the number of milliseconds the {@link EventChannelController} should delay between distribution
         * failures.
         *
         * @return time (in milliseconds)
         */
        public long getRestartDelay();


        /**
         * Determines the number of consecutive failures that must occur before a {@link EventChannelController} is
         * suspended, after which the {@link EventChannelController} will need to be restarted.
         *
         * @return An {@link Integer}.
         */
        public int getTotalConsecutiveFailuresBeforeSuspending();
    }


    /**
     * An {@link EventChannelController.Identifier} captures how a {@link EventChannelController} is uniquely
     * identified with in a {@link EventDistributor}.
     */
    @SuppressWarnings("serial")
    public static class Identifier implements ExternalizableLite, PortableObject
    {
        /**
         * The application provided symbolic name of the {@link EventChannelController}.
         */
        private String symbolicName;

        /**
         * The external name assigned to the {@link EventChannelController} by the {@link EventDistributor}.
         */
        private String externalName;


        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public Identifier()
        {
            // SKIP: deliberately empty
        }


        /**
         * Standard Constructor.
         *
         * @param symbolicName The application provided symbolic name of the {@link EventChannelController}.
         * @param externalName The external name assigned to the {@link EventChannelController}
         *                     (by a {@link EventChannelController})
         */
        public Identifier(String symbolicName,
                          String externalName)
        {
            this.symbolicName = symbolicName;
            this.externalName = externalName;
        }


        /**
         * The application specified and user-friendly symbolic name of the {@link EventChannelController}.
         *
         * @return A {@link String}
         */
        public String getSymbolicName()
        {
            return symbolicName;
        }


        /**
         * The external name of the {@link EventChannelController} by the {@link EventDistributorBuilder}.
         *
         * @return A {@link String}
         */
        public String getExternalName()
        {
            return externalName;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            final int prime  = 31;
            int       result = 1;

            result = prime * result + ((externalName == null) ? 0 : externalName.hashCode());
            result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());

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

            Identifier other = (Identifier) obj;

            if (externalName == null)
            {
                if (other.externalName != null)
                {
                    return false;
                }
            }
            else if (!externalName.equals(other.externalName))
            {
                return false;
            }

            if (symbolicName == null)
            {
                if (other.symbolicName != null)
                {
                    return false;
                }
            }
            else if (!symbolicName.equals(other.symbolicName))
            {
                return false;
            }

            return true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void readExternal(DataInput in) throws IOException
        {
            this.symbolicName = ExternalizableHelper.readSafeUTF(in);
            this.externalName = ExternalizableHelper.readSafeUTF(in);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeSafeUTF(out, symbolicName);
            ExternalizableHelper.writeSafeUTF(out, externalName);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void readExternal(PofReader reader) throws IOException
        {
            this.symbolicName = reader.readString(1);
            this.externalName = reader.readString(2);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeString(1, symbolicName);
            writer.writeString(2, externalName);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return String.format("EventChannelController.Identifier{symbolicName=%s, externalName=%s}",
                                 getSymbolicName(),
                                 getExternalName());
        }
    }
}
