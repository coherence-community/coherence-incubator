/*
 * File: ConfigurableStaticFactoryInstanceBuilderTest.java
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

import com.oracle.coherence.configuration.expressions.Constant;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.SimpleParameterProvider;
import org.junit.Test;

import java.awt.*;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * The {@link ConfigurableStaticFactoryInstanceBuilderTest} represents unit tests for the
 * {@link ConfigurableStaticFactoryClassScheme}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ConfigurableStaticFactoryInstanceBuilderTest
{
    /**
     * A test using a static method with no parameters.
     */
    @Test
    public void testStaticMethodWithNoParameters()
    {
        StaticFactoryClassSchemeBasedParameterizedBuilder builder =
            new StaticFactoryClassSchemeBasedParameterizedBuilder();

        builder.setFactoryClassName(new Constant("java.lang.System"));
        builder.setFactoryMethodName(new Constant("getProperties"));

        assertTrue(builder.realizesClassOf(Properties.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), System.getProperties());
    }


    /**
     * A test using a static method with the wrong number of parameters.
     */
    @Test(expected = Exception.class)
    public void testStaticMethodWrongNumberOfParameters()
    {
        StaticFactoryClassSchemeBasedParameterizedBuilder builder =
            new StaticFactoryClassSchemeBasedParameterizedBuilder();

        builder.setFactoryClassName(new Constant("java.lang.System"));
        builder.setFactoryMethodName(new Constant("getProperties"));

        MutableParameterProvider parameters = new SimpleParameterProvider();

        parameters.addParameter(new Parameter("autostart", false));
        builder.setParameters(parameters);

        assertFalse(builder.realizesClassOf(Properties.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), System.getProperties());
    }


    /**
     * A test using a static method with the corrent number and type of parameters.
     */
    @Test
    public void testStaticMethodWithCorrectParameters()
    {
        StaticFactoryClassSchemeBasedParameterizedBuilder builder =
            new StaticFactoryClassSchemeBasedParameterizedBuilder();

        builder.setFactoryClassName(new Constant("java.lang.System"));
        builder.setFactoryMethodName(new Constant("getProperty"));

        MutableParameterProvider parameters = new SimpleParameterProvider();

        parameters.addParameter(new Parameter("key", "java.class.path"));
        builder.setParameters(parameters);

        assertTrue(builder.realizesClassOf(String.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), System.getProperty("java.class.path"));
    }


    /**
     * A test using a non-static method.
     */
    @Test(expected = Exception.class)
    public void testNonStaticMethod()
    {
        StaticFactoryClassSchemeBasedParameterizedBuilder builder =
            new StaticFactoryClassSchemeBasedParameterizedBuilder();

        builder.setFactoryClassName(new Constant("java.lang.String"));
        builder.setFactoryMethodName(new Constant("size"));

        assertFalse(builder.realizesClassOf(int.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));

        assertEquals(builder.realize(EmptyParameterProvider.INSTANCE), 0);
    }
}
