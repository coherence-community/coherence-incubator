/*
 * File: EnrichmentSupport.java
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

import com.oracle.coherence.configuration.caching.CacheMapping;

import java.util.Iterator;

/**
 * The {@link EnrichmentSupport} interface defines methods for objects that support enrichment (addition of
 * strongly typed and named attribute values at runtime).
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EnrichmentSupport
{
    /**
     * Returns if the implementing object has the specified enrichment.
     *
     * @param <T>           The type of the enrichment
     * @param <N>           The type of the enrichment key
     * @param type          The type of the enrichment
     * @param enrichmentKey The key of the enrichment
     *
     * @return <code>true</code> if the named enrichment exists on the implementing object,
     *         <code>false</code> otherwise.
     */
    public <T, N> boolean hasEnrichment(Class<T> type,
                                        N        enrichmentKey);


    /**
     * Returns the enrichment associated with the specified name.
     *
     * @param <T>           The type of the enrichment
     * @param <N>           The type of the enrichment key
     * @param type          The type of the enrichment
     * @param enrichmentKey The name of the enrichment
     *
     * @return The enrichment or <code>null</code> if the enrichment is not defined.
     */
    public <T, N> T getEnrichment(Class<T> type,
                                  N        enrichmentKey);


    /**
     * Returns an {@link Iterable} over the enrichment keys for the specified type.
     *
     * @param type  The type of the enrichments required
     *
     * @return An {@link Iterable} over the enrichment keys of the specified type.
     */
    public Iterable<?> getEnrichmentKeys(Class<?> type);


    /**
     * Returns an {@link Iterable} over all of the enrichments defined for the specified type.
     *
     * @param <T>   The type of the enrichment
     * @param type  The type of the enrichments required
     *
     * @return An {@link Iterable} over the decoratations of the specified type.
     */
    public <T> Iterable<T> getEnrichments(Class<T> type);


    /**
     * Returns an {@link Iterator} over the types of enrichments that have been registered.
     *
     * @return An {@link Iterator}
     */
    public Iterable<Class<?>> getEnrichmentTypes();


    /**
     * Adds and/or overrides an existing enrichment with the specified type and name.
     *
     * @param <T>           The type of the enrichment
     * @param <N>           The type of the enrichment key
     * @param type          The type of the enrichment
     * @param enrichmentKey The name of the enrichment (should be unique for the {@link CacheMapping}).
     * @param enrichment The enrichment to add
     */
    public <T, N> void addEnrichment(Class<T> type,
                                     N        enrichmentKey,
                                     T        enrichment);


    /**
     * Adds all of the enrichments from the provided {@link EnrichmentSupport} instance to the this
     * {@link EnrichmentSupport} instance.
     *
     * @param enrichmentSupport The {@link EnrichmentSupport} from which to retrieve enrichments to add.
     */
    public void addEnrichmentsFrom(EnrichmentSupport enrichmentSupport);
}
