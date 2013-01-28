/*
 * File: ActivePassivePublisher.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.pushreplication.examples.activepassive;

import com.oracle.coherence.patterns.pushreplication.examples.PublisherHelper;

/**
 * The {@link ActivePassivePublisher} demonstrates how to establish and use a
 * RemoteInvocationPublisher to publish operations occurring on
 * cache entries (represented as EntryOperations) in an "Active" site to a
 * cache managed by a remote site.
 * <p>
 * This class will replicate active site Site1 to passive Site 2.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
public class ActivePassivePublisher
{
    /**
     * Main Application Entry Point
     *
     * @param args
     *
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {
        // ------------------------------
        // SETUP
        // ------------------------------
        PublisherHelper publisher = new PublisherHelper("publishing-cache",    // local site cache name
                                                        "site1");              // this site name

        // Start the background listener then load the cache
        publisher.loadCache();

        System.out.println("success.");
    }
}
