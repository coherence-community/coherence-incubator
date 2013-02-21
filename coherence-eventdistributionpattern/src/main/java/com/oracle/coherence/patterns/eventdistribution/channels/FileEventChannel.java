/*
 * File: FileEventChannel.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.patterns.eventdistribution.EventChannel;
import com.oracle.coherence.patterns.eventdistribution.EventChannelController;
import com.oracle.coherence.patterns.eventdistribution.EventChannelNotReadyException;
import com.oracle.coherence.patterns.eventdistribution.EventDistributor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

/**
 * A simple {@link EventChannel} to store {@link Event}s to a file.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class FileEventChannel implements EventChannel
{
    /**
     * The name of the directory (that must exist) to which the {@link FileEventChannel} will write results.
     */
    private String directoryName;

    /**
     * If the file should be opened for appending.
     */
    private boolean isAppending;

    /**
     * The {@link BufferedWriter} that will be used for writing to the file.
     */
    private BufferedWriter bufferedWriter;


    /**
     * Standard Constructor.
     *
     * @param directoryName     The directory for the file.
     * @param isAppending       Whether to append to the end of the file or not.
     */
    public FileEventChannel(String  directoryName,
                            boolean isAppending)
    {
        this.directoryName  = directoryName;
        this.isAppending    = isAppending;
        this.bufferedWriter = null;
    }


    /**
     * Returns the name of the directory (it must exist) in which the file {@link FileEventChannel} will write files.
     */
    public String getDirectoryName()
    {
        return directoryName;
    }


    /**
     * {@inheritDoc}
     */
    public int send(Iterator<Event> events)
    {
        int distributionCount = 0;

        try
        {
            while (events.hasNext())
            {
                Event event = events.next();

                bufferedWriter.write(String.format("%s\n", event));
                distributionCount++;
            }
        }
        catch (IOException ioException)
        {
            // TODO: log this property with a logger
            ioException.printStackTrace(System.err);

            throw new RuntimeException(ioException);
        }

        return distributionCount;
    }


    /**
     * {@inheritDoc}
     */
    public void connect(EventDistributor.Identifier       eventDistributorIdentifer,
                        EventChannelController.Identifier eventChannelControllerIdentifier)
                            throws EventChannelNotReadyException
    {
        try
        {
            String fileName = String.format("%s-%s-%d.log",
                                            eventDistributorIdentifer.getExternalName(),
                                            eventChannelControllerIdentifier.getExternalName(),
                                            System.currentTimeMillis());

            bufferedWriter = new BufferedWriter(new FileWriter(new File(directoryName, fileName), isAppending));

        }
        catch (IOException ioException)
        {
            ioException.printStackTrace(System.err);
            bufferedWriter = null;

            throw new RuntimeException(ioException);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void disconnect()
    {
        if (bufferedWriter != null)
        {
            try
            {
                bufferedWriter.close();
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace(System.err);
            }
            finally
            {
                bufferedWriter = null;
            }
        }
    }
}
