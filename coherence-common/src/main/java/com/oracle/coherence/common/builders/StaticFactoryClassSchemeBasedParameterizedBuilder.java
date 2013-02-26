/*
 * File: StaticFactoryClassSchemeBasedParameterizedBuilder.java
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

package com.oracle.coherence.common.builders;

import com.oracle.coherence.common.util.ReflectionHelper;
import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.parameters.EmptyParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ParameterProvider;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link StaticFactoryClassSchemeBasedParameterizedBuilder} captures the configuration of a <class-scheme> that
 * uses the <class-factory-name> strategy for instantiating an object.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"serial"})
public class StaticFactoryClassSchemeBasedParameterizedBuilder
    extends AbstractClassLoaderAwareParameterizedBuilder<Object> implements ExternalizableLite,
                                                                            PortableObject
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger =
        Logger.getLogger(StaticFactoryClassSchemeBasedParameterizedBuilder.class.getName());

    /**
     * The name of the class factory.
     */
    private Expression factoryClassName;

    /**
     * The name of the <strong>static</strong> method on the factory that will instantiate the required object.
     */
    private Expression factoryMethodName;

    /**
     * The defined {@link Parameter}s to use when calling the factory method.
     */
    private ParameterProvider parameters;


    /**
     * Standard Constructor.
     */
    public StaticFactoryClassSchemeBasedParameterizedBuilder()
    {
        // we always have a default empty set of parameters
        this.parameters = new EmptyParameterProvider();
    }


    /**
     * Determine the name of the factory class (as an {@link Expression}) to use to instantiate the require object.
     *
     * @return An {@link Expression} representing the factory class name
     */
    public Expression getFactoryClassName()
    {
        return factoryClassName;
    }


    /**
     * Sets the name of the factory class (as an {@link Expression}) to use instantiate the required object.</p>
     *
     * @param factoryClassName An {@link Expression} representing the factory class name
     */
    @Property("class-factory-name")
    @Type(Expression.class)
    @SubType(String.class)
    @Mandatory
    public void setFactoryClassName(Expression factoryClassName)
    {
        this.factoryClassName = factoryClassName;
    }


    /**
     * Determine the name of the method (as an {@link Expression}) to use to instantiate the require object.
     *
     * @return An {@link Expression} representing the method name on the factory class
     */
    public Expression getFactoryMethodName()
    {
        return factoryMethodName;
    }


    /**
     * Sets the name of the method (as an {@link Expression}) to use instantiate the required object.
     *
     * @param factoryMethodName An {@link Expression} representing the method on the factory class
     */
    @Property("method-name")
    @Type(Expression.class)
    @SubType(String.class)
    @Mandatory
    public void setFactoryMethodName(Expression factoryMethodName)
    {
        this.factoryMethodName = factoryMethodName;
    }


    /**
     * Determine the constructor {@link Parameter}s to use when instantiating the class.
     *
     * @return A {@link ParameterProvider} containing the constructor {@link Parameter}s.
     */
    public ParameterProvider getParameters()
    {
        return parameters;
    }


    /**
     * Sets the constructor {@link Parameter}s to use when instantiating the class.
     *
     * @param parameters A {@link ParameterProvider} containing the constructor {@link Parameter}s.
     */
    @Property("init-params")
    @Type(ParameterProvider.class)
    public void setParameters(ParameterProvider parameters)
    {
        this.parameters = parameters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizesClassOf(Class<?>          otherClazz,
                                   ParameterProvider parameterProvider)
    {
        try
        {
            // attempt to factory load the class name, but don't initialize it
            Class<?> clazz = Class.forName(factoryClassName.evaluate(parameterProvider).getString(),
                                           false,
                                           getContextClassLoader());

            // determine if there is a static method on the clazz that has the correct name and number of parameters
            // NOTE: we don't attempt to use the types to find an exact match as the parameters may not be provided)
            String methodName       = factoryMethodName.evaluate(parameterProvider).getString();
            Method compatibleMethod = null;

            for (Method method : clazz.getMethods())
            {
                if (method.getName().equals(methodName) && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() != null && method.getParameterTypes().length == parameters.size()
                    && otherClazz.isAssignableFrom(method.getReturnType()))
                {
                    compatibleMethod = method;
                    break;
                }
            }

            // did we find a compatible method
            return compatibleMethod != null;
        }
        catch (ClassNotFoundException classNotFoundException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           String
                           .format("Class %s not found while attempting to determine type information of a ReflectiveScheme",
                               factoryClassName),
                           classNotFoundException);
            }

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object realize(ParameterProvider parameterProvider)
    {
        try
        {
            // load the factory class using the provided loader
            Class<?> clazz =
                getContextClassLoader().loadClass(factoryClassName.evaluate(parameterProvider).getString());

            // find a static method with the required name and compatible parameter types
            String   methodName       = factoryMethodName.evaluate(parameterProvider).getString();
            Method   compatibleMethod = null;
            Object[] methodParameters = parameters.size() == 0 ? null : new Object[parameters.size()];
            Method[] methods          = clazz.getMethods();

            for (int i = 0; i < methods.length && compatibleMethod == null; i++)
            {
                // only consider static methods with the required number of parameter, the required name and a return type
                if (methods[i].getName().equals(methodName)
                    && methods[i].getParameterTypes().length == parameters.size()
                    && Modifier.isStatic(methods[i].getModifiers()) && methods[i].getReturnType() != null)
                {
                    // determine if each of the method parameter types is compatible with the parameters types
                    Class<?>[] methodParameterTypes = methods[i].getParameterTypes();

                    // assume all of the types are compatible
                    boolean isCompatible = true;

                    try
                    {
                        int j = 0;

                        for (Parameter proposedParameter : parameters)
                        {
                            // determine the proposed value for the parameter
                            Value valProposed = proposedParameter.evaluate(parameterProvider);

                            // determine the required type
                            Class<?> clzParameter = methodParameterTypes[j];

                            if (valProposed.isNull())
                            {
                                // we can't do anything with nulls, so just accept them as compatible parameters
                                methodParameters[j] = null;
                            }
                            else
                            {
                                // determine the proposed value type
                                Class<?> clzProposed = valProposed.getObject().getClass();

                                if (clzParameter.isAssignableFrom(clzProposed)
                                    || (clzParameter.isPrimitive()
                                        && ReflectionHelper.isAssignablePrimitive(clzParameter, clzProposed)))
                                {
                                    // the proposed parameter directly assignable to the method parameter
                                    methodParameters[j] = valProposed.getObject();
                                }

                                else if (valProposed.getObject() instanceof ParameterizedBuilder<?>
                                         && ((ReflectiveBuilder<?>) valProposed.getObject())
                                             .realizesClassOf(clzParameter, parameterProvider))
                                {
                                    // when the proposed parameter is a class scheme we might be able to realize the required type
                                    methodParameters[j] =
                                        ((ParameterizedBuilder<?>) valProposed.getObject()).realize(parameterProvider);
                                }

                                else if (proposedParameter.isStronglyTyped())
                                {
                                    // the proposed parameter declared with a specific type

                                    // load the proposed type
                                    clzProposed = getContextClassLoader().loadClass(proposedParameter.getType());

                                    if (clzParameter.isAssignableFrom(clzProposed)
                                        || (clzParameter.isPrimitive()
                                            && ReflectionHelper.isAssignablePrimitive(clzParameter, clzProposed)))
                                    {
                                        methodParameters[j] = valProposed.getValue(clzProposed);
                                    }
                                    else
                                    {
                                        isCompatible = false;
                                    }
                                }
                                else
                                {
                                    // we've done all of the type checking we can, so now just attempt to coerce the
                                    // proposed parameter into the required type for the method parameter
                                    methodParameters[j] = valProposed.getValue(clzParameter);
                                }
                            }

                            // next parameter
                            j++;
                        }
                    }
                    catch (Exception exception)
                    {
                        // log the exception, but we know that the method is incompatible
                        isCompatible = false;
                    }

                    if (isCompatible)
                    {
                        compatibleMethod = methods[i];
                    }
                }
            }

            // did we find a compatible method?
            if (compatibleMethod == null)
            {
                throw new NoSuchMethodException(String
                    .format("Unable to find a compatible method for %s with the parameters %s", methodName,
                            parameters));
            }
            else
            {
                return compatibleMethod.invoke(clazz, methodParameters);
            }
        }
        catch (Exception exception)
        {
            // TODO: log and re throw
            throw Base.ensureRuntimeException(exception);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.factoryClassName  = (Expression) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
        this.factoryMethodName = (Expression) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
        this.parameters = (ParameterProvider) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, factoryClassName);
        ExternalizableHelper.writeObject(out, factoryMethodName);
        ExternalizableHelper.writeObject(out, parameters);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.factoryClassName  = (Expression) reader.readObject(1);
        this.factoryMethodName = (Expression) reader.readObject(2);
        this.parameters        = (ParameterProvider) reader.readObject(3);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, factoryClassName);
        writer.writeObject(2, factoryMethodName);
        writer.writeObject(2, parameters);
    }
}
