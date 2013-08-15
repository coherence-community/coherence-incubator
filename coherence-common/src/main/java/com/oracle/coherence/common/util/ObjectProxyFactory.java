/*
 * File: ObjectProxyFactory.java
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

package com.oracle.coherence.common.util;

import com.oracle.coherence.common.processors.CreateRemoteObjectProcessor;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generic class {@link ObjectProxyFactory} is a factory to manage
 * proxies to objects residing in a {@link NamedCache}.</p>
 * <p>
 * Besides retrieving proxies, it can also create objects and destroy objects
 * in the remote {@link NamedCache}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 *
 * @param <InterfaceType> the type of the Interface that the proxies implements
 */
public class ObjectProxyFactory<InterfaceType>

{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(ObjectProxyFactory.class.getName());

    /**
     * The {@link NamedCache} where the objects reside.
     */
    private NamedCache mCache;

    /**
     * The interface class that the proxies (and the remote objects) implement.
     */
    private Class<InterfaceType> mClass;

    /**
     * The name of the named cache where the Objects live.
     */
    private String cacheName;


    /**
     * Standard constructor.
     *
     * @param cacheName the {@link NamedCache}
     * @param clazz the {@link Class} for the interface that the proxy implements.
     */
    public ObjectProxyFactory(String cacheName,
                              Class<InterfaceType> clazz)
    {
        this.cacheName = cacheName;
        this.mClass    = clazz;
        this.mCache    = null;

    }


    /**
     * Retrieves the proxy for the key. Note that this doesn't incur any remote operation,
     * thus the object may or may not exist in the remote end.
     *
     * @param key the key to the object
     *
     * @return the proxy object
     */
    @SuppressWarnings("unchecked")
    public InterfaceType getProxy(Object key)
    {
        if (key != null)
        {
            NamedCache               cache   = ensureCache();

            NamedCacheObjectProxy<?> handler = new NamedCacheObjectProxy<Object>(key, cache);
            InterfaceType            proxy   = (InterfaceType) Proxy.newProxyInstance(mClass.getClassLoader(),
                                                                                      new Class[] {mClass},
                                                                                      handler);

            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST,
                           "Creating a proxy object for the key {0} for cache {1}.",
                           new Object[] {key, cache.getCacheName()});
            }

            return proxy;
        }
        else
        {
            throw new NullPointerException("Can not create an ObjectProxy for a null key");

        }
    }


    /**
     * Creates a remote object if it doesn't exist already.
     * The parameters will be used to match against existing constructors in the class.
     *
     * @param key        the key to the new object
     * @param objectType the class type of the new object
     * @param parameters the parameters to the constructor
     *
     * @return a proxy to the remote object
     *
     * @throws Throwable throws
     */
    public InterfaceType createRemoteObjectIfNotExists(Object key,
                                                       Class<?> objectType,
                                                       Object[] parameters) throws Throwable
    {
        NamedCache cache  = ensureCache();
        Object     result = cache.invoke(key, new CreateRemoteObjectProcessor(objectType.getName(), parameters));

        if (result instanceof Throwable)
        {
            throw(Throwable) result;
        }

        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST,
                       "Creating a Remote object of class {0} for the key {1} for cache {2}.",
                       new Object[] {objectType.getName(), key, cache.getCacheName()});
        }

        return getProxy(key);
    }


    /**
     * Destroys the remote object.
     *
     * @param key the key to the object to destroy
     *
     * @throws Throwable throws a {@link Throwable} if it fails
     */
    public void destroyRemoteObject(Object key) throws Throwable
    {
        ensureCache().remove(key);
    }


    /**
     * Returns a JVM local copy of the remote object (i.e. not a proxy).
     *
     * @param key the key to the object
     *
     * @return the local copy of the object
     */
    @SuppressWarnings("unchecked")
    public InterfaceType getLocalCopyOfRemoteObject(Object key)
    {
        return (InterfaceType) ensureCache().get(key);
    }


    /**
     * Register a {@link ObjectChangeCallback} for a proxy object. The callback will be called
     * as changes to the underlying object is detected.
     *
     * @param proxyObject the proxy object for which we will register the callback
     * @param callback the {@link ObjectChangeCallback} to register
     */
    @SuppressWarnings("unchecked")
    public void registerChangeCallback(InterfaceType proxyObject,
                                       ObjectChangeCallback<InterfaceType> callback)
    {
        if (!isProxy(proxyObject))
        {
            throw new RuntimeException("The object is not a proxy object");
        }

        InvocationHandler invhandler = Proxy.getInvocationHandler(proxyObject);

        if (invhandler instanceof NamedCacheObjectProxy<?>)
        {
            ((NamedCacheObjectProxy<InterfaceType>) invhandler).registerChangeCallback(callback);
        }
    }


    /**
     * Unregister a {@link ObjectChangeCallback} for a proxy object.
     *
     * @param proxyObject the proxy object for which we will unregister the callback
     * @param callback the {@link ObjectChangeCallback} to unregister
     */

    @SuppressWarnings("unchecked")
    public void unregisterChangeCallback(InterfaceType proxyObject,
                                         ObjectChangeCallback<InterfaceType> callback)
    {
        if (!isProxy(proxyObject))
        {
            throw new RuntimeException("The object is not a proxy object");
        }

        InvocationHandler invhandler = Proxy.getInvocationHandler(proxyObject);

        if (invhandler instanceof NamedCacheObjectProxy<?>)
        {
            ((NamedCacheObjectProxy<InterfaceType>) invhandler).unregisterChangeCallback(callback);
        }
    }


    /**
     * Test an object whether it is a proxy object.
     *
     * @param object the object to test
     *
     * @return true if it is a proxy, false otherwise
     */
    public boolean isProxy(InterfaceType object)
    {
        if (object instanceof java.lang.reflect.Proxy)
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Returns the {@link NamedCache} that this interface represents.
     *
     * @return the {@link NamedCache}
     */
    public NamedCache getNamedCache()
    {
        return ensureCache();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "ObjectProxyFactory for class:" + mClass.getName() + " stored in cache:" + cacheName;
    }


    /**
     * Ensures that the {@link NamedCache} reference is valid.
     *
     * @return a valid {@link NamedCache} reference
     */
    private NamedCache ensureCache()
    {
        if (mCache == null)
        {
            mCache =
                CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(this.getClass().getClassLoader())
                    .ensureCache(cacheName, this.getClass().getClassLoader());
        }

        return mCache;
    }
}
