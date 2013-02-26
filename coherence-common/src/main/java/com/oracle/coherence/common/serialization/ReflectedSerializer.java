/*
 * File: ReflectedSerializer.java
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

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * A {@link ReflectedSerializer} is a {@link PofSerializer} implementation that has been generated at runtime
 * through the use of reflection.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
public interface ReflectedSerializer extends PofSerializer
{
    /**
     * Determines the type (as a {@link Class}) that this {@link PofSerializer} can serialize/deserialize.
     *
     * @return The type (as a {@link Class}).
     */
    public Class<?> getType();


    /**
     * The Pof version number that will be used for serializing the type when a {@link PofRemainder#getFromVersion()}
     * is unavailable.
     *
     * @return An Integer
     */
    public int getVersion();


    /**
     * Sets (forces) the Pof version number that will be used for serializing the type when a
     * {@link PofRemainder#getFromVersion()} is unavailable.
     *
     * @param version The Pof version number.
     */
    public void setVersion(int version);


    /**
     * {@inheritDoc}
     */
    public Object deserialize(PofReader reader) throws IOException;


    /**
     * {@inheritDoc}
     */
    public void serialize(PofWriter writer,
                          Object    object) throws IOException;
}
