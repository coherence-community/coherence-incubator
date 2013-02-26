/*
 * File: ReflectionHelper.java
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A collection of utilities to assist in using Reflection to create objects.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 */
public class ReflectionHelper
{
    /**
     * Get a compatible constructor to the supplied parameter types.
     *
     * @param clazz the class which we want to construct
     * @param parameterTypes the types required of the constructor
     *
     * @return a compatible constructor or null if none exists
     */
    public static Constructor<?> getCompatibleConstructor(Class<?>   clazz,
                                                          Class<?>[] parameterTypes)
    {
        Constructor<?>[] constructors = clazz.getConstructors();

        for (int i = 0; i < constructors.length; i++)
        {
            if (constructors[i].getParameterTypes().length == (parameterTypes != null ? parameterTypes.length : 0))
            {
                // If we have the same number of parameters there is a shot that we have a compatible
                // constructor
                Class<?>[] constructorTypes = constructors[i].getParameterTypes();
                boolean    isCompatible     = true;

                for (int j = 0; j < (parameterTypes != null ? parameterTypes.length : 0); j++)
                {
                    if (!constructorTypes[j].isAssignableFrom(parameterTypes[j]))
                    {
                        // The type is not assignment compatible, however
                        // we might be able to coerce from a basic type to a boxed type
                        if (constructorTypes[j].isPrimitive())
                        {
                            if (!isAssignablePrimitive(constructorTypes[j], parameterTypes[j]))
                            {
                                isCompatible = false;
                                break;
                            }
                        }
                    }
                }

                if (isCompatible)
                {
                    return constructors[i];
                }
            }
        }

        return null;
    }


    /**
     * Determines if a primitive type is assignable to a wrapper type.
     *
     * @param clzPrimitive  a primitive class type
     * @param clzWrapper    a wrapper class type
     *
     * @return true if primitive and wrapper are assignment compatible
     */
    public static boolean isAssignablePrimitive(Class<?> clzPrimitive,
                                                Class<?> clzWrapper)
    {
        return (clzPrimitive.equals(java.lang.Boolean.TYPE) && clzWrapper.equals(java.lang.Boolean.class))
               || (clzPrimitive.equals(java.lang.Byte.TYPE) && clzWrapper.equals(java.lang.Byte.class))
               || (clzPrimitive.equals(java.lang.Character.TYPE) && clzWrapper.equals(java.lang.Character.class))
               || (clzPrimitive.equals(java.lang.Double.TYPE) && clzWrapper.equals(java.lang.Double.class))
               || (clzPrimitive.equals(java.lang.Float.TYPE) && clzWrapper.equals(java.lang.Float.class))
               || (clzPrimitive.equals(java.lang.Integer.TYPE) && clzWrapper.equals(java.lang.Integer.class))
               || (clzPrimitive.equals(java.lang.Long.TYPE) && clzWrapper.equals(java.lang.Long.class))
               || (clzPrimitive.equals(java.lang.Short.TYPE) && clzWrapper.equals(java.lang.Short.class));
    }


    /**
     * Obtains the {@link Method} that is compatible to the supplied parameter types.
     *
     * @param clazz      the {@link Class} on which to find the {@link Method}
     * @param arguments  the arguments for the {@link Method}
     *
     * @return a compatible {@link Method} or <code>null</code> if one can't be found
     */
    public static Method getCompatibleMethod(Class<?>  clazz,
                                             String    methodName,
                                             Object... arguments)
    {
        // determine the types of the arguments
        Class<?>[] argumentTypes = new Class<?>[arguments.length];

        for (int i = 0; i < arguments.length; i++)
        {
            argumentTypes[i] = arguments[i] == null ? null : arguments[i].getClass();
        }

        try
        {
            // attempt to find the method on the specified class
            // (this may fail, in which case we should try super classes)
            return clazz.getDeclaredMethod(methodName, argumentTypes);
        }
        catch (SecurityException e)
        {
            return null;
        }
        catch (NoSuchMethodException e)
        {
            return clazz.getSuperclass() == null ? null : getCompatibleMethod(clazz.getSuperclass(),
                                                                              methodName,
                                                                              arguments);
        }
    }


    /**
     * <p>Create an Object via reflection (using the specified {@link ClassLoader}).</p>
     *
     * @param className The name of the class to instantiate.
     * @param classLoader The {@link ClassLoader} to use to load the class.
     *
     * @return A new instance of the class specified by the className
     *
     * @throws ClassNotFoundException if the class is not found
     * @throws NoSuchMethodException if there is no such constructor
     * @throws InstantiationException if it failed to instantiate
     * @throws IllegalAccessException if security doesn't allow the call
     * @throws InvocationTargetException if the constructor failed
     */
    public static Object createObject(String      className,
                                      ClassLoader classLoader)
                                          throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
                                                 IllegalAccessException, InvocationTargetException
    {
        Class<?>       clazz = Class.forName(className, true, classLoader);
        Constructor<?> con   = clazz.getDeclaredConstructor((Class[]) null);

        return con.newInstance((Object[]) null);
    }


