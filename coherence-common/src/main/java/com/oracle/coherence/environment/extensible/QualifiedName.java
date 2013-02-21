/*
 * File: QualifiedName.java
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

package com.oracle.coherence.environment.extensible;

import com.tangosol.run.xml.XmlElement;

import java.util.UnknownFormatConversionException;

/**
 * A {@link QualifiedName} is a useful class to separately capture the xml namespace prefix
 * and local name of xml elements and attributes (instead of having to parse them out of {@link String}s
 * all the time).
 * <p>
 * For example, the xmlName "movie:definition" has the namespace prefix "movie"
 * and the local name "definition".  If there is no namespace prefix declared, the prefix is
 * always returned as "".
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class QualifiedName
{
    /**
     * The prefix of a {@link QualifiedName} is usually the namespace to which it belongs.
     */
    private String prefix;

    /**
     * The localName of a {@link QualifiedName} is the name of the xml element/attribute with in
     * its namespace.
     */
    private String localName;


    /**
     * Standard Constructor.
     *
     * @param xmlName The name of an xml element/attribute from which to create a qualified name.
     *                Must be of the format "prefix:name" or simply "name" (in which case the prefix is
     *                considered "")
     *
     * @throws UnknownFormatConversionException When the specified xmlName is invalid (contains mulitple :'s)
     */
    public QualifiedName(String xmlName) throws UnknownFormatConversionException
    {
        String parts[] = xmlName.trim().split(":");

        if (parts.length == 1)
        {
            this.prefix    = "";
            this.localName = xmlName.trim();
        }
        else if (parts.length == 2)
        {
            this.prefix    = parts[0].trim();
            this.localName = parts[1].trim();
        }
        else
        {
            throw new UnknownFormatConversionException(String
                .format("The specified xmlName [%s] can't be parsed into a QualifiedName", xmlName));
        }
    }


    /**
     * Standard Constructor.
     *
     * @param xmlElement An {@link XmlElement} from which to return the {@link QualifiedName}
     *
     * @throws UnknownFormatConversionException When the specified xmlElement is invalid (contains mulitple :'s)
     */
    public QualifiedName(XmlElement xmlElement) throws UnknownFormatConversionException
    {
        this(xmlElement.getName());
    }


    /**
     * Standard Constructor.
     *
     * @param qualifiedName The {@link QualifiedName} namespace in which the new {@link QualifiedName} should be created.
     * @param localName     The localname for the {@link QualifiedName}.
     */
    public QualifiedName(QualifiedName qualifiedName,
                         String        localName)
    {
        this.prefix    = qualifiedName.getPrefix();
        this.localName = localName;
    }


    /**
     * Standard Constructor.
     *
     * @param prefix    The xmlns prefix for the {@link QualifiedName}
     * @param localName The localname for the {@link QualifiedName}
     */
    public QualifiedName(String prefix,
                         String localName)
    {
        this.prefix    = prefix.trim();
        this.localName = localName.trim();
    }


    /**
     * Returns the xml prefix of the {@link QualifiedName}.
     *
     * @return Returns "" if the name is not qualified with a namespace prefix
     */
    public String getPrefix()
    {
        return prefix;
    }


    /**
     * Returns if the {@link QualifiedName} has a namespace prefix.
     *
     * @return <code>true</code> If the {@link QualifiedName} has an xmlns prefix
     */
    public boolean hasPrefix()
    {
        return prefix.length() > 0;
    }


    /**
     * Returns the local name of the {@link QualifiedName}.
     *
     * @return Returns the local part of a qualified name.
     */
    public String getLocalName()
    {
        return localName;
    }


    /**
     * Returns the entire qualified name, including the prefix and local name.
     *
     * @return A string containing the entire qualified name, including prefix and local name.
     */
    public String getName()
    {
        return hasPrefix() ? String.format("%s:%s", getPrefix(), getLocalName()) : getLocalName();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getName();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((localName == null) ? 0 : localName.hashCode());
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());

        return result;
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

        QualifiedName other = (QualifiedName) obj;

        if (localName == null)
        {
            if (other.localName != null)
            {
                return false;
            }
        }
        else if (!localName.equals(other.localName))
        {
            return false;
        }

        if (prefix == null)
        {
            if (other.prefix != null)
            {
                return false;
            }
        }
        else if (!prefix.equals(other.prefix))
        {
            return false;
        }

        return true;
    }
}
