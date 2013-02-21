/*
 * File: ClusteredSequenceGenerator.java
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

package com.oracle.coherence.common.sequencegenerators;

import com.oracle.coherence.common.ranges.Range;
import com.oracle.coherence.common.ranges.Ranges;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link ClusteredSequenceGenerator} is an implementation
 * of a {@link SequenceGenerator} that uses Coherence to maintain
 * a cluster-scoped specifically named sequence
 * (based on a Coherence Cluster).
 *
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ClusteredSequenceGenerator implements SequenceGenerator
{
    /**
     * The name of the sequence.
     */
    private String sequenceName;

    /**
     * The initial value of the sequence to be used
     * if the named sequence has not been defined in the cluster.
     */
    private long initialValue;


    /**
     * Standard Constructor.
     *
     * @param sequenceName The name of the sequence being maintained
     * @param initialValue The initial value of the sequence if it does not
     *                     exist in the cluster.
     */
    public ClusteredSequenceGenerator(String sequenceName,
                                      long   initialValue)
    {
        this.sequenceName = sequenceName;
        this.initialValue = initialValue;
    }


    /**
     * {@inheritDoc}
     */
    public long next()
    {
        return next(1).getFrom();
    }


    /**
     * {@inheritDoc}
     */
    public Range next(long sequenceSize)
    {
        NamedCache namedCache = CacheFactory.getCache(State.CACHENAME);
        long from = (Long) namedCache.invoke(sequenceName,
                                             new GenerateSequenceNumberProcessor(initialValue, sequenceSize));

        return Ranges.newRange(from, from + sequenceSize - 1);
    }


    /**
     * The {@link GenerateSequenceNumberProcessor} is used to
     * generate one-or-more sequence numbers from a named {@link ClusteredSequenceGenerator}
     * (using the {@link State}).
     */
    @SuppressWarnings("serial")
    public static class GenerateSequenceNumberProcessor extends AbstractProcessor implements ExternalizableLite,
                                                                                             PortableObject
    {
        /**
         * The number of sequence numbers to generate (skip after generating the first).
         */
        private long sequenceSize;

        /**
         * The initial value of the sequence, if it does not exist.
         */
        private long initialValue;


        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public GenerateSequenceNumberProcessor()
        {
            this.sequenceSize = 1;
            this.initialValue = 1;
        }


        /**
         * Standard Constructor.
         *
         * @param initialValue Used iff the underlying sequence does not exist
         * @param sequenceSize The number of values to generate (allocate from the sequence)
         */
        public GenerateSequenceNumberProcessor(long initialValue,
                                               long sequenceSize)
        {
            this.initialValue = initialValue;
            this.sequenceSize = sequenceSize;
        }


        /**
         * {@inheritDoc}
         */
        public Object process(Entry entry)
        {
            State state;

            if (entry.isPresent())
            {
                state = (State) entry.getValue();
            }
            else
            {
                state = new State(initialValue);
            }

            long value = state.generate(sequenceSize);

            entry.setValue(state);

            return value;
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            this.initialValue = ExternalizableHelper.readLong(in);
            this.sequenceSize = ExternalizableHelper.readLong(in);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeLong(out, initialValue);
            ExternalizableHelper.writeLong(out, sequenceSize);
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            this.initialValue = reader.readLong(0);
            this.sequenceSize = reader.readLong(1);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeLong(0, initialValue);
            writer.writeLong(1, sequenceSize);
        }
    }


    /**
     * The {@link State} class represents the next available sequence number
     * for a named {@link ClusteredSequenceGenerator} in a Coherence Cache.
     */
    @SuppressWarnings("serial")
    public static class State implements ExternalizableLite, PortableObject
    {
        /**
         * The name of the Coherence Cache that will store {@link ClusteredSequenceGenerator} {@link State}s.
         */
        public static final String CACHENAME = "coherence.common.sequencegenerators";

        /**
         * The next value that will be issued for the named {@link ClusteredSequenceGenerator}.
         */
        private long nextValue;


        /**
         * Required for {@link ExternalizableLite} and {@link PortableObject}.
         */
        public State()
        {
        }


        /**
         * Standard Constructor.
         *
         * @param initialValue The initial value of the sequence.
         */
        public State(long initialValue)
        {
            this.nextValue = initialValue;
        }


        /**
         * Generates a specified number of sequential ids and returns
         * the first number in the sequence.
         *
         * @param sequenceSize The number of sequential ids to generate
         *
         * @return The first value in the sequence
         */
        public long generate(long sequenceSize)
        {
            long start = nextValue;

            nextValue = nextValue + sequenceSize;

            return start;
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
        {
            this.nextValue = ExternalizableHelper.readLong(in);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
        {
            ExternalizableHelper.writeLong(out, nextValue);
        }


        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader) throws IOException
        {
            this.nextValue = reader.readLong(0);
        }


        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer) throws IOException
        {
            writer.writeLong(0, nextValue);
        }


        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return String.format("ClusteredSequenceGenerator.State{nextValue=%d}", nextValue);
        }
    }
}
