/*
 * File: TaskProcessorMediatorInterceptor.java
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
import com.oracle.coherence.patterns.processing.internal.ProcessingPattern;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.internal.ServiceDispatcher;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.UID;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * The TaskProcessorMediatorInterceptor intercepts events for the TaskProcessorMediator.\
 * and processes both cache events and transfer events.
 *
 * @author Paul Mackin
 */
public class TaskProcessorMediatorInterceptor implements EventInterceptor, MemberListener
{
    /**
     * The keys of member-based {@link TaskProcessorMediator}s resident in this server, keyed by {@link Member#getUid()}.
     */
    private ConcurrentHashMap<UID, HashSet<TaskProcessorMediatorKey>> mediatorKeys;

    /**
     * An {@link ExecutorService} to perform recovery of tasks (asynchronously)
     */
    private ExecutorService recoveryService;


    /**
     * Constructs a {@link TaskProcessorMediatorInterceptor}.
     */
    public TaskProcessorMediatorInterceptor()
    {
        this.mediatorKeys = new ConcurrentHashMap<>();
    }


    @Override
    public void onEvent(Event event)
    {
        if (event instanceof EventDispatcher.InterceptorRegistrationEvent)
        {
            EventDispatcher.InterceptorRegistrationEvent registrationEvent =
                (EventDispatcher.InterceptorRegistrationEvent) event;

            if (registrationEvent.getType() == EventDispatcher.InterceptorRegistrationEvent.Type.INSERTED)
            {
                // use the Processing Pattern Executor Service for recovery
                recoveryService =
                    CacheFactory.getConfigurableCacheFactory().getResourceRegistry().getResource(ExecutorService.class,
                                                                                                 ProcessingPattern
                                                                                                     .RESOURCE);

                EventDispatcher dispatcher = registrationEvent.getDispatcher();

                if (dispatcher instanceof ServiceDispatcher)
                {
                    ServiceDispatcher serviceDispatcher = (ServiceDispatcher) dispatcher;

                    serviceDispatcher.getService().addMemberListener(this);
                }
            }
        }
        else if (event instanceof EntryEvent)
        {
            processEntryEvent((EntryEvent) event);
        }
        else if (event instanceof TransferEvent)
        {
            processTransferEvent((TransferEvent) event);
        }
    }


    /**
     * Process the EntryEvent.
     *
     * @param event  the EntryEvent
     */
    private void processEntryEvent(EntryEvent<Object, Object> event)
    {
        Cluster cluster =
            event.getDispatcher().getBackingMapContext().getManagerContext().getCacheService().getCluster();

        for (BinaryEntry entry : event.getEntrySet())
        {
            DefaultTaskProcessorMediator originalMediator = (DefaultTaskProcessorMediator) entry.getOriginalValue();
            DefaultTaskProcessorMediator updatedMediator  = (DefaultTaskProcessorMediator) entry.getValue();

            switch (event.getType())
            {
            case INSERTED :
                ProcessingPattern.ensureInfrastructureStarted(entry.getContext().getManager().getCacheFactory());
                updatedMediator.onInserted(entry);

                registerTaskProcessorMediator(updatedMediator, cluster);
                break;

            case UPDATED :
                updatedMediator.onUpdated(entry);
                break;

            case REMOVED :
                originalMediator.onRemoved(entry);

                deregisterTaskProcessorMediator(originalMediator);
                break;

            default :
                break;
            }
        }
    }


    /**
     * Process the TransferEvent.
     *
     * @param event  the TransferEvent
     */
    private void processTransferEvent(TransferEvent event)
    {
        Cluster                       cluster    = event.getDispatcher().getService().getCluster();
        Map<String, Set<BinaryEntry>> map        = event.getEntries();
        Set<BinaryEntry>              setEntries = map.get(DefaultTaskProcessorMediator.CACHENAME);

        if (setEntries == null)
        {
            return;
        }

        for (BinaryEntry entry : setEntries)
        {
            DefaultTaskProcessorMediator mediator = (DefaultTaskProcessorMediator) entry.getValue();

            switch (event.getType())
            {
            case ARRIVED :
            case RECOVERED :
                ProcessingPattern.ensureInfrastructureStarted(entry.getContext().getManager().getCacheFactory());
                mediator.onArrived(entry);

                registerTaskProcessorMediator(mediator, cluster);
                break;

            case DEPARTING :
                mediator.onDeparted(entry);

                deregisterTaskProcessorMediator(mediator);
                break;
            }
        }
    }


