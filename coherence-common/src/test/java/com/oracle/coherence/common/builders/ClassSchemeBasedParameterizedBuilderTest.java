/*
 * File: ClassSchemeBasedParameterizedBuilderTest.java
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

package com.oracle.coherence.common.builders;

import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.expressions.Constant;
import com.oracle.coherence.configuration.expressions.MacroParameterExpression;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.SimpleParameterProvider;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

/**
 * The {@link ClassSchemeBasedParameterizedBuilderTest} represents unit tests for the
 * {@link ClassSchemeBasedParameterizedBuilder}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ClassSchemeBasedParameterizedBuilderTest
{
    /**
     * A test using a constructor with no parameters.
     */
    @Test
    public void testConstructorWithNoParameters()
    {
        ClassSchemeBasedParameterizedBuilder builder = new ClassSchemeBasedParameterizedBuilder();

        builder.setClassName(new Constant("java.lang.String"));

        assertTrue(builder.realizesClassOf(String.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), "");
    }


    /**
     * A test using a constructor with the wrong number of parameters.
     */
    @Test(expected = Exception.class)
    public void testConstructorWithWrongNumberOfParameters()
    {
        ClassSchemeBasedParameterizedBuilder builder = new ClassSchemeBasedParameterizedBuilder();

        builder.setClassName(new Constant("java.lang.String"));

        MutableParameterProvider parameters = new SimpleParameterProvider();

        parameters.addParameter(new Parameter("autostart", false));
        parameters.addParameter(new Parameter("size", 1234));
        parameters.addParameter(new Parameter("height", 1234));
        builder.setParameters(parameters);

        assertTrue(builder.realizesClassOf(String.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), "");
    }


    /**
     * A test using a constructor with the correct number and type of parameters.
     */
    @Test
    public void testConstructorWithCorrectParameters()
    {
        ClassSchemeBasedParameterizedBuilder builder = new ClassSchemeBasedParameterizedBuilder();

        builder.setClassName(new Constant("java.lang.String"));

        MutableParameterProvider parameters = new SimpleParameterProvider();

        parameters.addParameter(new Parameter("string", "hello world"));
        builder.setParameters(parameters);

        assertTrue(builder.realizesClassOf(String.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), "hello world");
    }


    /**
     * A test using a constructor with parameterized parameters.
     */
    @Test
    public void testConstructorWithParameterizedParameters()
    {
        ClassSchemeBasedParameterizedBuilder builder = new ClassSchemeBasedParameterizedBuilder();

        builder.setClassName(new Constant("java.lang.String"));

        MutableParameterProvider parameters = new SimpleParameterProvider();

        parameters.addParameter(new Parameter("string", new MacroParameterExpression("{greeting}")));
        builder.setParameters(parameters);

        MutableParameterProvider parameterProvider = new SimpleParameterProvider();

        parameterProvider.addParameter(new Parameter("greeting", "hello world"));

        assertTrue(builder.realizesClassOf(String.class, parameterProvider));
        assertFalse(builder.realizesClassOf(Rectangle.class, parameterProvider));

        assertEquals(builder.realize(parameterProvider), "hello world");
    }


    /**
     * A test using a constructor with primitive parameters.
     */
    @Test
    public void testConstructorWithPrimitiveParameters()
    {
        ClassSchemeBasedParameterizedBuilder builder = new ClassSchemeBasedParameterizedBuilder();

        builder.setClassName(new Constant("java.awt.Point"));

        MutableParameterProvider parameters = new SimpleParameterProvider();

        parameters.addParameter(new Parameter("x", "java.lang.Integer", new Value(10)));
        parameters.addParameter(new Parameter("y", "java.lang.Integer", new Value(20)));
        builder.setParameters(parameters);

        assertTrue(builder.realizesClassOf(Point.class, parameters));
        assertFalse(builder.realizesClassOf(Rectangle.class, parameters));

        Point p = (Point) builder.realize(parameters);

        assertEquals(10, (int) p.getX());
        assertEquals(20, (int) p.getY());
    }
}
