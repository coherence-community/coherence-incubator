/*
 * File: AttributeConfig.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.processing.config.builder;

import com.tangosol.config.annotation.Injectable;

/**
 * The AttributeConfig class represents a Processing Pattern attribute
 * configuration.
 *
 * @author Paul Mackin
 */
public class AttributeConfig
{
    private String m_sName;
    private String m_sAttribute;


    /**
     * Set the attribute name.
     *
     * @param sName  the attribute name
     */
    @Injectable
    public void setName(String sName)
    {
        m_sName = sName;
    }


    /**
     * Return the attribute name.
     *
     * @return the attribute name
     */
    public String getName()
    {
        return m_sName;
    }


    /**
     * Set the attribute.
     *
     * @param sAttribute  the attribute
     */
    @Injectable
    public void setAttribute(String sAttribute)
    {
        m_sAttribute = sAttribute;
    }


    /**
     * Return the attribute.
     *
     * @return  the attribute
     */
    public String getsAttribute()
    {
        return m_sAttribute;
    }
}
