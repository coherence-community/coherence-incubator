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

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.util.LiteSet;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.extractor.IdentityExtractor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParallelAwareAggregatorIT
{
    @BeforeClass
    public static void setup()
    {
        DefaultCacheServer.startServerDaemon();
    }

    @AfterClass
    public static void cleanup()
    {
        DefaultCacheServer.shutdown();
    }

    @Test
    public void shouldAggregate()
    {
        NamedCache<String, String>             cache      = CacheFactory.getCache("test-cache");
        AggregatorStub<String, String, String> aggregator = new AggregatorStub<>(IdentityExtractor.INSTANCE());

        for (int i=0; i<1000; i++)
        {
            cache.put("key-" + i, String.valueOf(i % 10));
        }

        Collection<String> expected = cache.aggregate(new DistinctValues<>(IdentityExtractor.INSTANCE()));
        Collection<String> result   = cache.aggregate(aggregator);

        assertThat(result, is(expected));
    }

    /**
     * A {@link AbstractParallelAggregator} implementation that behaves the same
     * as the built-in Coherence {@link DistinctValues} aggregator.
     *
     * @param <K>  the cache key type
     * @param <V>  the cache value type
     * @param <R>  the expected result type
     */
    @SuppressWarnings("rawtypes")
    public static class AggregatorStub<K, V, R>
            extends AbstractParallelAggregator<K, V, R, Set<R>>
    {
        private Set<R> results;

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
            Set<R> set = results;
            if (set != null)
            {
                set.clear();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void process(Object o, boolean isFinal)
        {
            if (o != null)
            {
                if (isFinal)
                {
                    // aggregate partial results
                    Collection<R> partial = (Collection<R>) o;
                    if (!partial.isEmpty())
                    {
                        ensureSet().addAll(partial);
                    }
                }
                else
                {
                    // collect partial results
                    ensureSet().add(o);
                }
            }
        }

        @Override
        protected Set<R> finalizeResult(boolean isFinal)
        {
            Set<R> set = results;
            results = null;

            if (isFinal)
            {
                // return the final aggregated result
                return set == null ? NullImplementation.getSet() : set;
            }
            else
            {
                // return partial aggregation data
                return set;
            }
        }

        protected Set ensureSet()
        {
            Set<R> set = results;
            if (set == null)
            {
                set = results = new LiteSet<>();
            }
            return set;
        }
    }
}
