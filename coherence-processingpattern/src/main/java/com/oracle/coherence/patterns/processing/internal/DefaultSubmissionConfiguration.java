/*
 * File: DefaultSubmissionConfiguration.java
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

package com.oracle.coherence.patterns.processing.internal;

import com.oracle.coherence.patterns.processing.DispatcherFilter;
import com.oracle.coherence.patterns.processing.SubmissionConfiguration;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DefaultSubmissionConfiguration} Provides additional data about a submission.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class DefaultSubmissionConfiguration implements ExternalizableLite, PortableObject, SubmissionConfiguration
{
    /**
     * The groupAffinity object is used to co-locate processes that should be
     * executed on the same node.
     */
    private Object groupAffinity;

    /**
     * The amount of time in milliseconds a request should wait before being
     * executed. The time of execution will be based on the time of the system
     * where this request is dispatched from.
     */
    private long submissionDelay;

    /**
     * Map of generic data associated with the request.
     */
    @SuppressWarnings("rawtypes")
    private Map configurationDataMap;

    /**
     * The {@link DispatcherFilter} for this {@link SubmissionConfiguration}.
     */
    private DispatcherFilter dispatcherFilter;


    /**
     * Default constructor.
     */
    @SuppressWarnings("rawtypes")
    public DefaultSubmissionConfiguration()
    {
        configurationDataMap = new HashMap();
    }


    /**
     * Constructor which takes a delay parameter.
     *
     * @param configurationMap the configuration data map
     */
    @SuppressWarnings("rawtypes")
    public DefaultSubmissionConfiguration(final Map configurationMap)
    {
        this();

        if (configurationMap != null)
        {
            configurationDataMap = configurationMap;
        }
    }


    /**
     * Constructor which takes a delay parameter.
     *
     * @param lDelay           the delay before the request gets processed
     * @param dispatcherFilter the {@link DispatcherFilter} to use
     *
     */
    public DefaultSubmissionConfiguration(final long             lDelay,
                                          final DispatcherFilter dispatcherFilter)
    {
        this();
        submissionDelay       = lDelay;
        this.dispatcherFilter = dispatcherFilter;
    }


    /**
     * {@inheritDoc}
     */
    public Object getGroupAffinity()
    {
        return groupAffinity;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public Map getConfigurationDataMap()
    {
        return configurationDataMap;
    }


    /**
     * {@inheritDoc}
     */
    public long getSubmissionDelay()
    {
        return submissionDelay;
    }


    /**
     * {@inheritDoc}
     */
    public DispatcherFilter getDispatcherFilter()
    {
        return dispatcherFilter;
    }


    /**
     * Set the groupAffinity object used to co-locate
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}s
     * that should be processed on the same node.
     *
     * @param oGroupAffinity the object used to co-locate {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}s
     *                       that should be processed on the same node.
     */
    public void setGroupAffinity(final Object oGroupAffinity)
    {
        this.groupAffinity = oGroupAffinity;
    }


    /**
     * Set the {@link Map} with configuration data.
     *
     * @param data the configuration data map
     */
    @SuppressWarnings("rawtypes")
    public void setConfigurationDataMap(final Map data)
    {
        this.configurationDataMap = data;
    }


    /**
     * Set amount of time a
     * {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     * should wait before being executed in milliseconds.
     *
     * @param lSubmissionDelay the amount of time a {@link com.oracle.coherence.patterns.processing.internal.DefaultSubmission}
     *                         should wait before being executed in milliseconds
     */
    public void setSubmissionDelay(final long lSubmissionDelay)
    {
        this.submissionDelay = lSubmissionDelay;
    }


    /**
     * Sets the {@link DispatcherFilter} for this {@link SubmissionConfiguration}.
     *
     * @param dispatcherFilter the {@link DispatcherFilter} to set
     */
    public void setDispatcherFilter(final DispatcherFilter dispatcherFilter)
    {
        this.dispatcherFilter = dispatcherFilter;
    }


    /**
     * Return a String representation of the {@link DefaultSubmissionConfiguration}.
     *
     * @return a String representation of the {@link DefaultSubmissionConfiguration}
     */
    @Override
    public String toString()
    {
        return String.format("%s{requestDelay=%d, data=[%s] affinity=%s}",
                             this.getClass().getName(),
                             submissionDelay,
                             configurationDataMap,
                             groupAffinity);
    }


    /**
    * {@inheritDoc}
    */
    @SuppressWarnings("rawtypes")
    public void readExternal(final DataInput in) throws IOException
    {
        this.submissionDelay      = ExternalizableHelper.readLong(in);
        this.configurationDataMap = new HashMap();
        ExternalizableHelper.readMap(in, this.configurationDataMap, Thread.currentThread().getContextClassLoader());
        this.groupAffinity = ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final DataOutput out) throws IOException
    {
        ExternalizableHelper.writeLong(out, this.submissionDelay);
        ExternalizableHelper.writeMap(out, this.configurationDataMap);
        ExternalizableHelper.writeObject(out, this.groupAffinity);
    }


    /**
    * {@inheritDoc}
    */
    @SuppressWarnings("rawtypes")
    public void readExternal(final PofReader reader) throws IOException
    {
        this.submissionDelay      = reader.readLong(0);
        this.configurationDataMap = reader.readMap(1, new HashMap());
        this.groupAffinity        = reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(final PofWriter writer) throws IOException
    {
        writer.writeLong(0, this.submissionDelay);
        writer.writeMap(1, this.configurationDataMap);
        writer.writeObject(2, this.groupAffinity);
    }
}
