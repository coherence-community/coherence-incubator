/*
 * File: ComplexType.java
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

import java.util.Arrays;

/**
 * @author Christer Fahlgren
 */
@PofType(id = 1005)
public class ComplexType
{
    Object[] objectArrayField;


    /**
     * Constructs ...
     *
     */
    public ComplexType()
    {
    }


    /**
     * Method description
     */
    public void init()
    {
        objectArrayField = new Object[] {"a", "b", "c"};
    }


    /*
     *  (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */

    /**
     * Method description
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + Arrays.hashCode(objectArrayField);

        return result;
    }


    /*
     *  (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */

    /**
     * Method description
     *
     * @param obj
     *
     * @return
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

        ComplexType other = (ComplexType) obj;

        if (!Arrays.equals(objectArrayField, other.objectArrayField))
        {
            return false;
        }

        return true;
    }
}
