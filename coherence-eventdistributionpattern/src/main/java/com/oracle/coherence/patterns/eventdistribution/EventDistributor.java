/*
 * File: EventDistributor.java
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

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * An {@link EventDistributor} is an application defined domain to which related application {@link Event}s are
 * sent for distribution by {@link EventChannel}s.
 * <p>
 * {@link EventDistributor}s have two names.  A user-friendly application specific symbolic name and an external name.
 * The external name is often generated and used to manage references to implementation specific resources,
 * typically external to the system.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventDistributor
{
    /**
     * Determines the {@link Identifier} of the {@link EventDistributor}.
     *
     * @return An {@link Identifier}.
     */
    public Identifier getIdentifier();


    /**
     * Establishes an {@link EventChannelController} for the {@link EventDistributor} with the specified
     * {@link EventChannelController.Dependencies} that will be used to realize an {@link EventChannel}.
     * <p>
     * Note: This method may be <strong>called using a Coherence Thread</strong>.  Consequently care should be taken
     * when initializing resources.  Coherence however will be configured and running at this stage, so access
     * to caches is generally permitted.
     *
     * @param dependencies      The {@link EventChannelController.Dependencies} for the {@link EventChannelController}.
     * @param parameterProvider The {@link ParameterProvider} for resolving parameters when building {@link EventChannel}s.
     */
    public EventChannelController.Identifier establishEventChannelController(EventChannelController
        .Dependencies                                                                          dependencies,
                                                                             ParameterProvider parameterProvider);


    /**
     * Requests an {@link Event} to be distributed via the currently established {@link EventChannel}s.
     * <p>
     * Note: {@link Event} distribution may occur asynchronously, depending on the {@link EventDistributor}
     * implementation.
     *
     * @param event The {@link Event} to distribute.
     */
    public void distribute(Event event);


    /**
     * Requests a collection of {@link Event}s to be distributed via the currently established {@link EventChannel}s.
     * <p>
     * Note: {@link Event} distribution may occur asynchronously, depending on the {@link EventDistributor}
     * implementation.
     *
     * @param events The {@link List} of {@link Event}s to distribute.
     */
    public void distribute(List<Event> events);


    /**
     * An {@link Identifier} for a {@link EventDistributor}.
     */
    @SuppressWarnings("serial")
    public static class Identifier implements ExternalizableLite, PortableObject
    {
        /**
         * An application provided user-friendly symbolic name for a {@link EventDistributor}.
         */
        private String symbolicName;

        /**
         * An {@link EventDistributorBuilder} generated external name for a {@link EventDistributor}.
         * (as the application provided symbolic name may not be appropriate).
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
         * @param symbolicName The application provided symbolic name of a {@link EventDistributor}
         * @param externalName The {@link EventDistributorBuilder} specific name for a {@link EventDistributor}.
         */
        public Identifier(String symbolicName,
                          String externalName)
        {
            this.symbolicName = symbolicName;
            this.externalName = externalName;
        }


        /**
         * Determines application specific symbolic name of a {@link EventDistributor}.
         *
         * @return A {@link String}
         */
        public String getSymbolicName()
        {
            return symbolicName;
        }


        /**
         * Determines the {@link EventDistributorBuilder} external name for a {@link EventDistributor}.
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
        public String toString()
        {
            return String.format("EventDistributor.Identifier{symbolicName=%s, externalName=%s}",
                                 symbolicName,
                                 externalName);
        }
    }
}
