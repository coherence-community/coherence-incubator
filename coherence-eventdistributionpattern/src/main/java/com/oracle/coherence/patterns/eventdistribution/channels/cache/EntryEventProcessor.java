/*
 * File: EntryEventProcessor.java
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

package com.oracle.coherence.patterns.eventdistribution.channels.cache;

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.EntryEvent;
import com.oracle.coherence.common.events.Event;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.patterns.eventdistribution.channels.cache.ConflictResolution.Operation;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntryEvent;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link EntryEventProcessor} is an {@link EntryProcessor} that attempts to replicate an {@link EntryEvent} on a
 * {@link BinaryEntry}.
 * <p>
 * Prior to replicating the event the provided {@link ConflictResolver} is consulted.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class EntryEventProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(EntryEventProcessor.class.getName());

    /**
     * The {@link Event} to reproduce.
     */
    protected DistributableEntryEvent entryEvent;

    /**
     * The {@link ParameterizedBuilder} to realize the {@link ConflictResolver} to perform the {@link EntryEvent}.</p>
     */
    protected ParameterizedBuilder<ConflictResolver> conflictResolverBuilder;

    /**
     * The cache name of the target cache.
     */
    protected String targetCacheName;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public EntryEventProcessor()
    {
        // SKIP: deliberately empty
    }


    /**
     * Standard Constructor.
     *
     * @param entryEvent
     * @param conflictResolverBuilder
     * @param targetCacheName
     */
    public EntryEventProcessor(DistributableEntryEvent                entryEvent,
                               ParameterizedBuilder<ConflictResolver> conflictResolverBuilder,
                               String                                 targetCacheName)
    {
        this.entryEvent              = entryEvent;
        this.conflictResolverBuilder = conflictResolverBuilder;
        this.targetCacheName         = targetCacheName;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object process(Entry entry)
    {
        // the target entry is the entry being processed
        BinaryEntry targetEntry = (BinaryEntry) entry;

        // NOTE: here we assume that the DistributableEntry and the targetEntry have the same
        // backing map context

        // set the context of the entry
        entryEvent.getEntry().setContext(targetEntry.getContext());

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Entering process method");
        }

        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "..Processing Event of {0}", entryEvent);
            logger.log(Level.FINEST, "..Calling registered conflict resolver resolve method.");
        }

        // realize the conflict resolver to resolve any conflicts
        ConflictResolver conflictResolver = conflictResolverBuilder == null
                                            ? new BruteForceConflictResolver()
                                            : conflictResolverBuilder.realize(EmptyParameterProvider.INSTANCE);
        ConflictResolution result = conflictResolver.resolve(entryEvent, targetEntry);

        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "..Returning from resolve method");
        }

        // Operation = UseTgtValue is a noop.

        if (result.getOperation() == Operation.UseInComingValue)
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "..Using src value");
            }

            // Extract decorated value from the source and apply it as the
            // new value of the target.

            targetEntry.updateBinaryValue((Binary) entryEvent.getEntry().getBinaryValue());
        }
        else if (result.getOperation() == Operation.UseMergedValue)
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "..Using merged value");
            }

            // convert the merged value into a binary
            BackingMapManagerContext bmctx          = targetEntry.getContext();
            Converter                toInternal     = bmctx.getValueToInternalConverter();
            Binary                   binMergedValue = (Binary) toInternal.convert(result.getMergedValue());

            // strip the local value of decorations
            Binary binLocalValue = targetEntry.isPresent()
                                   ? ExternalizableHelper.getUndecorated(targetEntry.getBinaryValue()) : null;

            if (binMergedValue.equals(binLocalValue))
            {
                // when the merged value and the existing value are the same, we ignore the request to merge
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING,
                               String
                               .format("Ignoring request to use a merged value that is the same as the local value [%s]",
                                   targetEntry));
                }
            }
            else
            {
                // we use the merged value as though it's a local put
                targetEntry.updateBinaryValue(binMergedValue);
            }
        }
        else if (result.getOperation() == Operation.Remove && targetEntry.isPresent())
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "..Removing entry");
            }

            // simply add the "mark for erase decoration"
            Map srcDecorations =
                (Map<?, ?>) targetEntry.getContext().getInternalValueDecoration(entryEvent.getEntry().getBinaryValue(),
                                                                                BackingMapManagerContext.DECO_CUSTOM);

            srcDecorations.put(DistributableEntry.MARKED_FOR_ERASE_DECORATION_KEY, new Boolean(true));

            Binary decoratedTargetValue =
                (Binary) targetEntry.getContext().addInternalValueDecoration(targetEntry.getBinaryValue(),
                                                                             BackingMapManagerContext.DECO_CUSTOM,
                                                                             srcDecorations);

            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "..remove is decorating tgtEntry with MarkedForErase");
                logger.log(Level.FINEST, "..remove is forcing a synthetic backing map put");
            }

            targetEntry.getBackingMap().put(targetEntry.getBinaryKey(), decoratedTargetValue);

            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "..remove is calling remove for target entry");
            }

            targetEntry.remove(false);
        }
        else
        {
            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "..Using target value");
            }
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER, "Exiting process method");
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in) throws IOException
    {
        this.entryEvent              = (DistributableEntryEvent) ExternalizableHelper.readObject(in);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) ExternalizableHelper.readObject(in);
        this.targetCacheName         = ExternalizableHelper.readSafeUTF(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, entryEvent);
        ExternalizableHelper.writeObject(out, conflictResolverBuilder);
        ExternalizableHelper.writeSafeUTF(out, targetCacheName);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.entryEvent              = (DistributableEntryEvent) reader.readObject(1);
        this.conflictResolverBuilder = (ParameterizedBuilder<ConflictResolver>) reader.readObject(2);
        this.targetCacheName         = reader.readString(3);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, entryEvent);
        writer.writeObject(2, conflictResolverBuilder);
        writer.writeString(3, targetCacheName);
    }
}
