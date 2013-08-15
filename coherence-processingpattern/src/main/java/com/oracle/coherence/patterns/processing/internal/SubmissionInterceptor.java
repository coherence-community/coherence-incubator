/*
 * File: SubmissionInterceptor.java
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

package com.oracle.coherence.patterns.processing.internal;


import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.util.BinaryEntry;

import java.util.Map;
import java.util.Set;

/**
 * The SubmissionInterceptor intercepts events for the Submission cache and processes
 * both cache events and transfer events.
 *
 * @author Paul Mackin
 */
public class SubmissionInterceptor implements EventInterceptor
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(Event event)
    {
        if (event instanceof EntryEvent)
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
    private void processEntryEvent(EntryEvent event)
    {
        for (BinaryEntry entry : event.getEntrySet())
        {
            DefaultSubmission submission = (DefaultSubmission) entry.getValue();

            switch (event.getType())
            {
            case INSERTED :
                ProcessingPattern.ensureInfrastructureStarted(entry.getContext().getManager().getCacheFactory());
                submission.onInserted(entry);
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
        Map<String, Set<BinaryEntry>> map        = event.getEntries();
        Set<BinaryEntry>              setEntries = map.get(DefaultSubmission.CACHENAME);

        if (setEntries == null)
        {
            return;
        }

        for (BinaryEntry entry : setEntries)
        {
            DefaultSubmission submission = (DefaultSubmission) entry.getValue();

            switch (event.getType())
            {
            case ARRIVED :
                ProcessingPattern.ensureInfrastructureStarted(entry.getContext().getManager().getCacheFactory());
                submission.onArrived(entry);
                break;

            case DEPARTING :
                submission.onDeparting(entry);
                break;
            }
        }
    }
}
