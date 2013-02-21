/*
 * File: BasicType.java
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

package com.oracle.coherence.common.serialization;

import com.oracle.coherence.common.serialization.annotations.PofField;
import com.oracle.coherence.common.serialization.annotations.PofIgnore;
import com.oracle.coherence.common.serialization.annotations.PofType;
import junit.framework.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author coh 2010-05-23
 */
@PofType(
    id      = 1000,
    version = 1
)
public class BasicType extends BaseType
{
    @PofField(since = 1)
    private int[]                     aa;
    private String                    c;
    @PofIgnore
    private String[]                  cs;
    private int[]                     ia;
    private int                       a;
    private float                     b;
    private Map<Integer, String>      map = new HashMap<Integer, String>();
    private HashMap<Integer, Integer> map2;
    @PofField(type = ConcurrentHashMap.class)
    private Map<Integer, Integer[]>   map3;
    private PofRemainder              remainder;


    /**
     * Constructs ...
     *
     */
    public BasicType()
    {
    }


    /**
     * Method description
     */
    @Override
    public void init()
    {
        super.init();

        a  = 33;
        b  = 1.0f;
        c  = "AABCDEFGG";
        cs = new String[] {"aaa", "bbb"};
        ia = new int[] {1, 2, 3};
        aa = new int[] {4, 2, 3};
        map.put(1, "apa");
        map2 = new HashMap<Integer, Integer>();
        map2.put(2, 2);
        map3 = new HashMap<Integer, Integer[]>();
        map3.put(4, new Integer[] {2, 3, 4});
    }


    /**
     * Method description
     */
    public void verify()
    {
        super.verify();

        Assert.assertEquals(a, 33);
        Assert.assertEquals(b, 1.0f);
        Assert.assertEquals(c, "AABCDEFGG");

        Assert.assertTrue(Arrays.equals(ia, new int[] {1, 2, 3}));
        Assert.assertTrue(Arrays.equals(aa, new int[] {4, 2, 3}));

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(map.get(1), "apa");

        Assert.assertEquals(1, map2.size());
        Assert.assertEquals(map2.get(2), new Integer(2));

        Assert.assertEquals(1, map3.size());
        Assert.assertTrue(Arrays.equals(map3.get(4), new Integer[] {2, 3, 4}));
    }
}
