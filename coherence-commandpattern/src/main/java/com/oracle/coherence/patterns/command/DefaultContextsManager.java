/*
 * File: DefaultContextsManager.java
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

package com.oracle.coherence.patterns.command;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.identifiers.UUIDBasedIdentifier;

import com.oracle.coherence.patterns.command.internal.ContextWrapper;
import com.oracle.coherence.patterns.command.internal.CreateContextProcessor;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ExtractorProcessor;

/**
 * The default implementation of a {@link ContextsManager}.
 * <p>
 * Copyright (c) 2008-2012. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see ContextsManager
 *
 * @author Brian Oliver
 */
public class DefaultContextsManager implements ContextsManager
{
    /**
     * The default {@link ContextsManager}.
     */
    private final static ContextsManager INSTANCE = new DefaultContextsManager();


    /**
     * Standard Constructor
     */
    public DefaultContextsManager()
    {
    }


    /**
     * {@inheritDoc}
     */
    public Identifier registerContext(Identifier ctxIdentifier,
                                      Context context,
                                      ContextConfiguration contextConfiguration)
    {
        NamedCache cacheContexts = CacheFactory.getCache(ContextWrapper.CACHENAME);

        cacheContexts.invoke(ctxIdentifier, new CreateContextProcessor(context, contextConfiguration));

        return ctxIdentifier;
    }


    /**
     * {@inheritDoc}
     */
    public Identifier registerContext(Identifier ctxIdentifier,
                                      Context context)
    {
        return registerContext(ctxIdentifier, context, new DefaultContextConfiguration());
    }


    /**
     * {@inheritDoc}
     */
    public Identifier registerContext(String contextName,
                                      Context context,
                                      ContextConfiguration contextConfiguration)
    {
        return registerContext(StringBasedIdentifier.newInstance(contextName), context, contextConfiguration);
    }


    /**
     * {@inheritDoc}
     */
    public Identifier registerContext(String ctxName,
                                      Context context)
    {
        return registerContext(ctxName, context, new DefaultContextConfiguration());
    }


    /**
     * {@inheritDoc}
     */
    public Identifier registerContext(Context context,
                                      ContextConfiguration contextConfiguration)
    {
        return registerContext(UUIDBasedIdentifier.newInstance(), context, contextConfiguration);
    }


    /**
     * {@inheritDoc}
     */
    public Identifier registerContext(Context context)
    {
        return registerContext(context, new DefaultContextConfiguration());
    }


    /**
     * {@inheritDoc}
     */
    public Context getContext(Identifier ctxIdentifier)
    {
        NamedCache contextsCache = CacheFactory.getCache(ContextWrapper.CACHENAME);

        return (Context) contextsCache.invoke(ctxIdentifier, new ExtractorProcessor("getContext"));
    }


    /**
     * {@inheritDoc}
     */
    public Object extractValueFromContext(Identifier ctxIdentifier,
                                          ValueExtractor valueExtractor)
    {
        NamedCache contextsCache = CacheFactory.getCache(ContextWrapper.CACHENAME);

        return contextsCache.invoke(ctxIdentifier,
                                    new ConditionalProcessor(PresentFilter.INSTANCE,
                                                             new ExtractorProcessor(new ChainedExtractor(new ReflectionExtractor("getContext"),
                                                                                                         valueExtractor))));
    }


    /**
     * Returns an instance of the {@link DefaultContextsManager}.
     */
    public static ContextsManager getInstance()
    {
        return INSTANCE;
    }
}