    /**
     * <p>Create an Object via reflection (using the calling Thread context {@link ClassLoader}).</p>
     *
     * @param className The name of the class to instantiate.
     *
     * @return A new instance of the class specified by the className
     *
     * @throws ClassNotFoundException if the class is not found
     * @throws NoSuchMethodException if there is no such constructor
     * @throws InstantiationException if it failed to instantiate
     * @throws IllegalAccessException if security doesn't allow the call
     * @throws InvocationTargetException if the constructor failed
     */
    @Deprecated
    public static Object createObject(String className)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
               InvocationTargetException
    {
        return createObject(className, Thread.currentThread().getContextClassLoader());
    }


    /**
     * <p>Create an Object via reflection (using the specified {@link ClassLoader}).</p>
     *
     * @param className The name of the class to instantiate.
     * @param constructorParameterList The set of parameters to pass to the constructor
     * @param classLoader The {@link ClassLoader} to use to load the class.
     *
     * @return A new instance of the class specified by the className
     *
     * @throws ClassNotFoundException if the class is not found
     * @throws NoSuchMethodException if there is no such constructor
     * @throws InstantiationException if it failed to instantiate
     * @throws IllegalAccessException if security doesn't allow the call
     * @throws InvocationTargetException if the constructor failed
     */
    public static Object createObject(String      className,
                                      Object[]    constructorParameterList,
                                      ClassLoader classLoader)
                                          throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
                                                 IllegalAccessException, InvocationTargetException
    {
        Class<?>       clazz          = Class.forName(className, true, classLoader);
        Class<?>[]     parameterTypes = getClassArrayFromObjectArray(constructorParameterList);
        Constructor<?> con            = ReflectionHelper.getCompatibleConstructor(clazz, parameterTypes);

        return con.newInstance(constructorParameterList);
    }


    /**
     * <p>Create an Object via reflection (using the calling Thread context {@link ClassLoader}).</p>
     *
     * @param className The name of the class to instantiate.
     * @param constructorParameterList The set of parameters to pass to the constructor
     *
     * @return A new instance of the class specified by the className
     *
     * @throws ClassNotFoundException if the class is not found
     * @throws NoSuchMethodException if there is no such constructor
     * @throws InstantiationException if it failed to instantiate
     * @throws IllegalAccessException if security doesn't allow the call
     * @throws InvocationTargetException if the constructor failed
     */
    @Deprecated
    public static Object createObject(String   className,
                                      Object[] constructorParameterList)
                                          throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
                                                 IllegalAccessException, InvocationTargetException
    {
        return createObject(className, constructorParameterList, Thread.currentThread().getContextClassLoader());
    }


    /**
     * Returns an array of Class objects representing the class of the objects in the parameter.
     *
     * @param objectArray the array of Objects
     *
     * @return an array of Classes representing the class of the Objects
     */
    protected static Class<?>[] getClassArrayFromObjectArray(Object[] objectArray)
    {
        Class<?>[] parameterTypes = null;

        if (objectArray != null)
        {
            parameterTypes = new Class[objectArray.length];

            for (int i = 0; i < objectArray.length; i++)
            {
                parameterTypes[i] = objectArray[i].getClass();
            }
        }

        return parameterTypes;
    }


    /**
     * Obtains the concrete (non-parameterized) {@link Class} given a specified
     * (possibly parameterized) type.
     *
     * @param type  the type
     *
     * @return the concrete {@link Class} or <code>null</code> if there is
     *         no concrete class.
     */
    public static Class<?> getConcreteType(Type type)
    {
        if (type instanceof Class)
        {
            return (Class<?>) type;
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            return getConcreteType(parameterizedType.getRawType());
        }
        else
        {
            return null;
        }
    }


    /**
     * Determines if two types are assignment compatible, that is, the type of
     * y can be assigned to type x.
     *
     * @param x
     * @param y
     *
     * @return if a value of type y is assignable to the type x
     */
    public static boolean isAssignableFrom(Type x,
                                           Type y)
    {
        // NOTE: for now we're only supporting compatibility tests using classes
        // and parameterized types (but we don't check generics)
        if (x instanceof Class && y instanceof Class)
        {
            Class<?> clzX = (Class<?>) x;
            Class<?> clzY = (Class<?>) y;

            return clzX.isAssignableFrom(clzY);
        }
        else if (x instanceof ParameterizedType && y instanceof Class)
        {
            return isAssignableFrom(getConcreteType(x), getConcreteType(y));
        }
        else
        {
            return false;
        }
    }


    /**
     * Determines if the signature of a {@link Method} is compatible
     * with the specified parameters.
     *
     * @param method          the {@link Method} to check against
     * @param modifiers       the desired modifiers of the {@link Method}
     * @param returnType      the desired return type of the {@link Method}
     * @param parameterTypes  the parameters to the {@link Method}
     *
     * @return <code>true</code> if the {@link Method} signature is compatible
     *         with the specified parameters, <code>false</code> otherwise
     */
    public static boolean isCompatibleMethod(Method  method,
                                             int     modifiers,
                                             Type    returnType,
                                             Type... parameterTypes)
    {
        // check the parameters first
        Type[] methodParameterTypes = method.getGenericParameterTypes();

        if (methodParameterTypes.length == parameterTypes.length)
        {
            for (int i = 0; i < methodParameterTypes.length; i++)
            {
                if (!isAssignableFrom(methodParameterTypes[i], parameterTypes[i]))
                {
                    return false;
                }
            }

            return isAssignableFrom(method.getGenericReturnType(), returnType) && method.getModifiers() == modifiers;
        }
        else
        {
            return false;
        }
    }
}
