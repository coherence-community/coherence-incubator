/*
 * File: StringTransformerTest.java
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

import junit.framework.Assert;
import org.junit.Test;

/**
 * The {@link StringTransformerTest} provides unit tests for the {@link StringTransformer}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class StringTransformerTest
{
    /**
     * Test that we can match strings using regular expressions.
     */
    @Test
    public void testExactMatch()
    {
        StringTransformer transformer = new StringTransformer("dist-cache", "my-cache");

        Assert.assertTrue(transformer.isTransformable("dist-cache"));
        Assert.assertFalse(transformer.isTransformable("dist-me"));
        Assert.assertFalse(transformer.isTransformable("my-cache"));
        Assert.assertEquals("my-cache", transformer.transform("dist-cache"));
    }


    /**
     * Test that we can transform one string into another using regular expressions.
     */
    @Test
    public void testTranform()
    {
        StringTransformer transformer = new StringTransformer("dist-(.*)", "my-$1");

        Assert.assertTrue(transformer.isTransformable("dist-cache"));
        Assert.assertFalse(transformer.isTransformable("me"));
        Assert.assertFalse(transformer.isTransformable("my-cache"));
        Assert.assertEquals("my-cache", transformer.transform("dist-cache"));
    }
}
