/*
 * File: ClassSchemeBasedParameterizedBuilder.java
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
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link ClassSchemeBasedParameterizedBuilder} captures the configuration of a <class-scheme> that
 * uses the <class-name> strategy for instantiating an object.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings({"serial"})
public class ClassSchemeBasedParameterizedBuilder extends AbstractClassLoaderAwareParameterizedBuilder<Object>
    implements ExternalizableLite,
               PortableObject
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(ClassSchemeBasedParameterizedBuilder.class.getName());

    /**
     * The name of the class to instantiate.
     */
    private Expression className;

    /**
     * The defined {@link Parameter}s to use when constructing the class.
     */
    private ParameterProvider parameters;


    /**
     * Standard Constructor.
     */
    public ClassSchemeBasedParameterizedBuilder()
    {
        // we always have a default empty set of parameters
        this.parameters = new EmptyParameterProvider();
    }


    /**
     * Determine the name of the class (as an {@link Expression}) to instantiate when requested.
     *
     * @return An {@link Expression} representing the class name to instantiate
     */
    public Expression getClassName()
    {
        return className;
    }


    /**
     * Sets the name of the class (as an {@link Expression}) to instantiate when requested.
     *
     * @param className An {@link Expression} representing the class name to instantiate
     */
    @Property("class-name")
    @Type(Expression.class)
    @SubType(String.class)
    @Mandatory
    public void setClassName(Expression className)
    {
        this.className = className;
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
            // attempt to load the class name, but don't initialize it
            Class<?> clazz = Class.forName(className.evaluate(parameterProvider).getString(),
                                           false,
                                           getContextClassLoader());

            // determine if there is a constructor on the clazz that has the correct number of parameters
            // NOTE: we don't attempt to use the types to find an exact match as the parameters may not be provided)
            Constructor<?> compatibleConstructor = null;

            for (Constructor<?> constructor : clazz.getConstructors())
            {
                if (constructor.getParameterTypes().length == getParameters().size())
                {
                    compatibleConstructor = constructor;
                    break;
                }
            }

            // ensure the return type of the compatible constructor is assignable to
            return compatibleConstructor == null ? false : otherClazz.isAssignableFrom(clazz);
        }
        catch (ClassNotFoundException classNotFoundException)
        {
            if (logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING,
                           String
                           .format("Class %s not found while attempting to determine type information of a ReflectiveScheme",
                               className),
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
            // determine the actual class name
            String resolvedClassName = className.evaluate(parameterProvider).getString();

            // load the class using the provided loader
            Class<?> clazz = getContextClassLoader().loadClass(resolvedClassName);

            // find a constructor that is compatible with the parameter types
            Constructor<?>   compatibleConstructor = null;
            Object[]         constructorParameters = parameters.size() == 0 ? null : new Object[parameters.size()];
            Constructor<?>[] constructors          = clazz.getConstructors();

            for (int i = 0; i < constructors.length && compatibleConstructor == null; i++)
            {
                if (constructors[i].getParameterTypes().length == parameters.size())
                {
                    // determine if each of the constructor parameter types is compatible with the parameters types
                    Class<?>[] constructorParameterTypes = constructors[i].getParameterTypes();

                    // assume all of the types are compatible
                    boolean isCompatible = true;

                    try
                    {
                        int j = 0;

                        for (Parameter proposedParameter : parameters)
                        {
                            // determine the value for the parameter
                            Value valProposed = proposedParameter.evaluate(parameterProvider);

                            // determine the required type
                            Class<?> clzParameter = constructorParameterTypes[j];

                            if (valProposed.isNull())
                            {
                                // we can't do anything with nulls, so just accept them as compatible parameters
                                constructorParameters[j] = null;
                            }
                            else
                            {
                                // determine the proposed value type
                                Class<?> clzProposed = valProposed.getObject().getClass();

                                if (clzParameter.isAssignableFrom(clzProposed)
                                    || (clzParameter.isPrimitive()
                                        && ReflectionHelper.isAssignablePrimitive(clzParameter, clzProposed)))
                                {
                                    // the proposed parameter directly assignable to the constructor parameter
                                    constructorParameters[j] = valProposed.getObject();
                                }
                                else if (valProposed.getObject() instanceof ParameterizedBuilder<?>
                                         && ((ReflectiveBuilder<?>) valProposed.getObject())
                                             .realizesClassOf(clzParameter, parameterProvider))
                                {
                                    // when the proposed parameter is a class scheme we might be able to realize the required type
                                    constructorParameters[j] =
                                        ((ParameterizedBuilder<?>) valProposed.getObject()).realize(parameterProvider);
                                }
                                else if (proposedParameter.isStronglyTyped())
                                {
                                    // the proposed parameter was declared with a specific type

                                    // load the proposed type
                                    clzProposed = getContextClassLoader().loadClass(proposedParameter.getType());

                                    if (clzParameter.isAssignableFrom(clzProposed)
                                        || (clzParameter.isPrimitive()
                                            && ReflectionHelper.isAssignablePrimitive(clzParameter, clzProposed)))
                                    {
                                        constructorParameters[j] = valProposed.getValue(clzProposed);
                                    }
                                    else
                                    {
                                        isCompatible = false;
                                    }
                                }
                                else
                                {
                                    // we've done all of the type checking we can, so now just attempt to coerce the
                                    // proposed parameter into the required type for the constructor parameter
                                    constructorParameters[j] = valProposed.getValue(clzParameter);
                                }
                            }

                            // next parameter
                            j++;
                        }
                    }
                    catch (Exception exception)
                    {
                        // log the exception, but we know that the constructor is incompatible
                        isCompatible = false;
                    }

                    if (isCompatible)
                    {
                        compatibleConstructor = constructors[i];
                    }
                }
            }

            // did we find a compatible constructor?
            if (compatibleConstructor == null)
            {
                throw new NoSuchMethodException(String
                    .format("Unable to find a compatible constructor for %s with the parameters %s", resolvedClassName,
                            parameters));
            }
            else
            {
                return compatibleConstructor.newInstance(constructorParameters);
            }
        }
        catch (Exception exception)
        {
            throw Base.ensureRuntimeException(exception);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.className  = (Expression) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
        this.parameters = (ParameterProvider) ExternalizableHelper.readObject(in, this.getClass().getClassLoader());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, className);
        ExternalizableHelper.writeObject(out, parameters);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        this.className  = (Expression) reader.readObject(1);
        this.parameters = (ParameterProvider) reader.readObject(2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, className);
        writer.writeObject(2, parameters);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("ClassSchemeBasedParameterizedBuilder{className=%s, parameters=%s}",
                             className,
                             parameters);
    }
}
