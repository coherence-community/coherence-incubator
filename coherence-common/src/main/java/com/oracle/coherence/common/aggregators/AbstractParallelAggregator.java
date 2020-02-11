/*
 * File: AbstractParallelAggregator.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.common.aggregators;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An abstract base class implementation of
 * {@link com.tangosol.util.InvocableMap.EntryAggregator} that supports
 * parallel aggregation.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <E> the type of the extracted value to aggregate
 * @param <R> the type of the aggregation result
 */
public abstract class AbstractParallelAggregator<K, V, E, R>
        implements InvocableMap.ParallelAwareAggregator<K, V, Object, R>, ExternalizableLite, PortableObject
{
    /**
     * Set to true if this aggregator realizes that it is going to be used in
     * parallel.
     */
    private boolean parallel;

    /**
     * The ValueExtractor that obtains the value to aggregate from the value
     * that is stored in the Map.
     */
    private ValueExtractor<?, ? extends E> extractor;

    /**
     * Default constructor (necessary for serialization).
     */
    public AbstractParallelAggregator()
    {
    }

    /**
     * Construct an AbstractAggregator that will aggregate values extracted
     * from a set of {@link com.tangosol.util.InvocableMap.Entry} objects.
     *
     * @param extractor the extractor that provides values to aggregate
     * @param <T>       the type of the value to extract from
     *
     * @throws NullPointerException if the extractor parameter is {@code null}
     */
    public <T> AbstractParallelAggregator(ValueExtractor<? super T, ? extends E> extractor)
    {
        this.extractor = Lambdas.ensureRemotable(Objects.requireNonNull(extractor));
    }

    /**
     * Construct an AbstractAggregator that will aggregate values extracted
     * from a set of {@link com.tangosol.util.InvocableMap.Entry} objects.
     *
     * @param method the name of the method that could be invoked via
     *                reflection and that returns values to aggregate;
     *                this parameter can also be a dot-delimited sequence of
     *                method names which would result in an aggregator based
     *                on the {@link ChainedExtractor} that is based on an
     *                array of corresponding {@link ReflectionExtractor}
     *                objects
     *
     * @throws NullPointerException if the method parameter is {@code null}
     */
    public AbstractParallelAggregator(String method)
    {
        extractor = method.indexOf('.') < 0 ?
                new ReflectionExtractor<>(method) :
                new ChainedExtractor<>(method);
    }

    boolean isParallel()
    {
        return parallel;
    }

    public R aggregate(Stream<? extends InvocableMap.Entry<? extends K, ? extends V>> stream)
    {
        boolean isFinal = !parallel;
        init(isFinal);

        stream.forEach(this::processEntry);

        return finalizeResult(isFinal);
    }

    @Override
    public R aggregate(Set<? extends InvocableMap.Entry<? extends K, ? extends V>> entries)
    {
        boolean isFInal = !parallel;
        init(isFInal);

        for (InvocableMap.Entry<? extends K, ? extends V> setEntry : entries)
        {
            processEntry(setEntry);
        }

        return finalizeResult(isFInal);
    }

    /**
     * Incorporate one aggregatable entry into the result.
     *
     * @param entry the entry to incorporate into the aggregation result
     */
    protected void processEntry(InvocableMap.Entry<? extends K, ? extends V> entry)
    {
        process(entry.extract(getValueExtractor()), false);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public InvocableMap.EntryAggregator<K, V, Object> getParallelAggregator()
    {
        parallel = true;
        return (InvocableMap.EntryAggregator) this;
    }

    @Override
    public R aggregateResults(Collection<Object> results)
    {
        init(true);

        for (Object o : results)
        {
            process(o, true);
        }

        try
        {
            // the constructs in AbstractAggregator repel the use of generics,
            // hence the inevitable cast
            return finalizeResult(true);
        }
        finally
        {
            // reset the state to allow the aggregator re-use
            parallel = false;
        }
    }

    /**
     * Determine the ValueExtractor whose values this aggregator is
     * aggregating.
     *
     * @return the ValueExtractor used by this aggregator
     */
    public ValueExtractor<?, ? extends E> getValueExtractor()
    {
        return extractor;
    }

    /**
     * Initialize the aggregation result.
     *
     * @param isFinal {@code true} is passed if the aggregation process that is being
     *                initialized must produce a final aggregation result; this will
     *                only be false if a parallel approach is being used and the initial
     *                (partial) aggregation process is being initialized
     */
    protected abstract void init(boolean isFinal);

    /**
     * Incorporate one aggregatable value into the result.
     * <p/>
     * If the <tt>isFinal</tt> parameter is true, the given object is a partial
     * result (returned by an individual parallel aggregator) that should be
     * incorporated into the final result; otherwise, the object is a value
     * extracted from an {@link com.tangosol.util.InvocableMap.Entry}.
     *
     * @param o       the value to incorporate into the aggregated result
     * @param isFinal true to indicate that the given object is a partial
     *                result returned by a parallel aggregator
     */
    protected abstract void process(Object o, boolean isFinal);

    /**
     * Obtain the result of the aggregation.
     * <p/>
     * If the <tt>isFinal</tt> parameter is true, the returned object must be
     * the final result of the aggregation; otherwise, the returned object
     * will be treated as a partial result that should be incorporated into
     * the final result.
     *
     * @param isFinal {@code true} to indicate that the final result of the
     *                aggregation process should be returned; this will only
     *                be false if a parallel approach is being used
     *
     * @return the result of the aggregation process
     */
    protected abstract R finalizeResult(boolean isFinal);

    @Override
    public String toString()
    {
        return ClassHelper.getSimpleName(getClass()) + '('
               + getValueExtractor().toString() + ')';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        AbstractParallelAggregator<?, ?, ?, ?> that = (AbstractParallelAggregator<?, ?, ?, ?>) o;
        return parallel == that.parallel && Objects.equals(extractor, that.extractor);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(parallel, extractor);
    }

    @Override
    public void readExternal(DataInput in) throws IOException
    {
        parallel  = in.readBoolean();
        extractor = ExternalizableHelper.readObject(in);
    }

    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        out.writeBoolean(parallel);
        ExternalizableHelper.writeObject(out, extractor);
    }

    @Override
    public void readExternal(PofReader in) throws IOException
    {
        parallel  = in.readBoolean(0);
        extractor = in.readObject(1);
    }

    @Override
    public void writeExternal(PofWriter out) throws IOException
    {
        out.writeBoolean(0, parallel);
        out.writeObject(1, extractor);
    }
}
