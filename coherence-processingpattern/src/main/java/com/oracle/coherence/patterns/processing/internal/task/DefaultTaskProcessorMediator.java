/*
 * File: DefaultTaskProcessorMediator.java
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

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.common.events.processing.EventProcessor;
import com.oracle.coherence.common.events.processing.EventProcessorFactory;
import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.leasing.Lease;
import com.oracle.coherence.common.leasing.Leasing;
import com.oracle.coherence.common.util.ChangeIndication;
import com.oracle.coherence.patterns.processing.internal.SubmissionKey;
import com.oracle.coherence.patterns.processing.internal.SubmissionKeyPair;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link DefaultTaskProcessorMediator} represents the "in grid" state of a particular
 * {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
 *  <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class DefaultTaskProcessorMediator implements ExternalizableLite,
                                                     PortableObject,
                                                     TaskProcessorMediator,
                                                     EventProcessorFactory<Event>,
                                                     TaskProcessorMediatorProxyMBean,
                                                     ChangeIndication
{
    /**
     * The {@link EventProcessor} for this class.
     */
    private static EventProcessor<Event> eventProcessor;

    /**
     * A {@link ServerLeaseMonitor} helps keep track of when the {@link Lease} for the associated
     * {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} expires.
     */
    private static ServerLeaseMonitor leaseMonitor;

    /**
     * The name of the Coherence Cache that will store {@link DefaultTaskProcessorMediator}s.
     */
    public static final String CACHENAME = "coherence.patterns.processing.taskprocessormediator";

    /**
     * The {@link Logger} to use.
     */
    private static final Logger logger = Logger.getLogger(DefaultTaskProcessorMediator.class.getName());

    /**
     * Key for the Executor this queue belongs to.
     */
    private TaskProcessorMediatorKey taskProcessorKey;

    /**
     * A linked list of the tasks to be executed.
     */
    private LinkedList<SubmissionKeyPair> tasks;

    /**
     * A linked list of the tasks that have been claimed for processing.
     */
    private LinkedList<SubmissionKeyPair> tasksInProgress;

    /**
     * The number of accepted tasks.
     */
    private int noAcceptedTasks;

    /**
     * The number of executed tasks.
     */
    private int noExecutedTasks;

    /**
     * The {@link Lease} for the associated {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     */
    private Lease taskProcessorLease;

    /**
     * The state of this {@link com.oracle.coherence.patterns.processing.task.TaskProcessor}.
     */
    private TaskProcessorStateEnum stateEnum;

    /**
     * Keeps track of the total task execution duration.
     */
    private long totalTaskExecutionDuration;

    /**
     * Keeps track of the minimum task execution duration.
     */
    private long minimumTaskExecutionDuration;

    /**
     * Keeps track of the maximum task execution duration.
     */
    private long maximumTaskExecutionDuration;

    /**
     * Keeps track of the last executed tasks duration.
     */
    private long lastTaskExecutionDuration;

    /**
     * The number of times a task has yielded.
     */
    private int yieldTaskCount;

    /**
     *
     */
    @SuppressWarnings("rawtypes")
    private Map attributeMap;

    /**
     * This boolean is used by the ChangeIndication interface implementation
     * to keep track of whether the object really changed. This is an optimization to
     * avoid too many setValue and subsequent BackingMapEvents being generated.
     */
    private boolean changed;


    /**
     * Default constructor as required by PortableObject and ExternalizableLite.
     */
    @SuppressWarnings("rawtypes")
    public DefaultTaskProcessorMediator()
    {
        stateEnum                    = TaskProcessorStateEnum.INACTIVE;
        minimumTaskExecutionDuration = -1;    // -1 means no minimum has been established yet
        attributeMap                 = new HashMap();
    }


    /**
     * Constructor which takes the key of the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} it
     * belongs to.
     *
     * @param taskProcessorKey the {@link TaskProcessorMediatorKey} key
     */
    public DefaultTaskProcessorMediator(final TaskProcessorMediatorKey taskProcessorKey)
    {
        this();
        this.taskProcessorKey = taskProcessorKey;
        initialize();
    }


    /**
     * Constructor which takes the key of the {@link com.oracle.coherence.patterns.processing.task.TaskProcessor} it
     * belongs to.
     *
     * @param taskProcessorKey the {@link TaskProcessorMediatorKey} key
     * @param map a map with attributes
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultTaskProcessorMediator(final TaskProcessorMediatorKey taskProcessorKey,
                                        Map                            map)
    {
        this();
        this.taskProcessorKey = taskProcessorKey;
        attributeMap.putAll(map);
        initialize();
    }


    /**
     * Sets the {@link ServerLeaseMonitor} to use for this class.
     *
     * @param leaseMonitor the {@link ServerLeaseMonitor} to use.
     */
    public static void setLeaseMonitor(ServerLeaseMonitor leaseMonitor)
    {
        DefaultTaskProcessorMediator.leaseMonitor = leaseMonitor;
    }


    /**
     * Returns the {@link EventProcessor} for life cycle events for this class.
     *
     * @return the {@link EventProcessor} for this class
     */
    public static EventProcessor<Event> getEventProcessor()
    {
        return eventProcessor;
    }


    /**
     * Sets the {@link EventProcessor} for life cycle events for this class.
     *
     * @param eventProcessor the {@link EventProcessor} to use
     */
    public static void setEventProcessor(EventProcessor<Event> eventProcessor)
    {
        DefaultTaskProcessorMediator.eventProcessor = eventProcessor;
    }


    /**
     * {@inheritDoc}
     */
    public SubmissionKeyPair dequeueTask()
    {
        if (stateEnum == TaskProcessorStateEnum.ACTIVE)
        {
            if (tasks == null)
            {
                initialize();
            }

            final SubmissionKeyPair taskInProgress = tasks.poll();

            if (taskInProgress != null)
            {
                tasksInProgress.add(taskInProgress);
                setChanged();
            }

            return taskInProgress;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Rejecting DequeueTask because TaskProcessor is INACTIVE");
            }

            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public List<SubmissionKeyPair> drainQueueToBeExecuted()
    {
        if (stateEnum == TaskProcessorStateEnum.ACTIVE)
        {
            if (tasks == null)
            {
                initialize();
            }

            List<SubmissionKeyPair> result = new LinkedList<SubmissionKeyPair>();
            Iterator                iter   = tasks.iterator();

            while (iter.hasNext())
            {
                SubmissionKeyPair tp = (SubmissionKeyPair) iter.next();

                result.add(tp);
                tasksInProgress.add(tp);
            }

            tasks.clear();
            setChanged();

            return result;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Rejecting DequeueTask because TaskProcessor is INACTIVE");
            }

            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void enqueueTask(final SubmissionKey taskId,
                            Identifier          resultid)
    {
        if (tasks == null)
        {
            initialize();
        }

        SubmissionKeyPair keyPair = new SubmissionKeyPair(taskId, resultid);

        if (!tasks.contains(keyPair))
        {
            tasks.add(keyPair);
            noAcceptedTasks++;
            setChanged();
        }
    }


    /**
     * {@inheritDoc}
     */
    public TaskProcessorMediatorKey getTaskProcessorKey()
    {
        return taskProcessorKey;
    }


    /**
     * {@inheritDoc}
     */
    public int numberOfTasksInProgress()
    {
        return tasksInProgress == null ? 0 : tasksInProgress.size();
    }


    /**
     * {@inheritDoc}
     */
    public List<SubmissionKeyPair> getTasksInProgress()
    {
        return tasksInProgress;
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return tasks == null ? 0 : tasks.size();
    }


    /**
     * {@inheritDoc}
     */
    public void taskDone(final SubmissionKey oSubmissionKey,
                         long                executiontime,
                         boolean             yield)
    {
        for (SubmissionKeyPair tp : tasksInProgress)
        {
            if (tp.getKey().equals(oSubmissionKey))
            {
                tasksInProgress.remove(tp);
                break;
            }
        }

        for (SubmissionKeyPair tp : tasks)
        {
            if (tp.getKey().equals(oSubmissionKey))
            {
                tasks.remove(tp);
                break;
            }
        }

        noExecutedTasks++;

        if (yield)
        {
            yieldTaskCount++;
        }

        updateExecutionTiming(executiontime);
        setChanged();
    }


    /**
     * Updates the timing statistics.
     *
     * @param executionDuration the duration of the last execution.
     */
    private void updateExecutionTiming(long executionDuration)
    {
        totalTaskExecutionDuration += executionDuration;

        if (minimumTaskExecutionDuration == -1)
        {
            minimumTaskExecutionDuration = executionDuration;
        }
        else
        {
            if (minimumTaskExecutionDuration > executionDuration)
            {
                minimumTaskExecutionDuration = executionDuration;
            }
        }

        if (executionDuration > maximumTaskExecutionDuration)
        {
            maximumTaskExecutionDuration = executionDuration;
        }

        lastTaskExecutionDuration = executionDuration;
        setChanged();
    }


    /**
     * Method description
     *
     * @return
     */
    @Override
    public String toString()
    {
        return "DefaultTaskProcessorMediator [" + (attributeMap != null ? "attributeMap=" + attributeMap + ", " : "")
               + "lastTaskExecutionDuration=" + lastTaskExecutionDuration + ", maximumTaskExecutionDuration="
               + maximumTaskExecutionDuration + ", minimumTaskExecutionDuration=" + minimumTaskExecutionDuration
               + ", noAcceptedTasks=" + noAcceptedTasks + ", noExecutedTasks=" + noExecutedTasks + ", "
               + (stateEnum != null ? "stateEnum=" + stateEnum + ", " : "")
               + (taskProcessorKey != null ? "taskProcessorKey=" + taskProcessorKey + ", " : "")
               + (taskProcessorLease != null ? "taskProcessorLease=" + taskProcessorLease + ", " : "")
               + (tasks != null ? "tasks=" + tasks + ", " : "")
               + (tasksInProgress != null ? "tasksInProgress=" + tasksInProgress + ", " : "")
               + "totalTaskExecutionDuration=" + totalTaskExecutionDuration + ", yieldTaskCount=" + yieldTaskCount
               + "]";
    }


    /**
     * Internal initialize method.
     */
    private void initialize()
    {
        tasks           = new LinkedList<SubmissionKeyPair>();
        tasksInProgress = new LinkedList<SubmissionKeyPair>();
    }


    /**
     * {@inheritDoc}
     */
    public boolean offerTask(SubmissionKey submissionKey,
                             Identifier    resultid)
    {
        if (stateEnum == TaskProcessorStateEnum.ACTIVE)
        {
            if (tasks == null)
            {
                initialize();
            }

            enqueueTask(submissionKey, resultid);

            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getKey()
    {
        return taskProcessorKey.toString();
    }


    /**
     * {@inheritDoc}
     */
    public int getAcceptedTaskCount()
    {
        return noAcceptedTasks;
    }


    /**
     * {@inheritDoc}
     */
    public int getExecutedTaskCount()
    {
        return noExecutedTasks;
    }


    /**
     * {@inheritDoc}
     */
    public int getYieldTaskCount()
    {
        return yieldTaskCount;
    }


    /**
     * {@inheritDoc}
     */
    public String getIdsOfCurrentlyExecutingTask()
    {
        StringBuilder sResult = new StringBuilder("No task executing");

        if (tasksInProgress != null)
        {
            if (tasksInProgress.size() > 0)
            {
                sResult.setLength(0);
                sResult.append(" InProgress:");

                for (final SubmissionKeyPair tp : tasksInProgress)
                {
                    sResult.append(tp.toString());
                    sResult.append(" ");
                }
            }
        }

        return sResult.toString();
    }


    /**
     * {@inheritDoc}
     */
    public double getAverageTaskExecutionDuration()
    {
        if (noExecutedTasks > 0)
        {
            return totalTaskExecutionDuration / (double) noExecutedTasks;
        }
        else
        {
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getLastTaskExecutionDuration()
    {
        return lastTaskExecutionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public long getMaximumTaskExecutionDuration()
    {
        return maximumTaskExecutionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public long getMinimumTaskExecutionDuration()
    {
        return minimumTaskExecutionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public long getTotalTaskExecutionDuration()
    {
        return totalTaskExecutionDuration;
    }


    /**
     * {@inheritDoc}
     */
    public boolean extendTaskProcessorLease(long duration)
    {
        if (stateEnum == TaskProcessorStateEnum.ACTIVE)
        {
            boolean result = taskProcessorLease.extend(duration);

            leaseMonitor.registerLease(taskProcessorKey, taskProcessorLease);

            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST,
                           "Lease {0} extended with {1} ms for TaskProcessor {2}",
                           new Object[] {taskProcessorLease, duration, this});
            }

            setChanged();

            return result;
        }
        else
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, "Trying to extend lease for TaskProcessor {0} but it is INACTIVE", this);
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public Lease getTaskProcessorLease(long duration)
    {
        taskProcessorLease = Leasing.newLease(duration);
        stateEnum          = TaskProcessorStateEnum.ACTIVE;
        leaseMonitor.registerLease(taskProcessorKey, taskProcessorLease);
        setChanged();

        return taskProcessorLease;
    }


    /**
     * {@inheritDoc}
     */
    public void leaseExpired(Lease lease)
    {
        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST,
                       "Lease {0} expired for TaskProcessor {1} setting to INACTIVE",
                       new Object[] {lease, this});
        }

        stateEnum = TaskProcessorStateEnum.INACTIVE;
        recoverTasks();
        setChanged();
    }


    /**
     * Start the recovery of the tasks for this {@link DefaultTaskProcessorMediator}.
     */
    @SuppressWarnings("unchecked")
    private void recoverTasks()
    {
        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Recovering tasks {0}", tasks);
        }

        LinkedList<SubmissionKeyPair> mergedlist = (LinkedList<SubmissionKeyPair>) tasks.clone();

        mergedlist.addAll(tasksInProgress);

        RecoverTasks task          = new RecoverTasks(mergedlist);
        Thread       recoverthread = new Thread(task);

        recoverthread.start();
        tasks.clear();
        tasksInProgress.clear();
        setChanged();
    }


    /**
     * {@inheritDoc}
     */
    public void entryArrived()
    {
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "TaskProcessorMediator {0} arrived to this member.", this);
        }

        if (taskProcessorLease != null)
        {
            if (stateEnum == TaskProcessorStateEnum.ACTIVE)
            {
                leaseMonitor.registerLease(getTaskProcessorKey(), taskProcessorLease);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void entryDeparted()
    {
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "TaskProcessorMediator {0} departed from this member.", this);
        }

        if (taskProcessorLease != null)
        {
            leaseMonitor.deregisterLease(getTaskProcessorKey());
        }
    }


    /**
     * {@inheritDoc}
     */
    public TaskProcessorStateEnum getProcessorState()
    {
        return stateEnum;
    }


    /**
     * {@inheritDoc}
     */
    public EventProcessor<Event> getEventProcessor(Event e)
    {
        return eventProcessor;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public Map getAttributeMap()
    {
        return attributeMap;
    }


    ;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setAttribute(Object key,
                             Object value)
    {
        attributeMap.put(key, value);
        setChanged();
    }


    /**
     * Sets the change status based for the {@link ChangeIndication} implementation.
     */
    private void setChanged()
    {
        changed = true;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeChange()
    {
        changed = false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean changed()
    {
        return changed;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void readExternal(final DataInput in) throws IOException
    {
        taskProcessorKey             = (TaskProcessorMediatorKey) ExternalizableHelper.readObject(in);
        tasks                        = (LinkedList<SubmissionKeyPair>) ExternalizableHelper.readObject(in);
        tasksInProgress              = (LinkedList<SubmissionKeyPair>) ExternalizableHelper.readObject(in);
        noAcceptedTasks              = in.readInt();
        noExecutedTasks              = in.readInt();
        stateEnum                    = TaskProcessorStateEnum.valueOf(in.readUTF());
        taskProcessorLease           = (Lease) ExternalizableHelper.readObject(in);
        totalTaskExecutionDuration   = in.readLong();
        minimumTaskExecutionDuration = in.readLong();
        maximumTaskExecutionDuration = in.readLong();
        lastTaskExecutionDuration    = in.readLong();
        yieldTaskCount               = in.readInt();
        attributeMap                 = new HashMap();
        ExternalizableHelper.readMap(in, attributeMap, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, taskProcessorKey);
        ExternalizableHelper.writeObject(out, tasks);
        ExternalizableHelper.writeObject(out, tasksInProgress);
        out.writeInt(noAcceptedTasks);
        out.writeInt(noExecutedTasks);
        out.writeUTF(stateEnum.name());
        ExternalizableHelper.writeObject(out, taskProcessorLease);
        out.writeLong(totalTaskExecutionDuration);
        out.writeLong(minimumTaskExecutionDuration);
        out.writeLong(maximumTaskExecutionDuration);
        out.writeLong(lastTaskExecutionDuration);
        out.writeInt(yieldTaskCount);
        ExternalizableHelper.writeMap(out, attributeMap);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public void readExternal(final PofReader reader) throws IOException
    {
        taskProcessorKey = (TaskProcessorMediatorKey) reader.readObject(0);
        tasks            = new LinkedList<SubmissionKeyPair>();
        reader.readCollection(1, tasks);
        tasksInProgress = new LinkedList<SubmissionKeyPair>();
        reader.readCollection(2, tasksInProgress);
        noAcceptedTasks              = reader.readInt(3);
        noExecutedTasks              = reader.readInt(4);
        stateEnum                    = TaskProcessorStateEnum.valueOf(reader.readString(5));
        taskProcessorLease           = (Lease) reader.readObject(6);
        totalTaskExecutionDuration   = reader.readLong(7);
        minimumTaskExecutionDuration = reader.readLong(8);
        maximumTaskExecutionDuration = reader.readLong(9);
        lastTaskExecutionDuration    = reader.readLong(10);
        yieldTaskCount               = reader.readInt(11);
        attributeMap                 = new HashMap();
        reader.readMap(12, attributeMap);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final PofWriter writer) throws IOException
    {
        writer.writeObject(0, taskProcessorKey);
        writer.writeCollection(1, tasks);
        writer.writeCollection(2, tasksInProgress);
        writer.writeInt(3, noAcceptedTasks);
        writer.writeInt(4, noExecutedTasks);
        writer.writeString(5, stateEnum.name());
        writer.writeObject(6, taskProcessorLease);
        writer.writeLong(7, totalTaskExecutionDuration);
        writer.writeLong(8, minimumTaskExecutionDuration);
        writer.writeLong(9, maximumTaskExecutionDuration);
        writer.writeLong(10, lastTaskExecutionDuration);
        writer.writeInt(11, yieldTaskCount);
        writer.writeMap(12, attributeMap);
    }
}
