/*
 * File: EventDistributorBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution;

import com.oracle.coherence.common.builders.Builder;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.Event;
import com.tangosol.io.Serializer;
import com.tangosol.net.Guardian;

/**
 * An {@link EventDistributorBuilder} is a {@link Builder} for {@link EventDistributor}s.
 * <p>
 * For convenience, several {@link EventDistributorBuilder} implementations are provided in the
 * com.oracle.coherence.patterns.pushreplication.providers package, including the default implementation based
 * on the Coherence Messaging pattern and thus Coherence itself.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventDistributorBuilder extends Builder<EventDistributor>
{
    /**
     * Establishes a {@link EventDistributor} with the specified symbolic name.
     * <p>
     * The symbolic name is an end-user friendly name for a {@link EventDistributor}.  This is typically used in logs and
     * monitoring/management.  It can be any {@link String}.  Together with a symbolic name, an application must also
     * suggest an external name for a {@link EventDistributor}.  This name is usually short-hand, perhaps not
     * user-friendly and/or represents some external resource.  {@link EventDistributor}s typically use this name when
     * integrating with external sources/other components.
     * <p>
     * NOTE 1: While it's possible to suggest an external name, a {@link EventDistributorBuilder} implementation may
     * override, reformat and even replace it with something it considers more appropriate.  To determine the real
     * external name a {@link EventDistributorBuilder} uses for a {@link EventDistributor} you should call
     * {@link EventDistributor#getIdentifier()}.
     * <p>
     * NOTE 2: Typically the implementation of this method would create the necessary messaging infrastructure
     * (like topics) that will be used for {@link Event} distribution.
     * <p>
     * NOTE 3: As this method is typically called using a Coherence Thread, care should be taken when
     * initializing external resources in that a). the implementation <strong>should not expect</strong> Coherence to
     * be up and running prior to this call, and b). initialization should not be a lengthy process as it might
     * cause Coherence to hang or a {@link Guardian} to restart the calling service.
     *
     * @param symbolicName              The symbolic name for which a {@link EventDistributor} is required.
     * @param suggestedExternalName     A suggested external name for the {@link EventDistributor}.  This is usually the
     *                                  same as the symbolicName, but applications are free to suggest anything really.
     *                                  The important thing is that the suggested name should be unique across
     *                                  the cluster, devices and/or other clusters that are using Event Distribution.
     * @param serializerBuilder         The {@link ParameterizedBuilder} that will produce a {@link Serializer} to be
     *                                  used for (de)serializing {@link Event}s during distribution.
     *
     * @return A {@link EventDistributor}
     */
    public EventDistributor realize(String                           symbolicName,
                                    String                           suggestedExternalName,
                                    ParameterizedBuilder<Serializer> serializerBuilder);
}
