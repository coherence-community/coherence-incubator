/*
 * File: ReflectionHelperTest.java
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

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A {@link ReflectionHelperTest}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ReflectionHelperTest
{
    /**
     * Tests the getCompatibleConstructor helper method.
     *
     * @throws SecurityException         if access is not allowed
     * @throws NoSuchMethodException     if no such method exists
     * @throws IllegalArgumentException  if an argument is illegal
     * @throws InstantiationException    if instantiation fails
     * @throws IllegalAccessException    if access is not allowed
     * @throws InvocationTargetException if constructor fails
     * @throws ClassNotFoundException    if the class is not found
     */
    @Test
    public void testgetCompatibleConstructor()
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
               IllegalAccessException, InvocationTargetException, ClassNotFoundException
    {
        Object[]       parameterList = new Object[] {new Integer(100), new Integer(200)};
        Class<?>[]     classList     = ReflectionHelper.getClassArrayFromObjectArray(parameterList);
        Constructor<?> con           = null;

        try
        {
            con = java.awt.Point.class.getDeclaredConstructor(classList);
        }
        catch (NoSuchMethodException exception)
        {
        }

        assertNull(con);
        con = ReflectionHelper.getCompatibleConstructor(java.awt.Point.class, classList);
        assertTrue(con != null);

        Object obj = ReflectionHelper.createObject(java.awt.Point.class.getName(),
                                                   parameterList,
                                                   Thread.currentThread().getContextClassLoader());

        assertTrue(obj.getClass().equals(java.awt.Point.class));
    }


    /**
     * Tests the getCompatibleConstructor helper method.
     *
     * @throws SecurityException         if access is not allowed
     * @throws NoSuchMethodException     if no such method exists
     * @throws IllegalArgumentException  if an argument is illegal
     * @throws InstantiationException    if instantiation fails
     * @throws IllegalAccessException    if access is not allowed
     * @throws InvocationTargetException if constructor fails
     */
    @Test
    public void testgetCompatibleConstructorNoMatch()
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
               IllegalAccessException, InvocationTargetException
    {
        Object[]       parameterList = new Object[] {new Integer(100), new Boolean(false)};
        Class<?>[]     classList     = ReflectionHelper.getClassArrayFromObjectArray(parameterList);
        Constructor<?> con           = null;

        try
        {
            con = java.awt.Point.class.getDeclaredConstructor(classList);
        }
        catch (NoSuchMethodException exception)
        {
        }

        assertNull(con);
        con = ReflectionHelper.getCompatibleConstructor(java.awt.Point.class, classList);
        assertNull(con);
    }
}
