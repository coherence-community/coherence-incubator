/*
 * File: AbstractMultiplexingBackingMapListener.java
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

package com.oracle.coherence.common.backingmaplisteners;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.ObservableMap;

/**
 * The {@link AbstractMultiplexingBackingMapListener} provides an extensible implementation
 * of the {@link MultiplexingBackingMapListener}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Andy Nguyen
 */
public abstract class AbstractMultiplexingBackingMapListener extends MultiplexingMapListener
    implements MultiplexingBackingMapListener
{
    /**
     * The {@link BackingMapManagerContext} that owns this listener.
     * (all Backing {@link MapListener}s require a {@link BackingMapManagerContext})
     */
    private BackingMapManagerContext context;


    /**
     * Standard Constructor.
     * <p>
     * The {@link BackingMapManagerContext} will be injected by Coherence during
     * initialization and construction of the {@link com.tangosol.net.BackingMapManager}.
     *
     * @param context The BackingMapManagerContext associated with this listener
     */
    public AbstractMultiplexingBackingMapListener(BackingMapManagerContext context)
    {
        this.context = context;
    }


    /**
     * The {@link BackingMapManagerContext} in which the Backing {@link com.tangosol.util.MapListener}
     * is operating.
     *
     * @return {@link BackingMapManagerContext}
     */
    public BackingMapManagerContext getContext()
    {
        return context;
    }


    /**
     * Determines whether the given decoration has been removed from the event's new
     * value, i.e., the decoration exists on the old value but not on the new.
     *
     * @param evt           The event to check
     * @param nDecorationId The decoration to look for.
     *
     * @return true if the decoration has been removed for the new value
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
     * This is the standard {@link MultiplexingMapListener} event handler.  In here
     * we convert the internally formatted event into something a developer would
     * expect if using a client-side {@link com.tangosol.util.MapListener}.
     * <p>
     * After converting the internally formatted event, this method calls the abstract
     * {@link #onBackingMapEvent(MapEvent, Cause)}
     * method that may be used to handle the actual event.
     *
     * @param mapEvent the MapEvent that has occurred
     */
    protected final void onMapEvent(MapEvent mapEvent)
    {
        // determine the underlying cause of the map event
        Cause cause;

        if (context.isKeyOwned(mapEvent.getKey()))
        {
            if (isDecorationRemoved(mapEvent, ExternalizableHelper.DECO_STORE))
            {
                cause = Cause.StoreCompleted;
            }
            else
            {
                cause = mapEvent instanceof CacheEvent && ((CacheEvent) mapEvent).isSynthetic()
                        ? Cause.Eviction : Cause.Regular;
            }
        }
        else
        {
            cause = Cause.PartitionManagement;
        }

        // now call the abstract event handler with the event and underlying cause
        onBackingMapEvent(new LazyMapEvent(context,
                                           mapEvent.getMap(),
                                           mapEvent.getId(),
                                           mapEvent.getKey(),
                                           mapEvent.getOldValue(),
                                           mapEvent.getNewValue()), cause);
    }


    /**
     * An extension to the standard {@link MapEvent} that will lazily
     * deserialize attributes (if necessary) when first accessed.
     */
    @SuppressWarnings("serial")
    private static class LazyMapEvent extends MapEvent
    {
        /**
         * The {@link BackingMapManagerContext} that should be used
         * to lazily deserialize the event attributes.
         */
        private BackingMapManagerContext backingMapManagerContext;


        /**
         * Standard Constructor.
         *
         * @param backingMapManagerContext The BackingMapManagerContext associated with this event
         * @param observableMap            The underlying map.
         * @param id                       The event id
         * @param key                      The key that this event was triggered for
         * @param oldValue                 The old value
         * @param newValue                 The new value
         */
        LazyMapEvent(BackingMapManagerContext backingMapManagerContext,
                     ObservableMap            observableMap,
                     int                      id,
                     Object                   key,
                     Object                   oldValue,
                     Object                   newValue)
        {
            super(observableMap, id, key, oldValue, newValue);
            this.backingMapManagerContext = backingMapManagerContext;
        }


        /**
         * {@inheritDoc}
         */
        public Object getKey()
        {
            if (m_oKey instanceof Binary)
            {
                m_oKey = backingMapManagerContext.getKeyFromInternalConverter().convert(m_oKey);
            }

            return m_oKey;
        }


        /**
         * {@inheritDoc}
         */
        public Object getOldValue()
        {
            if (m_oValueOld instanceof Binary)
            {
                m_oValueOld = backingMapManagerContext.getValueFromInternalConverter().convert(m_oValueOld);
            }

            return m_oValueOld;
        }


        /**
         * {@inheritDoc}
         */
        public Object getNewValue()
        {
            if (m_oValueNew instanceof Binary)
            {
                m_oValueNew = backingMapManagerContext.getValueFromInternalConverter().convert(m_oValueNew);
            }

            return m_oValueNew;
        }


        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return String.format("LazyEventMap{%s}", super.toString());
        }
    }
}
