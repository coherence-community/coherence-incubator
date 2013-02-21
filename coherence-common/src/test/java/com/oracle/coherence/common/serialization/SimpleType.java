/*
 * File: SimpleType.java
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

import com.oracle.coherence.common.serialization.annotations.PofType;
import junit.framework.Assert;

/**
 * Class description
 *
 * @version        Enter version here..., 12/08/20
 * @author         Enter your name here...
 */
@PofType(id = 1002)
public class SimpleType
{
    private int               a;
    private float             b;
    private double            c;
    private byte              d;
    private boolean           f;
    private short             g;
    private long              h;
    private transient int     ta;
    private transient float   tb;
    private transient double  tc;
    private transient byte    td;
    private transient boolean tf;
    private transient short   tg;
    private transient long    th;


    /**
     * Constructs ...
     *
     */
    public SimpleType()
    {
    }


    /**
     * Method description
     */
    public void init()
    {
        a  = 1;
        b  = 2.0f;
        c  = 3.0;
        d  = 4;
        f  = true;
        g  = 6;
        h  = 7;

        ta = 1;
        tb = 2.0f;
        tc = 3.0;
        td = 4;
        tf = true;
        tg = 6;
        th = 7;
    }


    /**
     * Method description
     */
    public void verify()
    {
        Assert.assertEquals(1, a);
        Assert.assertEquals(2.0f, b);
        Assert.assertEquals(3.0, c);
        Assert.assertEquals(4, d);
        Assert.assertEquals(true, f);
        Assert.assertEquals(6, g);
        Assert.assertEquals(7, h);

        Assert.assertEquals(0, ta);
        Assert.assertEquals(0.0f, tb);
        Assert.assertEquals(0.0, tc);
        Assert.assertEquals(0, td);
        Assert.assertEquals(false, tf);
        Assert.assertEquals(0, tg);
        Assert.assertEquals(0, th);
    }
}
