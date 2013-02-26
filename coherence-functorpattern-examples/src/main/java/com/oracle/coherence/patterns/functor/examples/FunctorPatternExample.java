/*
 * File: FunctorPatternExample.java
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

package com.oracle.coherence.patterns.functor.examples;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.command.ContextsManager;
import com.oracle.coherence.patterns.command.DefaultContextsManager;
import com.oracle.coherence.patterns.functor.DefaultFunctorSubmitter;
import com.oracle.coherence.patterns.functor.FunctorSubmitter;
import com.tangosol.net.CacheFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The {@link FunctorPatternExample} will create a {@link Counter} Context
 * after which it will updates the value 50 times.  After each update
 * the example will wait on a {@link Future} to return the value of the
 * {@link Counter}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 * @author Noah Arliss
 */
public class FunctorPatternExample
{
    /**
     * The Main Entry Point for the {@link FunctorPatternExample}.
     *
     * @param args  the arguments for the application
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws InterruptedException, ExecutionException
    {
        ContextsManager  contextsManager   = DefaultContextsManager.getInstance();
        Identifier       contextIdentifier = contextsManager.registerContext("myCounter", new Counter(0));

        FunctorSubmitter functorSubmitter  = DefaultFunctorSubmitter.getInstance();

        functorSubmitter.submitCommand(contextIdentifier, new LoggingCommand("Commenced", 0));

        for (int i = 0; i < 50; i++)
        {
            Future<Long> future = functorSubmitter.submitFunctor(contextIdentifier, new NextValueFunctor());

            System.out.println(future.get());
        }

        functorSubmitter.submitCommand(contextIdentifier, new LoggingCommand("Completed", 0));

        Thread.sleep(2000);

        Counter counter = (Counter) contextsManager.getContext(contextIdentifier);

        System.out.println(counter);

        CacheFactory.shutdown();
    }
}
