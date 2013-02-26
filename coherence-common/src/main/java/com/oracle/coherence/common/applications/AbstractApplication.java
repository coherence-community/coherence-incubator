/*
 * File: AbstractApplication.java
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

package com.oracle.coherence.common.applications;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link AbstractApplication} is a base implementation for an {@link Application} that
 * uses a java.lang.{@link Process} as a means of controlling the said {@link Application}
 * at the operating system level.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Harvey Raja
 */
@Deprecated
public abstract class AbstractApplication implements Application
{
    /**
     * The {@link Process} representing the runtime {@link Application}.
     */
    private final Process process;

    /**
     * The name of the {@link Application}.
     */
    private String name;

    /**
     * The {@link ApplicationConsole} that will be used for the {@link Application} I/O.
     */
    private final ApplicationConsole console;

    /**
     * The environment variables used when establishing the {@link Application}.
     */
    private Properties environmentVariables;

    /**
     * The {@link Thread} that is used to capture output from the underlying {@link Process}.
     */
    private Thread ioThread;

    /**
     * The os process id this abstract application represents
     */
    private long pid = -1;


    /**
     * Standard Constructor.
     *
     * @param process               The {@link Process} representing the {@link Application}.
     *
     * @param name                  The name of the application.
     *
     * @param console               The {@link ApplicationConsole} that will be used for I/O by the {@link Application}.
     *
     * @param environmentVariables  The environment variables used when establishing the {@link Application}.
     */
    public AbstractApplication(Process            process,
                               String             name,
                               ApplicationConsole console,
                               Properties         environmentVariables)
    {
        this.process              = process;
        this.name                 = name;
        this.console              = console == null ? new SystemApplicationConsole() : console;
        this.environmentVariables = environmentVariables;
        this.pid                  = determinePID(process);

        // start a thread to capture the output and redirect it to the console
        this.ioThread = new Thread(new Runnable()
        {
            public void run()
            {
                long lineNumber = 1;

                try
                {
                    BufferedReader reader =
                        new BufferedReader(new InputStreamReader(new BufferedInputStream(AbstractApplication.this
                            .process.getInputStream())));

                    while (true)
                    {
                        String line = reader.readLine();

                        if (line == null)
                        {
                            break;
                        }

                        AbstractApplication.this.console.printf("[%s%s] %4d: %s\n",
                                                                AbstractApplication.this.name,
                                                                pid < 0 ? "" : "-" + pid,
                                                                lineNumber++,
                                                                line);
                    }
                }
                catch (Exception exception)
                {
                    // deliberately empty as we safely assume exceptions
                    // are always due to process termination.
                }

                AbstractApplication.this.console.printf("[%s] %4d: (terminated)\n",
                                                        AbstractApplication.this.name,
                                                        lineNumber++);
            }
        });

        ioThread.setDaemon(true);
        ioThread.start();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getEnvironmentVariables()
    {
        return environmentVariables;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return name;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy()
    {
        // terminate the ioThread that is reading from the process
        ioThread.interrupt();

        // close the io streams being used by the process
        try
        {
            process.getInputStream().close();
        }
        catch (IOException e)
        {
            // nothing to do here as we don't care
        }

        try
        {
            process.getOutputStream().close();
        }
        catch (IOException e)
        {
            // nothing to do here as we don't care
        }

        try
        {
            process.getErrorStream().close();
        }
        catch (IOException e)
        {
            // nothing to do here as we don't care
        }

        // terminate the process
        process.destroy();

        // wait for it to actually terminate (because the above line may not finish for a while)
        // (if we don't wait the process may be left hanging/orphanned)
        try
        {
            process.waitFor();
        }
        catch (InterruptedException e)
        {
            // nothing to do here as we don't care
        }
    }


    /**
     * <p>Return the os process id encapsulated within this {@link Application}</p>
     *
     * <p><b>NOTE:</b> iff the process id could not be determined a -1 will be
     * returned</p>
     *
     * @return The underlying {@link Process} id.
     */
    public long getPid()
    {
        return pid;
    }


    /**
     * <p>Determine the process id from the process that is encapsulated by
     * this application
     * </p>
     *
     * @param p the process to attain the process id from
     * @return the os process id this abstract application represents
     */
    private long determinePID(final Process p)
    {
        long pid = -1;

        try
        {
            // Unix variants incl. OSX
            if (p.getClass().getSimpleName().equals("UNIXProcess"))
            {
                final Class<?> clazz = p.getClass();
                final Field    pidF  = clazz.getDeclaredField("pid");

                pidF.setAccessible(true);

                Object oPid = pidF.get(p);

                if (oPid instanceof Number)
                {
                    pid = ((Number) oPid).longValue();
                }
                else if (oPid instanceof String)
                {
                    pid = Long.parseLong((String) oPid);
                }
            }

            // Windows processes, i.e. Win32Process or ProcessImpl
            else
            {
                RuntimeMXBean rtb      = ManagementFactory.getRuntimeMXBean();
                final String  sProcess = rtb.getName();
                final int     iPID     = sProcess.indexOf('@');

                if (iPID > 0)
                {
                    String sPID = sProcess.substring(0, iPID);

                    pid = Long.parseLong(sPID);
                }
            }
        }
        catch (SecurityException e)
        {
        }
        catch (NoSuchFieldException e)
        {
        }
        catch (IllegalArgumentException e)
        {
        }
        catch (IllegalAccessException e)
        {
        }

        return pid;
    }
}
