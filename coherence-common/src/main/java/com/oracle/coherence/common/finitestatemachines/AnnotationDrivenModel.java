/*
 * File: AnnotationDrivenModel.java
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
import com.oracle.coherence.common.finitestatemachines.annotations.Transitions;
import com.oracle.coherence.common.util.ReflectionHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map;

/**
 * An {@link AnnotationDrivenModel} is a {@link Model} created through extracting
 * information defined by {@link Transitions} annotation.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of the state for the {@link AnnotationDrivenModel}
 * @param <f>  the type of {@link Transitions}
 *
 * @author Brian Oliver
 */
public class AnnotationDrivenModel<S extends Enum<S>> implements Model<S>
{
    /**
     * The actual {@link Model}.
     */
    private SimpleModel<S> m_model;


    /**
     * Constructs an {@link AnnotationDrivenModel} based on the specified
     * annotated class.
     *
     * @param clzState  the {@link Class} of the state of the {@link Transitions}
     * @param instance  the instance from which the {@link Model} will be
     *                  reflected
     */
    public AnnotationDrivenModel(Class<S> clzState,
                                 Object   instance)
    {
        if (instance == null)
        {
            throw new IllegalArgumentException(String.format("Can't create an %s from a null instance",
                                                             this.getClass().getName()));
        }
        else
        {
            // construct the model based on the annotations
            Class<?> clzInstance = instance.getClass();

            m_model = new SimpleModel<S>(clzState);

            // define maps to hold the reflected transition information
            EnumMap<S, EnumMap<S, String>> mapTransitionNames = new EnumMap<S, EnumMap<S, String>>(clzState);
            EnumMap<S, EnumMap<S, TransitionAction<S>>> mapTransitionActions =
                new EnumMap<S, EnumMap<S, TransitionAction<S>>>(clzState);

            for (S state : m_model.getStates())
            {
                mapTransitionNames.put(state, new EnumMap<S, String>(clzState));
                mapTransitionActions.put(state, new EnumMap<S, TransitionAction<S>>(clzState));
            }

            // reflect out the defined transitions
            Transitions annTransitions = clzInstance.getAnnotation(Transitions.class);

            if (annTransitions != null)
            {
                // determine the valid state transitions based on the
                // @Transitions annotation in the specified class
                for (com.oracle.coherence.common.finitestatemachines.annotations.Transition annTransition :
                    annTransitions.value())
                {
                    // determine the name of the transition
                    String sProvidedTransitionName = annTransition.name();

                    // ensure the transition name is null if it wasn't specified
                    sProvidedTransitionName = sProvidedTransitionName == null
                                              || sProvidedTransitionName.trim().isEmpty() ? null
                                                                                          : sProvidedTransitionName;

                    // determine the ending state for the transition
                    String sStateToName = annTransition.toState();
                    S      stateTo      = m_model.getState(sStateToName);

                    if (stateTo == null)
                    {
                        throw new IllegalArgumentException(String
                            .format("The %s defined on %s declares a to state %s that is not defined by %s.",
                                    annTransition, clzInstance, sStateToName, clzState));
                    }

                    // determine the starting states for the transition
                    for (String sStateFromName : annTransition.fromStates())
                    {
                        // determine the starting states for the transition
                        S stateFrom = m_model.getState(sStateFromName);

                        if (stateFrom == null)
                        {
                            throw new IllegalArgumentException(String
                                .format("The %s defined on %s declares a from state %s that is not defined by %s.",
                                        annTransition, clzInstance, sStateFromName, clzState));
                        }
                        else
                        {
                            // ensure we have a transition name
                            String sTransitionName = sProvidedTransitionName == null ? String.format("%s to %s",
                                                                                                     stateFrom.name(),
                                                                                                     stateTo.name()) : sProvidedTransitionName;

                            // define the transition
                            mapTransitionNames.get(stateFrom).put(stateTo, sTransitionName);
                        }
                    }
                }
            }

            // reflect and add state entry, exit and transition actions from
            // annotated methods into the model
            for (Method method : clzInstance.getMethods())
            {
                // add the StateEntryAction for the method (if annotated)
                OnEnterState annOnEnterState = method.getAnnotation(OnEnterState.class);

                if (annOnEnterState != null)
                {
                    // determine the state
                    S state = m_model.getState(annOnEnterState.value());

                    if (state == null)
                    {
                        throw new IllegalArgumentException(String
                            .format("The %s annotation on method %s defined in %s declares the state %s that is not defined in %s.",
                                    annOnEnterState, method, clzInstance, annOnEnterState.value(), clzState));
                    }

                    // ensure the method has the correct signature for the
                    if (ReflectionHelper.isCompatibleMethod(method, Modifier.PUBLIC, Instruction.class, clzState,
                                                            clzState, Event.class, ExecutionContext.class))
                    {
                        // register the StateEntryAction
                        m_model.addStateEntryAction(state, new StateEntryActionMethod<S>(instance, method));
                    }
                    else
                    {
                        throw new IllegalArgumentException(String
                            .format("The method %s defined in class %s annotated with %s is not compatible with the required method signature 'Instruction method(State, State, Context<State>);'.",
                                    method, clzInstance, annOnEnterState));
                    }
                }

                // add the StateEntryAction for the method (if annotated)
                OnExitState annOnExitState = method.getAnnotation(OnExitState.class);

                if (annOnExitState != null)
                {
                    // determine the state
                    S state = m_model.getState(annOnExitState.value());

                    if (state == null)
                    {
                        throw new IllegalArgumentException(String
                            .format("The %s annotation on method %s defined in %s declares the state %s that is not defined in %s.",
                                    annOnExitState, method, clzInstance, annOnExitState.value(), clzState));
                    }

                    // ensure the method has the correct signature for the
                    if (ReflectionHelper.isCompatibleMethod(method, Modifier.PUBLIC, Void.TYPE, clzState, Event.class,
                                                            ExecutionContext.class))
                    {
                        // register the StateExitAction
                        m_model.addStateExitAction(state, new StateExitActionMethod<S>(instance, method));
                    }
                    else
                    {
                        throw new IllegalArgumentException(String
                            .format("The method %s defined in class %s annotated with %s is not compatible with the required method signature 'void method(State, Context<State>);'.",
                                    method, clzInstance, annOnExitState));
                    }
                }

                // add the TransitionAction for the method (if annotated)
                OnTransition annOnTransition = method.getAnnotation(OnTransition.class);

                if (annOnTransition != null)
                {
                    // ensure that the method has the correct signature
                    if (ReflectionHelper.isCompatibleMethod(method, Modifier.PUBLIC, Void.TYPE, String.class, clzState,
                                                            clzState, Event.class, ExecutionContext.class))
                    {
                        TransitionAction<S> action = new TransitionActionMethod<S>(instance, method);

                        // add the action for each of the transitions
                        for (String sStateFromName : annOnTransition.fromStates())
                        {
                            // determine the starting state for the transition
                            S stateFrom = m_model.getState(sStateFromName);

                            if (stateFrom == null)
                            {
                                throw new IllegalArgumentException(String
                                    .format("The %s defined on method %s in %s declares a from state %s that is not defined by %s.",
                                            annOnTransition, method, clzInstance, sStateFromName, clzState));
                            }
                            else
                            {
                                for (String sStateToName : annOnTransition.toStates())
                                {
                                    // determine the ending state for the transition
                                    S stateTo = m_model.getState(sStateToName);

                                    if (stateTo == null)
                                    {
                                        throw new IllegalArgumentException(String
                                            .format("The %s defined on method %s in %s declares a from state %s that is not defined by %s.",
                                                    annOnTransition, method, clzInstance, sStateToName, clzState));
                                    }
                                    else if (mapTransitionNames.get(stateFrom).containsKey(stateTo))
                                    {
                                        // add the transition action to the transition
                                        mapTransitionActions.get(stateFrom).put(stateTo, action);
                                    }
                                    else
                                    {
                                        throw new IllegalArgumentException(String
                                            .format("The %s defined on method %s in %s specifies a transition from %s to %s that is not defined by the Finite State Machine.",
                                                    annOnTransition, method, clzInstance, stateFrom, stateTo));
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(String
                            .format("The method %s defined in class %s annotated with %s is not compatible with the required method signature 'void method(String, State, State, Event<State>, Context<State>);'.",
                                    method, clzInstance, annOnTransition));

                    }
                }
            }

            // define the transitions on the model
            for (S stateFrom : m_model.getStates())
            {
                for (S stateTo : mapTransitionNames.keySet())
                {
                    String sTransitionName = mapTransitionNames.get(stateFrom).get(stateTo);

                    if (sTransitionName != null)
                    {
                        TransitionAction<S> action = mapTransitionActions.get(stateFrom).get(stateTo);

                        m_model.addTransition(new Transition<S>(sTransitionName, stateFrom, stateTo, action));
                    }
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<S> getStateClass()
    {
        return m_model.getStateClass();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<S, StateEntryAction<S>> getStateEntryActions()
    {
        return m_model.getStateEntryActions();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<S, StateExitAction<S>> getStateExitActions()
    {
        return m_model.getStateExitActions();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public S[] getStates()
    {
        return m_model.getStates();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Transition<S>> getTransitions()
    {
        return m_model.getTransitions();
    }


    /**
     * A {@link StateEntryActionMethod} is an {@link StateEntryAction}
     * implementation based on a specified {@link Method} and instance.
     */
    private static class StateEntryActionMethod<S extends Enum<S>> implements StateEntryAction<S>
    {
        /**
         * The instance on which to invoke the methods.
         */
        private Object m_instance;

        /**
         * The {@link Method} to execute for the {@link StateEntryAction}.
         */
        private Method m_method;


        /**
         * Constructs an {@link StateEntryActionMethod}.
         *
         * @param instance  the instance on which to invoke the {@link StateEntryAction}
         * @param method    the method to use for performing the {@link StateEntryAction}
         */
        public StateEntryActionMethod(Object instance,
                                      Method method)
        {
            m_instance = instance;
            m_method   = method;
        }


        /**
         * {@inheritDoc}
         */
        public Instruction onEnterState(S                previousState,
                                        S                newState,
                                        Event<S>         event,
                                        ExecutionContext context)
        {
            try
            {
                return (Instruction) m_method.invoke(m_instance, previousState, newState, event, context);
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * A {@link StateExitActionMethod} is an {@link StateExitAction}
     * implementation based on a specified {@link Method} and instance.
     */
    private static class StateExitActionMethod<S extends Enum<S>> implements StateExitAction<S>
    {
        /**
         * The instance on which to invoke the methods.
         */
        private Object m_instance;

        /**
         * The {@link Method} to execute for the {@link StateEntryAction}.
         */
        private Method m_method;


        /**
         * Constructs an {@link StateExitActionMethod}.
         *
         * @param instance  the instance on which to invoke the {@link StateEntryAction}
         * @param method    the method to use for performing the {@link StateEntryAction}
         */
        public StateExitActionMethod(Object instance,
                                     Method method)
        {
            m_instance = instance;
            m_method   = method;
        }


        /**
                 * {@inheritDoc}
         */
        public void onExitState(S                state,
                                Event<S>         event,
                                ExecutionContext context)
        {
            try
            {
                m_method.invoke(m_instance, state, event, context);
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * An {@link TransitionActionMethod} is an {@link TransitionAction}
     * implementation based on a specified {@link Method} and instance.
     */
    private static class TransitionActionMethod<S extends Enum<S>> implements TransitionAction<S>
    {
        /**
         * The instance on which to invoke the methods.
         */
        private Object m_instance;

        /**
         * The {@link Method} to execute for the {@link TransitionAction}.
         */
        private Method m_method;


        /**
         * Constructs an {@link TransitionActionMethod}.
         *
         * @param instance  the instance on which to invoke the {@link TransitionAction}
         * @param method    the method to use for performing the {@link TransitionAction}
         */
        public TransitionActionMethod(Object instance,
                                      Method method)
        {
            m_instance = instance;
            m_method   = method;
        }


        /**
         * {@inheritDoc}
         */
        public void onTransition(String           sTransitionName,
                                 S                stateFrom,
                                 S                stateTo,
                                 Event<S>         event,
                                 ExecutionContext context) throws RollbackTransitionException
        {
            try
            {
                m_method.invoke(m_instance, sTransitionName, stateFrom, stateTo, event, context);
            }
            catch (Exception e)
            {
                throw new RollbackTransitionException(stateFrom, stateTo, e);

            }
        }
    }
}
