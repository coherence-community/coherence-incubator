/*
 * File: ContextBackingMapListener.java
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

package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.backingmaplisteners.AbstractMultiplexingBackingMapListener;
import com.oracle.coherence.common.backingmaplisteners.Cause;
import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.patterns.command.Context;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.MapEvent;

import java.util.concurrent.TimeUnit;

/**
 * A {@link ContextBackingMapListener} is responsible for starting
 * and stopping {@link CommandExecutor}s for {@link Context}s as they are
 * inserted and removed from the {@link ContextWrapper#CACHENAME} cache
 * respectively.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ContextBackingMapListener extends AbstractMultiplexingBackingMapListener
{
    /**
     * Standard Constructor.
     *
     * @param backingMapManagerContext
     */
    public ContextBackingMapListener(BackingMapManagerContext backingMapManagerContext)
    {
        super(backingMapManagerContext);
    }


    /**
     * {@inheritDoc}
     */
    public void onBackingMapEvent(MapEvent    mapEvent,
                                  final Cause cause)
    {
        if (mapEvent.getId() == MapEvent.ENTRY_INSERTED)
        {
            Identifier contextIdentifier = (Identifier) mapEvent.getKey();

            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG, "Context %s has been inserted into this member", contextIdentifier);
            }

            final CommandExecutor commandExecutor = CommandExecutorManager.ensureCommandExecutor(contextIdentifier,
                                                                                                 getContext());

            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG, "Scheduling ContextExecutor for %s to start", contextIdentifier);
            }

            CommandExecutorManager.schedule(new Runnable()
            {
                public void run()
                {
                    commandExecutor.start();
                }
            }, 0, TimeUnit.SECONDS);

        }
        else if (mapEvent.getId() == MapEvent.ENTRY_DELETED)
        {
            Identifier contextIdentifier = (Identifier) mapEvent.getKey();

            if (Logger.isEnabled(Logger.DEBUG))
            {
                Logger.log(Logger.DEBUG, "Context %s has been removed from this member", contextIdentifier);
            }

            final CommandExecutor commandExecutor = CommandExecutorManager.getCommandExecutor(contextIdentifier);

            if (commandExecutor != null)
            {
                commandExecutor.stop();
            }
        }
    }
}
