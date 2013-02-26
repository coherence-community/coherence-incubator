/*
 * File: MacroParameterExpressionTest.java
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

package com.oracle.coherence.configuration.expressions;

import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.SimpleParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link MacroParameterExpression} class.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class MacroParameterExpressionTest
{
    /**
     * Tests a boolean Macro without parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testBooleanMacroWithoutParameter() throws ClassNotFoundException
    {
        Expression expression = new MacroParameterExpression("true");

        assertTrue(expression.evaluate(SystemPropertyParameterProvider.INSTANCE).getBoolean());
    }


    /**
     * Tests a boolean macro without parameters but with default value.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testBooleanMacroWithParameterUsingDefaultValue() throws ClassNotFoundException
    {
        Expression expression = new MacroParameterExpression("{my-parameter true}");

        assertTrue(expression.evaluate(SystemPropertyParameterProvider.INSTANCE).getBoolean());
    }


    /**
     * Tests a boolean macro using a parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testBooleanMacroWithParameter() throws ClassNotFoundException
    {
        MutableParameterProvider parameterProvider = new SimpleParameterProvider();

        parameterProvider.addParameter(new Parameter("my-parameter", false));

        Expression expression = new MacroParameterExpression("{my-parameter}");

        assertTrue(!expression.evaluate(parameterProvider).getBoolean());
    }


    /**
     * Tests a string macro without parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithoutParameter() throws ClassNotFoundException
    {
        Expression expression = new MacroParameterExpression("Hello World");

        assertTrue(expression.evaluate(SystemPropertyParameterProvider.INSTANCE).getString().equals("Hello World"));
    }


    /**
     * Tests a string macro with an undefined parameter but with a default.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParameterUsingDefaultValue() throws ClassNotFoundException
    {
        Expression expression = new MacroParameterExpression("{my-parameter Gudday}");

        assertTrue(expression.evaluate(SystemPropertyParameterProvider.INSTANCE).getString().equals("Gudday"));
    }


    /**
     * Tests a macro with a single parameter.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParameter() throws ClassNotFoundException
    {
        MutableParameterProvider parameterProvider = new SimpleParameterProvider();

        parameterProvider.addParameter(new Parameter("my-parameter", "Hello World"));

        Expression expression = new MacroParameterExpression("{my-parameter}");

        assertTrue(expression.evaluate(parameterProvider).getString().equals("Hello World"));
    }


    /**
     * Tests a macro with multiple parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParameters() throws ClassNotFoundException
    {
        MutableParameterProvider parameterProvider = new SimpleParameterProvider();

        parameterProvider.addParameter(new Parameter("my-parameter-1", "Hello"));
        parameterProvider.addParameter(new Parameter("my-parameter-2", "World"));

        Expression expression = new MacroParameterExpression("({my-parameter-1}-{my-parameter-2})");

        assertTrue(expression.evaluate(parameterProvider).getString().equals("(Hello-World)"));
    }


    /**
     * Tests a macro with multiple parameters using defaults.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParametersAndDefaults() throws ClassNotFoundException
    {
        MutableParameterProvider parameterProvider = new SimpleParameterProvider();

        parameterProvider.addParameter(new Parameter("my-parameter-1", "Hello"));
        parameterProvider.addParameter(new Parameter("my-parameter-2", "World"));

        Expression expression =
            new MacroParameterExpression("Greeting is \\{{my-parameter-0 Gudday}-{my-parameter-2}\\}");

        assertTrue(expression.evaluate(parameterProvider).getString().equals("Greeting is {Gudday-World}"));
    }
}
