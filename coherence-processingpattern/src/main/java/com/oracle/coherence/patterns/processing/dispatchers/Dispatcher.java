/*
 * File: Dispatcher.java
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

package com.oracle.coherence.patterns.processing.dispatchers;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

import java.io.Serializable;

/**
 * A {@link Dispatcher} is responsible for dispatching a
 * {@link PendingSubmission} to one or more components in a (possibly
 * distributed) system that are capable of processing the said
 * {@link PendingSubmission}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Brian Oliver
 */
public interface Dispatcher extends PortableObject, ExternalizableLite, Serializable
{
    /**
     * Return the name of the {@link Dispatcher}. Each {@link Dispatcher} requires a unique
     * name at runtime. This name will be used for logging as well as for
     * registration and un-registration.
     *
     * @return the name of the dispatcher.
     */
    public String getName();


    /**
     * Attempts to dispatch the specified {@link PendingSubmission} for
     * processing (by an appropriate implementation), returning a
     * {@link DispatchOutcome} that represents the outcome.
     * <p>
     * <strong>NOTE:</strong>This method should not attempt to actually
     * execute or process the supplied {@link PendingSubmission}, especially
     * if the said processing will take a long period of time. The purpose of
     * this method is strictly to dispatch a {@link PendingSubmission} for
     * processing, not do the actual processing.
     *
     * @param pendingProcess the {@link PendingSubmission} to process
     *
     * @return A {@link DispatchOutcome} of the result of dispatching the
     *         {@link PendingSubmission}
     */
    public DispatchOutcome dispatch(PendingSubmission pendingProcess);


    /**
     * Called by the {@link DispatchController} that owns the
     * {@link Dispatcher} after the said {@link Dispatcher} has been
     * registered but before {@link PendingSubmission}s are provided to the
     * {@link #dispatch(PendingSubmission)} method.
     *
     * @param dispatchController the {@link DispatchController} that controls
     *            this {@link Dispatcher}
     */
    public void onStartup(DispatchController dispatchController);


    /**
     * Called by the {@link DispatchController} that owns the
     * {@link Dispatcher} before the said {@link Dispatcher} is removed
     * (unregistered).
     *
     * @param dispatchController the {@link DispatchController} that controls
     *            this {@link Dispatcher}
     */
    public void onShutdown(DispatchController dispatchController);
}
