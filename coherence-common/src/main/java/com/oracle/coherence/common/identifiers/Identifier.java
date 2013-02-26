/*
 * File: Identifier.java
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

package com.oracle.coherence.common.identifiers;

/**
 * An {@link Identifier} is an object that may be used to uniquely
 * identify some other object, class, concept, value or subject.
 * <p>
 * The "life-time" or "validity" of an {@link Identifier} is
 * entirely dependent on the "life-time" of the subject to which
 * the {@link Identifier} is identifying. Meaning, an {@link Identifier}
 * may represent the identity of a subject that has been destroyed, lost
 * or expired.
 * <p>
 * Likewise the "domain" in which an {@link Identifier} is "unique" is
 * entirely dependent on the "domain" of the subject to which
 * the {@link Identifier} is identifying.  Meaning, a single
 * {@link Identifier} may actually identify more than one subject
 * when it is used in different domains.
 * <p>
 * When used with Coherence, {@link Identifier}s are often used as
 * keys for entries in a cache.  In which case they must always
 * provide a "correct" implementation of {@link Object#hashCode()} and
 * {@link Object#equals(Object)}, together with a mechanism for serializing
 * and deserializing themselves.
 * <p>
 * An {@link Identifier} is essentially a marker interface to signify
 * that some object is used to identify subjects. There are no specific
 * requirements for a subject to be consider identifiable.
 * <p>
 * Copyright (c) 2008. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface Identifier
{
    /**
     * As this is a marker interface, there are no declarations.
     */
}
