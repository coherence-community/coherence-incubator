/*
 * File: AbstractApplicationGroup.java
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

package com.oracle.coherence.common.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link AbstractApplicationGroup} is a base implementation of an {@link ApplicationGroup}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A> The type of {@link Application} that belongs to the {@link ApplicationGroup}.
 *
 * @author Brian Oliver
 */
@Deprecated
public abstract class AbstractApplicationGroup<A extends Application> implements ApplicationGroup<A>
{
    /**
     * The collection of {@link Application}s that belong to the {@link ApplicationGroup}.
     */
    protected ArrayList<A> m_applications;


    /**
     * Constructs an {@link AbstractApplicationGroup} given a list of {@link Application}s.
     *
     * @param applications  The list of {@link Application}s in the {@link ApplicationGroup}.
     */
    public AbstractApplicationGroup(List<A> applications)
    {
        m_applications = new ArrayList<A>(applications);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<A> iterator()
    {
        return m_applications.iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy()
    {
        for (A application : m_applications)
        {
            if (application != null)
            {
                try
                {
                    application.destroy();
                }
                catch (Exception e)
                {
                    // skip: we always ignore
                }
            }
        }
    }
}
