/*
 * File: DefaultTaskDispatcher.java
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

package com.oracle.coherence.patterns.processing.dispatchers.task;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.dispatchers.AbstractDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchOutcome;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.oracle.coherence.patterns.processing.internal.Submission;
import com.oracle.coherence.patterns.processing.internal.SubmissionResult;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.internal.task.DefaultTaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediator;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorKey;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorMediatorProxyMBean;
import com.oracle.coherence.patterns.processing.internal.task.TaskProcessorStateEnum;
import com.oracle.coherence.patterns.processing.task.Task;
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.oracle.coherence.patterns.processing.task.TaskProcessorDefinition;
import com.oracle.coherence.patterns.processing.task.TaskProcessorType;
import com.oracle.coherence.patterns.processing.taskprocessor.ClientLeaseMaintainer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link DefaultTaskDispatcher} is the main {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}
 * for Tasks. It needs to be registered with the processing pattern. The Dispatching policy can be customized by
 * supplying a {@link TaskDispatchPolicy}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class DefaultTaskDispatcher extends AbstractDispatcher implements ExternalizableLite,
                                                                         PortableObject,
                                                                         DefaultTaskDispatcherMBean
{
    /**
     * The number of retries of TaskProcessorSelection.
     */
    private static final int NO_SELECTION_RETRIES = 3;

    /**
     * A {@link ClientLeaseMaintainer} heartbeats (extends) the {@link com.oracle.coherence.common.leasing.Lease} that
     * this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} is dependent on.
     */
    private static ClientLeaseMaintainer clientLeaseMaintainer;

    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(DefaultTaskDispatcher.class.getName());

    /**
     * The {@link NamedCache} for {@link TaskProcessorMediator}s.
     */
    private transient NamedCache taskProcessorMediatorCache;

    /**
     * A map to keep track of {@link TaskProcessorMediator}s.
     */
    private ConcurrentHashMap<TaskProcessorMediatorKey, TaskProcessorMediator> taskProcessorMediators;

    /**
     * The policy for task dispatching.
     */
    private TaskDispatchPolicy taskDispatchPolicy;

    /**
     * The cache for {@TaskProcessorDefinition}s.
     */
    private transient NamedCache taskProcessorDefinitionCache;

    /**
     * The local list of {@TaskProcessorDefinition}s.
     */
    private transient ConcurrentHashMap<Identifier, TaskProcessorDefinition> taskProcessorDefinitionList;

    /**
     * A list of local {@link TaskProcessors} on this JVM.
     */
    private transient LinkedBlockingQueue<TaskProcessor> localTaskProcessors;

    /**
     * The {@link MapListener} for {@link TaskProcessorMediator} changes.
     */
    private transient MapListener taskProcessorMediatorListener;

    /**
     * The {@link MapListener} for {@link TaskProcessorDefinition} changes.
     */
    private transient MapListener taskProcessorDefinitionListener;

    /**
     * The {@link ObjectProxyFactory} for {@link SubmissionResult}s.
     */
    private transient ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory;

    /**
     * The {@link ObjectProxyFactory} for {@link Submission}s.
     */
    private transient ObjectProxyFactory<Submission> submissionProxyFactory;

    /**
     * The {@link ObjectProxyFactory} for {@link TaskProcessorMediator}s.
     */
    private transient ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorFactory;

    /**
     * The {@link ObjectProxyFactory} for {@link TaskProcessorMediatorProxyMBean}s.
     */
    private transient ObjectProxyFactory<TaskProcessorMediatorProxyMBean> taskProcessorMediatorMBeanFactory;

    /**
     * The number of Submissions offered to this {@link DefaultTaskDispatcher}.
     */
    private transient int noOfferedSubmissions;

    /**
     * The number of Submissions accepted by this {@link DefaultTaskDispatcher}.
     */
    private transient int noAcceptedSubmissions;

    /**
     * The {@link Environment} for this {@link TaskDispatcher}.
     */
    private transient Environment environment;


    /**
     * The default constructor as required by PortableObject and ExternalizableLite.
     */
    public DefaultTaskDispatcher()
    {
        taskProcessorDefinitionList = new ConcurrentHashMap<Identifier, TaskProcessorDefinition>();
        localTaskProcessors         = new LinkedBlockingQueue<TaskProcessor>();
        taskProcessorMediators      = new ConcurrentHashMap<TaskProcessorMediatorKey, TaskProcessorMediator>();
    }


    /**
     * A constructor taking the name and the dispatch policy as input.
     *
     * @param environment the {@link Environment} to use
     * @param name name of the dispatcher
     * @param taskdispatchpolicy the {@link TaskDispatchPolicy}
     */
    public DefaultTaskDispatcher(Environment              environment,
                                 final String             name,
                                 final TaskDispatchPolicy taskdispatchpolicy)
    {
        super(name);
        this.environment            = environment;
        taskDispatchPolicy          = taskdispatchpolicy;
        taskProcessorDefinitionList = new ConcurrentHashMap<Identifier, TaskProcessorDefinition>();
        localTaskProcessors         = new LinkedBlockingQueue<TaskProcessor>();
        taskProcessorMediators      = new ConcurrentHashMap<TaskProcessorMediatorKey, TaskProcessorMediator>();
    }


    /**
     * A constructor taking the name and the dispatch policy as input.
     *
     * @param environment the {@link Environment} to use
     * @param name name of the dispatcher
     * @param taskdispatchpolicy the {@link TaskDispatchPolicy}
     * @param taskProcessorMediatorFactory the {@link ObjectProxyFactory} for {@link TaskProcessorMediator}s to use
     * @param submissionResultProxyFactory the {@link ObjectProxyFactory} for {@link SubmissionResult}s to use
     * @param submissionProxyFactory the {@link ObjectProxyFactory} for {@link Submission}s to use
     */
    public DefaultTaskDispatcher(Environment                               environment,
                                 final String                              name,
                                 final TaskDispatchPolicy                  taskdispatchpolicy,
                                 ObjectProxyFactory<TaskProcessorMediator> taskProcessorMediatorFactory,
                                 ObjectProxyFactory<SubmissionResult>      submissionResultProxyFactory,
                                 ObjectProxyFactory<Submission>            submissionProxyFactory)
    {
        this(environment, name, taskdispatchpolicy);
        this.taskProcessorMediatorFactory = taskProcessorMediatorFactory;
        this.submissionProxyFactory       = submissionProxyFactory;
        this.submissionResultProxyFactory = submissionResultProxyFactory;
    }


    /**
     * Sets the static {@link ClientLeaseMaintainer} for this class.
     *
     * @param clientLeaseMaintainer the {@link ClientLeaseMaintainer} to use.
     */
    public static void setClientLeaseMaintainer(ClientLeaseMaintainer clientLeaseMaintainer)
    {
        DefaultTaskDispatcher.clientLeaseMaintainer = clientLeaseMaintainer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown(final DispatchController dispatchController)
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Shutting down DefaultTaskDispatcher");
        }

        // unregisterMBean();
        taskProcessorMediatorCache.removeMapListener(taskProcessorMediatorListener);
        taskProcessorDefinitionCache.removeMapListener(taskProcessorDefinitionListener);

        Iterator<TaskProcessor> iter = localTaskProcessors.iterator();

        while (iter.hasNext())
        {
            ((TaskProcessor) iter.next()).onShutdown();
        }

        super.onShutdown(dispatchController);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartup(final DispatchController dispatchController)
    {
        super.onStartup(dispatchController);
        initialize(dispatchController.getConfigurableCacheFactory());
        registerMBean();
    }


    /**
     * {@inheritDoc}
     */
    public DispatchOutcome dispatch(final PendingSubmission oPendingProcess)
    {
        noOfferedSubmissions++;

        Object payload = oPendingProcess.getPayload();

        if (payload instanceof Task)
        {
            // submit the pending process to the dispatching policy
            // execute
            if (dispatchTask(oPendingProcess, (Task) payload))
            {
                noAcceptedSubmissions++;

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Accepting submission {0}", oPendingProcess.getSubmissionKey());
                }

                return DispatchOutcome.ACCEPTED;
            }
            else
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Rejecting submission {0}", oPendingProcess.getSubmissionKey());
                }

                return DispatchOutcome.REJECTED;
            }
        }
        else
        {
            // we can't handle this type of pending process
            return DispatchOutcome.REJECTED;
        }
    }


    /**
     * Dispatch a task depending on the supplied {@link TaskDispatchPolicy}.
     *
     * @param pendingSubmission the {@link PendingSubmission} to dispatch
     * @param task              the {@link Task} that was extracted from the {@link PendingSubmission}
     *
     * @return whether dispatching was successful
     */
    protected boolean dispatchTask(final PendingSubmission pendingSubmission,
                                   Task                    task)
    {
        if (task != null)
        {
            boolean result = false;

            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "Dispatching task {0}, key {1}",
                           new Object[] {task.toString(), pendingSubmission.getSubmissionKey()});
            }

            // When selecting a TaskProcessor, we will only select from the TaskProcessors we know are ACTIVE
            // at the time of selection, however during the process of selecting, the selection may change
            // so that all the selected TaskProcessors become INACTIVE before the offerTask method is called
            // to cover that case we will retry the selection three times to allow reselection from a current
            // set of ACTIVE TaskProcessors
            int retries = 0;

            while (!result && retries++ < NO_SELECTION_RETRIES)
            {
                if (logger.isLoggable(Level.FINEST))
                {
                    logger.log(Level.FINEST,
                               "Selecting TaskProcessor for id {0} - try {1}",
                               new Object[] {pendingSubmission.getResultIdentifier(), retries});
                }

                // We select from the currently known ACTIVE TaskProcessors
                Map<TaskProcessorMediatorKey, TaskProcessorMediator> selectedSet =
                    taskDispatchPolicy.selectTaskProcessorSet(task,
                                                              pendingSubmission.getSubmissionConfiguration(),
                                                              taskProcessorMediators,
                                                              taskProcessorDefinitionList);

                if (selectedSet != null)
                {
                    if (logger.isLoggable(Level.FINEST))
                    {
                        logger.log(Level.FINEST,
                                   "Dispatching and selecting TaskProcessor from this set: {0}",
                                   selectedSet);
                    }

                    Iterator<TaskProcessorMediatorKey> iter = selectedSet.keySet().iterator();

                    while (iter.hasNext() &&!result)
                    {
                        TaskProcessorMediatorKey taskProcessorKey = iter.next();
                        TaskProcessorMediator taskProcessorMediatorProxy =
                            taskProcessorMediatorFactory.getProxy(taskProcessorKey);

                        // We must first assign it to the key before offering
                        // If not there will be a race between the event resulting
                        // from OfferTask
                        // and the assign statement after the offer
                        // this leaves a small window where the SubmissionResult may think it is assigned
                        // but the TaskProcessor doesn't know about it
                        // if the owner is already assigned, we will assert that the TaskProcessor still knows about
                        // the Submission
                        SubmissionResult submissionResult =
                            submissionResultProxyFactory.getProxy(pendingSubmission.getResultIdentifier());
                        Object alreadyAssignedOwner = submissionResult.assign(taskProcessorKey);

                        if (alreadyAssignedOwner == null)
                        {
                            result = taskProcessorMediatorProxy.offerTask(pendingSubmission.getSubmissionKey(),
                                                                          pendingSubmission.getResultIdentifier());

                            if (!result)
                            {
                                // We speculatively assigned it to the TaskProcessor
                                // and we need to clear
                                // the assignment if it wasn't accepted.
                                setSubmissionToRetryStatus(pendingSubmission, taskProcessorKey, submissionResult);
                            }
                            else
                            {
                                // This is the success case, the Submission was successfully assigned to a TaskProcessor
                                if (logger.isLoggable(Level.FINER))
                                {
                                    logger.log(Level.FINER,
                                               "TaskProcessor {0} accepted submission {1}",
                                               new Object[] {taskProcessorKey, pendingSubmission.getSubmissionKey()});
                                }

                                break;    // Don't iterate again
                            }
                        }
                        else
                        {
                            // In this case there is already an assigned owner, we will make sure this
                            // owner still knows about this
                            if (logger.isLoggable(Level.INFO))
                            {
                                logger.log(Level.INFO,
                                           "Task {0} with result id {1} was already assigned.",
                                           new Object[] {pendingSubmission.getSubmissionKey(),
                                                         pendingSubmission.getResultIdentifier()});
                            }

                            // We will offer the task again to the owner, just to make sure it didn't fall through the cracks
                            // offerTask needs to be idempotent
                            TaskProcessorMediator existingOwnerProxy =
                                taskProcessorMediatorFactory.getProxy(alreadyAssignedOwner);
                            boolean offerTaskResult = existingOwnerProxy.offerTask(pendingSubmission.getSubmissionKey(),
                                                                                   pendingSubmission
                                                                                       .getResultIdentifier());

                            if (!offerTaskResult)
                            {
                                setSubmissionToRetryStatus(pendingSubmission, taskProcessorKey, submissionResult);
                            }
                            else
                            {
                                // In this case the owner accepted and all is well
                                result = true;
                                break;    // Don't iterate again
                            }
                        }
                    }
                }
                else
                {
                    if (logger.isLoggable(Level.SEVERE))
                    {
                        logger.log(Level.SEVERE, "The TaskDispatchPolicy returned no TaskProcessorMediators");
                    }
                }
            }

            return result;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "The Task was null, for the Id:{0}", pendingSubmission.getResultIdentifier());
            }

            return false;
        }
    }


    private void setSubmissionToRetryStatus(final PendingSubmission  pendingSubmission,
                                            TaskProcessorMediatorKey taskProcessorKey,
                                            SubmissionResult         submissionResult)
    {
        boolean retryResult = submissionResult.retry();

        if (retryResult)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER,
                           "TaskProcessor {0} rejected submission {1}",
                           new Object[] {taskProcessorKey, pendingSubmission.getSubmissionKey()});
            }
        }
        else
        {
            if (logger.isLoggable(Level.SEVERE))
            {
                logger.log(Level.SEVERE,
                           "TaskProcessor {0} rejected submission {1}, failed to restore owner",
                           new Object[] {taskProcessorKey, pendingSubmission.getSubmissionKey()});
            }
        }
    }


    /**
     * Initialization of the {@link DefaultTaskDispatcher}.
     *
     * @param oCCFactory the {@link ConfigurableCacheFactory} to use
     *
     */
    @SuppressWarnings("unchecked")
    private void initialize(final ConfigurableCacheFactory oCCFactory)
    {
        this.environment = (Environment) oCCFactory;
        submissionResultProxyFactory =
            (ObjectProxyFactory<SubmissionResult>) environment.getResource(SubmissionResult.class);
        submissionProxyFactory = (ObjectProxyFactory<Submission>) environment.getResource(Submission.class);
        taskProcessorMediatorFactory =
            (ObjectProxyFactory<TaskProcessorMediator>) environment.getResource(TaskProcessorMediator.class);
        taskProcessorMediatorCache = oCCFactory.ensureCache(DefaultTaskProcessorMediator.CACHENAME, null);
        taskProcessorMediatorMBeanFactory =
            new ObjectProxyFactory<TaskProcessorMediatorProxyMBean>(DefaultTaskProcessorMediator.CACHENAME,
                                                                    TaskProcessorMediatorProxyMBean.class);
        taskProcessorMediatorMBeanFactory.onDependenciesSatisfied(environment);
        initializeTaskProcessorMediatorListener();
        taskProcessorDefinitionCache = oCCFactory.ensureCache(DefaultTaskProcessorDefinition.CACHENAME, null);
        initializeTaskProcessorDefinitionListener(oCCFactory);
    }


    /**
     * Initializes the {@link MapListener} to the {@link TaskProcessorDefinition}s.
     *
     * @param oCCFactory the {@link ConfigurableCacheFactory} to use
     */
    @SuppressWarnings("unchecked")
    private void initializeTaskProcessorDefinitionListener(final ConfigurableCacheFactory oCCFactory)
    {
        taskProcessorDefinitionCache.addMapListener(taskProcessorDefinitionListener = new MultiplexingMapListener()
        {
            @Override
            protected void onMapEvent(final MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED)
                {
                    final TaskProcessorDefinition taskProcessorDefinition =
                        (TaskProcessorDefinition) mapEvent.getNewValue();

                    try
                    {
                        taskProcessorDefinitionList.put(taskProcessorDefinition.getIdentifier(),
                                                        taskProcessorDefinition);

                        if (logger.isLoggable(Level.INFO))
                        {
                            logger.log(Level.INFO,
                                       "Registering TaskProcessorDefinition {0}",
                                       taskProcessorDefinition.toString());
                        }

                        handleTaskProcessorDefinition(oCCFactory, taskProcessorDefinition);
                    }
                    catch (Throwable e)
                    {
                        if (logger.isLoggable(Level.SEVERE))
                        {
                            logger.log(Level.SEVERE,
                                       "Could not handle TaskProcessorDefinition {0} exception {1}",
                                       new Object[] {taskProcessorDefinition, e});
                        }
                    }
                }
                else if (mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                {
                    final TaskProcessorDefinition oTaskProcessorDefinition =
                        (TaskProcessorDefinition) mapEvent.getNewValue();

                    if (logger.isLoggable(Level.INFO))
                    {
                        logger.log(Level.INFO,
                                   "TaskProcessorDefinition {0} updated.",
                                   oTaskProcessorDefinition.toString());
                    }
                }
                else if (mapEvent.getId() == MapEvent.ENTRY_DELETED)
                {
                    final TaskProcessorDefinition taskProcessorDefinition =
                        (TaskProcessorDefinition) mapEvent.getOldValue();

                    taskProcessorDefinitionList.remove(taskProcessorDefinition.getIdentifier());

                    if (logger.isLoggable(Level.INFO))
                    {
                        logger.log(Level.INFO,
                                   "Removing TaskProcessorDefinition {0}",
                                   taskProcessorDefinition.toString());
                    }
                }
            }
        });

        final Iterator<TaskProcessorDefinition> taskProcessorDefinitionIterator =
            taskProcessorDefinitionCache.values().iterator();

        while (taskProcessorDefinitionIterator.hasNext())
        {
            final TaskProcessorDefinition taskProcessorDefinition = taskProcessorDefinitionIterator.next();

            taskProcessorDefinitionList.put(taskProcessorDefinition.getIdentifier(), taskProcessorDefinition);

            try
            {
                handleTaskProcessorDefinition(oCCFactory, taskProcessorDefinition);
            }
            catch (Throwable e)
            {
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE,
                               "Could not handle TaskProcessorDefinition {0} exception {1}",
                               new Object[] {taskProcessorDefinition, e});
                }
            }
        }
    }


    /**
     * Initializes the {@link MapListener} to {@link TaskProcessorMediator}s.
     */
    @SuppressWarnings("unchecked")
    private void initializeTaskProcessorMediatorListener()
    {
        // We synchronize here because we don't want to accept an update
        // until we have installed the listener and added all the
        // existing {@link TaskProcessorMediatorKey}s
        taskProcessorMediatorListener = new MultiplexingMapListener()
        {
            @Override
            protected void onMapEvent(final MapEvent mapEvent)
            {
                if (mapEvent.getId() == MapEvent.ENTRY_INSERTED)
                {
                    final TaskProcessorMediatorKey oTaskProcessorKey = (TaskProcessorMediatorKey) mapEvent.getKey();

                    if (oTaskProcessorKey != null)
                    {
                        if (logger.isLoggable(Level.INFO))
                        {
                            logger.log(Level.INFO, "TaskProcessorMediator inserted {0}", oTaskProcessorKey.toString());
                        }
                    }

                    if (!(taskProcessorMediators.containsKey(oTaskProcessorKey)))
                    {
                        final TaskProcessorMediator taskProcessorMediator =
                            (TaskProcessorMediator) mapEvent.getNewValue();

                        if (taskProcessorMediator.getProcessorState() == TaskProcessorStateEnum.ACTIVE)
                        {
                            taskProcessorMediators.put(oTaskProcessorKey, taskProcessorMediator);
                        }
                    }
                    else
                    {
                        if (logger.isLoggable(Level.SEVERE))
                        {
                            logger.log(Level.SEVERE,
                                       "TaskProcessorMediator can't be inserted twice: {0}",
                                       oTaskProcessorKey.toString());
                        }
                    }
                }
                else if (mapEvent.getId() == MapEvent.ENTRY_UPDATED)
                {
                    final TaskProcessorMediatorKey oTaskProcessorKey = (TaskProcessorMediatorKey) mapEvent.getKey();

                    if (oTaskProcessorKey != null)
                    {
                        TaskProcessorMediator mediator = (TaskProcessorMediator) mapEvent.getNewValue();

                        if (mediator.getProcessorState() == TaskProcessorStateEnum.ACTIVE)
                        {
                            taskProcessorMediators.put(oTaskProcessorKey, mediator);
                        }
                        else
                        {
                            taskProcessorMediators.remove(oTaskProcessorKey);
                        }

                        if (logger.isLoggable(Level.FINER))
                        {
                            logger.log(Level.FINER, "TaskProcessorMediator updated: {0}", oTaskProcessorKey.toString());
                        }
                    }
                }
                else if (mapEvent.getId() == MapEvent.ENTRY_DELETED)
                {
                    final TaskProcessorMediatorKey oTaskProcessorKey = (TaskProcessorMediatorKey) mapEvent.getKey();

                    if (logger.isLoggable(Level.INFO))
                    {
                        logger.log(Level.INFO, "TaskProcessorMediator deleted: {0}", oTaskProcessorKey.toString());
                    }

                    taskProcessorMediators.remove(oTaskProcessorKey);
                }
            }
        };
        taskProcessorMediatorCache.addMapListener(taskProcessorMediatorListener);

        final Iterator<Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator>> iter =
            taskProcessorMediatorCache.entrySet().iterator();

        while (iter.hasNext())
        {
            final Map.Entry<TaskProcessorMediatorKey, TaskProcessorMediator> oTaskProcessorEntry =
                (Entry<TaskProcessorMediatorKey, TaskProcessorMediator>) iter.next();

            if (oTaskProcessorEntry.getValue().getProcessorState() == TaskProcessorStateEnum.ACTIVE)
            {
                taskProcessorMediators.put(oTaskProcessorEntry.getKey(), oTaskProcessorEntry.getValue());
            }
        }
    }


    /**
     * Handle a particular {@link TaskProcessorDefinition}.
     *
     * @param oCCFactory the {@link ConfigurableCacheFactory} to use
     * @param oTaskProcessorDefinition the {@link TaskProcessorDefinition} to handle
     *
     * @throws Throwable if an error occurs
     */
    private void handleTaskProcessorDefinition(final ConfigurableCacheFactory oCCFactory,
                                               final TaskProcessorDefinition  oTaskProcessorDefinition) throws Throwable
    {
        if (oTaskProcessorDefinition.getTaskProcessorType() == TaskProcessorType.GRID)
        {
            // This is a grid taskprocessor that shall
            // be initialized on every node that has localstorage = true
            CacheService cs = taskProcessorMediatorCache.getCacheService();

            if ((cs instanceof DistributedCacheService) && ((DistributedCacheService) cs).isLocalStorageEnabled())
            {
                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "HandleTaskProcessorDefinition: {0}", oTaskProcessorDefinition.toString());
                }

                // We create a {@link TaskProcessorMediatorKey} for this
                // TaskProcessor

                Member member = CacheFactory.getCluster().getLocalMember();
                TaskProcessorMediatorKey key = new TaskProcessorMediatorKey(oTaskProcessorDefinition.getIdentifier(),
                                                                            member.getId(),
                                                                            member.getUid());

                if (logger.isLoggable(Level.FINER))
                {
                    logger.log(Level.FINER, "Created key for TaskProcessorMediator: {0}", key.toString());
                }

                TaskProcessorMediator tps = taskProcessorMediatorFactory.createRemoteObjectIfNotExists(key,
                                                                                                       DefaultTaskProcessorMediator.class,
                                                                                                       new Object[] {
                                                                                                           key,
                                                                                                           oTaskProcessorDefinition
                                                                                                               .getAttributeMap()});

                tps.setAttribute("machinename", CacheFactory.ensureCluster().getLocalMember().getMachineName());
                tps.setAttribute("hostname", CacheFactory.ensureCluster().getLocalMember().getAddress().getHostName());
                tps.setAttribute("rackname", CacheFactory.ensureCluster().getLocalMember().getRackName());
                tps.setAttribute("membername", CacheFactory.ensureCluster().getLocalMember().getMemberName());
                tps.setAttribute("processname", CacheFactory.ensureCluster().getLocalMember().getProcessName());
                tps.setAttribute("rolename", CacheFactory.ensureCluster().getLocalMember().getRoleName());

                if (oTaskProcessorDefinition.getTaskProcessorType() == TaskProcessorType.GRID)
                {
                    tps.setAttribute("taskprocessortype", "grid");
                }
                else
                {
                    tps.setAttribute("taskprocessortype", "single");
                }

                TaskProcessor processor = oTaskProcessorDefinition.getTaskProcessor();

                if (processor != null)
                {
                    localTaskProcessors.add(processor);

                    // seed the processor with the new key
                    processor.onStartup(tps,
                                        key,
                                        submissionProxyFactory,
                                        submissionResultProxyFactory,
                                        taskProcessorMediatorFactory,
                                        clientLeaseMaintainer);
                }
                else
                {
                    if (logger.isLoggable(Level.FINER))
                    {
                        logger.log(Level.FINER, "The attached processor was null");
                    }
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getDispatcherName()
    {
        return getName();
    }


    /**
     * {@inheritDoc}
     */
    public int getSubmissionsAccepted()
    {
        return noAcceptedSubmissions;
    }


    /**
     * {@inheritDoc}
     */
    public int getSubmissionsOffered()
    {
        return noOfferedSubmissions;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final DataInput in) throws IOException
    {
        super.readExternal(in);
        taskDispatchPolicy = (TaskDispatchPolicy) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeObject(out, taskDispatchPolicy);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final PofReader reader) throws IOException
    {
        super.readExternal(reader);
        taskDispatchPolicy = (TaskDispatchPolicy) reader.readObject(100);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeObject(100, taskDispatchPolicy);
    }
}
