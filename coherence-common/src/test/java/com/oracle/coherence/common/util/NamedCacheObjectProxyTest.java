/*
 * File: NamedCacheObjectProxyTest.java
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
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * A {@link NamedCacheObjectProxyTest} testing the dynamic proxy Invocation handler.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class NamedCacheObjectProxyTest extends AbstractTest
{
    /**
     * Tests invocation using the proxy.
     *
     * @throws Throwable if invocation fails.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testInvoke() throws Throwable
    {
        NamedCache    mockCache = mock(NamedCache.class);
        MockInterface obj       = mock(MockInterface.class);
        Method        meth      = MockInterface.class.getMethod("getMessage", (Class<?>[]) null);

        stub(mockCache.invoke(isA(String.class), isA(InvokeMethodProcessor.class))).toReturn("testmessage");

        NamedCacheObjectProxy proxy  = new NamedCacheObjectProxy("key", mockCache);
        Object                result = proxy.invoke(obj, meth, null);

        assertTrue(result.equals("testmessage"));
    }


    /**
     * Tests an entry deleted event and its propagation to a callback.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEntryDeleted()
    {
        ObjectChangeCallback mockcb = mock(ObjectChangeCallback.class);

        mockcb.objectDeleted(isA(String.class));

        NamedCache mockCache = mock(NamedCache.class);

        mockCache.addMapListener(isA(MapListener.class), eq("key"), eq(false));

        MapEvent evt = mock(MapEvent.class);

        stub(evt.getKey()).toReturn("key");

        NamedCacheObjectProxy proxy = new NamedCacheObjectProxy("key", mockCache);

        proxy.registerChangeCallback(mockcb);
        proxy.entryDeleted(evt);
        verify(mockcb).objectDeleted(eq("key"));
    }


    /**
     * Tests an entry inserted event and its propagation to a callback.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEntryInserted()
    {
        ObjectChangeCallback mockcb = mock(ObjectChangeCallback.class);

        mockcb.objectDeleted(isA(String.class));

        NamedCache mockCache = mock(NamedCache.class);

        mockCache.addMapListener(isA(MapListener.class), eq("key"), eq(false));

        MapEvent evt = mock(MapEvent.class);

        stub(evt.getKey()).toReturn("key");
        stub(evt.getNewValue()).toReturn("value");

        NamedCacheObjectProxy proxy = new NamedCacheObjectProxy("key", mockCache);

        proxy.registerChangeCallback(mockcb);
        proxy.entryInserted(evt);
        verify(mockcb).objectCreated(eq("value"));
    }


    /**
     * Tests an entry updated event and its propagation to a callback.
     *
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEntryUpdated()
    {
        ObjectChangeCallback mockcb = mock(ObjectChangeCallback.class);

        mockcb.objectDeleted(isA(String.class));

        NamedCache mockCache = mock(NamedCache.class);

        mockCache.addMapListener(isA(MapListener.class), eq("key"), eq(false));

        MapEvent evt = mock(MapEvent.class);

        stub(evt.getKey()).toReturn("key");
        stub(evt.getNewValue()).toReturn("value");

        NamedCacheObjectProxy proxy = new NamedCacheObjectProxy("key", mockCache);

        proxy.registerChangeCallback(mockcb);
        proxy.entryUpdated(evt);
        verify(mockcb).objectChanged(eq("value"));
    }
}
