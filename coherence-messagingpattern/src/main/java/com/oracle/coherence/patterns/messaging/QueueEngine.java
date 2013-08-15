/*
 * File: QueueFSM.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.messaging;

import com.oracle.coherence.common.finitestatemachines.AnnotationDrivenModel;
import com.oracle.coherence.common.finitestatemachines.Event;
import com.oracle.coherence.common.finitestatemachines.Instruction;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine;
import com.oracle.coherence.common.finitestatemachines.NonBlockingFiniteStateMachine.CoalescedEvent;

import com.oracle.coherence.common.identifiers.Identifier;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.BinaryEntry;

import com.tangosol.util.processor.UpdaterProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The engine containing a finite state machine (FSM) used for coalescing queue event processing.
 *
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *
 * @author David Rowlands
 */
public class QueueEngine extends AbstractEngine 
{
    protected QueueEngine(Identifier destinationIdentifier)
    {
        super(destinationIdentifier);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        m_fsm = new NonBlockingFiniteStateMachine<State>("Queue",
                                                         new AnnotationDrivenModel<State>(State.class,
                                                                                          new Model()),
                                                         State.IDLE,
                                                         executor,
                                                         false);
    }


    /**
     * Create and process a Queue Event using a FSM coalesced event.  
     *
     * @param entry BinaryEntry for which event is to be processed. 
     */
    public void processRunEvent(BinaryEntry entry)
    {
        Event<State> queueEvent = new QueueEvent<State>(new StateEvent(State.RUNNING),
                                                        NonBlockingFiniteStateMachine.CoalescedEvent.Process
                                                            .MOST_RECENT,
                                                        destinationIdentifier,
                                                        entry);

        m_fsm.process(queueEvent);
    }


    /**
     * {@inheritDoc}
     */
    Instruction onRunning(Event<State> event)
    {
        QueueEvent<State> evt   = (QueueEvent<State>) event;
        BinaryEntry       entry = evt.getEntry();

        // Need to deserialze the queue here and then update the MBeans
        BackingMapManagerContext context = entry.getBackingMapContext().getManagerContext();
        Queue queue =
            (Queue) context.getValueFromInternalConverter()
                .convert(entry.getBackingMapContext().getBackingMap().get(entry.getBinaryKey()));

        if (queue != null)
        {
            // Deliver any messages to waiting subscriptions
            QueueDeliveryResults results = queue.doDelivery();

            if (results.hasMessage())
            {
                // Update the queue object with the delivery results
                NamedCache destinationCache = CacheFactory.getCache(Destination.CACHENAME);

                destinationCache.invoke(queue.getIdentifier(), new UpdaterProcessor("processDeliveryResults", results));
            }
        }

        return new Instruction.TransitionTo<State>(State.IDLE);
    }


    /**
     * Class description
     *
     * @param <S>
     *
     * @version        Enter version here..., 13/07/03
     * @author         Enter your name here...
     */
    public static class QueueEvent<S extends Enum<S>> extends NonBlockingFiniteStateMachine.CoalescedEvent<S>
    {
        private final BinaryEntry entry;


        /**
         * Constructs ...
         *
         *
         * @param event          the {@link Event} to be coalesced
         * @param mode           which {@link CoalescedEvent}s to process
         * @param discriminator  the descriminator used to uniquely coalesce the {@link Event}
         * @param entry			 BinaryEntry which is stored in the Queue Event and used in the event processing
         */
        public QueueEvent(Event<S>    event,
                          Process     mode,
                          Object      discriminator,
                          BinaryEntry entry)
        {
            super(event, mode, discriminator);
            this.entry = entry;
        }


        /**
         * Method description
         *
         * @return BinaryEntry 
         */
        public BinaryEntry getEntry()
        {
            return entry;
        }
    }
}
