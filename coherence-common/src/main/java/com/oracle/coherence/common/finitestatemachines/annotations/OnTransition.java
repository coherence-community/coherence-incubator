/*
 * File: OnTransition.java
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

package com.oracle.coherence.common.finitestatemachines.annotations;

import com.oracle.coherence.common.finitestatemachines.Transition;
import com.oracle.coherence.common.finitestatemachines.TransitionAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates that a method defines an {@link TransitionAction} to
 * be performed as part of a {@link Transition}.
 * <p>
 * Note: The signature of the annotated method should be the same as that defined by
 * {@link TransitionAction#onTransition(String, Enum, Enum, com.oracle.coherence.common.finitestatemachines.Event, ExecutionContext)
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnTransition
{
    /**
     * The name of the states from which the {@link Transition} may occur.
     */
    String[] fromStates();

    /**
     * The name of the states to which the {@link Transition} will occur.
     */
    String[] toStates();
}
