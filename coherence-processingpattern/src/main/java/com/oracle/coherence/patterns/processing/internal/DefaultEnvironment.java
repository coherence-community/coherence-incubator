/*
 * File: DefaultEnvironment.java
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


import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


/**
 * An {@link DefaultEnvironment} provides access to strongly typed resources and contextual information for one or
 * more applications, components and/or extensions <strong>within</strong> a Coherence-based process (ie: JVM).
 *
 * @author Brian Oliver
 */
public class DefaultEnvironment
        implements Environment
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger =
            Logger.getLogger(Environment.class.getName());

    /**
     * /** The set of resources that have been registered with the
     * environment, organized by {@link Class} and name
     */
    private ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>>
            resourcesByClass = new
            ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>>();


    /**
     * {@inheritDoc}
     */
    public <R> R getResource(Class<R> clazz)
    {
        return getResource(clazz, "");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResource(Class<R> clazz,
            String name)
    {
        if (resourcesByClass.containsKey(clazz))
        {
            return (R) resourcesByClass.get(clazz).get(name);
        }
        else
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R registerResource(Class<R> clazz,
            Object resource)
    {
        return registerResource(clazz, "", resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R registerResource(Class<R> clazz,
            String name,
            final Object resource)
    {
        if (name == null)
        {
            throw new IllegalArgumentException(String
                    .format("Attempted to registerResource(%s, null) with a null name.  null names are not supported",
                            clazz));
        }
        else
        {
            ConcurrentHashMap<String, Object> resourcesByName;

            resourcesByName = resourcesByClass.get(clazz);

            if (resourcesByName == null)
            {
                resourcesByName = new ConcurrentHashMap<String, Object>();
                resourcesByClass.put(clazz, resourcesByName);
            }

            R oldRes = (R) resourcesByName.get(name);

            if (oldRes == null)
            {
                oldRes = (R) resource;
                resourcesByName.put(name, resource);

            }
            else
            {
                logger.warning(String
                        .format("Environment resource [%s] of type [%s] is already registered as [%s].  Skipping requested registration.",
                                name, clazz, resource));

            }

            return oldRes;
        }
    }


}
