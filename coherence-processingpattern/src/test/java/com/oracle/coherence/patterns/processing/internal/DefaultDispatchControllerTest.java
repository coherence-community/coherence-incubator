/*
 * File: DefaultDispatchControllerTest.java
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

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.identifiers.StringBasedIdentifier;
import com.oracle.coherence.common.util.ObjectProxyFactory;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.DispatcherFilter;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchOutcome;
import com.oracle.coherence.patterns.processing.dispatchers.Dispatcher;
import com.oracle.coherence.patterns.processing.task.ResumableTask;
import com.tangosol.net.ConfigurableCacheFactory;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * Testing a {@link DefaultDispatchController}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class DefaultDispatchControllerTest
{
    /**
     * Test an accepting dispatcher.
     */
    @Test
    public void testDispatcherFilterAcceptingLogic()
    {
        ConfigurableCacheFactory factory = EasyMock.createMock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactoryMock =
            EasyMock.createMock(ObjectProxyFactory.class);
        DefaultDispatchController ctrl = new DefaultDispatchController(factory, submissionResultProxyFactoryMock);
        Dispatcher                dispatcher              = EasyMock.createMock(Dispatcher.class);
        DefaultPendingSubmission  submission              = PowerMock.createMock(DefaultPendingSubmission.class);
        SubmissionConfiguration   submissionConfiguration = EasyMock.createMock(SubmissionConfiguration.class);
        DispatcherFilter          filter                  = EasyMock.createMock(DispatcherFilter.class);

        // Set up expectations of behavior

        EasyMock.expect(submission.getSubmissionConfiguration()).andReturn(submissionConfiguration).anyTimes();
        EasyMock.expect(submissionConfiguration.getDispatcherFilter()).andReturn(filter);
        EasyMock.expect(filter.filterDispatcher(dispatcher)).andReturn(true);
        EasyMock.expect(dispatcher.dispatch(submission)).andReturn(DispatchOutcome.ACCEPTED);

        // Set the behavior in stone

        EasyMock.replay(submission);
        EasyMock.replay(submissionConfiguration);
        EasyMock.replay(dispatcher);
        EasyMock.replay(filter);

        assertEquals(ctrl.tryDispatchSubmissionToDispatcher(submission, dispatcher), DispatchOutcome.ACCEPTED);

        // Validate the behavior

        EasyMock.verify(submission);
        EasyMock.verify(submissionConfiguration);
        EasyMock.verify(dispatcher);
        EasyMock.verify(filter);

    }


    /**
     * Test a  rejecting dispatcher.
     */
    @Test
    public void testDispatcherFilterRejectingLogic()
    {
        ConfigurableCacheFactory factory = EasyMock.createMock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactoryMock =
            EasyMock.createMock(ObjectProxyFactory.class);
        DefaultDispatchController ctrl = new DefaultDispatchController(factory, submissionResultProxyFactoryMock);
        Dispatcher                dispatcher              = EasyMock.createMock(Dispatcher.class);
        DefaultPendingSubmission  submission              = PowerMock.createMock(DefaultPendingSubmission.class);
        SubmissionConfiguration   submissionConfiguration = EasyMock.createMock(SubmissionConfiguration.class);
        DispatcherFilter          filter                  = EasyMock.createMock(DispatcherFilter.class);

        // Set up expectations of behavior

        EasyMock.expect(submission.getSubmissionConfiguration()).andReturn(submissionConfiguration).anyTimes();
        EasyMock.expect(submissionConfiguration.getDispatcherFilter()).andReturn(filter);
        EasyMock.expect(filter.filterDispatcher(dispatcher)).andReturn(false);

        // Set the behavior in stone

        EasyMock.replay(submission);
        EasyMock.replay(submissionConfiguration);
        EasyMock.replay(dispatcher);
        EasyMock.replay(filter);

        assertEquals(ctrl.tryDispatchSubmissionToDispatcher(submission, dispatcher), DispatchOutcome.REJECTED);

        // Validate the behavior

        EasyMock.verify(submission);
        EasyMock.verify(submissionConfiguration);
        EasyMock.verify(dispatcher);
        EasyMock.verify(filter);

    }


    /**
     * Test a no filter dispatcher.
     */
    @Test
    public void testDispatcherNoFilter()
    {
        ConfigurableCacheFactory factory = EasyMock.createMock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactoryMock =
            EasyMock.createMock(ObjectProxyFactory.class);
        DefaultDispatchController ctrl = new DefaultDispatchController(factory, submissionResultProxyFactoryMock);
        Dispatcher                dispatcher              = EasyMock.createMock(Dispatcher.class);
        DefaultPendingSubmission  submission              = PowerMock.createMock(DefaultPendingSubmission.class);
        SubmissionConfiguration   submissionConfiguration = EasyMock.createMock(SubmissionConfiguration.class);

        // Set up expectations of behavior

        EasyMock.expect(submission.getSubmissionConfiguration()).andReturn(submissionConfiguration).anyTimes();
        EasyMock.expect(submissionConfiguration.getDispatcherFilter()).andReturn(null);
        EasyMock.expect(dispatcher.dispatch(submission)).andReturn(DispatchOutcome.ACCEPTED);

        // Set the behavior in stone

        EasyMock.replay(submission);
        EasyMock.replay(submissionConfiguration);
        EasyMock.replay(dispatcher);

        assertEquals(ctrl.tryDispatchSubmissionToDispatcher(submission, dispatcher), DispatchOutcome.ACCEPTED);

        // Validate the behavior

        EasyMock.verify(submission);
        EasyMock.verify(submissionConfiguration);
        EasyMock.verify(dispatcher);

    }


    /**
     * Test a dispatcher with no configuration.
     */
    @Test
    public void testDispatcherNoConfiguration()
    {
        ConfigurableCacheFactory factory = EasyMock.createMock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactoryMock =
            EasyMock.createMock(ObjectProxyFactory.class);

        DefaultDispatchController ctrl       = new DefaultDispatchController(factory, submissionResultProxyFactoryMock);
        Dispatcher                dispatcher = EasyMock.createMock(Dispatcher.class);
        DefaultPendingSubmission  submission = PowerMock.createMock(DefaultPendingSubmission.class);

        // Set up expectations of behavior

        EasyMock.expect(submission.getSubmissionConfiguration()).andReturn(null).anyTimes();
        EasyMock.expect(dispatcher.dispatch(submission)).andReturn(DispatchOutcome.ACCEPTED);

        // Set the behavior in stone

        EasyMock.replay(submission);
        EasyMock.replay(dispatcher);

        assertEquals(ctrl.tryDispatchSubmissionToDispatcher(submission, dispatcher), DispatchOutcome.ACCEPTED);

        // Validate the behavior

        EasyMock.verify(submission);
        EasyMock.verify(dispatcher);

    }


    /**
     * Test a Submission that doesn't exist anymore in the cache.
     */
    @Test
    public void testDispatchSubmission()
    {
        ConfigurableCacheFactory             factory                      = mock(ConfigurableCacheFactory.class);
        Environment                          environmentMock              = mock(Environment.class);

        ObjectProxyFactory<SubmissionResult> submissionresultproxyfactory = mock(ObjectProxyFactory.class);
        DefaultDispatchController            ctrl = new DefaultDispatchController(factory,
                                                                                  submissionresultproxyfactory);
        DefaultPendingSubmission             pendingSubmission            = mock(DefaultPendingSubmission.class);
        SubmissionKey                        submissionkey                = new SubmissionKey();

        stub(pendingSubmission.getSubmissionKey()).toReturn(submissionkey);

        Identifier submissionId = StringBasedIdentifier.newInstance("test");

        stub(pendingSubmission.getResultIdentifier()).toReturn(submissionId);
        stub(pendingSubmission.getPayload()).toThrow(new NoSuchElementException());
        ctrl.dispatchSubmission(pendingSubmission);
    }


    /**
     * Test a Submission that doesn't exist anymore in the cache.
     */
    @Test
    public void testDispatchSubmissionThatExists()
    {
        ConfigurableCacheFactory             factory                          = mock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactoryMock = mock(ObjectProxyFactory.class);
        DefaultDispatchController ctrl = new DefaultDispatchController(factory, submissionResultProxyFactoryMock);

        DefaultPendingSubmission             pendingSubmission                = mock(DefaultPendingSubmission.class);
        SubmissionKey                        submissionkey                    = new SubmissionKey();

        stub(pendingSubmission.getSubmissionKey()).toReturn(submissionkey);

        Identifier submissionId = StringBasedIdentifier.newInstance("test");

        stub(pendingSubmission.getResultIdentifier()).toReturn(submissionId);

        ResumableTask task = mock(ResumableTask.class);

        stub(pendingSubmission.getPayload()).toReturn(task);
        ctrl.dispatchSubmission(pendingSubmission);
    }
}
