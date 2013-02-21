/*
 * File: SupervisedResourceProvider.java
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

package com.oracle.coherence.common.resourcing;

/**
 * A {@link SupervisedResourceProvider} is a {@link ResourceProvider} that has knowledge of the last known
 * availability of a Resource, the purpose of which is to ensure that the Resource is protected from repeated
 * requests to access it when may it be unavailable for a long period of time.
 * <p>
 * This interface is part of a work-around for INC-757 (and COH-3681). It's designed to restrict repeated attempts
 * to request a Resource that itself may take significant time and/or infrastructure to retrieve.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface SupervisedResourceProvider<T> extends ResourceProvider<T>
{
    /**
     * Returns if the underlying Resource may be requested for accessed.
     * <p>
     * While a Resource may be accessible, this does not mean that it is actually available or usable.
     * Being accessible simply determines if an attempt to access a resource is possible at the current point in time
     * or if it will be futile (known to fail immediately).
     *
     * @return <code>true</code> the Resource is accessible, <code>false</code> otherwise.
     */
    public boolean isResourceAccessible();


    /**
     * Applications should call this method when they determine a Resource they have acquired from the associated
     * {@link SupervisedResourceProvider} is no longer available.  This ensures that a {@link SupervisedResourceProvider}
     * can protect repeated requests for the Resource from other parts of an Application.
     */
    public void resourceNoLongerAvailable();
}
