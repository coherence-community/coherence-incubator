/*
 * File: AbstractDispatcher.java
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
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.Registry;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base implementation of a {@link Dispatcher}.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class AbstractDispatcher implements Dispatcher, PortableObject, ExternalizableLite
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(AbstractDispatcher.class.getName());

    /**
     * The {@link DispatchController} that owns this {@link Dispatcher}.
     */
    private DispatchController dispatchController;

    /**
     * The name of this {@link Dispatcher}.
     */
    private String name;

    /**
     * The name of the MBean associated with this {@link Dispatcher}.
     */
    private String mBeanName;


    /**
     * Standard Constructor.
     */
    public AbstractDispatcher()
    {
    }


    /**
     * Constructor that includes the name of the {@link Dispatcher}.
     *
     * @param name the name of the {@link Dispatcher}
     */
    public AbstractDispatcher(final String name)
    {
        this.name = name;
    }


    /**
     * Sets the cluster-wide unique JMX bean name for the {@link Dispatcher}.
     *
     * @param mBeanName the name of the mBean
     */
    protected void setMBeanName(String mBeanName)
    {
        this.mBeanName = mBeanName;
    }


    /**
     * @return the registered mBean name
     */
    protected String getMBeanName()
    {
        return mBeanName;
    }


    /**
     * Register an MBean for this Dispatcher.
     */
    protected void registerMBean()
    {
        Registry registry = CacheFactory.ensureCluster().getManagement();

        if (registry != null)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Registering JMX management extensions for Dispatcher {0}", getName());
            }

            setMBeanName(registry.ensureGlobalName(String.format("type=ProcessingPattern,subType=Dispatcher,id=%s",
                                                                 getClass().getSimpleName() + ":" + getName())));
            registry.register(getMBeanName(), this);
        }
    }


    /**
     * unregister an MBean for this Dispatcher.
     */
    protected void unregisterMBean()
    {
        Registry registry = CacheFactory.ensureCluster().getManagement();

        if (registry != null)
        {
            if (logger.isLoggable(Level.FINER))
            {
                logger.log(Level.FINER, "Unregistering JMX management extensions for Dispatcher {0}", getName());
            }

            registry.unregister(getMBeanName());
        }
    }


    /**
     * Returns the {@link DispatchController} that owns this {@link Dispatcher}.
     *
     * @return the DispatchController
     */
    public DispatchController getDispatchController()
    {
        return dispatchController;
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns if the {@link Dispatcher} is active (ie: has a
     * {@link DispatchController}).
     *
     * @return whether the {@link DispatchController} is active - i.e. not null
     */
    public boolean isActive()
    {
        return dispatchController != null;
    }


    /**
     * {@inheritDoc}
     */
    public void onShutdown(final DispatchController dispatchController)
    {
        this.dispatchController = null;

        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Shutting down dispatcher {0}", getName());
        }
    }


    /**
     * {@inheritDoc}
     */
    public void onStartup(final DispatchController dispatchController)
    {
        this.dispatchController = dispatchController;

        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, "Starting dispatcher {0}", getName());
        }
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        name = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeSafeUTF(out, name);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        name = reader.readString(0);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeString(0, name);
    }
}
