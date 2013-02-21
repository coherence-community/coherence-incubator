/*
 * File: DelegatingBackingMapListener.java
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

package com.oracle.coherence.common.events.dispatching.listeners;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryArrivedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryDepartedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryEvictedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryInsertedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryRemovedEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryStoredEvent;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryUpdatedEvent;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.events.processing.EventProcessorFactory;
import com.oracle.coherence.common.events.processing.annotations.EventProcessorFor;
import com.oracle.coherence.common.events.processing.annotations.LiveObject;
import com.oracle.coherence.common.events.processing.annotations.SupportsEventProcessing;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link DelegatingBackingMapListener} is a Server-Side Backing {@link MapListener} that normalizes and dispatches
 * {@link EntryEvent}s to {@link EventProcessor}s for processing.
 * <p>
 * The {@link EventProcessor} to use to process the {@link EntryEvent}s is determined by looking at the
 * {@link java.util.Map.Entry} value on which the said {@link EntryEvent}s have occurred.
 * <p>
 * If an {@link java.util.Map.Entry} value implements the {@link EventProcessor} interface then the {@link EntryEvent}s
 * are dispatched to  the {@link java.util.Map.Entry} value itself for processing.
 * <p>
 * Note: {@link com.oracle.coherence.common.events.processing.LifecycleAwareEntry}s are {@link EventProcessor}s,
 * meaning you can simply make {@link java.util.Map.Entry} values implement
 * {@link com.oracle.coherence.common.events.processing.LifecycleAwareEntry} to support "self" processing of
 * {@link EntryEvent}s.
 * <p>
 * Alternatively if a {@link java.util.Map.Entry} value implements the {@link EventProcessorFactory} interface,
 * then the {@link Event}s are dispatched to the {@link EventProcessor}s provided by the said
 * {@link EventProcessorFactory} implementation on the {@link java.util.Map.Entry}.
 * <p>
 * Lastly, if an {@link java.util.Map.Entry} value is annotated with {@link SupportsEventProcessing} or {@link LiveObject}
 * then  the {@link Event}s are dispatched to the appropriate method annotated with {@link EventProcessorFor} on the said
 * {@link java.util.Map.Entry} value class.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class DelegatingBackingMapListener implements MapListener
{
    /**
     * The logger to use.
     */
    private static final Logger logger = Logger.getLogger(DelegatingBackingMapListener.class.getName());

    /**
     * The {@link BackingMapManagerContext} that owns this listener. (all Backing {@link MapListener}s require a
     * {@link BackingMapManagerContext}).
     */
    private BackingMapManagerContext m_ctxBackingMapManager;

    /**
     * The name of the {@link com.tangosol.net.NamedCache} on which this {@link DelegatingBackingMapListener} is registered for
     * server-side processing of {@link Event}s.
     */
    private String m_sCacheName;

    /**
     * The {@link EventProcessor} to be used for the {@link DelegatingBackingMapListener}.  If this is specified
     * it overrides all other {@link EventProcessor}s.
     */
    private volatile EventProcessor<Event> m_eventProcessor;

    /**
     * The cached {@link EventDispatcher} for delegating {@link Event}s.
     */
    private volatile EventDispatcher m_eventDispatcher;

    /**
     * The cached {@link Environment}.
     */
    private Environment m_environment;

    /**
     * A {@link ConcurrentMap} of {@link Class}es and their associated event handler methods.
     */
    private ConcurrentHashMap<Class<?>, LinkedHashMap<Class<? extends Event>, EventProcessor<? extends Event>>> m_mapEventProcessingMethodsByClass;


    /**
     * Construct a {@link DelegatingBackingMapListener}.
     * <p>
     * NOTE: The {@link BackingMapManagerContext} will be injected by Coherence during initialization and
     * construction of the {@link com.tangosol.net.BackingMapManager}.
     *
     * @param backingMapManagerContext The {@link BackingMapManagerContext} associated with this listener
     * @param cacheName                The cache name associated with this listener
     */
    public DelegatingBackingMapListener(BackingMapManagerContext backingMapManagerContext,
                                        String                   cacheName)
    {
        this(backingMapManagerContext, cacheName, ((Environment) CacheFactory.getConfigurableCacheFactory()));
    }


    /**
     * Construct a {@link DelegatingBackingMapListener}.
     * <p>
     * NOTE: The {@link BackingMapManagerContext} will be injected by Coherence during initialization and
     * construction of the {@link com.tangosol.net.BackingMapManager}.
     *
     * @param backingMapManagerContext The {@link BackingMapManagerContext} associated with this listener
     * @param cacheName                The cache name associated with this listener
     * @param environment              The {@link Environment} to use
     */
    @SuppressWarnings("unchecked")
    public DelegatingBackingMapListener(BackingMapManagerContext backingMapManagerContext,
                                        String                   cacheName,
                                        Environment              environment)
    {
        m_ctxBackingMapManager = backingMapManagerContext;
        m_sCacheName           = cacheName;
        m_environment          = environment;
        m_mapEventProcessingMethodsByClass = new ConcurrentHashMap<Class<?>,
                                                                   LinkedHashMap<Class<? extends Event>,
                                                                                 EventProcessor<? extends Event>>>();

        // grab the CacheMapping for this cache - it has decorations containing the EventProcessor
        CacheMapping cacheMapping = environment.getResource(CacheMappingRegistry.class).findCacheMapping(cacheName);
        ParameterizedBuilder<?> builder = cacheMapping.getEnrichment(ParameterizedBuilder.class, "event-processor");

        if (builder != null)
        {
            m_eventProcessor = (EventProcessor<Event>) builder.realize(SystemPropertyParameterProvider.INSTANCE);
        }
    }


    /**
     * Obtains the name of the {@link com.tangosol.net.NamedCache} on which the {@link DelegatingBackingMapListener} is
     * registered.
     *
     * @return the name of the {@link com.tangosol.net.NamedCache} on which the {@link DelegatingBackingMapListener} is
     * registered
     */
    public String getCacheName()
    {
        return m_sCacheName;
    }


    /**
     * Obtains the {@link BackingMapManagerContext} in which the Backing {@link MapListener} is operating.
     *
     * @return {@link BackingMapManagerContext}
     */
    public BackingMapManagerContext getContext()
    {
        return m_ctxBackingMapManager;
    }


    /**
     * Obtains the {@link EventDispatcher} that should be used to dispatch {@link Event}s.
     *
     * @return the {@link EventDispatcher} that should be used to dispatch {@link Event}s
     */
    public EventDispatcher getEventDispatcher()
    {
        if (m_eventDispatcher == null)
        {
            synchronized (this)
            {
                // Double checked locking (works in JDK 1.5+ and using volatile for the reference)
                if (m_eventDispatcher == null)
                {
                    m_eventDispatcher = m_environment.getResource(EventDispatcher.class);

                    if (m_eventDispatcher == null)
                    {
                        throw new RuntimeException("Failed to locate the EventDispatcher resource.  Your application appears to be "
                                                   + "incorrectly configured or your Environment does not support EventDispatching");
                    }
                    else
                    {
                        return m_eventDispatcher;
                    }
                }
                else
                {
                    return m_eventDispatcher;
                }
            }
        }
        else
        {
            return m_eventDispatcher;
        }
    }


    /**
     * Determines whether the given decoration has been removed from the event's new value, i.e., the decoration
     * exists on the old value but not on the new.
     *
     * @param evt           The event to check
     * @param nDecorationId The decoration to look for
     *
     * @return true if the decoration has been removed from the event's new value
     */
    protected boolean isDecorationRemoved(MapEvent evt,
                                          int      nDecorationId)
    {
        Binary                   binOldValue = (Binary) evt.getOldValue();
        Binary                   binNewValue = (Binary) evt.getNewValue();
        BackingMapManagerContext ctx         = getContext();

        return (binOldValue != null && ctx.isInternalValueDecorated(binOldValue,
                                                                    nDecorationId) &&!ctx
                                                                        .isInternalValueDecorated(binNewValue,
                                                                                                  nDecorationId));
    }


    /**
     * {@inheritDoc}
     */
    public void entryInserted(MapEvent mapEvent)
    {
        EntryEvent<?> event;

        if (getContext().isKeyOwned(mapEvent.getKey()))
        {
            // Coherence marks an Entry that needs to be persisted with the DECO_STORE flag. Once finished it's removed.
            // We use this to determine if it's an EntryStoredEvent or not
            if (isDecorationRemoved(mapEvent, ExternalizableHelper.DECO_STORE))
            {
                event = new BackingMapEntryStoredEvent(getContext(),
                                                       getCacheName(),
                                                       mapEvent.getKey(),
                                                       mapEvent.getNewValue());
            }
            else
            {
                event = new BackingMapEntryInsertedEvent(getContext(),
                                                         getCacheName(),
                                                         mapEvent.getKey(),
                                                         mapEvent.getNewValue());
            }
        }
        else
        {
            event = new BackingMapEntryArrivedEvent(getContext(),
                                                    getCacheName(),
                                                    mapEvent.getKey(),
                                                    mapEvent.getNewValue());
        }

        scheduleProcessor(event);
    }


    /**
     * {@inheritDoc}
     */
    public void entryUpdated(MapEvent mapEvent)
    {
        EntryEvent<?> event = new BackingMapEntryUpdatedEvent(getContext(),
                                                              getCacheName(),
                                                              mapEvent.getKey(),
                                                              mapEvent.getOldValue(),
                                                              mapEvent.getNewValue());

        scheduleProcessor(event);
    }


    /**
     * {@inheritDoc}
     */
    public void entryDeleted(MapEvent mapEvent)
    {
        EntryEvent<?> event;

        if (m_ctxBackingMapManager.isKeyOwned(mapEvent.getKey()))
        {
            if (mapEvent instanceof CacheEvent && ((CacheEvent) mapEvent).isSynthetic())
            {
                event = new BackingMapEntryEvictedEvent(getContext(),
                                                        getCacheName(),
                                                        mapEvent.getKey(),
                                                        mapEvent.getOldValue());
            }
            else
            {
                event = new BackingMapEntryRemovedEvent(getContext(),
                                                        getCacheName(),
                                                        mapEvent.getKey(),
                                                        mapEvent.getOldValue());
            }
        }
        else
        {
            event = new BackingMapEntryDepartedEvent(getContext(),
                                                     getCacheName(),
                                                     mapEvent.getKey(),
                                                     mapEvent.getOldValue());
        }

        scheduleProcessor(event);
    }


    /**
     * Determines the {@link EventProcessor}s (methods) defined for each {@link Event} type on the specified class.
     *
     * @param clzEventProcessing  the {@link Class} that performs event processing
     *
     * @return a map of {@link EventProcessor} (methods) by {@link Event} type
     */
    private LinkedHashMap<Class<? extends Event>,
                          EventProcessor<? extends Event>> getEventProcessingMethods(Class<?> clzEventProcessing)
    {
        LinkedHashMap<Class<? extends Event>, EventProcessor<? extends Event>> mapEventProcessingMethods =
            m_mapEventProcessingMethodsByClass.get(clzEventProcessing);

        if (mapEventProcessingMethods == null)
        {
            synchronized (clzEventProcessing)
            {
                // check again (double checked locking)
                mapEventProcessingMethods = m_mapEventProcessingMethodsByClass.get(clzEventProcessing);

                if (mapEventProcessingMethods == null)
                {
                    // search for the annotated class (including super classes)
                    Class<?> clzAnnotated = clzEventProcessing;

                    while (clzAnnotated != null && clzAnnotated.getAnnotation(SupportsEventProcessing.class) == null
                           && clzAnnotated.getAnnotation(LiveObject.class) == null)
                    {
                        clzAnnotated = clzAnnotated.getSuperclass();
                    }

                    // search for the annotated methods on the class
                    mapEventProcessingMethods = new LinkedHashMap<Class<? extends Event>,
                                                                  EventProcessor<? extends Event>>();

                    if (clzAnnotated != null)
                    {
                        // search all classes including super classes for annotate methods
                        Class<?> clz = clzEventProcessing;

                        // (we don't need to search the Object class)
                        while (clz != Object.class && clz != null)
                        {
                            // reflect and create method-based event processors for each of the declared methods
                            // on the class that have been annotated as event processors
                            for (Method method : clz.getDeclaredMethods())
                            {
                                if (method.getParameterTypes().length == 2)
                                {
                                    EventProcessorFor eventProcessorFor = method.getAnnotation(EventProcessorFor.class);

                                    if (eventProcessorFor != null)
                                    {
                                        for (Class<? extends Event> clzEvent : eventProcessorFor.events())
                                        {
                                            if (!mapEventProcessingMethods.containsKey(clzEvent))
                                            {
                                                mapEventProcessingMethods.put(clzEvent,
                                                                              new MethodBasedEventProcessor(method));
                                            }
                                        }
                                    }
                                }
                            }

                            clz = clz.getSuperclass();
                        }
                    }

                    // remember the event processing methods for the class
                    m_mapEventProcessingMethodsByClass.put(clzEventProcessing, mapEventProcessingMethods);
                }
            }
        }

        return mapEventProcessingMethods;
    }


    /**
     * Schedule the processing of an event.
     *
     * @param event The event to schedule for processing
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void scheduleProcessor(Event event)
    {
        if (event instanceof EntryEvent)
        {
            EntryEvent entryEvent = (EntryEvent) event;

            // determine the event processor for processing the entry event
            EventProcessor processor;

            if (m_eventProcessor == null)
            {
                // as there is no event processor defined for the delegating backing map listener,
                // determine the processor based on the entry value
                Object oValue = entryEvent.getEntry().getValue();

                if (oValue == null)
                {
                    processor = null;
                }
                else if (oValue instanceof EventProcessor)
                {
                    // the value itself is an event processor
                    processor = (EventProcessor) oValue;
                }
                else if (oValue instanceof EventProcessorFactory)
                {
                    // the value is an event processor factory, so ask the factory for an event processor
                    processor = ((EventProcessorFactory) oValue).getEventProcessor(entryEvent);
                }
                else
                {
                    // determine the event processor based on annotated object and methods
                    Class<?> clzValue = oValue.getClass();

                    LinkedHashMap<Class<? extends Event>, EventProcessor<? extends Event>> mapEventProcessingMethods =
                        getEventProcessingMethods(clzValue);

                    if (mapEventProcessingMethods.isEmpty())
                    {
                        processor = null;
                    }
                    else
                    {
                        // find a compatible processor for the event class
                        processor = null;

                        for (Class<? extends Event> clzEvent : mapEventProcessingMethods.keySet())
                        {
                            if (clzEvent.isInstance(entryEvent))
                            {
                                processor = mapEventProcessingMethods.get(clzEvent);
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                // use the overriding event processor defined for the cache
                processor = m_eventProcessor;
            }

            // dispatch the event
            if (processor == null)
            {
                getEventDispatcher().dispatchEvent(entryEvent);
            }
            else
            {
                getEventDispatcher().dispatchEvent(entryEvent, processor);
            }
        }
        else
        {
            // SKIP: we don't support dispatching non-EntryEvents here ie: PartitionEvents.
        }
    }


    /**
     * A {@link MethodBasedEventProcessor} is useful for processing {@link Event}s using a specific {@link Method}.
     */
    private static class MethodBasedEventProcessor implements EventProcessor<EntryEvent<?>>
    {
        /**
         * The {@link Method} that can process the {@link Event}.
         */
        private Method method;


        /**
         * Constructor for a {@link MethodBasedEventProcessor}.
         *
         * @param method the {@link Method} to execute
         */
        public MethodBasedEventProcessor(Method method)
        {
            this.method = method;
        }


        /**
         * {@inheritDoc}
         */
        public void process(EventDispatcher eventDispatcher,
                            EntryEvent<?>   event)
        {
            try
            {
                method.invoke(event.getEntry().getValue(), eventDispatcher, event);
            }
            catch (Exception exception)
            {
                logger.log(Level.SEVERE, "Exception in process method ", exception);

                throw Base.ensureRuntimeException(exception);
            }
        }
    }
}
