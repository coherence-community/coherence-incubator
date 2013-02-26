/*
 * File: DefaultTaskProcessorDefinitionManager.java
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

import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.dependencies.DependencyReference;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.dependencies.DependentResourceReference;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.task.TaskProcessorType;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.tangosol.io.pof.PortableException;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.UID;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalRemove;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link DefaultTaskProcessorDefinitionManager} registers
 * {@link TaskProcessorDefinition}s in the cache and creates
 * {@link TaskProcessorMediator} objects for TaskProcessors of type SINGLE.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class DefaultTaskProcessorDefinitionManager implements TaskProcessorDefinitionManager
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(DefaultTaskProcessorDefinitionManager.class.getName());

    /**
     * TaskProcessorDefinition named cache.
     */
    private NamedCache taskProcessorDefinitionCache;

    /**
     * We need a reference to the ConfigurableCacheFactory.
     */
    private final ConfigurableCacheFactory configurableCacheFactory;

    /**
     * A reference to the {@link Environment}.
     */
    private Environment environment;

    /**
     * Indicating whether this {@link DefaultTaskProcessorDefinitionManager} has started.
     */
    private volatile boolean started;

    /**
     * The list of {@link TaskProcessorDefinition}s that will be registered as we start.
     */
    private LinkedList<TaskProcessorDefinition> tobeRegistered;


    /**
     * Standard constructor.
     *
     * @param environment the {@link Environment} to use
     * @param configurableCacheFactory the {@link ConfigurableCacheFactory} to use
     */
    public DefaultTaskProcessorDefinitionManager(Environment              environment,
                                                 ConfigurableCacheFactory configurableCacheFactory)
    {
        this.configurableCacheFactory = configurableCacheFactory;
        tobeRegistered                = new LinkedList<TaskProcessorDefinition>();
        this.environment              = environment;
    }


    /**
     * {@inheritDoc}
     */
    public void registerTaskProcessor(TaskProcessorDefinition definition) throws Throwable
    {
        if (!started)
        {
            tobeRegistered.add(definition);
        }
        else
        {
            storeTaskProcessorDefinition(definition);
        }
    }


    /**
     * Stores a {@link TaskProcessorDefinition} in the TaskProcessorDefinitionCache.
     *
     * @param definition the {@link TaskProcessorDefinition}
     *
     * @throws Throwable if the store fails.
     */
    @SuppressWarnings("unchecked")
    private void storeTaskProcessorDefinition(TaskProcessorDefinition definition) throws Throwable
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Storing TaskProcessorDefinition {0}", definition);
        }

        Filter filter = new NotFilter(PresentFilter.INSTANCE);

        taskProcessorDefinitionCache.invoke(definition.getIdentifier(), new ConditionalPut(filter, definition));

        if (definition.getTaskProcessorType() == TaskProcessorType.SINGLE)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Starting SINGLE TaskProcessor for definition {0}", definition);
            }

            ObjectProxyFactory<Submission> submissionProxyFactory =
                (ObjectProxyFactory<Submission>) environment.getResource(Submission.class);
            ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory =
                (ObjectProxyFactory<SubmissionResult>) environment.getResource(SubmissionResult.class);
            ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorProxyFactory =
                (ObjectProxyFactory<TaskProcessorMediator>) environment.getResource(TaskProcessorMediator.class);
            ClientLeaseMaintainer    clientLeaseMaintainer = environment.getResource(ClientLeaseMaintainer.class);

            TaskProcessorMediatorKey key = new TaskProcessorMediatorKey(definition.getIdentifier(), 0, new UID());

            try
            {
                TaskProcessorMediator tps = taskProcessorMediatorProxyFactory.createRemoteObjectIfNotExists(key,
                                                                                                            DefaultTaskProcessorMediator.class,
                                                                                                            new Object[] {
                                                                                                                key,
                                                                                                                definition
                                                                                                                    .getAttributeMap()});
                CacheService cs = taskProcessorMediatorProxyFactory.getNamedCache().getCacheService();

                if (cs instanceof DistributedCacheService)
                {
                    // We know now that we are on a cluster node
                    tps.setAttribute("machinename", CacheFactory.ensureCluster().getLocalMember().getMachineName());
                    tps.setAttribute("hostname",
                                     CacheFactory.ensureCluster().getLocalMember().getAddress().getHostName());
                    tps.setAttribute("rackname", CacheFactory.ensureCluster().getLocalMember().getRackName());
                    tps.setAttribute("membername", CacheFactory.ensureCluster().getLocalMember().getMemberName());
                    tps.setAttribute("processname", CacheFactory.ensureCluster().getLocalMember().getProcessName());
                    tps.setAttribute("rolename", CacheFactory.ensureCluster().getLocalMember().getRoleName());
                    tps.setAttribute("taskprocessortype", "single");
                }

                definition.getTaskProcessor().onStartup(tps,
                                                        key,
                                                        submissionProxyFactory,
                                                        submissionResultProxyFactory,
                                                        taskProcessorMediatorProxyFactory,
                                                        clientLeaseMaintainer);
            }
            catch (PortableException e)
            {
                if (e.getName().equals("Portable(java.lang.IllegalStateException)"))
                {
                    logger.log(Level.SEVERE,
                               "A TaskProcessor with the ID {0} already exists (SINGLE)",
                               definition.getIdentifier());

                    throw new RuntimeException(e);
                }
                else
                {
                    logger.log(Level.SEVERE,
                               "A TaskProcessor with the ID {0} wasn't possible to create due to exception {1}",
                               new Object[] {definition.getIdentifier(), e});

                    throw new RuntimeException(e);

                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterTaskProcessor(TaskProcessorDefinition definition)
    {
        taskProcessorDefinitionCache.invoke(definition.getIdentifier(), new ConditionalRemove(PresentFilter.INSTANCE));

    }


    /**
     * {@inheritDoc}
     */
    public Set<DependencyReference> getDependencyReferences()
    {
        Set<DependencyReference> dependencies = new HashSet<DependencyReference>();

        dependencies
            .add(new DependentResourceReference((DependentResource) environment
                .getResource(TaskProcessorMediator.class)));
        dependencies.add(new DependentResourceReference((DependentResource) environment.getResource(Submission.class)));
        dependencies
            .add(new DependentResourceReference((DependentResource) environment.getResource(SubmissionResult.class)));

        return dependencies;
    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesSatisfied(Environment environment)
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "DefaultTask Processor Definition Manager starting");
        }

        taskProcessorDefinitionCache = configurableCacheFactory.ensureCache(DefaultTaskProcessorDefinition.CACHENAME,
                                                                            null);

        Iterator<TaskProcessorDefinition> iter = tobeRegistered.iterator();

        while (iter.hasNext())
        {
            try
            {
                storeTaskProcessorDefinition(iter.next());
            }
            catch (Throwable e)
            {
                logger.log(Level.SEVERE, "Could not store TaskProcessorDefinition", e);
            }
        }

        started = true;
        environment.getResource(EventDispatcher.class)
            .dispatchEvent(new LifecycleStartedEvent<DependentResource>(this));

    }


    /**
     * {@inheritDoc}
     */
    public void onDependenciesViolated(Environment environment)
    {
        taskProcessorDefinitionCache = null;
    }
}
