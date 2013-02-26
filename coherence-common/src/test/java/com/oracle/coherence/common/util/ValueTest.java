/*
 * File: ValueTest.java
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

import com.oracle.coherence.environment.extensible.ConfigurationException;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UnknownFormatConversionException;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Value} class.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ValueTest
{
    /**
     * Testing using a valid String.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidString() throws ConfigurationException
    {
        Value value = new Value("This is a string");

        assertEquals(value.getString(), "This is a string");
    }


    /**
     * Test using a valid integer.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidInteger() throws ConfigurationException
    {
        Value value = new Value("100");

        assertEquals(value.getInt(), 100);
    }


    /**
     * Test that a invalid integer fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidInteger() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertEquals(value.getInt(), 100);
    }


    /**
     * Test using a valid long.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidLong() throws ConfigurationException
    {
        Value value = new Value("100");

        assertEquals(value.getLong(), 100);
    }


    /**
     * Test that a invalid long fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidLong() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertEquals(value.getLong(), 100);
    }


    /**
     * Test using a valid true boolean.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidTrueBoolean() throws ConfigurationException
    {
        Value value = new Value("true");

        assertTrue(value.getBoolean());
    }


    /**
     * Test using a valid false boolean.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidFalseBoolean() throws ConfigurationException
    {
        Value value = new Value("false");

        assertFalse(value.getBoolean());
    }


    /**
     * Test that a invalid boolean to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = UnknownFormatConversionException.class)
    public void testInvalidBoolean() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertTrue(value.getBoolean());
    }


    /**
     * Test using a valid double.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidDouble() throws ConfigurationException
    {
        Value value = new Value("123.456");

        assertTrue(value.getDouble() == 123.456);
    }


    /**
     * Test that a invalid double fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidDouble() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertTrue(value.getDouble() == 123.456);
    }


    /**
     * Test using a valid BigDecimal.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidBigDecimal() throws ConfigurationException
    {
        Value value = new Value("123.456");

        assertEquals(value.getBigDecimal(), new BigDecimal("123.456"));
    }


    /**
     * Test that a invalid BigDecimal fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidBigDecimal() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertEquals(value.getBigDecimal(), new BigDecimal("123.456"));
    }


    /**
     * Test using a valid float.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidFloat() throws ConfigurationException
    {
        Value value = new Value("123.00");

        assertTrue(value.getFloat() == 123.00);
    }


    /**
     * Test that a invalid float fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidFloat() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertTrue(value.getFloat() == 123.00);
    }


    /**
     * Test using a valid short.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidShort() throws ConfigurationException
    {
        Value value = new Value("100");

        assertEquals(value.getShort(), 100);
    }


    /**
     * Test that a invalid short fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidShort() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertEquals(value.getShort(), 100);
    }


    /**
     * Test using a valid byte.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidByte() throws ConfigurationException
    {
        Value value = new Value("100");

        assertEquals(value.getByte(), 100);
    }


    /**
     * Test that a invalid byte fails to parse.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test(expected = NumberFormatException.class)
    public void testInvalidByte() throws ConfigurationException
    {
        Value value = new Value("gibberish");

        assertEquals(value.getByte(), 100);
    }


    /**
     * Test a valid null.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidNull() throws ConfigurationException
    {
        Value value = new Value((Object) null);

        assertTrue(value.isNull());
    }


    /**
     * Test using valid xml.
     *
     * @throws ConfigurationException if config is invalid
     */
    @Test
    public void testValidXml() throws ConfigurationException
    {
        String xml   = "<element attribute=\"value\">content</element>";
        Value  value = new Value(xml);

        assertEquals(value.getXmlValue(), XmlHelper.loadXml(xml));
    }
}
