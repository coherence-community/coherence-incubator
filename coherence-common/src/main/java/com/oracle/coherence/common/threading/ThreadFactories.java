/*
 * File: ThreadFactories.java
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

package com.oracle.coherence.common.threading;

import java.util.concurrent.ThreadFactory;

/**
 * The {@link ThreadFactories} class provides a simple mechanism to
 * provide parameterized {@link ThreadFactory}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ThreadFactories
{
    /**
     * <p>A helper to create a {@link ThreadFactory} based on the following parameters.</p>
     *
     * @param isDaemon Should the {@link ThreadFactory} produce daemon threads.
     * @param threadName The threadName of the produced threads.
     * @param threadGroup The threadGroup for the produced threads.
     *
     * @return A {@link ThreadFactory}
     */
    public static ThreadFactory newThreadFactory(final boolean     isDaemon,
                                                 final String      threadName,
                                                 final ThreadGroup threadGroup)
    {
        return new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                Thread thread = new Thread(threadGroup, r);

                thread.setDaemon(isDaemon);
                thread.setName(threadName + ":" + thread.getName());

                return thread;
            }
        };
    }
}
