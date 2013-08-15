/*
 * File: DefaultProcessingSessionTest.java
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
import com.oracle.coherence.patterns.processing.ProcessingSession;
import com.oracle.coherence.patterns.processing.SubmissionOutcome;
import com.oracle.coherence.patterns.processing.SubmissionRetentionPolicy;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.InvocableMap.EntryProcessor;
import org.junit.Test;
import org.mockito.Matchers;

import static org.mockito.Mockito.*;

/**
 * A {@link DefaultProcessingSessionTest}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class DefaultProcessingSessionTest
{
    /**
     * Tests submitting a object.
     *
     * @throws Throwable if submission fails.
     */
    @Test
    public void testSubmitObj() throws Throwable
    {
        ConfigurableCacheFactory             cacheFactory                 = mock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<Submission>       submissionProxyFactory       = mock(ObjectProxyFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory = mock(ObjectProxyFactory.class);
        NamedCache                           submissionCache              = mock(NamedCache.class);
        NamedCache                           submissionResultCache        = mock(NamedCache.class);

        stub(cacheFactory.ensureCache(DefaultSubmission.CACHENAME, null)).toReturn(submissionCache);
        stub(cacheFactory.ensureCache(DefaultSubmissionResult.CACHENAME, null)).toReturn(submissionResultCache);

        Identifier identifier = StringBasedIdentifier.newInstance("SessionID");
        ProcessingSession session = new DefaultProcessingSession(cacheFactory,
                                                                 submissionProxyFactory,
                                                                 submissionResultProxyFactory,
                                                                 identifier);
        SubmissionOutcome outcome = session.submit(new Object(), new DefaultSubmissionConfiguration());

        verify(submissionCache).invoke(Matchers.anyObject(), Matchers.isA(EntryProcessor.class));
        verify(submissionResultProxyFactory).createRemoteObjectIfNotExists(Matchers.anyObject(),
                                                                           Matchers.eq(DefaultSubmissionResult.class),
                                                                           Matchers.any(Object[].class));
    }


    /**
     * Tests submitting a object - setting it to a final state and test if it was removed.
     *
     * @throws Throwable if submission fails.
     */
    @Test
    public void testSubmitObjAndFinalState() throws Throwable
    {
        ConfigurableCacheFactory             cacheFactory                 = mock(ConfigurableCacheFactory.class);
        ObjectProxyFactory<Submission>       submissionProxyFactory       = mock(ObjectProxyFactory.class);
        ObjectProxyFactory<SubmissionResult> submissionResultProxyFactory = mock(ObjectProxyFactory.class);
        NamedCache                           submissionCache              = mock(NamedCache.class);
        NamedCache                           submissionResultCache        = mock(NamedCache.class);

        stub(cacheFactory.ensureCache(DefaultSubmission.CACHENAME, null)).toReturn(submissionCache);
        stub(cacheFactory.ensureCache(DefaultSubmissionResult.CACHENAME, null)).toReturn(submissionResultCache);

        Identifier identifier = StringBasedIdentifier.newInstance("SessionID");
        DefaultProcessingSession session = new DefaultProcessingSession(cacheFactory,
                                                                        submissionProxyFactory,
                                                                        submissionResultProxyFactory,
                                                                        identifier);
        SubmissionOutcome outcome = session.submit(new Object(),
                                                   new DefaultSubmissionConfiguration(),
                                                   StringBasedIdentifier.newInstance("Id"),
                                                   SubmissionRetentionPolicy.RemoveOnFinalState,
                                                   null);

        SubmissionResult newResult = new DefaultSubmissionResult(StringBasedIdentifier.newInstance("Id"),
                                                                 null,
                                                                 null,
                                                                 "SUBMITTED");

        newResult.assign(null);
        newResult.processingStarted(null);
        newResult.processingSucceeded(null);
        session.handleResultChange(newResult, StringBasedIdentifier.newInstance("Id"));
        Thread.currentThread().sleep(1000);
        verify(submissionCache).invoke(Matchers.anyObject(), Matchers.isA(EntryProcessor.class));
        verify(submissionResultProxyFactory).createRemoteObjectIfNotExists(Matchers.anyObject(),
                                                                           Matchers.eq(DefaultSubmissionResult.class),
                                                                           Matchers.any(Object[].class));
        verify(submissionResultProxyFactory).destroyRemoteObject(Matchers.isA(Identifier.class));
        verify(submissionProxyFactory).destroyRemoteObject(Matchers.any(Object.class));
    }
}
