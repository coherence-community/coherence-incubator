/*
 * File: Rendering.java
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

package com.oracle.coherence.common.finitestatemachines;

/**
 * Provides Render capabilities for FiniteStateMachines, in particular
 * their Models.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class Render
{
    /**
     * Produces a GraphViz representation of a Model.
     *
     * @param model  the Model to render
     *
     * @return a String representing a GraphViz model
     */
    public static <S extends Enum<S>> String asGraphViz(Model<S> model)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("digraph finite_state_machine {\n");
        builder.append("    rankdir=LR;\n");
        builder.append("    node [shape=circle];\n");

        for (S state : model.getStates())
        {
            builder.append("    " + state.name() + " [ label=\"" + state.name() + "\" ];\n");
        }

        for (S state : model.getStates())
        {
            for (Transition<S> transition : model.getTransitions())
            {
                if (transition.isStartingState(state))
                {
                    builder.append("    " + state.name() + " -> " + transition.getEndingState().name() + "[ label=\"" + transition.getName() + "\" ];\n");
                }
            }
        }
        builder.append("}\n");

        return builder.toString();
    }
}
