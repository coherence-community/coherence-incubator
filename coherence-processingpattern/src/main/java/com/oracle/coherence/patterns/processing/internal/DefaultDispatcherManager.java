/*
 * File: DefaultDispatcherManager.java
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

package com.oracle.coherence.patterns.processing.internal;

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.lifecycle.LifecycleEvent;
import com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent;
import com.oracle.coherence.common.events.lifecycle.LifecycleStoppedEvent;
import com.oracle.coherence.common.events.lifecycle.NamedCacheStorageReleasedEvent;
import com.oracle.coherence.common.events.processing.AbstractAsynchronousEventProcessor;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.LifecycleEventFilter;
import com.oracle.coherence.environment.extensible.dependencies.DependencyReference;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.dependencies.EnvironmentReference;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.friends.DispatcherManager;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalRemove;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the {@link DispatcherManager} interface.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class DefaultDispatcherManager extends AbstractAsynchronousEventProcessor<LifecycleEvent<?>>
    implements DispatcherManager
{
    /**
     * The name of the {@link DispatcherManager} cache.
     */
    public static final String CACHENAME = "coherence.patterns.processing.dispatchers";

    /**
     * Dispatchers cache.
     */
    private NamedCache dispatcherCache;

    /**
     * The {@link ConfigurableCacheFactory} for this {@link DispatcherManager}.
     */
    private ConfigurableCacheFactory ccFactory;

    /**
     * A boolean indicating whether the {@link DispatcherManager} has started.
     */
    private volatile boolean started;

    /**
     * The {@link Dispatcher}s that are to be registered when the {@link DispatcherManager} starts.
     */
    private ConcurrentHashMap<Integer, Dispatcher> toBeRegistered;


    /**
     * Standard constructor.
     *
     * @param ccFactory the ConfigurableCacheFactory to use.
     */
    public DefaultDispatcherManager(ConfigurableCacheFactory ccFactory)
    {
        started        = false;
        this.ccFactory = ccFactory;
        toBeRegistered = new ConcurrentHashMap<Integer, Dispatcher>();
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesSatisfied(Environment environment)
    {
        dispatcherCache = ccFactory.ensureCache(CACHENAME, null);
        started         = true;

        Iterator<Integer> iter = toBeRegistered.keySet().iterator();

        while (iter.hasNext())
        {
            Integer key = iter.next();

            registerDispatcher(key, toBeRegistered.get(key));
        }

        // Start listening for lifecycle events since we want to know when we are shutting down
        environment.getResource(EventDispatcher.class).registerEventProcessor(LifecycleEventFilter.INSTANCE, this);

        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStartedEvent<DependentResource>(this));

    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesViolated(Environment environment)
    {
        dispatcherCache = null;
        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStoppedEvent<DependentResource>(this));
    }


    /**
     * {@inheritDoc}
     */
    public void registerDispatcher(int              priority,
                                   final Dispatcher dispatcher)
    {
        if (!started)
        {
            toBeRegistered.put(priority, dispatcher);
        }
        else
        {
            Filter filter = new NotFilter(PresentFilter.INSTANCE);

            dispatcherCache.invoke(priority, new ConditionalPut(filter, dispatcher));
        }
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterDispatcher(int              priority,
                                     final Dispatcher dispatcher)
    {
        dispatcherCache.invoke(priority, new ConditionalRemove(PresentFilter.INSTANCE));
    }


    /**
     * {@inheritDoc}
     */
    public Set<DependencyReference> getDependencyReferences()
    {
        return Collections.singleton((DependencyReference) new EnvironmentReference());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void processLater(EventDispatcher   eventDispatcher,
                             LifecycleEvent<?> event)
    {
        if (event instanceof NamedCacheStorageReleasedEvent)
        {
            if (CACHENAME.equals(((NamedCacheStorageReleasedEvent) event).getCacheName()))
            {
                onDependenciesViolated(eventDispatcher.getEnvironment());
            }
        }
    }
}
