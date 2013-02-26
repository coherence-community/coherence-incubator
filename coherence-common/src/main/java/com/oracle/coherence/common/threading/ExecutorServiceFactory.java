/*
 * File: ExecutorServiceFactory.java
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ExecutorServiceFactory} to create different {@link ExecutorService} variants.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class ExecutorServiceFactory
{
    /**
     * A fixed thread pool ExecutorService.
     *
     * @param nThreads the number of threads in the pool
     *
     * @return the {@link ExecutorService}
     */
    public static ExecutorService newFixedThreadPool(int nThreads)
    {
        return new LogExceptionThreadPoolExecutor(nThreads,
                                                  nThreads,
                                                  0L,
                                                  TimeUnit.MILLISECONDS,
                                                  new LinkedBlockingQueue<Runnable>());
    }


    /**
     * A fixed thread pool ExecutorService.
     *
     * @param nThreads      the number of threads in the pool
     * @param threadFactory the {@link ThreadFactory} to use
     *
     * @return the {@link ExecutorService}
     */
    public static ExecutorService newFixedThreadPool(int           nThreads,
                                                     ThreadFactory threadFactory)
    {
        return new LogExceptionThreadPoolExecutor(nThreads,
                                                  nThreads,
                                                  0L,
                                                  TimeUnit.MILLISECONDS,
                                                  new LinkedBlockingQueue<Runnable>(),
                                                  threadFactory);
    }


    /**
     * A single thread {@link ExecutorService}.
     *
     * @return the {@link ExecutorService}
     */
    public static ExecutorService newSingleThreadExecutor()
    {
        return new LogExceptionThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }


    /**
     * A single thread {@link ExecutorService}.
     *
     * @param threadFactory the {@link ThreadFactory} to use
     *
     * @return the {@link ExecutorService}
     */
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory)
    {
        return new LogExceptionThreadPoolExecutor(1,
                                                  1,
                                                  0L,
                                                  TimeUnit.MILLISECONDS,
                                                  new LinkedBlockingQueue<Runnable>(),
                                                  threadFactory);
    }


    /**
     * A fixed thread pool ScheduledExecutorService.
     *
     * @param nThreads the number of threads in the pool
     *
     * @return the {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newScheduledThreadPool(int nThreads)
    {
        return new LogExceptionScheduledThreadPoolExecutor(nThreads);
    }


    /**
     * A fixed thread pool ScheduledExecutorService.
     *
     * @param nThreads      the number of threads in the pool
     * @param threadFactory the {@link ThreadFactory} to use
     *
     * @return the {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newScheduledThreadPool(int           nThreads,
                                                                  ThreadFactory threadFactory)
    {
        return new LogExceptionScheduledThreadPoolExecutor(nThreads, threadFactory);
    }


    /**
     * A single thread {@link ScheduledExecutorService}.
     *
     * @return the {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor()
    {
        return new LogExceptionScheduledThreadPoolExecutor(1);
    }


    /**
     * A single thread {@link ScheduledExecutorService}.
     *
     * @param threadFactory the {@link ThreadFactory} to use
     *
     * @return the {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory)
    {
        return new LogExceptionScheduledThreadPoolExecutor(1, threadFactory);
    }
}
