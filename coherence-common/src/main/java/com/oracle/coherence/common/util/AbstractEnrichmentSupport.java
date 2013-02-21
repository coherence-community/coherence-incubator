/*
 * File: AbstractEnrichmentSupport.java
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

package com.oracle.coherence.common.util;

import java.util.Collections;
import java.util.HashMap;

/**
 * An {@link AbstractEnrichmentSupport} provides a base implementation for {@link EnrichmentSupport}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractEnrichmentSupport implements EnrichmentSupport
{
    /**
     * The enrichments by type and name being tracked.
     */
    private HashMap<Class<?>, HashMap<?, ?>> enrichmentsByType;


    /**
     * Standard Constructor.
     */
    public AbstractEnrichmentSupport()
    {
        this.enrichmentsByType = new HashMap<Class<?>, HashMap<?, ?>>();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T, N> void addEnrichment(java.lang.Class<T> type,
                                     N                  enrichmentKey,
                                     T                  enrichment)
    {
        HashMap<N, T> namedEnrichments = (HashMap<N, T>) enrichmentsByType.get(type);

        if (namedEnrichments == null)
        {
            namedEnrichments = new HashMap<N, T>();
            enrichmentsByType.put(type, namedEnrichments);
        }

        namedEnrichments.put(enrichmentKey, enrichment);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void addEnrichmentsFrom(EnrichmentSupport enrichmentSupport)
    {
        for (Class<?> type : enrichmentSupport.getEnrichmentTypes())
        {
            for (Object enrichmentKey : enrichmentSupport.getEnrichmentKeys(type))
            {
                addEnrichment((Class<Object>) type,
                              (Object) enrichmentKey,
                              (Object) enrichmentSupport.getEnrichment(type, enrichmentKey));
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T, N> T getEnrichment(java.lang.Class<T> type,
                                  N                  enrichmentKey)
    {
        return (T) (enrichmentsByType.containsKey(type) ? enrichmentsByType.get(type).get(enrichmentKey) : null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<?> getEnrichmentKeys(Class<?> type)
    {
        return (enrichmentsByType.containsKey(type) ? enrichmentsByType.get(type).keySet() : Collections.EMPTY_LIST);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> getEnrichments(Class<T> type)
    {
        return (enrichmentsByType.containsKey(type) ? enrichmentsByType.get(type).values() : Collections.EMPTY_LIST);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Class<?>> getEnrichmentTypes()
    {
        return enrichmentsByType.keySet();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T, N> boolean hasEnrichment(java.lang.Class<T> type,
                                        N                  enrichmentKey)
    {
        return getEnrichment(type, enrichmentKey) != null;
    }
}
