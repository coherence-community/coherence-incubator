/*
 * File: ConfigurationException.java
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

/**
 * A {@link ConfigurationException} captures information concerning an invalid configuration of Coherence.
 * Specifically it details what the problem was and advice on resolving the issue.  Optionally
 * a {@link ConfigurationException} may be include the causing {@link Exception}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class ConfigurationException extends Exception
{
    /**
     * The problem that occurred.
     */
    private String problem;

    /**
     * Advice for resolving the issue.
     */
    private String advice;


    /**
     * Standard Constructor.
     *
     * @param problem the problem that occured
     * @param advice the advice to fix the problem
     */
    public ConfigurationException(String problem,
                                  String advice)
    {
        this.problem = problem;
        this.advice  = advice;
    }


    /**
     * Standard Constructor (with cause).
     *
     * @param problem the problem that occured
     * @param advice the advice to fix the problem
     * @param cause the {@link Throwable} causing the problem
     */
    public ConfigurationException(String    problem,
                                  String    advice,
                                  Throwable cause)
    {
        super(cause);
        this.problem = problem;
        this.advice  = advice;
    }


    /**
     * Returns what the problem was.
     *
     * @return A string detailing the problem
     */
    public String getProblem()
    {
        return problem;
    }


    /**
     * Returns advice to resolve the issue.
     *
     * @return A string detailing advice to resolve the issue
     */
    public String getAdvice()
    {
        return advice;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage()
    {
        String result = "";

        result += "Configuration Exception\n";
        result += "-----------------------\n";
        result += "Problem   : " + getProblem() + "\n";
        result += "Advice    : " + getAdvice() + "\n";

        if (this.getCause() != null)
        {
            result += "Caused By : " + getCause().toString() + "\n";
        }

        return result;
    }
}
