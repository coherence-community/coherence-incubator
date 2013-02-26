/*
 * File: LoggingDispatcher.java
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

package com.oracle.coherence.patterns.processing.dispatchers.logging;

import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.patterns.processing.dispatchers.AbstractDispatcher;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchController;
import com.oracle.coherence.patterns.processing.dispatchers.DispatchOutcome;
import com.oracle.coherence.patterns.processing.dispatchers.PendingSubmission;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}
 * that logs {@link PendingSubmission} information to the configured Java Logger
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class LoggingDispatcher extends AbstractDispatcher implements ExternalizableLite,
                                                                     PortableObject,
                                                                     LoggingDispatcherMBean
{
    /**
     * The {@link Logger} to use.
     */
    private static Logger logger = Logger.getLogger(LoggingDispatcher.class.getName());

    /**
     * <p>
     * The level at which this logger will log.
     * </p>
     */
    private Level logLevel;

    /**
     * The number of
     * {@link com.oracle.coherence.patterns.processing.internal.Submission}s
     * that this
     * {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}
     * has seen.
     */
    private int noOfferedSubmissions;

    /**
     * The {@link Environment} for this {link LoggingDispatcher}.
     */
    private Environment environment;


    /**
     * <p>
     * Standard Constructor (logs at the level INFO by default).
     * </p>
     */
    public LoggingDispatcher()
    {
        this.logLevel = Level.INFO;
    }


    /**
     * Standard Constructor (to log at a specified level).
     *
     * @param logLevel the log level to use
     */
    public LoggingDispatcher(final Level logLevel)
    {
        this.logLevel = logLevel;
    }


    /**
     * <p>
     * Constructor that includes the
     * {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}
     * name.
     * </p>
     *
     * @param environment the {@link Environment} for this {@link LoggingDispatcher}.
     * @param name        the name of this {@link com.oracle.coherence.patterns.processing.dispatchers.Dispatcher}
     */
    public LoggingDispatcher(Environment  environment,
                             final String name)
    {
        super(name);
        this.environment = environment;
        this.logLevel    = Level.INFO;
    }


    // ----- Dispatcher interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public DispatchOutcome dispatch(final PendingSubmission oPendingProcess)
    {
        noOfferedSubmissions++;

        if (logger.isLoggable(logLevel))
        {
            logger.log(logLevel,
                       "[LoggingDispatcher] SubmissionId={0}, Payload={1}",
                       new Object[] {oPendingProcess.getSubmissionKey(), oPendingProcess.getPayload()});
        }

        return DispatchOutcome.CONTINUE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown(final DispatchController dispatchController)
    {
        // unregisterMBean();
        super.onShutdown(dispatchController);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartup(final DispatchController dispatchController)
    {
        super.onStartup(dispatchController);
        registerMBean();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final DataInput in) throws IOException
    {
        super.readExternal(in);
        this.logLevel = Level.parse(ExternalizableHelper.readUTF(in));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeUTF(out, this.logLevel.getName());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.logLevel = Level.parse(reader.readString(100));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeString(100, this.logLevel.getName());
    }


    /**
     * {@inheritDoc}
     */
    public String getDispatcherName()
    {
        return getName();
    }


    /**
     * {@inheritDoc}
     */
    public int getSubmissionsAccepted()
    {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    public int getSubmissionsOffered()
    {
        return noOfferedSubmissions;
    }
}
