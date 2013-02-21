/*
 * File: TristateTest.java
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for the {@link Tristate} class.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class TristateTest
{
    /**
     * Test Boolean compatibility.
     */
    @Test
    public void testBooleanCompatibility()
    {
        assertTrue(Tristate.TRUE.equals(new Tristate(true)));
        assertTrue(Tristate.FALSE.equals(new Tristate(false)));
    }


    /**
     * Test constants.
     */
    @Test
    public void testContants()
    {
        assertTrue(Tristate.TRUE.isTrue());
        assertFalse(Tristate.TRUE.isFalse());
        assertFalse(Tristate.TRUE.isUndefined());

        assertFalse(Tristate.FALSE.isTrue());
        assertTrue(Tristate.FALSE.isFalse());
        assertFalse(Tristate.FALSE.isUndefined());

        assertFalse(Tristate.UNDEFINED.isTrue());
        assertFalse(Tristate.UNDEFINED.isFalse());
        assertTrue(Tristate.UNDEFINED.isUndefined());
    }


    /**
     * Test not().
     */
    @Test
    public void testNot()
    {
        assertTrue(Tristate.TRUE.not().equals(Tristate.FALSE));
        assertTrue(Tristate.FALSE.not().equals(Tristate.TRUE));
        assertTrue(Tristate.UNDEFINED.not().equals(Tristate.UNDEFINED));
    }


    /**
     * Test or().
     */
    @Test
    public void testOr()
    {
        assertTrue(Tristate.TRUE.or(Tristate.TRUE).equals(Tristate.TRUE));
        assertTrue(Tristate.TRUE.or(Tristate.FALSE).equals(Tristate.TRUE));
        assertTrue(Tristate.TRUE.or(Tristate.UNDEFINED).equals(Tristate.TRUE));

        assertTrue(Tristate.FALSE.or(Tristate.TRUE).equals(Tristate.TRUE));
        assertTrue(Tristate.FALSE.or(Tristate.FALSE).equals(Tristate.FALSE));
        assertTrue(Tristate.FALSE.or(Tristate.UNDEFINED).equals(Tristate.FALSE));

        assertTrue(Tristate.UNDEFINED.or(Tristate.TRUE).equals(Tristate.TRUE));
        assertTrue(Tristate.UNDEFINED.or(Tristate.FALSE).equals(Tristate.FALSE));
        assertTrue(Tristate.UNDEFINED.or(Tristate.UNDEFINED).equals(Tristate.UNDEFINED));
    }


    /**
     * Test and().
     */
    @Test
    public void testAnd()
    {
        assertTrue(Tristate.TRUE.and(Tristate.TRUE).equals(Tristate.TRUE));
        assertTrue(Tristate.TRUE.and(Tristate.FALSE).equals(Tristate.FALSE));
        assertTrue(Tristate.TRUE.and(Tristate.UNDEFINED).equals(Tristate.UNDEFINED));

        assertTrue(Tristate.FALSE.and(Tristate.TRUE).equals(Tristate.FALSE));
        assertTrue(Tristate.FALSE.and(Tristate.FALSE).equals(Tristate.FALSE));
        assertTrue(Tristate.FALSE.and(Tristate.UNDEFINED).equals(Tristate.UNDEFINED));

        assertTrue(Tristate.UNDEFINED.and(Tristate.TRUE).equals(Tristate.UNDEFINED));
        assertTrue(Tristate.UNDEFINED.and(Tristate.FALSE).equals(Tristate.UNDEFINED));
        assertTrue(Tristate.UNDEFINED.and(Tristate.UNDEFINED).equals(Tristate.UNDEFINED));
    }
}
