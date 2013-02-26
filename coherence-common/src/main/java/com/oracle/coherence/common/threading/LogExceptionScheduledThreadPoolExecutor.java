/*
 * File: LogExceptionScheduledThreadPoolExecutor.java
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link LogExceptionScheduledThreadPoolExecutor} is a ThreadPoolExecutor that logs the potential
 * exceptions being thrown during the execution of a Runnable.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class LogExceptionScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements ObservableExecutor
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(LogExceptionScheduledThreadPoolExecutor.class.getName());

    /**
     * The callback method to call before starting and after completion of a Runnable.
     */
    private ExecutorListener callback;


    /**
     * Standard constructor.
     *
     * @param corePoolSize      the default number of threads
     */
    public LogExceptionScheduledThreadPoolExecutor(int corePoolSize)
    {
        super(corePoolSize);
    }


    /**
     * Standard constructor.
     *
     * @param corePoolSize      the default number of threads
     * @param threadFactory     the ThreadFactory to use
     */
    public LogExceptionScheduledThreadPoolExecutor(int           corePoolSize,
                                                   ThreadFactory threadFactory)
    {
        super(corePoolSize, threadFactory);
    }


    /**
     * Called after the execution of a Runnable. Checks if an exception was thrown and logs it in that case.
     * @param r   the Runnable
     * @param t   the Throwable
     */
    protected void afterExecute(Runnable  r,
                                Throwable t)
    {
        super.afterExecute(r, t);

        if (t == null && r instanceof Future<?>)
        {
            try
            {
                if (((Future<?>) r).isDone())
                {
                    ((Future<?>) r).get();
                }
            }
            catch (CancellationException ce)
            {
                t = ce;
            }
            catch (ExecutionException ee)
            {
                t = ee.getCause();
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();    // ignore/reset
            }
            catch (Throwable th)
            {
                t = th;
            }
        }

        if (callback != null)
        {
            callback.afterExecute(r, t);
        }

        if (t != null)
        {
            logger.log(Level.SEVERE, "Exception {0} thrown during execution of {1}.", new Object[] {t, r});

            StringWriter resultStringWriter = new StringWriter();
            PrintWriter  pw                 = new PrintWriter(resultStringWriter);

            t.printStackTrace(pw);
            logger.log(Level.SEVERE, "{0}", resultStringWriter.toString());
        }
    }


    /**
     * Called before the execution of a Runnable.
     *
     * @param t   the {@link Thread}
     * @param r   the {@link Runnable}
     */
    @Override
    protected void beforeExecute(Thread   t,
                                 Runnable r)
    {
        super.beforeExecute(t, r);

        if (callback != null)
        {
            callback.beforeExecute(r);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void setCallback(ExecutorListener callback)
    {
        this.callback = callback;
    }
}
