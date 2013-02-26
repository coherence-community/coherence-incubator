/*
 * File: StringTransformer.java
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link StringTransformer} is a {@link UniformTransformer} that uses regular expressions to conditionally match and
 * transform strings into other strings.  These are very useful for mapping cache names into other names.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class StringTransformer implements UniformTransformer<String>
{
    /**
     * The regular expression pattern used to match a string.
     */
    private String matchingRegEx;

    /**
     * The regular expression pattern used to transform the matched string.
     */
    private String transformationRegEx;

    /**
     * The compiled matching pattern.
     */
    private Pattern matchingPattern;


    /**
     * Standard Constructor.
     *
     * @param matchingRegEx         The regular expression pattern to match a string
     * @param transformationRegEx   The regular expression to transform the matched string
     */
    public StringTransformer(String matchingRegEx,
                             String transformationRegEx)
    {
        this.matchingRegEx       = matchingRegEx;
        this.transformationRegEx = transformationRegEx;
        this.matchingPattern     = Pattern.compile(matchingRegEx);
    }


    /**
     * Returns if the specified string may be transformed with this {@link StringTransformer}.
     *
     * @param string The string to check for a match
     *
     * @return <code>true</code> if the string may be transformed by this {@link StringTransformer},
     *         <code>false</code> otherwise.
     */
    public boolean isTransformable(String string)
    {
        return string.matches(matchingRegEx);
    }


    /**
     * Returns the transformed string given a string using the regular pattern expressions provided when creating
     * the {@link StringTransformer}.
     *
     * @param string The string to transform
     *
     * @return The transformed string from the provided string
     */
    public String transform(String string)
    {
        Matcher matcher = matchingPattern.matcher(string);

        return matcher.replaceAll(transformationRegEx);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("StringTransformer{matchingRegEx=%s, transformationRegEx=%s}",
                             matchingRegEx,
                             transformationRegEx);
    }
}
