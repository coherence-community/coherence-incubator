/*
 * File: ParameterizedInstanceBuilderTest.java
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

import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The {@link ParameterizedInstanceBuilderTest} represents unit tests for the {@link ParameterizedClassScheme}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ParameterizedInstanceBuilderTest
{
    /**
     * Tests the {@link ParameterizedClassScheme}.
     */
    @Test
    public void testParameterizedClassScheme()
    {
        VarArgsParameterizedBuilder builder = new VarArgsParameterizedBuilder("java.awt.Point", 1, 1);

        assertTrue(builder.realizesClassOf(Point.class, EmptyParameterProvider.INSTANCE));
        assertFalse(builder.realizesClassOf(Rectangle.class, EmptyParameterProvider.INSTANCE));
    }
}
