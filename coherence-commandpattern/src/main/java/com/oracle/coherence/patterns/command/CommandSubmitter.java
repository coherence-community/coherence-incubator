/*
 * File: CommandSubmitter.java
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

package com.oracle.coherence.patterns.command;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.ticketing.Ticket;

/**
 * A {@link CommandSubmitter} provides the mechanisms by which
 * we submit {@link Command} execution within {@link Context}s.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface CommandSubmitter
{
    /**
     * Asynchronously submits the provided {@link Command} for execution
     * against the {@link Context} with the specified {@link Identifier} and
     * returns a {@link Ticket} that may be used to cancel the execution of the {@link Command}.
     *
     * @param contextIdentifier
     * @param command
     *
     * @return The {@link Identifier} that was issued to track the execution of the {@link Command}
     *
     * @throws IllegalArgumentException If no {@link Context} with the specified {@link Identifier} exists.
     */
    public <C extends Context> Identifier submitCommand(Identifier contextIdentifier,
                                                        Command<C> command);


    /**
     * Asynchronously submits the provided {@link Command} for execution
     * against the {@link Context} with the specified {@link Identifier} and
     * returns a {@link Ticket} that may be used to cancel the execution of the {@link Command}.
     * <p>
     * If the specified {@link Context} does not exist, but the allowSubmissionWhenContextDoesNotExist
     * parameter is <code>true</code>, the provided {@link Command} will be queued for
     * execution when the {@link Context} is created.
     *
     * @param contextIdentifier
     * @param command
     * @param allowSubmissionWhenContextDoesNotExist
     *
     * @return The {@link Identifier} that was issued to track the execution of the {@link Command}
     *
     * @throws IllegalArgumentException If no {@link Context} with the specified {@link Identifier} exists
     *                                  and allowSubmissionWhenContextDoesNotExist is <code>false</code>
     */
    public <C extends Context> Identifier submitCommand(Identifier contextIdentifier,
                                                        Command<C> command,
                                                        boolean    allowSubmissionWhenContextDoesNotExist);


    /**
     * Attempts to cancel the execution of the {@link Command} that was issued with the specified {@link Ticket}.
     * <p>
     * NOTE: If the {@link Command} has already been executed or has commenced execution, it may not be canceled.
     * Only {@link Command}s that have not been executed may be canceled.  All other requests are ignored.
     *
     * @param <C>
     * @param commandIdentifier
     */
    public <C extends Context> boolean cancelCommand(Identifier commandIdentifier);
}
