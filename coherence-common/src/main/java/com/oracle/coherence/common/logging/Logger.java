/*
 * File: Logger.java
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

package com.oracle.coherence.common.logging;

import com.tangosol.net.CacheFactory;

/**
 * A simple wrapper around the Coherence logging framework
 * (to avoid polluting source code with {@link CacheFactory} calls).
 *
 * <strong>NOTICE:</strong> This class may not be supported in the future.
 * All logging should now use the java.util.logging framework, for which there
 * is now a {@link CoherenceLogHandler}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public class Logger
{
    /**
     * The ALWAYS level constant.
     */
    public static final int ALWAYS = CacheFactory.LOG_ALWAYS;

    /**
     * The ERROR level constant.
     */
    public static final int ERROR = CacheFactory.LOG_ERR;

    /**
     * The WARN level constant.
     */
    public static final int WARN = CacheFactory.LOG_WARN;

    /**
     * The INFO level constant.
     */
    public static final int INFO = CacheFactory.LOG_INFO;

    /**
     * The DEBUG level constant.
     */
    public static final int DEBUG = CacheFactory.LOG_DEBUG;

    /**
     * The QUIET level constant.
     */
    public static final int QUIET = CacheFactory.LOG_QUIET;


    /**
     * Determines if the specified logging severity level is enabled.
     *
     * @param severity the level to check
     *
     * @return true if the specified severity level is enabled
     */
    public static final boolean isEnabled(int severity)
    {
        return CacheFactory.isLogEnabled(severity);
    }


    /**
     * Logs the specifically (Java 5+) formatted string at the specified level of severity.
     *
     * @param severity the severity level to log
     * @param format   the format of the log message
     * @param params   parameters to the formatted log message
     */
    public static void log(int       severity,
                           String    format,
                           Object... params)
    {
        CacheFactory.log(String.format(format, params), severity);
    }
}
