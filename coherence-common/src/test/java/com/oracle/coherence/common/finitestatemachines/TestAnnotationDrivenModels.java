/*
 * File: TestAnnotationDrivenModels.java
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

import com.oracle.coherence.common.finitestatemachines.annotations.OnEnterState;
import com.oracle.coherence.common.finitestatemachines.annotations.OnExitState;
import com.oracle.coherence.common.finitestatemachines.annotations.OnTransition;
import com.oracle.coherence.common.finitestatemachines.annotations.Transition;
import com.oracle.coherence.common.finitestatemachines.annotations.Transitions;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link AnnotationDrivenModel}s.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class TestAnnotationDrivenModels
{
    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an {@link OnTransition} that specifies an invalid/unmatching state class.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTransitionStateClass() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidTransitionStateClassAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid state class");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an {@link OnTransition} that specifies an invalid from state.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTransitionFromState() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidTransitionFromStateAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid from state");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an {@link OnTransition} that specifies an invalid to state.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTransitionToState() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidTransitionToStateAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid to state");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an {@link OnTransition} for a method with an incorrect signature.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTransitionMethod() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidTransitionMethod());

        Assert.fail("Should not be able to create a model when specifying an invalid method signature");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an invalid {@link OnEnterState} state class.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOnEnterStateClass() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidOnEnterStateClassAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid state class");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an invalid {@link OnEnterState} state name.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOnEnterState() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidOnEnterStateAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid state");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an invalid {@link OnEnterState} method signature.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOnEnterStateMethod() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidOnEnterStateMethod());

        Assert.fail("Should not be able to create a model when specifying an invalid method signature");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an invalid {@link OnExitState} state class.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOnExitStateClass() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidOnExitStateClassAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid state class");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an invalid {@link OnExitState} state name.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOnExitState() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidOnExitStateAnnotation());

        Assert.fail("Should not be able to create a model when specifying an invalid state");
    }


    /**
     * Ensure that we can't create a {@link Model} from a class containing
     * an invalid {@link OnExitState} method signature.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOnExitStateMethod() throws Exception
    {
        new AnnotationDrivenModel<Light>(Light.class, new InvalidOnExitStateMethod());

        Assert.fail("Should not be able to create a model when specifying an invalid method signature");
    }


    /**
     * A class containing an invalid {@link OnEnterState} state annotation.
     */
    public static class InvalidOnEnterStateAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnEnterState("hello world")
        public void onInvalidEnterState()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnEnterState} class annotation.
     */
    public static class InvalidOnEnterStateClassAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnEnterState("hello world")
        public void onInvalidEnterState()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnEnterState} method signature.
     */
    public static class InvalidOnEnterStateMethod
    {
        /**
         * {@inheritDoc}
         */
        @OnEnterState("ON")
        public void onInvalidEnterState()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnExitState} annotation.
     */
    public static class InvalidOnExitStateAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnExitState("hello world")
        public void onInvalidExitState()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnExitState} class annotation.
     */
    public static class InvalidOnExitStateClassAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnExitState("hello world")
        public void onInvalidExitState()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnExitState} method signature.
     */
    public static class InvalidOnExitStateMethod
    {
        /**
         * {@inheritDoc}
         */
        @OnExitState("ON")
        public void onInvalidExitState()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnTransition} from state annotation.
     */
    @Transitions({@Transition(
        name       = "Turning On",
        fromStates = "OFF",
        toState    = "ON"
    ) , @Transition(
        name       = "Turning Off",
        fromStates = "ON",
        toState    = "OFF"
    ) , @Transition(
        name       = "Broken",
        fromStates = {"ON", "OFF"},
        toState    = "BROKEN"
    ) })
    public static class InvalidTransitionFromStateAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnTransition(
            fromStates = {"hello world"},
            toStates   = {"OFF"}
        )
        public void onInvalidTransistion()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnTransition} method signature.
     */
    @Transitions({@Transition(
        name       = "Turning On",
        fromStates = "OFF",
        toState    = "ON"
    ) , @Transition(
        name       = "Turning Off",
        fromStates = "ON",
        toState    = "OFF"
    ) , @Transition(
        name       = "Broken",
        fromStates = {"ON", "OFF"},
        toState    = "BROKEN"
    ) })
    public static class InvalidTransitionMethod
    {
        /**
         * {@inheritDoc}
         */
        @OnTransition(
            fromStates = {"ON"},
            toStates   = {"OFF"}
        )
        public void onInvalidTransistion()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnTransition} class annotation.
     */
    @Transitions({@Transition(
        name       = "Turning On",
        fromStates = "OFF",
        toState    = "ON"
    ) , @Transition(
        name       = "Turning Off",
        fromStates = "ON",
        toState    = "OFF"
    ) , @Transition(
        name       = "Broken",
        fromStates = {"ON", "OFF"},
        toState    = "BROKEN"
    ) })
    public static class InvalidTransitionStateClassAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnTransition(
            fromStates = {"hello"},
            toStates   = {"world"}
        )
        public void onInvalidTransistion()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * A class containing an invalid {@link OnTransition} to state annotation.
     */
    @Transitions({@Transition(
        name       = "Turning On",
        fromStates = "OFF",
        toState    = "ON"
    ) , @Transition(
        name       = "Turning Off",
        fromStates = "ON",
        toState    = "OFF"
    ) , @Transition(
        name       = "Broken",
        fromStates = {"ON", "OFF"},
        toState    = "BROKEN"
    ) })
    public static class InvalidTransitionToStateAnnotation
    {
        /**
         * {@inheritDoc}
         */
        @OnTransition(
            fromStates = {"ON"},
            toStates   = {"hello world"}
        )
        public void onInvalidTransistion()
        {
            throw new UnsupportedOperationException();
        }
    }
}
