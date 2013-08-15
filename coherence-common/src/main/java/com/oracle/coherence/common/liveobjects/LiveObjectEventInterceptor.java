/*
 * File: LiveObjectEventInterceptor.java
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

package com.oracle.coherence.common.liveobjects;

import com.oracle.coherence.common.tuples.Pair;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link LiveObjectEventInterceptor} is the {@link EventInterceptor}
 * that forwards {@link Event}s to objects annotated with {@link LiveObject}.
 * <p>
 * This implementation is inspired by and based upon the concepts
 * provided by the DelegatingBackingMapListener implementation of Coherence
 * Incubator releases 8, 9, 10 and 11.  It has been refactored here to adopt
 * the new Unified (Server Side) Event Model introduced in Coherence 12.1.2.
 * <p>
 * Copyright (c) 2012. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Interceptor(
    identifier  = "live-objects-interceptor",
    entryEvents =
    {
        EntryEvent.Type.INSERTING, EntryEvent.Type.INSERTED, EntryEvent.Type.REMOVING, EntryEvent.Type.REMOVED,
        EntryEvent.Type.UPDATING, EntryEvent.Type.UPDATED
    },
    transferEvents = {TransferEvent.Type.ARRIVED, TransferEvent.Type.DEPARTING}
)
public class LiveObjectEventInterceptor implements EventInterceptor
{
    /**
     * A map providing the ability to look up an {@link Event} interface class
     * together with an associated event type that a given annotated
     * interceptor method will process.
     */
    @SuppressWarnings("serial")
    private static final HashMap<Class<? extends Annotation>, Pair<Class<? extends Event>, Enum<?>>> s_mapEventTypeByAnnotation =
        new HashMap<Class<? extends Annotation>, Pair<Class<? extends Event>, Enum<?>>>()
    {
        {
            put(OnInserting.class,
                new Pair<Class<? extends Event>, Enum<?>>(EntryEvent.class, EntryEvent.Type.INSERTING));
            put(OnInserted.class,
                new Pair<Class<? extends Event>, Enum<?>>(EntryEvent.class, EntryEvent.Type.INSERTED));
            put(OnUpdating.class,
                new Pair<Class<? extends Event>, Enum<?>>(EntryEvent.class, EntryEvent.Type.UPDATING));
            put(OnUpdated.class, new Pair<Class<? extends Event>, Enum<?>>(EntryEvent.class, EntryEvent.Type.UPDATED));
            put(OnRemoving.class,
                new Pair<Class<? extends Event>, Enum<?>>(EntryEvent.class, EntryEvent.Type.REMOVING));
            put(OnRemoved.class, new Pair<Class<? extends Event>, Enum<?>>(EntryEvent.class, EntryEvent.Type.REMOVED));
            put(OnArrived.class,
                new Pair<Class<? extends Event>, Enum<?>>(TransferEvent.class, TransferEvent.Type.ARRIVED));
            put(OnDeparting.class,
                new Pair<Class<? extends Event>, Enum<?>>(TransferEvent.class, TransferEvent.Type.DEPARTING));
        }
    };

    /**
     * The name of the Cache to which the EventInterceptor has been associated
     */
    private String m_cacheName;

    /**
     * A {@link ConcurrentMap} of {@link Class}es mapped to the Events they handle
     * mapping each Event Type to an appropriate {@link Method} annotated with
     * @EventProcessorFor, defined with the same signature as {@link EventInterceptor#onEvent(Event)}.
     */
    private ConcurrentHashMap<Class<?>, LinkedHashMap<Class<? extends Event>, LinkedHashMap<Enum<?>, Method>>> m_mapInterceptorMethodsByClass;

    /**
     * A mapping from concrete {@link Event} classes to the {@link Event} interfaces they implement.
     */
    private ConcurrentHashMap<Class<? extends Event>, Class<? extends Event>> m_mapEventIterfacesByEventClass;


    /**
     * Constructs a {@link LiveObjectEventInterceptor}.
     */
    public LiveObjectEventInterceptor()
    {
        m_mapInterceptorMethodsByClass = new ConcurrentHashMap<Class<?>,
                                                               LinkedHashMap<Class<? extends Event>,
                                                                             LinkedHashMap<Enum<?>, Method>>>();

        m_mapEventIterfacesByEventClass = new ConcurrentHashMap<Class<? extends Event>, Class<? extends Event>>();

        m_cacheName                     = null;
    }


    /**
     * Sets the cache name on which this interceptor has been registered.
     *
     * @param cacheName
     */
    @Injectable("cache-name")
    public void setCacheName(String cacheName)
    {
        m_cacheName = cacheName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(Event event)
    {
        @SuppressWarnings("unchecked") Set<BinaryEntry> setBinaryEntries = Collections.EMPTY_SET;

        if (event instanceof EntryEvent)
        {
            setBinaryEntries = ((EntryEvent) event).getEntrySet();

        }
        else if (event instanceof TransferEvent)
        {
            setBinaryEntries = ((TransferEvent) event).getEntries().get(m_cacheName);
        }

        if (setBinaryEntries != null)
        {
            for (BinaryEntry binaryEntry : setBinaryEntries)
            {
                Object oValue = binaryEntry.getValue();

                // should the value be null (because of a remove), try
                // to use the original value
                if (oValue == null)
                {
                    oValue = binaryEntry.getOriginalValue();
                }

                if (oValue == null)
                {
                    // SKIP: there's nothing we can do... the LiveObject is null!
                }
                else
                {
                    // we first notify LiveObjects implementing EventInterceptor of the Event
                    if (oValue instanceof EventInterceptor)
                    {
                        // when the value is an EventInterceptor, simply pass
                        // the event onto the onEvent method
                        EventInterceptor interceptor = (EventInterceptor) oValue;

                        // raise the event
                        interceptor.onEvent(event);
                    }

                    // we then call the appropriately annotated method.
                    Method method = getEventTypeInteceptorMethod(oValue.getClass(), event);

                    if (method == null)
                    {
                        // when there's no annotated method, we do nothing
                    }
                    else
                    {
                        try
                        {
                            method.invoke(oValue, binaryEntry);
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException("Failed to process event [" + event + "] for LiveObject [" + oValue
                                                       + "]",
                                                       e);
                        }
                    }
                }
            }
        }
    }


    /**
     * Determines the interceptor {@link Method} to use on a {@link LiveObject}
     * class for a specified {@link Event}.
     *
     * @param clzLiveObject  the {@link Class} that we think is annotated as a {@link LiveObject}
     * @param event          the {@link Event} for which we're looking to find an interceptor {@link Method}
     *
     * @return the {@link Method} on the {@link LiveObject} that can be used
     *         to intercept the {@link Event} or <code>null</code> if no
     *         such interceptor can be found.
     */
    private Method getEventTypeInteceptorMethod(Class<?> clzLiveObject,
                                                Event    event)
    {
        // attempt to locate the interceptor methods defined on the live object class
        LinkedHashMap<Class<? extends Event>, LinkedHashMap<Enum<?>, Method>> mapInterceptorMethodsByEvent =
            m_mapInterceptorMethodsByClass.get(clzLiveObject);

        if (mapInterceptorMethodsByEvent == null)
        {
            // as there are no methods defined, reflect out all of the possible
            // interceptor methods to build a table for the class
            synchronized (clzLiveObject)
            {
                // check again (double checked locking)
                mapInterceptorMethodsByEvent = m_mapInterceptorMethodsByClass.get(clzLiveObject);

                if (mapInterceptorMethodsByEvent == null)
                {
                    // search for the @LiveObject annotated class (including super classes)
                    Class<?> clzAnnotated = clzLiveObject;

                    while (clzAnnotated != null && clzAnnotated.getAnnotation(LiveObject.class) == null)
                    {
                        clzAnnotated = clzAnnotated.getSuperclass();
                    }

                    // create the interceptor methods map for the new live object
                    mapInterceptorMethodsByEvent = new LinkedHashMap<Class<? extends Event>,
                                                                     LinkedHashMap<Enum<?>, Method>>();

                    if (clzAnnotated == null)
                    {
                        // unfortunately the class isn't a liveobject so there are no interceptors available
                        // so we simply register that there are no interceptors for the class
                        m_mapInterceptorMethodsByClass.put(clzLiveObject, mapInterceptorMethodsByEvent);
                    }
                    else
                    {
                        // as the class is a liveobject, we must reflect and resolve
                        // all of the interceptor methods

                        // search for the well-known interceptor @On* annotated methods (including super classes)
                        clzAnnotated = clzLiveObject;

                        // (we don't need to search the Object class)
                        while (clzAnnotated != Object.class && clzAnnotated != null)
                        {
                            // examine each declared method and determine if it
                            // is an event interceptor
                            for (Method method : clzAnnotated.getDeclaredMethods())
                            {
                                for (Class<? extends Annotation> clzAnnotation : s_mapEventTypeByAnnotation.keySet())
                                {
                                    // should the method be annotated with the annotation
                                    // add the method to the map of interceptors for the event and type.
                                    Annotation annotation = method.getAnnotation(clzAnnotation);

                                    if (annotation != null)
                                    {
                                        // grab the event class and type for the method
                                        Pair<Class<? extends Event>, Enum<?>> pair =
                                            s_mapEventTypeByAnnotation.get(clzAnnotation);

                                        if (method.getParameterTypes().length == 1)
                                        {
                                            // TODO: we should ensure that the first parameter
                                            // of the method is a BinaryEntry

                                            // register the method as being an interceptor for annotated event class and type
                                            LinkedHashMap<Enum<?>, Method> mapInterceptorMethodsByEventType =
                                                mapInterceptorMethodsByEvent.get(pair.getX());

                                            if (mapInterceptorMethodsByEventType == null)
                                            {
                                                mapInterceptorMethodsByEventType = new LinkedHashMap<Enum<?>, Method>();
                                                mapInterceptorMethodsByEvent.put(pair.getX(),
                                                                                 mapInterceptorMethodsByEventType);
                                            }

                                            // SAFETY: we should raise advice here if the event type already has an interceptor method

                                            mapInterceptorMethodsByEventType.put(pair.getY(), method);
                                        }
                                        else
                                        {
                                            // TODO: raise advice that the annotation method
                                            // has too few or too many parameters
                                        }
                                    }
                                }
                            }

                            // next parent class
                            clzAnnotated = clzAnnotated.getSuperclass();
                        }

                        // remember the interceptors found for the class
                        m_mapInterceptorMethodsByClass.put(clzLiveObject, mapInterceptorMethodsByEvent);
                    }
                }
            }
        }

        // attempt to locate the event interface that the provided event implements
        Class<? extends Event> clzEventInterface = m_mapEventIterfacesByEventClass.get(event.getClass());

        if (clzEventInterface == null)
        {
            // search our interceptor methods by event class to determine which
            // is implemented by the provided event
            for (Class<? extends Event> clzEvent : mapInterceptorMethodsByEvent.keySet())
            {
                if (clzEvent.isAssignableFrom(event.getClass()))
                {
                    clzEventInterface = clzEvent;
                    m_mapEventIterfacesByEventClass.put(event.getClass(), clzEventInterface);
                    break;
                }
            }
        }

        if (clzEventInterface == null)
        {
            // there's no event handler for the specified event
            return null;
        }
        else
        {
            // locate the interceptor method for the event
            LinkedHashMap<Enum<?>, Method> mapInterceptorMethodsByEventType =
                mapInterceptorMethodsByEvent.get(clzEventInterface);

            return mapInterceptorMethodsByEventType == null
                   ? null : mapInterceptorMethodsByEventType.get(event.getType());
        }
    }
}
