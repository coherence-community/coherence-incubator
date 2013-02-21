/*
 * File: ExecutorListener.java
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

package com.oracle.coherence.common.threading;

/**
 * A {@link ExecutorListener} is an interface to get notified about the execution of a {@link Runnable}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public interface ExecutorListener
{
    /**
     * Just before execution starts the beforeExecute method is called.
     *
     * @param runnable the {@link Runnable} that is about to be executed.
     */

    public void beforeExecute(Runnable runnable);


    /**
     * Just after execution stopped, the afterExecute method is called.
     *
     * @param runnable the {@link Runnable} that was just executed.
     * @param t if the Runnable threw an Exception, t contains the exception - null otherwise
     */
    public void afterExecute(Runnable  runnable,
                             Throwable t);
}