    private void registerTaskProcessorMediator(DefaultTaskProcessorMediator mediator,
                                               Cluster                      cluster)
    {       
        final TaskProcessorMediatorKey key = mediator.getTaskProcessorKey();

        if (key.getMemberId() > 0)
        {
            UID memberUID = key.getUniqueId();

            // determine if the Member actually exists.  If it doesn't we need to recover tasks!

            // determine the members of the cluster
            Set<Member> memberSet = cluster.getMemberSet();

            // ensure the member is in the cluster
            boolean exists = false;

            for (Iterator<Member> iterator = memberSet.iterator(); iterator.hasNext(); )
            {
                Member member = iterator.next();

                exists = exists || member.getUid().equals(memberUID);
            }

            if (exists)
            {
                HashSet<TaskProcessorMediatorKey> keys = mediatorKeys.get(memberUID);

                if (keys == null)
                {
                    synchronized (mediatorKeys)
                    {
                        keys = mediatorKeys.putIfAbsent(memberUID, new HashSet<TaskProcessorMediatorKey>());

                        if (keys == null)
                        {
                            keys = mediatorKeys.get(memberUID);
                        }

                        keys.add(key);
                    }
                }
            }
            else
            {
                recoveryService.submit(new Runnable()
                                       {
                                           @Override
                                           public void run()
                                           {
                                               // acquire the mediator cache
                                               NamedCache taskProcessorMediatorCache =
                                                   CacheFactory.getCache(DefaultTaskProcessorMediator.CACHENAME);

                                               // asynchronously request recovery of the tasks
                                               taskProcessorMediatorCache.invoke(key,
                                                                                 new InvokeMethodProcessor("recoverTasks"));
                                           }
                                       });
            }
        }
    }


    private void deregisterTaskProcessorMediator(DefaultTaskProcessorMediator mediator)
    {
        TaskProcessorMediatorKey key = mediator.getTaskProcessorKey();

        if (key.getMemberId() > 0)
        {
            UID                               memberUID = key.getUniqueId();

            HashSet<TaskProcessorMediatorKey> keys      = mediatorKeys.get(memberUID);

            if (keys != null)
            {
                synchronized (mediatorKeys)
                {
                    keys.remove(key);

                    if (keys.isEmpty())
                    {
                        mediatorKeys.remove(memberUID);
                    }
                }
            }
        }
    }


    @Override
    public void memberJoined(MemberEvent memberEvent)
    {
        // nothing to do when joining
    }


    @Override
    public void memberLeaving(MemberEvent memberEvent)
    {
        // nothing to do prior to leaving
    }


    @Override
    public void memberLeft(MemberEvent memberEvent)
    {
        UID memberUID = memberEvent.getMember().getUid();

        // determine the TaskProcessorMediators that should be recovered due to the member leaving
        final HashSet<TaskProcessorMediatorKey> keys = mediatorKeys.remove(memberUID);

        if (keys == null)
        {
            // nothing to do when there are no mediators for the left member
        }
        else
        {
            recoveryService.submit(new Runnable()
                                   {
                                       @Override
                                       public void run()
                                       {
                                           // acquire the mediator cache
                                           NamedCache taskProcessorMediatorCache =
                                               CacheFactory.getCache(DefaultTaskProcessorMediator.CACHENAME);

                                           for (TaskProcessorMediatorKey key : keys)
                                           {
                                               // asynchronously request recovery of the task
                                               taskProcessorMediatorCache.invoke(key,
                                                                                 new InvokeMethodProcessor("recoverTasks"));
                                           }
                                       }
                                   });
        }
    }
}
