/*
 * File: SubmissionTest.java
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

package com.oracle.coherence.patterns.processing.internal;

import com.oracle.coherence.common.backingmaplisteners.Cause;
import com.oracle.coherence.common.events.backingmap.BackingMapEntryDepartedEvent;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.identifiers.UUIDBasedIdentifier;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.MapEvent;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Testing {@link Submission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *  @author Christer Fahlgren
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {MapEvent.class, Cause.class})
public class SubmissionTest
{
    /**
     * This is a test case for the event when a Submission is deleted from this particular node because of
     * PartitionManagement.
     *
     * A bug was reported for this (INC-255)
     */
    @Test
    public void testSubmissionDeletionByRepartitioning()
    {
        // Set up the mock objects
        BackingMapEntryDepartedEvent evt             = PowerMock.createNiceMock(BackingMapEntryDepartedEvent.class);
        BinaryEntry                  entry           = PowerMock.createNiceMock(BinaryEntry.class);
        EventDispatcher              eventDispatcher = PowerMock.createNiceMock(EventDispatcher.class);
        DispatchController           controller      = EasyMock.createMock(DispatchController.class);
        DefaultSubmission            sub             = PowerMock.createMock(DefaultSubmission.class);
        SubmissionContent            pl              = EasyMock.createMock(SubmissionContent.class);
        SubmissionConfiguration      conf            = EasyMock.createMock(SubmissionConfiguration.class);

        EasyMock.expect(evt.getEntry()).andReturn(entry).anyTimes();
        EasyMock.expect(entry.getValue()).andReturn(sub).anyTimes();
        EasyMock.expect(entry.getKey()).andReturn(null).anyTimes();
        ;
        EasyMock.expect(sub.getContent()).andReturn(pl).anyTimes();
        EasyMock.expect(pl.getSubmissionConfiguration()).andReturn(conf);
        controller.discard(EasyMock.isA(PendingSubmission.class));

        EasyMock.expect(sub.getResultIdentifier()).andReturn(UUIDBasedIdentifier.newInstance());

        DefaultSubmission.setDispatchController(controller);
        EasyMock.replay(evt);
        EasyMock.replay(controller);
        EasyMock.replay(sub);
        EasyMock.replay(entry);

        DefaultSubmission realsub = new DefaultSubmission();

        realsub.processLater(eventDispatcher, evt);

        EasyMock.verify(evt);
        EasyMock.verify(controller);
        EasyMock.verify(sub);
        EasyMock.verify(entry);
    }
}
