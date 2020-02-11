/*
 * File: ParallelAwareAggregatorIT.java
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

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractParallelAggregatorTest
{
    private static final Serializer DEFAULT_SERIALIZER = new DefaultSerializer();
    private static final Serializer POF_SERIALIZER = new ConfigurablePofContext("test-pof-config.xml");

    @Test
    public void shouldCreateAggregator()
    {
        ValueExtractor<?, ?> extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub       aggregator = new AggregatorStub(extractor);

        assertThat(aggregator.getValueExtractor(), is(sameInstance(extractor)));
        assertThat(aggregator.isParallel(), is(false));
    }

    @Test
    public void shouldCreateAggregatorFromMethod()
    {
        ValueExtractor<?, ?> extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub       aggregator = new AggregatorStub("foo");

        assertThat(aggregator.getValueExtractor(), is(extractor));
        assertThat(aggregator.isParallel(), is(false));
    }

    @Test
    public void shouldCreateAggregatorFromChainedMethod()
    {
        ValueExtractor<?, ?> extractor  = new ChainedExtractor<>("foo.bar");
        AggregatorStub       aggregator = new AggregatorStub("foo.bar");

        assertThat(aggregator.getValueExtractor(), is(extractor));
        assertThat(aggregator.isParallel(), is(false));
    }

    @Test
    public void shouldGetParallelAggregator()
    {
        ValueExtractor<?, ?> extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub       aggregator = new AggregatorStub(extractor);
        AggregatorStub       parallel   = (AggregatorStub) aggregator.getParallelAggregator();

        assertThat(parallel, is(notNullValue()));
        assertThat(parallel.getValueExtractor(), is(extractor));
        assertThat(parallel.isParallel(), is(true));
    }

    @Test
    public void shouldSerializeUsingExternalizableLite()
    {
        ValueExtractor<?, ?>  extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub        aggregator = new AggregatorStub(extractor);

        asseertSerialization(DEFAULT_SERIALIZER, aggregator);
    }

    @Test
    public void shouldSerializeParallelAggregatorUsingExternalizableLite()
    {
        ValueExtractor<?, ?>  extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub        aggregator = (AggregatorStub) new AggregatorStub(extractor).getParallelAggregator();

        asseertSerialization(DEFAULT_SERIALIZER, aggregator);
    }

    @Test
    public void shouldSerializeUsingPOF()
    {
        ValueExtractor<?, ?>  extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub        aggregator = new AggregatorStub(extractor);

        asseertSerialization(POF_SERIALIZER, aggregator);
    }

    @Test
    public void shouldSerializeParallelAggregatorUsingPOF()
    {
        ValueExtractor<?, ?>  extractor  = new ReflectionExtractor<>("foo");
        AggregatorStub        aggregator = (AggregatorStub) new AggregatorStub(extractor).getParallelAggregator();

        asseertSerialization(POF_SERIALIZER, aggregator);
    }

    public void asseertSerialization(Serializer serializer, AggregatorStub aggregator)
    {
        Binary                 binary     = ExternalizableHelper.toBinary(aggregator, serializer);
        AggregatorStub         result     = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.isParallel(), is(aggregator.isParallel()));
        assertThat(result.getValueExtractor(), is(aggregator.getValueExtractor()));
    }


    @SuppressWarnings("rawtypes")
    public static class AggregatorStub
        extends AbstractParallelAggregator
    {
        public AggregatorStub()
        {
        }

        @SuppressWarnings("unchecked")
        public AggregatorStub(ValueExtractor extractor)
        {
            super(extractor);
        }

        public AggregatorStub(String method)
        {
            super(method);
        }

        @Override
        protected void init(boolean isFinal)
        {
        }

        @Override
        protected void process(Object o, boolean isFinal)
        {
        }

        @Override
        protected Object finalizeResult(boolean isFinal)
        {
            return null;
        }

        @Override
        public Object aggregateResults(Collection collection)
        {
            return null;
        }
    }
}
