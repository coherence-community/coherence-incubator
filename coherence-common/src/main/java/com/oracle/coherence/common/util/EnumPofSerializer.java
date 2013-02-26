/*
 * File: EnumPofSerializer.java
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

import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * {@link PofSerializer} implementation that can be used to serialize all {@link Enum}
 * values.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Aleks Seovic
 */
public class EnumPofSerializer implements PofSerializer
{
    /**
     * {@inheritDoc}
     */
    public void serialize(PofWriter writer,
                          Object    o) throws IOException
    {
        if (!o.getClass().isEnum())
        {
            throw new IllegalArgumentException("EnumPofSerializer can only be used to serialize enum types.");
        }

        writer.writeString(0, ((Enum<?>) o).name());
        writer.writeRemainder(null);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object deserialize(PofReader reader) throws IOException
    {
        PofContext ctx = reader.getPofContext();
        Class<?>   clz = ctx.getClass(reader.getUserTypeId());

        if (!clz.isEnum())
        {
            throw new IllegalArgumentException("EnumPofSerializer can only be used to deserialize enum types.");
        }

        Enum<?> enumValue = (Enum<?>) Enum.valueOf((Class<Enum>) clz, reader.readString(0));

        reader.readRemainder();

        return enumValue;
    }
}
