/*
 * File: SingleFunctorResult.java
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

package com.oracle.coherence.patterns.functor.internal;

import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.functor.Functor;
import com.oracle.coherence.patterns.functor.FunctorSubmitter;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The {@link SingleFunctorResult} captures the result of executing
 * a single {@link Functor}.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class SingleFunctorResult<T> implements FunctorResult, ExternalizableLite, PortableObject
{
    /**
     * The {@link Identifier} of the {@link FunctorSubmitter} that submitted
     * the {@link Functor} for execution.
     */
    private Identifier functorSubmitterIdentifier;

    /**
     * The {@link Identifier} that will be used to locate the result
     * of the {@link Functor} execution in the functor-results cache.
     */
    private Identifier functorResultIdentifier;

    /**
     * The result value of executing the {@link Functor}.
     */
    private T value;

    /**
     * The (possible) exception thrown during the execution of the {@link Functor}.
     */
    private Exception exception;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public SingleFunctorResult()
    {
    }


    /**
     * Standard Constructor.
     *
     * @param functorSubmitterIdentifier
     * @param functorResultIdentifier
     * @param value
     * @param exception
     */
    public SingleFunctorResult(Identifier functorSubmitterIdentifier,
                               Identifier functorResultIdentifier,
                               T          value,
                               Exception  exception)
    {
        this.functorSubmitterIdentifier = functorSubmitterIdentifier;
        this.functorResultIdentifier    = functorResultIdentifier;
        this.value                      = value;
        this.exception                  = exception;
    }


    /**
     * Returns the {@link Identifier} for the {@link FunctorSubmitter} that
     * submitted the {@link Functor} for execution.
     */
    public Identifier getFunctorSubmitterIdentifier()
    {
        return functorSubmitterIdentifier;
    }


    /**
     * Returns the {@link Identifier} that the {@link FunctorSubmitter} will
     * use to differentiate results from {@link Functor} executions.
     */
    public Identifier getFunctorResultIdentifier()
    {
        return functorResultIdentifier;
    }


    /**
     * Returns the resulting value from executing the {@link Functor}
     * (may be null).
     */
    public T getValue()
    {
        return value;
    }


    /**
     * Return the {@link Exception} that was (possibly) thrown
     * during the execution of the {@link Functor} that produced
     * this result.
     */
    public Exception getException()
    {
        return exception;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isComplete()
    {
        // The result for a single {@link Functor} execution is always complete.
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput input) throws IOException
    {
        this.functorSubmitterIdentifier = (Identifier) ExternalizableHelper.readObject(input);
        this.functorResultIdentifier    = (Identifier) ExternalizableHelper.readObject(input);
        this.value                      = (T) ExternalizableHelper.readObject(input);
        this.exception                  = (Exception) ExternalizableHelper.readObject(input);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput output) throws IOException
    {
        ExternalizableHelper.writeObject(output, functorSubmitterIdentifier);
        ExternalizableHelper.writeObject(output, functorResultIdentifier);
        ExternalizableHelper.writeObject(output, value);
        ExternalizableHelper.writeObject(output, exception);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader reader) throws IOException
    {
        this.functorSubmitterIdentifier = (Identifier) reader.readObject(0);
        this.functorResultIdentifier    = (Identifier) reader.readObject(1);
        this.value                      = (T) reader.readObject(2);
        this.exception                  = (Exception) reader.readObject(3);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, functorSubmitterIdentifier);
        writer.writeObject(1, functorResultIdentifier);
        writer.writeObject(2, value);
        writer.writeObject(3, exception);
    }
}
