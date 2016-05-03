/*
 * File: PrimesTest.java
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link Primes}.
 * <p>
 * Copyright (c) 2016. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class PrimesTest
{
    /**
     * Should return 1 as the prime when requesting 1 and below.
     */
    @Test
    public void shouldReturnOneForClosestPrimesToOneAndBelow()
    {
        assertThat(Primes.closestPrime(1), is(1));
        assertThat(Primes.closestPrime(0), is(1));
        assertThat(Primes.closestPrime(-1), is(1));
    }


    /**
     * Should return the prime number when it is specified.
     */
    @Test
    public void shouldReturnPrimeForClosestOfAPrime()
    {
        assertThat(Primes.closestPrime(2), is(2));
        assertThat(Primes.closestPrime(3), is(3));
        assertThat(Primes.closestPrime(5), is(5));
        assertThat(Primes.closestPrime(7), is(7));
    }


    /**
     * Should return closest primes.
     */
    @Test
    public void shouldReturnClosestPrimes()
    {
        assertThat(Primes.closestPrime(4), is(5));
        assertThat(Primes.closestPrime(6), is(7));
        assertThat(Primes.closestPrime(8), is(7));
        assertThat(Primes.closestPrime(9), is(11));
        assertThat(Primes.closestPrime(10), is(11));
        assertThat(Primes.closestPrime(32), is(31));
    }
}
