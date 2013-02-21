/*
 * File: VersionedType.java
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
import com.oracle.coherence.common.serialization.annotations.PofType;
import junit.framework.Assert;

/**
 * @author coh 2010-05-23
 */
@PofType(
    id      = 1003,
    version = 1
)
public class VersionedType
{
    /**
     * Field description
     */
    public static final byte[] binaryRemainder = new byte[] {7, 115, 8, 125, 9, 69, 64, 62, 0, 0, 0, 0, 0, 0, 10, 75,
                                                             40, 11, 97, 12, 64, 60, 13, 66, -122, 1};

    int               a;
    float             b;
    double            c;
    byte              d;
    boolean           f;
    short             g;
    long              h;
    @PofField(since = 1)
    int               a1;
    @PofField(since = 1)
    float             b1;
    @PofField(since = 1)
    double            c1;
    @PofField(since = 1)
    byte              d1;
    @PofField(since = 1)
    boolean           f1;
    @PofField(since = 1)
    short             g1;
    @PofField(since = 1)
    long              h1;
    PofRemainder      remainder;
    transient int     ta;
    transient float   tb;
    transient double  tc;
    transient byte    td;
    transient boolean tf;
    transient short   tg;
    transient long    th;


    /**
     * Constructs ...
     *
     */
    public VersionedType()
    {
    }


    /**
     * Method description
     */
    public void assertRemainder()
    {
        int i = 0;

        for (byte b : remainder.getBinary().toByteArray())
        {
            Assert.assertEquals(binaryRemainder[i++], b);
        }

        Assert.assertEquals(26, remainder.getBinary().length());
    }


    /**
     * @{inheritDoc
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (!(obj instanceof VersionedType))
        {
            return false;
        }

        VersionedType other = (VersionedType) obj;

        if (a != other.a)
        {
            return false;
        }

        if (a1 != other.a1)
        {
            return false;
        }

        if (Float.floatToIntBits(b) != Float.floatToIntBits(other.b))
        {
            return false;
        }

        if (Float.floatToIntBits(b1) != Float.floatToIntBits(other.b1))
        {
            return false;
        }

        if (Double.doubleToLongBits(c) != Double.doubleToLongBits(other.c))
        {
            return false;
        }

        if (Double.doubleToLongBits(c1) != Double.doubleToLongBits(other.c1))
        {
            return false;
        }

        if (d != other.d)
        {
            return false;
        }

        if (d1 != other.d1)
        {
            return false;
        }

        if (f != other.f)
        {
            return false;
        }

        if (f1 != other.f1)
        {
            return false;
        }

        if (g != other.g)
        {
            return false;
        }

        if (g1 != other.g1)
        {
            return false;
        }

        if (h != other.h)
        {
            return false;
        }

        if (h1 != other.h1)
        {
            return false;
        }

        if (remainder == null)
        {
            if (other.remainder != null)
            {
                return false;
            }
        }
        else if (!remainder.equals(other.remainder))
        {
            return false;
        }

        return true;
    }


    /**
     * @{inheritDoc
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + a;
        result = prime * result + a1;
        result = prime * result + Float.floatToIntBits(b);
        result = prime * result + Float.floatToIntBits(b1);

        long temp;

        temp   = Double.doubleToLongBits(c);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp   = Double.doubleToLongBits(c1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + d;
        result = prime * result + d1;
        result = prime * result + (f ? 1231 : 1237);
        result = prime * result + (f1 ? 1231 : 1237);
        result = prime * result + g;
        result = prime * result + g1;
        result = prime * result + (int) (h ^ (h >>> 32));
        result = prime * result + (int) (h1 ^ (h1 >>> 32));
        result = prime * result + ((remainder == null) ? 0 : remainder.hashCode());

        return result;
    }


    /**
     * Method description
     *
     * @param version
     */
    public void init(int version)
    {
        a = 1;
        b = 2.0f;
        c = 3.0;
        d = 4;
        f = true;
        g = 6;
        h = 7;

        if (version == 1)
        {
            a1 = 10;
            b1 = 20.0f;
            c1 = 30.0;
            d1 = 40;
            f1 = true;
            g1 = 60;
            h1 = 70;
        }

        ta = 1;
        tb = 2.0f;
        tc = 3.0;
        td = 4;
        tf = true;
        tg = 6;
        th = 7;
    }


    /**
     * @{inheritDoc
     */
    @Override
    public String toString()
    {
        return "VersionedType [a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + ", f=" + f + ", g=" + g + ", h=" + h
               + ", a1=" + a1 + ", b1=" + b1 + ", c1=" + c1 + ", d1=" + d1 + ", f1=" + f1 + ", g1=" + g1 + ", h1=" + h1
               + ", remainder=" + remainder + "]";
    }


    /**
     * Method description
     *
     * @param version
     */
    public void updateTo(int version)
    {
        if (version == 1)
        {
            a1 = 10;
            b1 = 20.0f;
            c1 = 30.0;
            d1 = 40;
            f1 = true;
            g1 = 60;
            h1 = 70;
        }
    }


    /**
     * Method description
     *
     * @param version
     */
    public void verify(int version)
    {
        Assert.assertEquals(1, a);
        Assert.assertEquals(2.0f, b);
        Assert.assertEquals(3.0, c);
        Assert.assertEquals(4, d);
        Assert.assertEquals(true, f);
        Assert.assertEquals(6, g);
        Assert.assertEquals(7, h);

        if (version == 1)
        {
            Assert.assertEquals(10, a1);
            Assert.assertEquals(20.0f, b1);
            Assert.assertEquals(30.0, c1);
            Assert.assertEquals(40, d1);
            Assert.assertEquals(true, f1);
            Assert.assertEquals(60, g1);
            Assert.assertEquals(70, h1);
        }

        Assert.assertEquals(0, ta);
        Assert.assertEquals(0.0f, tb);
        Assert.assertEquals(0.0, tc);
        Assert.assertEquals(0, td);
        Assert.assertEquals(false, tf);
        Assert.assertEquals(0, tg);
        Assert.assertEquals(0, th);
    }
}
