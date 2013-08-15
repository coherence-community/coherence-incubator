/*
 * File: RecoverTasks.java
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

package com.oracle.coherence.patterns.processing.internal.task;

import com.oracle.coherence.common.processors.InvokeMethodProcessor;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmission;
import com.oracle.coherence.patterns.processing.internal.DefaultSubmissionResult;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a {@link Runnable} being launched whenever a {@link TaskProcessorMediator} has an expired
 * {@link com.oracle.coherence.common.leasing.Lease} for a
 * {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}. Typically this happens in case of a failure in a
 * node where a {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} is running.
 * <p>
 * The current accepted tasks for the {@link TaskProcessorMediator} are reset and redispatched.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class RecoverTasks implements Runnable
{
    /**
     * The {@link Logger} to use.
     */
    private static final Logger logger = Logger.getLogger(RecoverTasks.class.getName());

    /**
     * The tasks to recover.
     */
    private LinkedList<SubmissionKeyPair> tasks;

    /**
     * The {@link ConfigurableCacheFactory} to use.
     */
    private ConfigurableCacheFactory ccFactory;

    /**
     * The {@link NamedCache} where {@link com.oracle.coherence.patterns.processing.internal.SubmissionResult}s are
     * stored.
     */
    private NamedCache submissionResultCache;

    /**
     * The {@link NamedCache} where {@link com.oracle.coherence.patterns.processing.internal.Submission}s are stored.
     */
    private NamedCache submissionCache;


    /**
     * Standard constructor.
     *
     * @param tasks the Tasks to recover
     */
    public RecoverTasks(LinkedList<SubmissionKeyPair> tasks)
    {
        this.tasks = tasks;
        this.ccFactory =
            CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(this.getClass().getClassLoader());
        this.submissionResultCache = ccFactory.ensureCache(DefaultSubmissionResult.CACHENAME, null);
        this.submissionCache       = ccFactory.ensureCache(DefaultSubmission.CACHENAME, null);

    }


    /**
     * {@inheritDoc}
     */
    public void run()
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Starting recovery of {0,number} tasks.", tasks.size());
        }

        for (SubmissionKeyPair keyPair : tasks)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Recovering {0}", keyPair);
            }

            submissionResultCache.invoke(keyPair.getResultId(), new InvokeMethodProcessor("retry", new Object[]
            {
            }));

            Object result = submissionCache.invoke(keyPair.getKey(),
                                                   new InvokeMethodProcessor("reDispatch", new Object[] {0L}));

            if (result != null)
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Redispatched {0}", keyPair.getKey());
                }
            }
            else
            {
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE, "Unable to redispatch {0}", keyPair.getKey());
                }
            }
        }
    }
}
