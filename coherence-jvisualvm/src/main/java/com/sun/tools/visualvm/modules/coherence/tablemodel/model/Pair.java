/*
 * File: Pair.java
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

package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import java.util.Map;
import java.util.Map.Entry;

/**
 * The representation of a two value {@link Tuple}.
 *
 * @author Brian Oliver/ Tim Middleton
 *
 * @param <X>  type of X value
 * @param <Y>  type of Y value
 */
public class Pair<X, Y> implements Tuple, Comparable<Pair<X, Y>>
{
    private static final long serialVersionUID = 2463266056607221797L;

    /**
     * Value for <X>.
     */
    private X                 x;
    
    /**
     * Value for <Y>.
     */
    private Y                 y;


    /**
     * Default constructor.
     */
    public Pair()
    {
    }


    /**
     * Create a Pair given a {@link Entry}
     *
     * @param entry {@link Entry} of X,Y
     */
    public Pair(Map.Entry<X, Y> entry)
    {
        this.x = entry.getKey();
        this.y = entry.getValue();
    }


    /**
     * Create a Pair given a X,Y value.
     *
     * @param x X value
     * @param y Y value
     */
    public Pair(X x,
                Y y)
    {
        this.x = x;
        this.y = y;
    }


    /**
     * Get the entry given an index
     *
     * @param index the index to return
     *
     * @return the entry given the index.
     *
     * @throws IndexOutOfBoundsException if the entry is outside the max value
     */
    public Object get(int index) throws IndexOutOfBoundsException
    {
        if (index == 0)
        {
            return x;
        }
        else if (index == 1)
        {
            return y;
        }
        else
        {
            throw new IndexOutOfBoundsException(String.format("%d is an illegal index for a Pair", index));
        }
    }


    /**
     * Returns the size of the Pair.
     */
    public int size()
    {
        return 2;
    }


    /**
     * Returns the X value of the Pair.
     *
     * @return the X value of the Pair
     */
    public X getX()
    {
        return x;
    }


    /**
     * Returns the Y value of the Pair.
     *
     * @return the Y value of the Pair
     */
    public Y getY()
    {
        return y;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("%s / %s", x == null ? "null" : x.toString(), y == null ? "null" : y.toString());
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(Pair<X, Y> pair)
    {
        if (pair.getX().equals(this.getX()))
        {
            return ((Comparable) this.getY()).compareTo(pair.getY());
        }
        else
        {
            return ((Comparable) this.getX()).compareTo(pair.getX());
        }

    }


    /**
     * {@inheritDoc}
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

        if (getClass() != obj.getClass())
        {
            return false;
        }

        Pair other = (Pair) obj;

        if (x == null)
        {
            if (other.x != null)
            {
                return false;
            }
        }
        else if (!x.equals(other.x))
        {
            return false;
        }

        if (y == null)
        {
            if (other.y != null)
            {
                return false;
            }
        }
        else if (!y.equals(other.y))
        {
            return false;
        }

        return true;
    }
}
