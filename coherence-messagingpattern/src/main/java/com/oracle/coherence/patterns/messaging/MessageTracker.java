/*
 * File: MessageTracker.java
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

package com.oracle.coherence.patterns.messaging;

import com.oracle.coherence.common.identifiers.Identifier;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A {@link MessageTracker} is responsible for tracking messages identifiers.  The MessageTracker
 * is used both internally to route messages through the system and externally to consume messages.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */

public interface MessageTracker extends ExternalizableLite, PortableObject
{
    /**
     * Add a {@link MessageIdentifier} to the {@link MessageTracker}.
     *
     * @param messageIdentifier message identifier
     * @return true if the message was added
     */
    boolean add(MessageIdentifier messageIdentifier);


    /**
     * Add all the contents of a {@link MessageTracker} to this {@link MessageTracker}.
     *
     * @param tracker message tracker
     * @return true if the message was added
     */
    boolean addAll(MessageTracker tracker);


    /**
     * Clear the {@link MessageTracker}.
     */
    void clear();


    /**
     * Get the last {@link MessageIdentifier} has been added to this {@link MessageTracker}.
     *
     * @return the last {@link MessageIdentifier} added to the {@link MessageTracker}
     */
    MessageIdentifier getLast();


    /**
     * Get an {@link ArrayList} of {@link MessageKey}s for all {@link MessageIdentifier}s in the {@link MessageTracker}.
     *
     * @param destinationIdentifier destination identifier
     * @return {@link ArrayList}s of {@link MessageKey}s
     */
    ArrayList<MessageKey> getMessageKeys(Identifier destinationIdentifier);


    /**
     * Check if the input {@link MessageIdentifier} has been added to the range at some point in time.
     *
     * @param messageIdentifier message identifier
     * @return true if the {@link MessageIdentifier} has been added to the range.
     */
    boolean isDuplicateMessage(MessageIdentifier messageIdentifier);


    /**
     * Return true if the {@link DefaultMessageTracker} is empty.
     *
     * @return true if the tracker is empty
     */
    boolean isEmpty();


    /**
     * Get an iterator for this {@link MessageTracker}.
     *
     * @return iterator
     */
    Iterator<MessageIdentifier> iterator();


    /**
     * Remove a {@link MessageIdentifier} from the {@link DefaultMessageTracker}.
     *
     * @param messageIdentifier message identifier
     * @return true if the message was removed
     */
    boolean remove(MessageIdentifier messageIdentifier);


    /**
     * Remove all the {@link MessageIdentifier}s of a {@link DefaultMessageTracker} from this {@link DefaultMessageTracker}.
     *
     *
     * @param tracker message tracker
     * @return true if the message was removed
     */
    boolean removeAll(MessageTracker tracker);


    /**
     * Get the number of {@link MessageIdentifier}s in the {@link DefaultMessageTracker}.
     *
     * @return number of {@link MessageIdentifier}s
     */
    long size();


    /**
     * Format the {@link MessageTracker} as a string.
     *
     * @return string
     */
    String toString();
}
