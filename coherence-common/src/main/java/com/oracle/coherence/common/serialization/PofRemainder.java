/*
 * File: PofRemainder.java
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
import com.tangosol.util.Binary;

import java.lang.reflect.Field;

/**
 * A {@link PofRemainder} is holds Pof stream remainder {@link Binary} and other associated evolvable
 * type information to support the evolvability of {@link PofType}s.
 * <p>
 * To support evolution, backwards and forwards compatibility of a {@link PofType}, declare a single
 * {@link PofRemainder} in the said {@link PofType}.  This {@link Field} will then be used to
 * store runtime type evolution and serialization information.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @see PofType
 *
 * @author Charlie Helin
 */
public final class PofRemainder
{
    /**
     * From which version of the serialized {@link PofType} was the {@link PofRemainder} read.
     */
    private int fromVersion;

    /**
     * The unread {@link Binary} containing the remaining bytes from a Pof stream
     */
    private Binary remainder;


    /**
     * Standard Constructor.
     *
     * @param fromVersion The version of the User Type from which the {@link PofRemainder} was created.
     * @param remainder The {@link Binary} remainder of the User Type from a Pof stream.
     */
    public PofRemainder(int    fromVersion,
                        Binary remainder)
    {
        this.fromVersion = fromVersion;
        this.remainder   = remainder;
    }


    /**
     * Returns the version from which the {@link PofRemainder} was created.
     *
     * @return The integer version number
     */
    public int getFromVersion()
    {
        return fromVersion;
    }


    /**
     * The {@link Binary} remainder.
     *
     * @return A {@link Binary} containing the remainder.
     */
    public Binary getBinary()
    {
        return remainder;
    }
}
