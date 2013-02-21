/*
 * File: NamedCacheObjectProxy.java
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

import com.oracle.coherence.common.processors.InvokeMethodProcessor;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link NamedCacheObjectProxy} implements the Java Dynamic Proxy
 * pattern for objects residing in a {@link NamedCache}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <InterfaceType> is the type of the interface the proxy implements
 *
 * @author Christer Fahlgren
 */
class NamedCacheObjectProxy<InterfaceType> implements InvocationHandler, MapListener
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(NamedCacheObjectProxy.class.getName());

    /**
     * The key for the object.
     */
    private Object key;

    /**
     * The {@link NamedCache} where the object resides.
     */
    private NamedCache mCache;

    /**
     * The list of registered callbacks.
     */
    private volatile CopyOnWriteArrayList<ObjectChangeCallback<InterfaceType>> callbacks;


    /**
     * Standard constructor.
     *
     * @param key   the key to the object.
     * @param cache the {@link NamedCache} where the object resides.
     */
    public NamedCacheObjectProxy(Object     key,
                                 NamedCache cache)
    {
        this.key    = key;
        this.mCache = cache;
    }


    /**
     * {@inheritDoc}
     */
    public Object invoke(Object   proxy,
                         Method   method,
                         Object[] args) throws Throwable
    {
        if (method.getName() != "toString")
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST,
                           "Calling method {0} for object with key {1} on cache {2}",
                           new Object[] {method.getName(), key, mCache.getCacheName()});
            }

            InvokeMethodProcessor processor = new InvokeMethodProcessor(method.getName(), args);
            Object                value     = mCache.invoke(key, processor);

            if (value instanceof Throwable)
            {
                throw(Throwable) value;
            }

            return value;
        }
        else
        {
            return (Object) toString();
        }
    }


    /**
     * Register an {@link ObjectChangeCallback} with this {@link NamedCacheObjectProxy}.
     *
     * @param callback the {@link ObjectChangeCallback} to register
     */
    public void registerChangeCallback(ObjectChangeCallback<InterfaceType> callback)
    {
        if (callbacks == null)
        {
            callbacks = new CopyOnWriteArrayList<ObjectChangeCallback<InterfaceType>>();
        }

        synchronized (callbacks)
        {
            if (callbacks.isEmpty())
            {
                mCache.addMapListener(this, key, false);
            }

            callbacks.add(callback);
        }
    }


    /**
     * UnRegister an {@link ObjectChangeCallback} with this {@link NamedCacheObjectProxy}.
     *
     * @param callback the {@link ObjectChangeCallback} to unregister
     */
    public void unregisterChangeCallback(ObjectChangeCallback<InterfaceType> callback)
    {
        if (callbacks == null)
        {
            callbacks = new CopyOnWriteArrayList<ObjectChangeCallback<InterfaceType>>();
        }

        synchronized (callbacks)
        {
            callbacks.remove(callback);

            if (callbacks.isEmpty())
            {
                mCache.removeMapListener(this);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "NamedCacheObjectProxy {" + key + "," + mCache.getCacheName() + "}";
    }


    /**
     * {@inheritDoc}
     */
    public void entryDeleted(MapEvent mapEvent)
    {
        if (callbacks != null)
        {
            for (Iterator<ObjectChangeCallback<InterfaceType>> iter = callbacks.iterator(); iter.hasNext(); )
            {
                iter.next().objectDeleted(mapEvent.getKey());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void entryInserted(MapEvent mapEvent)
    {
        if (callbacks != null)
        {
            for (Iterator<ObjectChangeCallback<InterfaceType>> iter = callbacks.iterator(); iter.hasNext(); )
            {
                iter.next().objectCreated((InterfaceType) mapEvent.getNewValue());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void entryUpdated(MapEvent mapEvent)
    {
        if (callbacks != null)
        {
            for (Iterator<ObjectChangeCallback<InterfaceType>> iter = callbacks.iterator(); iter.hasNext(); )
            {
                iter.next().objectChanged((InterfaceType) mapEvent.getNewValue());
            }
        }
    }
}
