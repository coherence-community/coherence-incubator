/*
 * File: LifecycleInterceptor.java
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


import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.application.LifecycleEvent;

/**
 * The class is a LifecycleEvent interceptor which gets called
 * when a CCF is activate/disposed.
 *
 * @author Paul Mackin
 */
public class LifecycleInterceptor implements EventInterceptor<LifecycleEvent>
{
    /**
     * Method description
     *
     * @param event
     */
    @Override
    public void onEvent(LifecycleEvent event)
    {
        // Start up the processing pattern infrastructure.  This code calls back
        // to CCF do execute from a thread.
        final ConfigurableCacheFactory ccf = event.getConfigurableCacheFactory();

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                ProcessingPattern.ensureInfrastructureStarted(ccf);
            }
        }).start();
    }
}
