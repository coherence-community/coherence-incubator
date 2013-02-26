/*
 * File: DefaultReflectedPofSerializer.java
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

package com.oracle.coherence.common.serialization;

import com.tangosol.io.pof.PofContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link DefaultReflectedPofSerializer} is the base implementation of a {@link ReflectedContext}.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 * @author Brian Oliver
 */
public class DefaultReflectedPofSerializer implements ReflectedContext
{
    /**
     * The {@link FieldSerializationProvider} that {@link DefaultReflectedSerializer}s may use to serialize {@link Field}s.
     */
    private FieldSerializationProvider serializationProvider;

    /**
     * The {@link DefaultReflectedSerializer}s arranged by User Type Id.
     */
    private Map<Integer, ReflectedSerializer> pofSerializersByUserTypeId;

    /**
     * The User Type Ids arranged by {@link Class}.
     */
    private Map<Class<?>, Integer> pofUserTypeIdsByClass;

    /**
     * The User Type Ids arranged by {@link Class} name.
     */
    private Map<String, Integer> pofUserTypeIdsByClassName;

    /**
     * The Type to the IReflectedSerializer.
     */
    private ConcurrentMap<Class<?>, ReflectedSerializer> typeSerializerMap;


    /**
     * Standard Constructor
     *
     * @param serializationProvider The {@link FieldSerializationProvider}
     */
    public DefaultReflectedPofSerializer(FieldSerializationProvider serializationProvider)
    {
        this.serializationProvider = serializationProvider;

        this.typeSerializerMap = new ConcurrentHashMap<Class<?>, ReflectedSerializer>(100,
                                                                                      0.75f,
                                                                                      Runtime.getRuntime()
                                                                                          .availableProcessors());

        this.pofSerializersByUserTypeId = new HashMap<Integer, ReflectedSerializer>();
        this.pofUserTypeIdsByClass      = new HashMap<Class<?>, Integer>();
        this.pofUserTypeIdsByClassName  = new HashMap<String, Integer>();
    }


    /**
     * {@inheritDoc}
     */
    public Class<?> getClass(int userTypeId)
    {
        ReflectedSerializer serializer = pofSerializersByUserTypeId.get(userTypeId);

        if (serializer != null)
        {
            return serializer.getType();
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unknown User Type Id: %d", userTypeId));
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getClassName(int userTypeId)
    {
        ReflectedSerializer serializer = pofSerializersByUserTypeId.get(userTypeId);

        if (serializer != null)
        {
            return serializer.getType().getName();
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unknown User Type Id: %d", userTypeId));
        }
    }


    /**
     * {@inheritDoc}
     */
    public ReflectedSerializer getPofSerializer(int userTypeId)
    {
        ReflectedSerializer serializer = pofSerializersByUserTypeId.get(userTypeId);

        if (serializer != null)
        {
            return serializer;
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unknown User Type Id: %d", userTypeId));
        }
    }


    /**
     * {@inheritDoc}
     */
    public int getUserTypeIdentifier(Class<?> type)
    {
        Integer id = pofUserTypeIdsByClass.get(type);

        if (id != null)
        {
            return id;
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unknown User Type: %s", type));
        }
    }


    /**
     * {@inheritDoc}
     */
    public int getUserTypeIdentifier(Object object)
    {
        return getUserTypeIdentifier(object.getClass());
    }


    /**
     * {@inheritDoc}
     */
    public int getUserTypeIdentifier(String className)
    {
        Integer id = pofUserTypeIdsByClassName.get(className);

        if (id != null)
        {
            return id;
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unknown User Type: %s", className));
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isUserType(Class<?> type)
    {
        return pofUserTypeIdsByClass.containsKey(type);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isUserType(Object object)
    {
        return isUserType(object.getClass());
    }


    /**
     * {@inheritDoc}
     */
    public boolean isUserType(String className)
    {
        return pofUserTypeIdsByClassName.containsKey(className);
    }


    /**
     * {@inheritDoc}
     */
    public ReflectedSerializer ensurePofSerializer(Class<?>   type,
                                                   PofContext pofContext)
    {
        ReflectedSerializer serializer = typeSerializerMap.get(type);

        if (serializer == null)
        {
            serializer = new DefaultReflectedSerializer(type, this, pofContext);

            int id = ((DefaultReflectedSerializer) serializer).getUserTypeId();

            if (typeSerializerMap.putIfAbsent(type, serializer) == null)
            {
                pofSerializersByUserTypeId.put(id, serializer);
                pofUserTypeIdsByClass.put(type, id);
                pofUserTypeIdsByClassName.put(type.getName(), id);

                synchronized (this)
                {
                    // read-write memory barrier, flushes the non-concurrent
                    // maps
                }
            }
        }

        return serializer;
    }


    /**
     * {@inheritDoc}
     */
    public FieldSerializationProvider getFieldSerializationProvider()
    {
        return serializationProvider;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ReflectedSerializer getTypeSerializer(Class<?> clazz)
    {
        return typeSerializerMap.get(clazz);
    }
}
